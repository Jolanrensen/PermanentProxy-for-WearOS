package nl.jolanrensen.permanentproxy

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_main.*
import nl.jolanrensen.permanentproxy.Constants.getTurnOnProxyCommand
import nl.jolanrensen.permanentproxy.Constants.logD
import nl.jolanrensen.permanentproxy.Constants.logE
import nl.jolanrensen.permanentproxy.Constants.startProxy
import nl.jolanrensen.permanentproxy.Constants.stopProxy
import nl.jolanrensen.permanentproxy.Constants.toast
import nl.jolanrensen.permanentproxy.Constants.toastLong
import kotlin.concurrent.thread

class MainActivity : WearableActivity() {

    var p: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        p = getSharedPreferences(packageName, Context.MODE_PRIVATE)

        /*
            Turn on proxy with:
            settings put global http_proxy address:port
            for example 5.9.73.93:8080

            Turn off proxy with:
            adb shell settings delete global http_proxy
            adb shell settings delete global global_http_proxy_host
            adb shell settings delete global global_http_proxy_port

            adb over wifi port: 5555
            adb over bluetooth port: 7272

         */

        // Enables Always-on
        setAmbientEnabled()

        setupStatus(continueSetup = true)
    }

    private fun setupStatus(continueSetup: Boolean) {
        thread(start = true) {
            val logs = arrayListOf<String>()

            try {
                SendSingleCommand(
                    logs = logs,
                    context = this,
                    ip = "localhost",
                    port = 7272,
                    command = "settings get global http_proxy",
                    timeout = 500,
                    ctrlC = false
                ) {
                    logD(it.toString())
                    runOnUiThread {
                        setAllEnabled(main_menu)
                        val proxy = it!![2].let { if (it == "null") null else it }
                        status.text = proxy ?: getString(R.string.not_enabled)
                        if (continueSetup) continueSetup(enabled = proxy != null)
                    }
                }
            } catch (e: Exception) {
                logE("$logs", e)
                runOnUiThread {
                    runOnUiThread {
                        toastLong(getString(R.string.adb_enabled))
                    }
                }
            }
        }
    }

    private fun continueSetup(enabled: Boolean) {
        proxy_switch.apply {
            isChecked = enabled
            isEnabled = p!!.getString("address", "")!! != ""
                && p!!.getInt("port", -1) != -1

            setOnClickListener {
                status.text = getString(R.string.loading)
                if (!isChecked) {
                    logE("turning off proxy")
                    stopProxy(
                        onSuccess = {
                            runOnUiThread {
                                isChecked = false
                                setupStatus(continueSetup = false)
                            }
                        },
                        onFailure = { _, _ ->
                            runOnUiThread {
                                isChecked = true
                                toast(getString(R.string.something_wrong))
                                setupStatus(continueSetup = false)
                            }
                        }
                    )
                } else {
                    logE("turning on proxy")
                    startProxy(
                        command = getTurnOnProxyCommand(
                            p!!.getString("address", "")!!,
                            p!!.getInt("port", -1)
                        ),
                        onSuccess = {
                            runOnUiThread {
                                isChecked = true
                                setupStatus(continueSetup = false)
                            }
                        },
                        onFailure = { _, _ ->
                            runOnUiThread {
                                isChecked = false
                                setupStatus(continueSetup = false)
                                toast(getString(R.string.something_wrong))
                            }
                        }
                    )
                }
            }
        }

        on_boot_switch.apply {
            isChecked = p!!.getBoolean("onBoot", false)
            isEnabled = p!!.getString("address", "")!! != ""
                && p!!.getInt("port", -1) != -1
            setOnCheckedChangeListener { _, isChecked ->
                p!!.edit {
                    putBoolean("onBoot", isChecked)
                }
            }
        }

        set_proxy_address.setOnClickListener {
            if (proxy_switch.isChecked) stopProxy(
                onSuccess = {
                    runOnUiThread {
                        proxy_switch.isChecked = false
                        setupStatus(continueSetup = false)
                    }
                },
                onFailure = { _, _ ->
                    runOnUiThread {
                        toast(getString(R.string.something_wrong))
                        setupStatus(continueSetup = false)
                    }
                }
            )
            text_input.isVisible = true
            text_input.setText(p!!.getString("address", ""))
            showSoftKeyboard(text_input)
            text_input.setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    IME_ACTION_SEARCH -> {
                        p!!.edit(commit = true) {
                            putString("address", text_input.text.toString())
                        }
                        logE("address updated to ${text_input.text.toString()}")
                        text_input.isVisible = false

                        on_boot_switch.isEnabled = p!!.getString("address", "")!! != ""
                            && p!!.getInt("port", -1) != -1
                        proxy_switch.isEnabled = p!!.getString("address", "")!! != ""
                            && p!!.getInt("port", -1) != -1

                        true
                    }
                    IME_ACTION_DONE -> {
                        text_input.isVisible = false
                        true
                    }
                    else -> false
                }
            }
        }

        set_proxy_port.setOnClickListener {
            if (proxy_switch.isChecked) stopProxy(
                onSuccess = {
                    runOnUiThread {
                        proxy_switch.isChecked = false
                        setupStatus(continueSetup = false)
                    }
                },
                onFailure = { _, _ ->
                    runOnUiThread {
                        toast(getString(R.string.something_wrong))
                        setupStatus(continueSetup = false)
                    }
                }
            )
            port_input.isVisible = true
            port_input.setText(p!!.getInt("port", -1).let { if (it == -1) "" else it.toString() })
            showSoftKeyboard(port_input)
            port_input.setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    IME_ACTION_SEARCH -> {
                        p!!.edit(commit = true) {
                            putInt("port", port_input.text.toString().toInt())
                        }
                        logE("port updated to ${port_input.text.toString().toInt()}")
                        port_input.isVisible = false

                        on_boot_switch.isEnabled = p!!.getString("address", "")!! != ""
                            && p!!.getInt("port", -1) != -1
                        proxy_switch.isEnabled = p!!.getString("address", "")!! != ""
                            && p!!.getInt("port", -1) != -1

                        true
                    }
                    IME_ACTION_DONE -> {
                        port_input.isVisible = false
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun showSoftKeyboard(view: View) {
        if (view.requestFocus()) {
            getSystemService<InputMethodManager>()?.apply {
                showSoftInput(view, SHOW_IMPLICIT)
                toggleSoftInput(0, 0)
            }
        }
    }

    private fun setAllEnabled(parent: ViewGroup, vararg except: Int) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child.id in except) continue
            child.isEnabled = true

            if (child is ViewGroup) setAllEnabled(child, *except)
        }
    }
}
