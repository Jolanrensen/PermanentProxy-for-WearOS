package nl.jolanrensen.permanentproxy

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
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
import kotlin.concurrent.thread
import android.content.pm.PackageManager
import nl.jolanrensen.permanentproxy.Constants.getCurrentProxy
import nl.jolanrensen.permanentproxy.Constants.toastLong

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

        // first check if app has permission to write Secure Settings, else ask to turn on adb over bluetooth
        if (checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS")
            != PackageManager.PERMISSION_GRANTED) {
            thread(start = true) {
                val logs = arrayListOf<String>()
                try {
                    SendSingleCommand(
                        logs = logs,
                        context = this,
                        ip = "localhost",
                        port = 7272,
                        command = "pm grant \\\nnl.jolanrensen.permanentproxy \\\nandroid.permission.WRITE_SECURE_SETTINGS",
                        timeout = 4000,
                        ctrlC = false
                    ) {
                        logD(it.toString())
                        runOnUiThread {
                            if (checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS")
                                == PackageManager.PERMISSION_GRANTED) {
                                continueSetup()
                            } else {
                                toastLong(getString(R.string.something_wrong))
                            }
                        }
                    }
                } catch (e: Exception) {
                    logE("$logs", e)
                    runOnUiThread {
                        runOnUiThread {
                            status.text = getString(R.string.adb_enabled)
                            show_me_how.isVisible = true
                        }
                    }
                }
            }
        } else {
            continueSetup()
        }


        show_me_how.setOnClickListener {
            startActivity(
                Intent(this, EnableADBBluetoothActivity::class.java)
            )
        }
    }

    private fun setupStatus(): Boolean {
        val proxy = getCurrentProxy()
        status.text = proxy ?: getString(R.string.not_enabled)
        return proxy != null
    }

    private fun continueSetup() {
        setAllEnabled(main_menu)
        show_me_how.isVisible = false

        val enabled = setupStatus()

        proxy_switch.apply {
            isChecked = enabled
            isEnabled = p!!.getString("address", "")!! != ""
                && p!!.getInt("port", -1) != -1

            setOnClickListener {
                if (!isChecked) {
                    logE("turning off proxy")
                    stopProxy()
                } else {
                    logE("turning on proxy")
                    startProxy(
                        address = p!!.getString("address", "")!!,
                        port = p!!.getInt("port", -1),
                        updateGooglePay = true
                    )
                }
                setupStatus()
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
            if (proxy_switch.isChecked) {
                stopProxy()
                setupStatus()
            }
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
            if (proxy_switch.isChecked) {
                stopProxy()
                setupStatus()
            }
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
