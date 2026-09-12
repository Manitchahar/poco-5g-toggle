package dev.manit.networktoggle

import android.content.Context
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** One process-wide queue prevents the activity, widget, and tile from racing each other. */
object NetworkOperations {
    private val worker = Executors.newSingleThreadExecutor()
    private val switching = AtomicBoolean(false)

    fun read(context: Context, done: (ModeState) -> Unit) {
        val app = context.applicationContext
        worker.execute { done(NetworkModeController(app).read()) }
    }

    fun toggle(context: Context, done: (ModeState) -> Unit) {
        if (!switching.compareAndSet(false, true)) {
            done(ModeState(NetworkMode.UNKNOWN, message = "A switch is already in progress"))
            return
        }
        val app = context.applicationContext
        worker.execute {
            try {
                done(NetworkModeController(app).toggle())
            } finally {
                switching.set(false)
            }
        }
    }
}
