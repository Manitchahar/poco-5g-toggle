package dev.manit.networktoggle

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : Activity() {
    private val requestSequence = AtomicInteger()
    private lateinit var status: TextView
    private lateinit var simContainer: LinearLayout
    private lateinit var toggle: Button

    private val binderListener = Shizuku.OnBinderReceivedListener { runOnUiThread { refresh() } }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { runOnUiThread { refresh() } }
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == SHIZUKU_REQUEST) runOnUiThread {
            Toast.makeText(this, if (result == PackageManager.PERMISSION_GRANTED) "Shizuku granted" else "Shizuku denied", Toast.LENGTH_SHORT).show()
            refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "4G ↔ 5G"
        setContentView(buildUi())
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        requestPhonePermissionIfNeeded()
        refreshSubscriptions()
        refresh()
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        refreshSubscriptions()
        refresh()
    }

    private fun buildUi(): View {
        val pad = (24 * resources.displayMetrics.density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(TextView(context).apply {
                text = getString(R.string.screen_title)
                textSize = 26f
            })
            addView(TextView(context).apply {
                text = getString(R.string.screen_description)
                textSize = 14f
                setPadding(0, 8, 0, 20)
            })
            simContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            addView(simContainer)
            status = TextView(context).apply {
                textSize = 18f
                setPadding(0, 20, 0, 12)
            }
            addView(status)
            toggle = Button(context).apply {
                text = getString(R.string.toggle_button)
                setOnClickListener { performToggle() }
            }
            addView(toggle)
            addView(Button(context).apply {
                text = getString(R.string.grant_shizuku)
                setOnClickListener { requestShizuku() }
            })
            addView(Button(context).apply {
                text = getString(R.string.open_network_settings)
                setOnClickListener { startActivity(NetworkModeController(context).settingsIntent()) }
            })
            addView(TextView(context).apply {
                text = getString(R.string.background_note)
                textSize = 12f
                setPadding(0, 20, 0, 0)
            })
        }
    }

    private fun requestPhonePermissionIfNeeded() {
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), PHONE_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PHONE_REQUEST) {
            refreshSubscriptions()
            refresh()
        }
    }

    private fun refreshSubscriptions() {
        simContainer.removeAllViews()
        val target = CurrentDataSubscription.resolve(this)
        simContainer.addView(TextView(this).apply {
            val detail = target.fold(
                { getString(R.string.target_mobile_data, it.label) },
                { it.message ?: getString(R.string.unavailable) }
            )
            text = getString(R.string.automatic_target, detail)
            textSize = 16f
        })
    }

    private fun requestShizuku() {
        when {
            !Shizuku.pingBinder() -> Toast.makeText(this, "Start Shizuku first", Toast.LENGTH_LONG).show()
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED ->
                Toast.makeText(this, "Shizuku already granted", Toast.LENGTH_SHORT).show()
            Shizuku.shouldShowRequestPermissionRationale() ->
                Toast.makeText(this, "Allow this app in Shizuku Manager", Toast.LENGTH_LONG).show()
            else -> Shizuku.requestPermission(SHIZUKU_REQUEST)
        }
    }

    private fun refresh() {
        val sequence = requestSequence.incrementAndGet()
        status.text = getString(R.string.checking)
        toggle.isEnabled = false
        NetworkOperations.read(this) { state ->
            runOnUiThread update@{
                if (isDestroyed || sequence != requestSequence.get()) return@update
                refreshSubscriptions()
                status.text = format(state)
                toggle.isEnabled = state.mask != null
                NetworkWidgetProvider.showState(this, state)
            }
        }
    }

    private fun performToggle() {
        val sequence = requestSequence.incrementAndGet()
        toggle.isEnabled = false
        status.text = getString(R.string.switching)
        NetworkOperations.toggle(this) { state ->
            runOnUiThread update@{
                if (isDestroyed || sequence != requestSequence.get()) return@update
                refreshSubscriptions()
                status.text = format(state)
                toggle.isEnabled = state.mask != null
                NetworkWidgetProvider.showState(this, state)
                NetworkTileService.requestRefresh(this)
            }
        }
    }

    private fun format(state: ModeState): String = buildString {
        append(state.mode.label)
        state.target?.let { append("\n${it.label}, subscription ${it.subscriptionId}") }
        state.mask?.let { append(" · mask $it") }
        state.backend?.let { append("\nvia $it") }
        state.message?.let { append("\n$it") }
    }

    companion object {
        private const val SHIZUKU_REQUEST = 42
        private const val PHONE_REQUEST = 43
    }
}
