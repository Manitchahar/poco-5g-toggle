package dev.manit.networktoggle

import android.telephony.TelephonyManager

/** User-visible policy. This never claims to force NR-only. */
enum class NetworkMode(val label: String) {
    FOUR_G("4G (5G disabled)"),
    FIVE_G("5G enabled"),
    UNKNOWN("Unknown")
}

data class ModeState(
    val mode: NetworkMode,
    val mask: Long? = null,
    val backend: String? = null,
    val message: String? = null,
    val target: CurrentDataSubscription.Target? = null
)

object NetworkMasks {
    // Android bitmask uses 1 << (TelephonyManager.NETWORK_TYPE_* - 1).
    val NR: Long = 1L shl (TelephonyManager.NETWORK_TYPE_NR - 1)
    val LTE: Long = 1L shl (TelephonyManager.NETWORK_TYPE_LTE - 1)

    fun mode(mask: Long): NetworkMode =
        if (mask and NR != 0L) NetworkMode.FIVE_G else NetworkMode.FOUR_G

    /** Preserve every carrier/OEM-selected RAT and alter only NR availability. */
    fun with5g(mask: Long, enabled: Boolean): Long =
        if (enabled) mask or NR else mask and NR.inv()

    fun isSafeBase(mask: Long): Boolean = mask > 0 && mask and LTE != 0L
}
