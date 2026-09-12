package dev.manit.networktoggle

import android.content.Context
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Non-daemon Shizuku user service. It accepts only the two verified phone commands. */
class CommandUserService() : ICommandService.Stub() {
    @Suppress("UNUSED_PARAMETER")
    constructor(context: Context) : this()

    override fun execute(command: Array<out String>?): Array<String> {
        val args = command?.toList() ?: error("Missing command")
        validate(args)
        val process = ProcessBuilder(args).start()
        val readers = Executors.newFixedThreadPool(2)
        val stdout = readers.submit<String> { process.inputStream.bufferedReader().use { it.readText() } }
        val stderr = readers.submit<String> { process.errorStream.bufferedReader().use { it.readText() } }
        try {
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                error("Phone command timed out")
            }
            return arrayOf(
                process.exitValue().toString(),
                stdout.get(1, TimeUnit.SECONDS),
                stderr.get(1, TimeUnit.SECONDS)
            )
        } finally {
            readers.shutdownNow()
        }
    }

    private fun validate(command: List<String>) {
        check(command.size == 5 || command.size == 6) { "Invalid command length" }
        check(command[0] == "/system/bin/cmd" && command[1] == "phone") { "Command is not allowed" }
        check(command[2] == "get-allowed-network-types-for-users" ||
            command[2] == "set-allowed-network-types-for-users") { "Phone command is not allowed" }
        check(command[3] == "-s" && command[4].matches(Regex("\\d+"))) { "Invalid slot" }
        if (command[2].startsWith("get-")) check(command.size == 5) { "Unexpected read arguments" }
        if (command[2].startsWith("set-")) {
            check(command.size == 6 && command[5].matches(Regex("[01]+"))) { "Invalid binary mask" }
        }
    }
}
