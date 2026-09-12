package dev.manit.networktoggle

interface NetworkBackend {
    val name: String
    fun isAvailable(): Boolean
    fun readUserAllowedTypes(target: CurrentDataSubscription.Target): Result<Long>
    fun writeUserAllowedTypes(target: CurrentDataSubscription.Target, mask: Long): Result<Unit>
}
