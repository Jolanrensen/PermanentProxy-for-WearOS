package nl.jolanrensen.permanentproxy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import androidx.core.content.edit
import nl.jolanrensen.permanentproxy.Constants.getTurnOnProxyCommand
import nl.jolanrensen.permanentproxy.Constants.logD
import nl.jolanrensen.permanentproxy.Constants.logE
import nl.jolanrensen.permanentproxy.Constants.startProxy

class OnBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_BOOT_COMPLETED -> {
                val p = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
                if (p.getBoolean("onBoot", false)) {
                    val address = p.getString("address", "")!!
                    val port = p.getInt("port", -1)
                    if (address != "" && port != -1) {
                        context.startProxy(
                            command = getTurnOnProxyCommand(address, port),
                            onSuccess = {
                                logD("Successfully set proxy to $address:$port")
                                p.edit {
                                    putString("onBootLogs", it.toString())
                                }
                            },
                            onFailure = { it, e ->
                                logE("Could not set proxy to $address:$port")
                                p.edit {
                                    putString("onBootLogs", it.toString())
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}