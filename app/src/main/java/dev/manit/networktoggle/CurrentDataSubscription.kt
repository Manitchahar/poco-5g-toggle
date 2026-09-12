package dev.manit.networktoggle

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager

object CurrentDataSubscription {
    data class Target(val subscriptionId: Int, val slotIndex: Int, val label: String)

    /** Resolve on each operation, never from preferences or a cached SIM selection. */
    fun resolve(context: Context): Result<Target> = runCatching {
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            error("Allow Phone permission to detect the mobile-data SIM")
        }
        val manager = context.getSystemService(SubscriptionManager::class.java)
        val active = manager.activeSubscriptionInfoList.orEmpty()
        val subId = DataSimPolicy.resolve(
            SubscriptionManager.getActiveDataSubscriptionId(),
            SubscriptionManager.getDefaultDataSubscriptionId(),
            active.map { it.subscriptionId }.toIntArray()
        )
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) error("No available mobile-data SIM")
        val info = active.singleOrNull { it.subscriptionId == subId }
            ?: error("The mobile-data subscription is not active")
        if (info.simSlotIndex < 0) error("The mobile-data SIM has no usable slot")
        Target(subId, info.simSlotIndex, "SIM ${info.simSlotIndex + 1}: ${info.displayName}")
    }
}
