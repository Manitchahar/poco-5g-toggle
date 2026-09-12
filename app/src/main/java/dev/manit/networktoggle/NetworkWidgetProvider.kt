package dev.manit.networktoggle

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class NetworkWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE -> runAsync(context, toggle = true)
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> runAsync(context, toggle = false)
            else -> super.onReceive(context, intent)
        }
    }

    private fun runAsync(context: Context, toggle: Boolean) {
        val pendingResult = goAsync()
        val app = context.applicationContext
        showLoading(app, if (toggle) "Switching..." else "Checking...")
        val callback: (ModeState) -> Unit = { state ->
            try {
                showState(app, state)
                NetworkTileService.requestRefresh(app)
            } finally {
                pendingResult.finish()
            }
        }
        if (toggle) NetworkOperations.toggle(app, callback) else NetworkOperations.read(app, callback)
    }

    companion object {
        private const val ACTION_TOGGLE = "dev.manit.networktoggle.TOGGLE"

        fun refreshAll(context: Context) {
            val app = context.applicationContext
            showLoading(app, "Checking...")
            NetworkOperations.read(app) { showState(app, it) }
        }

        fun showState(context: Context, state: ModeState) {
            updateAll(context) { views ->
                when (state.mode) {
                    NetworkMode.FOUR_G -> {
                        views.setTextViewText(R.id.widget_mode, "4G")
                        views.setTextViewText(R.id.widget_detail, "Tap to enable 5G")
                    }
                    NetworkMode.FIVE_G -> {
                        views.setTextViewText(R.id.widget_mode, "5G")
                        views.setTextViewText(R.id.widget_detail, "Tap to disable 5G")
                    }
                    NetworkMode.UNKNOWN -> {
                        val setup = state.message?.contains("Shizuku", ignoreCase = true) == true
                        views.setTextViewText(R.id.widget_mode, if (setup) "Setup" else "Error")
                        views.setTextViewText(R.id.widget_detail, state.message?.take(55) ?: "Open app")
                    }
                }
            }
        }

        private fun showLoading(context: Context, text: String) = updateAll(context) { views ->
            views.setTextViewText(R.id.widget_mode, "...")
            views.setTextViewText(R.id.widget_detail, text)
        }

        private fun updateAll(context: Context, change: (RemoteViews) -> Unit) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, NetworkWidgetProvider::class.java))
            for (id in ids) {
                val views = RemoteViews(app.packageName, R.layout.network_widget)
                val intent = Intent(app, NetworkWidgetProvider::class.java).setAction(ACTION_TOGGLE)
                val pending = PendingIntent.getBroadcast(
                    app, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pending)
                change(views)
                manager.updateAppWidget(id, views)
            }
        }
    }
}
