package dev.manit.networktoggle

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ShizukuCommandClient {
    @Volatile private var service: ICommandService? = null
    @Volatile private var connection: ServiceConnection? = null

    fun execute(context: Context, vararg command: String): CommandResult {
        val response = connect(context.applicationContext).execute(command)
            ?: error("Shizuku command service returned no result")
        check(response.size == 3) { "Shizuku command service returned an invalid result" }
        val exitCode = response[0].toIntOrNull() ?: error("Invalid command exit code")
        return CommandResult(exitCode, response[1], response[2])
    }

    @Synchronized
    private fun connect(context: Context): ICommandService {
        service?.takeIf { it.asBinder().pingBinder() }?.let { return it }

        val connected = CountDownLatch(1)
        val newConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                service = ICommandService.Stub.asInterface(binder)
                connected.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) {
                service = null
            }
        }
        connection = newConnection
        val args = Shizuku.UserServiceArgs(ComponentName(context, CommandUserService::class.java))
            .daemon(false)
            .processNameSuffix("phone_command")
            .tag("phone-command-v1")
            .version(1)
        Shizuku.bindUserService(args, newConnection)
        check(connected.await(10, TimeUnit.SECONDS)) { "Timed out starting the Shizuku command service" }
        return service ?: error("Shizuku command service did not connect")
    }
}
