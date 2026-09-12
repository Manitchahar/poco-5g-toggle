package dev.manit.networktoggle

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class NetworkTileService : TileService() {
    private val requestSequence = AtomicInteger()
    private val switching = AtomicBoolean(false)

    override fun onStartListening() {
        if (!switching.get()) refresh(toggle = false)
    }

    override fun onClick() {
        if (switching.compareAndSet(false, true)) refresh(toggle = true)
    }

    override fun onDestroy() {
        requestSequence.incrementAndGet()
        switching.set(false)
        super.onDestroy()
    }

    private fun refresh(toggle: Boolean) {
        val sequence = requestSequence.incrementAndGet()
        val callback: (ModeState) -> Unit = { result ->
            mainExecutor.execute {
                if (sequence == requestSequence.get()) {
                    qsTile?.apply {
                        label = "5G"
                        icon = Icon.createWithResource(applicationContext, R.drawable.ic_5g)
                        state = when (result.mode) {
                            NetworkMode.FIVE_G -> Tile.STATE_ACTIVE
                            NetworkMode.FOUR_G -> Tile.STATE_INACTIVE
                            NetworkMode.UNKNOWN -> Tile.STATE_UNAVAILABLE
                        }
                        subtitle = when (result.mode) {
                            NetworkMode.FIVE_G -> "On"
                            NetworkMode.FOUR_G -> "Off"
                            NetworkMode.UNKNOWN -> result.message?.take(40) ?: "Unavailable"
                        }
                        updateTile()
                    }
                    if (toggle) switching.set(false)
                }
                NetworkWidgetProvider.showState(applicationContext, result)
            }
        }
        if (toggle) NetworkOperations.toggle(applicationContext, callback)
        else NetworkOperations.read(applicationContext, callback)
    }

    companion object {
        fun requestRefresh(context: Context) {
            runCatching {
                requestListeningState(context, ComponentName(context, NetworkTileService::class.java))
            }
        }
    }
}
