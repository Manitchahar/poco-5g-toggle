#!/usr/bin/env bash
set -euo pipefail

action="${1:-probe}"
nr=$((1 << 19))
lte=$((1 << 12))

if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  adb_cmd=(adb -s "$ANDROID_SERIAL")
else
  mapfile -t devices < <(adb devices | awk 'NR>1 && $2=="device" && $1 !~ /^emulator-/ {print $1}')
  [[ ${#devices[@]} -eq 1 ]] || { echo "Expected one physical ADB device; set ANDROID_SERIAL if needed" >&2; exit 2; }
  adb_cmd=(adb -s "${devices[0]}")
fi

dump="$("${adb_cmd[@]}" shell dumpsys isub)"
sub_id="$(sed -nE 's/^activeDataSubId=([0-9]+)[[:space:]]*$/\1/p' <<<"$dump" | head -n1)"
[[ "$sub_id" =~ ^[0-9]+$ ]] || { echo "Could not resolve active data subscription" >&2; exit 3; }
slot="$(sed -nE "s/^[[:space:]]*Logical SIM slot ([0-9]+): subId=$sub_id[[:space:]]*$/\1/p" <<<"$dump" | head -n1)"
[[ "$slot" =~ ^[0-9]+$ ]] || { echo "Could not map subscription $sub_id to a slot" >&2; exit 3; }

read_types() {
  "${adb_cmd[@]}" shell cmd phone get-allowed-network-types-for-users -s "$slot" | tr -d '\r\n'
}

types_to_mask() {
  awk -F'|' '
    BEGIN { b["GPRS"]=0;b["EDGE"]=1;b["UMTS"]=2;b["CDMA"]=3;b["EVDO_0"]=4;b["EVDO_A"]=5;
      b["1xRTT"]=6;b["HSDPA"]=7;b["HSUPA"]=8;b["HSPA"]=9;b["IDEN"]=10;b["EVDO_B"]=11;
      b["LTE"]=12;b["EHRPD"]=13;b["HSPA+"]=14;b["GSM"]=15;b["TD_SCDMA"]=16;
      b["IWLAN"]=17;b["LTE_CA"]=18;b["NR"]=19 }
    NF < 1 { exit 2 }
    { for(i=1;i<=NF;i++){ if(!($i in b) || seen[$i]++) exit 2; m += 2 ^ b[$i] } }
    END { if(m<=0 || int(m/4096)%2==0) exit 2; printf "%.0f",m }
  '
}

to_binary() {
  local n="$1" out=""
  while (( n > 0 )); do out="$((n & 1))$out"; n=$((n >> 1)); done
  [[ -n "$out" ]] || return 1
  printf '%s' "$out"
}

set_mask() {
  local result
  result="$("${adb_cmd[@]}" shell cmd phone set-allowed-network-types-for-users -s "$slot" "$(to_binary "$1")" | tr -d '\r\n')"
  [[ "$result" == "set-allowed-network-types-for-users completed" ]] || { echo "Set was not confirmed: $result" >&2; return 1; }
}

case "$action" in
  probe)
    "${adb_cmd[@]}" shell getprop ro.product.model
    "${adb_cmd[@]}" shell getprop ro.build.version.release
    "${adb_cmd[@]}" shell getprop ro.build.version.sdk
    "${adb_cmd[@]}" shell getprop ro.build.version.incremental
    echo "Active data: subId=$sub_id slot=$slot"
    "${adb_cmd[@]}" shell cmd phone help
    ;;
  read)
    types="$(read_types)"; mask="$(types_to_mask <<<"$types")"
    echo "Active data: subId=$sub_id slot=$slot"
    echo "Mask: $mask"
    echo "Types: $types"
    ;;
  proof)
    original_types="$(read_types)"; original="$(types_to_mask <<<"$original_types")"
    (( (original & nr) != 0 )) || { echo "Enable 5G in Settings before this two-way proof" >&2; exit 4; }
    four_g=$((original & ~nr))
    restore() { set_mask "$original"; restored_types="$(read_types)"; restored="$(types_to_mask <<<"$restored_types")"; [[ "$restored" -eq "$original" ]]; echo "5G/restore read-back: $restored $restored_types"; }
    trap restore EXIT
    set_mask "$four_g"
    sleep 0.35
    after_types="$(read_types)"; after="$(types_to_mask <<<"$after_types")"
    [[ "$after" -eq "$four_g" ]] || { echo "4G verification failed" >&2; exit 5; }
    echo "Original: $original $original_types"
    echo "4G read-back: $after $after_types"
    ;;
  *) echo "Usage: $0 probe|read|proof" >&2; exit 2 ;;
esac
