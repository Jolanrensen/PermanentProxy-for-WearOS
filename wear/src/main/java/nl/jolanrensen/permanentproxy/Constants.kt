package nl.jolanrensen.permanentproxy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.CATEGORY_BROWSABLE
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.wear.activity.ConfirmationActivity
import androidx.wear.activity.ConfirmationActivity.EXTRA_ANIMATION_TYPE
import androidx.wear.activity.ConfirmationActivity.OPEN_ON_PHONE_ANIMATION
import com.google.android.wearable.intent.RemoteIntent
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

    fun Activity.startProxy(
        address: String,
        port: Int,
        updateGooglePay: Boolean = true
    ) {
        Settings.Global.putString(contentResolver, Settings.Global.HTTP_PROXY, "$address:$port")
        if (updateGooglePay) {
            GlobalScope.launch {
                delay(1000)
                runOnUiThread {
                    sendBroadcast(Intent("android.server.checkin.CHECKIN"))
                    logD("Broadcast message to check country for GPay")
                }
            }

        }
    }

    fun Context.stopProxy(
        onFailure: (() -> Unit)? = null
    ) {
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

    fun getCurrentIP(callback: (ip: String?, countryCode: String?) -> Unit) {
        val process = GlobalScope.launch(Dispatchers.IO) {
            try {
                val ip: String
                callback(
                    URL("https://api64.ipify.org")
                        .readText()
                        .also { ip = it },
                    JSONObject(
                        URL("http://ip-api.com/json/$ip").readText()
                    ).getString("countryCode")
                )
            } catch (e: Exception) {
                logE("", e)
                callback(null, null)
            }
        }

        GlobalScope.launch {
            delay(100)
            if (process.isActive) {
                process.cancel()
                callback(null, null)
            }
        }
    }

    fun Context.launchUrlOnPhone(url: String) {
        RemoteIntent.startRemoteActivity(
            this,
            Intent(ACTION_VIEW)
                .addCategory(CATEGORY_BROWSABLE)
                .setData(Uri.parse(url)),
            null
        )
        startActivity(
            Intent(this, ConfirmationActivity::class.java)
                .putExtra(EXTRA_ANIMATION_TYPE, OPEN_ON_PHONE_ANIMATION)
        )
    }
}



