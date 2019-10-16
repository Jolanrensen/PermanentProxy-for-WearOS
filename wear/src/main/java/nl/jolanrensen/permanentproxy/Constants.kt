package nl.jolanrensen.permanentproxy

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread

object Constants {
    const val LOGTAG = "PermanentProxy"
    const val PORT = 7272 // 5555

    fun logD(message: String) = Log.d(LOGTAG, message)
    fun logI(message: String) = Log.i(LOGTAG, message)
    fun logE(message: String, e: java.lang.Exception? = null) = Log.e(LOGTAG, message, e)

    fun Context.toast(message: String, length: Int = Toast.LENGTH_SHORT) =
        Toast.makeText(this, message, length).show()

    fun Context.toastLong(message: String) = toast(message, Toast.LENGTH_LONG)

    fun getTurnOnProxyCommand(address: String, port: Int) =
        "settings put global http_proxy $address:$port\n"

    val turnOffProxyCommand =
        "settings delete global http_proxy; \\\nsettings delete global global_http_proxy_host; \\\nsettings delete global global_http_proxy_port; \\\nsettings delete global global_http_proxy_exclusion_list; \\\nsettings delete global global_proxy_pac_url"

    fun Context.startProxy(
        p: SharedPreferences? = null,
        address: String,
        port: Int,
        updateGooglePay: Boolean = true
    ) {
        p?.edit(commit = true) {
            putBoolean("onBoot", true)
        }
        Settings.Global.putString(contentResolver, Settings.Global.HTTP_PROXY, "$address:$port")
        if (updateGooglePay) sendBroadcast(
            Intent("android.server.checkin.CHECKIN")
        )
    }

    fun Context.stopProxy(
        p: SharedPreferences,
        onFailure: (() -> Unit)? = null
    ) {
        p.edit(commit = true) {
            putBoolean("onBoot", false)
        }
        thread(start = true) {
            val logs = arrayListOf<String>()
            try {
                SendSingleCommand(
                    logs = logs,
                    context = this,
                    ip = "localhost",
                    port = PORT,
                    command = turnOffProxyCommand,
                    timeout = 500,
                    ctrlC = false
                ) {
                    logD(it.toString())

                    SendSingleCommand(
                        logs = logs,
                        context = this,
                        ip = "localhost",
                        port = PORT,
                        command = "reboot",
                        timeout = 500,
                        ctrlC = false
                    ) {
                        logD(it.toString())

                    }
                }
            } catch (e: Exception) {
                logE("$logs", e)
                onFailure?.invoke()
            }

        }
    }

    val Context.currentProxy
        get() = try {
            (Settings.Global.getString(contentResolver, "global_http_proxy_host") + ":" +
                Settings.Global.getString(contentResolver, "global_http_proxy_port")).let {
                if (it == "null:null") null else it
            }
        } catch (e: Exception) {
            logE("proxy", e)
            null
        }

    fun getCurrentIP(callback: (String?) -> Unit) {
        val process = GlobalScope.launch(Dispatchers.IO) {
            try {
                callback(
                    JSONObject(URL("https://api.ipify.org?format=json").readText()).getString("ip")
                )
            } catch (e: Exception) {
                logE("", e)
                callback(null)
            }
        }

        GlobalScope.launch {
            delay(5)
            if (process.isActive) {
                process.cancel()
                callback(null)
            }
        }
    }
}



