package nl.jolanrensen.permanentproxy

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.jolanrensen.permanentproxy.Constants.currentProxy
import nl.jolanrensen.permanentproxy.Constants.getCurrentIP
import nl.jolanrensen.permanentproxy.Constants.logE
import nl.jolanrensen.permanentproxy.Constants.startProxy
import nl.jolanrensen.permanentproxy.Constants.stopProxy
import nl.jolanrensen.permanentproxy.Constants.toastLong

class MainActivity : WearableActivity() {


    private val PERMISSION = 23

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupStatus(true)

        /*
            Turn on proxy with:
            settings put global http_proxy address:port
            for example 50.116.3.101:3128

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

        scroll_view.requestFocus()
    }

    private fun setupStatus(wait: Boolean = false): Boolean {
        if (wait) {
            GlobalScope.launch {
                delay(1000)
                runOnUiThread {
                    val proxy = currentProxy
                    status.text = proxy ?: getString(R.string.not_enabled)
                }
                getCurrentIP { ip, country ->
                    logE("current IP is $ip, $country")
                    runOnUiThread {
                        ext_ip.text =
                            if (ip == null) getString(R.string.NA) else "$ip, $country"
                    }
                }

            }
            return false
        }
        val proxy = currentProxy
        status.text = proxy ?: getString(R.string.not_enabled)
        getCurrentIP { ip, country ->
            logE("current IP is $ip, $country")
            runOnUiThread {
                ext_ip.text = if (ip == null) getString(R.string.NA) else "$ip, $country"
            }
        }
        return proxy != null
    }

    private fun updateSetProxyIsEnabled() {
        set_proxy.isEnabled = address_input.text.isNotEmpty() && port_input.text.isNotEmpty()
    }

    private fun continueSetup() {
        setAllEnabled(main_menu)
        show_me_how.isVisible = false
        request_permission.isVisible = false

        val enabled: Boolean = setupStatus()

        set_proxy.apply {
            isVisible = true

            setOnClickListener {
                val address = address_input.text.toString()
                val port = port_input.text.toString().toInt()

                logE("setting proxy")
                startProxy(
                    address = address,
                    port = port,
                    updateGooglePay = true
                )
                toastLong(getString(R.string.other_apps))
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
                    onFailure = {
                        runOnUiThread {
                            toastLong(getString(R.string.something_wrong))
                        }
                    }
                )
            }
        }

        proxy_address.isVisible = true
        proxy_port.isVisible = true

        val (currentAddress, currentPort) = currentProxy?.split(":") ?: listOf(null, null)

        address_input.apply {
            isVisible = true
            setText(currentAddress ?: "")
            doOnTextChanged { _, _, _, _ -> updateSetProxyIsEnabled() }
        }

        port_input.apply {
            isVisible = true
            setText(currentPort ?: "")
            doOnTextChanged { _, _, _, _ -> updateSetProxyIsEnabled() }
        }

        updateSetProxyIsEnabled()
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
