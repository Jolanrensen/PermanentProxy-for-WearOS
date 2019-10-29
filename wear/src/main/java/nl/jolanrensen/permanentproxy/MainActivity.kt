package nl.jolanrensen.permanentproxy

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.jolanrensen.permanentproxy.Constants.currentProxy
import nl.jolanrensen.permanentproxy.Constants.getCurrentIP
import nl.jolanrensen.permanentproxy.Constants.logD
import nl.jolanrensen.permanentproxy.Constants.logE
import nl.jolanrensen.permanentproxy.Constants.startProxy
import nl.jolanrensen.permanentproxy.Constants.stopProxy
import nl.jolanrensen.permanentproxy.Constants.toastLong

class MainActivity : WearableActivity() {

    private var p: SharedPreferences? = null

    val PERMISSION = 23

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        p = getSharedPreferences(packageName, Context.MODE_PRIVATE)


        getCurrentIP {
            logE("current IP is $it")
            runOnUiThread {
                ext_ip.text = it ?: getString(R.string.NA)
            }
        }

        /*
            Turn on proxy with:
            settings put global http_proxy address:port
            for example 50.116.3.101:3128

            Turn wifi off!
            Somehow a proxy survives a reboot lol


            Turn off proxy with:
            adb shell settings delete global http_proxy
            adb shell settings delete global global_http_proxy_host
            adb shell settings delete global global_http_proxy_port

            settings delete global http_proxy; settings delete global global_http_proxy_host; settings delete global global_http_proxy_port; settings delete global global_http_proxy_exclusion_list; settings delete global global_proxy_pac_url

            adb over wifi port: 5555
            adb over bluetooth port: 7272

         */

        // Enables Always-on
        setAmbientEnabled()

        // first check if app has permission to write Secure Settings, else ask to turn on adb over bluetooth
        if (checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS")
            == PackageManager.PERMISSION_GRANTED
        ) {
            continueSetup()
        }

        show_me_how.setOnClickListener {
            startActivity(
                Intent(this, EnableADBBluetoothActivity::class.java)
            )
        }

        request_permission.setOnClickListener {
            startActivityForResult(
                Intent(this, RequestPermissionActivity::class.java),
                PERMISSION
            )
            onPause()
        }

        donate.setOnClickListener {
            startActivity(
                Intent(this, DonateActivity::class.java)
            )
        }

        old_watch.setOnClickListener {
            startActivity(
                Intent(this, OldWatchActivity::class.java)
            )
        }
    }

    private fun setupStatus(wait: Boolean = false): Boolean {
        if (wait) {
            GlobalScope.launch {
                delay(200)
                runOnUiThread {
                    val proxy = currentProxy
                    status.text = proxy ?: getString(R.string.not_enabled)
                }
                getCurrentIP {
                    logE("current IP is $it")
                    runOnUiThread {
                        ext_ip.text = it ?: getString(R.string.NA)
                    }
                }
            }
            return false
        }
        val proxy = currentProxy
        status.text = proxy ?: getString(R.string.not_enabled)
        getCurrentIP {
            logE("current IP is $it")
            runOnUiThread {
                ext_ip.text = it ?: getString(R.string.NA)
            }
        }
        return proxy != null
    }

    private fun continueSetup() {
        setAllEnabled(main_menu)
        show_me_how.isVisible = false
        request_permission.isVisible = false

        val enabled = setupStatus()

        enable_proxy.apply {
            isVisible = !enabled
            isEnabled = p!!.getString("address", "")!! != ""
                && p!!.getInt("port", -1) != -1

            setOnClickListener {
                logE("turning on proxy")
                startProxy(
                    address = p!!.getString("address", "")!!,
                    port = p!!.getInt("port", -1),
                    updateGooglePay = true
                )
                toastLong(getString(R.string.other_apps))
                isVisible = false
                disable_proxy.isVisible = true
                setupStatus(wait = true)
            }
        }

        disable_proxy.apply {
            isVisible = enabled
            setOnClickListener {
                logE("turning off proxy")
                toastLong(getString(R.string.turning_off))
                stopProxy(
                    p = p!!,
                    onFailure = {
                        runOnUiThread {
                            toastLong(getString(R.string.something_wrong))
                        }
                    }
                )
            }
        }

        set_proxy_address.setOnClickListener {
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

                        enable_proxy.isEnabled = p!!.getString("address", "")!! != ""
                            && p!!.getInt("port", -1) != -1

                        // update proxy if already running
                        if (currentProxy != null) {
                            startProxy(
                                p = p!!,
                                address = text_input.text.toString(),
                                port = p!!.getInt("port", -1),
                                updateGooglePay = true
                            )
                            toastLong(getString(R.string.other_apps))
                            setupStatus(wait = true)
                        }

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
            port_input.isVisible = true
            port_input.setText(p!!.getInt("port", -1).let { if (it == -1) "" else it.toString() })
            showSoftKeyboard(port_input)
            port_input.setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    IME_ACTION_SEARCH -> {
                        try {
                            p!!.edit(commit = true) {
                                putInt("port", port_input.text.toString().toInt())
                            }

                            logE("port updated to ${port_input.text.toString().toInt()}")
                            port_input.isVisible = false

                            enable_proxy.isEnabled = p!!.getString("address", "")!! != ""
                                && p!!.getInt("port", -1) != -1

                            // update proxy if already running
                            if (currentProxy != null) {
                                startProxy(
                                    p = p!!,
                                    address = p!!.getString("address", "")!!,
                                    port = port_input.text.toString().toInt(),
                                    updateGooglePay = true
                                )
                                setupStatus(wait = true)
                            }
                        } catch (e: Exception) {
                            logE("", e)
                            toastLong(getString(R.string.valid_port))
                        }

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

    override fun onPause() {
        super.onPause()
        logD("onPause")
    }

    override fun onResume() {
        super.onResume()
        logD("onResume")
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        onResume()
        when {
            requestCode == PERMISSION && resultCode == RESULT_OK -> {
                continueSetup()
            }
        }
    }
}
