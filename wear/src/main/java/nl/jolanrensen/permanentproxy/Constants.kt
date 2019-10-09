package nl.jolanrensen.permanentproxy

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import kotlin.concurrent.thread

object Constants {
    const val LOGTAG = "PermanentProxy"

    fun logD(message: String) = Log.d(LOGTAG, message)
    fun logI(message: String) = Log.i(LOGTAG, message)
    fun logE(message: String, e: java.lang.Exception? = null) = Log.e(LOGTAG, message, e)

    fun Context.toast(message: String, length: Int = Toast.LENGTH_SHORT) =
        Toast.makeText(this, message, length).show()

    fun Context.toastLong(message: String) = toast(message, Toast.LENGTH_LONG)

    fun getTurnOnProxyCommand(address: String, port: Int) =
        "settings put global http_proxy $address:$port\n"

    fun getTurnOffProxyCommand() =
        "settings delete global http_proxy; settings delete global global_http_proxy_host; settings delete global global_http_proxy_port"

    fun Context.startProxy(
        command: String,
        onSuccess: ((logs: ArrayList<String>?) -> Unit)? = null,
        onFailure: ((logs: ArrayList<String>, e: Exception) -> Unit)? = null,
        updateGooglePay: Boolean = true
    ) {
        thread(start = true) {
            val logs = arrayListOf<String>()

            try {
                SendSingleCommand(
                    logs = logs,
                    context = this,
                    ip = "localhost",
                    port = 7272,
                    command = command,
                    timeout = 150,
                    ctrlC = false
                ) {
                    logD(it.toString())
                    if (updateGooglePay) sendBroadcast(
                        Intent("android.server.checkin.CHECKIN")
                    )
                    onSuccess?.invoke(it)
                }
            } catch (e: Exception) {
                logE("$logs", e)
                onFailure?.invoke(logs, e)
            }
        }
    }

    fun Context.stopProxy(
        onSuccess: ((logs: ArrayList<String>?) -> Unit)? = null,
        onFailure: ((logs: ArrayList<String>, e: Exception) -> Unit)? = null
    ) {
        thread(start = true) {
            val logs = arrayListOf<String>()

            try {
                SendSingleCommand(
                    logs = logs,
                    context = this,
                    ip = "localhost",
                    port = 7272,
                    command = getTurnOffProxyCommand(),
                    timeout = 150,
                    ctrlC = false
                ) {
                    logD(it.toString())
                    sendBroadcast(
                        Intent("android.server.checkin.CHECKIN")
                    )
                    onSuccess?.invoke(it)
                }
            } catch (e: Exception) {
                logE("$logs", e)
                onFailure?.invoke(logs, e)
            }
        }
    }
}