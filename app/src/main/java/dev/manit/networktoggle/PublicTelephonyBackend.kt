package dev.manit.networktoggle

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.TelephonyManager

/** Works for carrier-privileged or system installs. A normal sideloaded APK is denied. */
class PublicTelephonyBackend(private val context: Context) : NetworkBackend {
    override val name = "Telephony API"

    // Try it: carrier-privileged apps may call this without holding the signature permission.
    override fun isAvailable(): Boolean = true

    private fun manager(subId: Int): TelephonyManager =
        context.getSystemService(TelephonyManager::class.java).createForSubscriptionId(subId)

    @SuppressLint("MissingPermission") // Carrier privilege is checked by TelephonyManager at runtime.
    override fun readUserAllowedTypes(target: CurrentDataSubscription.Target): Result<Long> = runCatching {
        manager(target.subscriptionId).getAllowedNetworkTypesForReason(
            TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER
        )
    }

    @SuppressLint("MissingPermission") // Carrier privilege is checked by TelephonyManager at runtime.
    override fun writeUserAllowedTypes(target: CurrentDataSubscription.Target, mask: Long): Result<Unit> = runCatching {
        manager(target.subscriptionId).setAllowedNetworkTypesForReason(
            TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER,
            mask
        )
    }
}
