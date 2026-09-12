#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
test_dir="$(mktemp -d)"
java -m jdk.compiler/com.sun.tools.javac.Main -d "$test_dir" \
  "$project_dir/app/src/main/java/dev/manit/networktoggle/DataSimPolicy.java" \
  "$project_dir/tools/DataSimPolicyTest.java"
java -cp "$test_dir" DataSimPolicyTest
