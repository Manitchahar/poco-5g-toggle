@file:Suppress("DEPRECATION")

package dev.manit.networktoggle

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/** Executes the typed phone shell command as Android's shell UID through Shizuku. */
class ShizukuBackend(private val context: Context) : NetworkBackend {
    override val name = "Shizuku shell"

    override fun isAvailable(): Boolean = runCatching {
        Shizuku.pingBinder() &&
            Shizuku.getVersion() >= 11 &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    override fun readUserAllowedTypes(target: CurrentDataSubscription.Target): Result<Long> = runCatching {
        val result = execute(
            "/system/bin/cmd", "phone", "get-allowed-network-types-for-users",
            "-s", target.slotIndex.toString()
        )
        check(result.exitCode == 0 && result.stderr.isBlank()) { result.describe() }
        AllowedTypesCodec.parse(result.stdout)
            ?: error("Unexpected allowed-types output: ${result.stdout.trim().take(160)}")
    }

    override fun writeUserAllowedTypes(
        target: CurrentDataSubscription.Target,
        mask: Long
    ): Result<Unit> = runCatching {
        val binary = AllowedTypesCodec.toBinary(mask)
            ?: error("Refusing an empty or invalid network policy")
        val result = execute(
            "/system/bin/cmd", "phone", "set-allowed-network-types-for-users",
            "-s", target.slotIndex.toString(), binary
        )
        check(result.exitCode == 0 && result.stderr.isBlank()) { result.describe() }
        check(result.stdout.trim() == "set-allowed-network-types-for-users completed") {
            "Phone service did not confirm the change: ${result.stdout.trim().take(160)}"
        }
    }

    private fun execute(vararg command: String): CommandResult {
        check(isAvailable()) { "Shizuku is stopped, too old, or permission is not granted" }
        return ShizukuCommandClient.execute(context, *command)
    }
}

data class CommandResult(val exitCode: Int, val stdout: String, val stderr: String) {
    fun describe(): String {
        val detail = stderr.ifBlank { stdout }.trim().take(200)
        return "Phone command failed, exit=$exitCode${if (detail.isBlank()) "" else ": $detail"}"
    }
}

/** Codec for the exact AOSP/HyperOS shell format observed on the target phone. */
object AllowedTypesCodec {
    private val bits = linkedMapOf(
        "GPRS" to 0, "EDGE" to 1, "UMTS" to 2, "CDMA" to 3,
        "EVDO_0" to 4, "EVDO_A" to 5, "1xRTT" to 6, "HSDPA" to 7,
        "HSUPA" to 8, "HSPA" to 9, "IDEN" to 10, "EVDO_B" to 11,
        "LTE" to 12, "EHRPD" to 13, "HSPA+" to 14, "GSM" to 15,
        "TD_SCDMA" to 16, "IWLAN" to 17, "LTE_CA" to 18, "NR" to 19
    )
    private val output = Regex("[A-Za-z0-9_+]+(?:\\|[A-Za-z0-9_+]+)*")

    fun parse(raw: String): Long? {
        val lines = raw.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        if (lines.size != 1 || !output.matches(lines.single())) return null
        val names = lines.single().split('|')
        if (names.distinct().size != names.size) return null
        var mask = 0L
        for (name in names) {
            val bit = bits[name] ?: return null
            mask = mask or (1L shl bit)
        }
        return mask.takeIf { it > 0L }
    }

    fun toBinary(mask: Long): String? = mask.takeIf { it > 0L }?.let(java.lang.Long::toBinaryString)
}
