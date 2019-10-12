package nl.jolanrensen.permanentproxy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.content.pm.PackageManager
import nl.jolanrensen.permanentproxy.Constants.startProxy

class OnBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_BOOT_COMPLETED -> {
                val p = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
                if (p.getBoolean("onBoot", false)) {
                    val address = p.getString("address", "")!!
                    val port = p.getInt("port", -1)
                    if (address != ""
                        && port != -1
                        && context.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS")
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        context.startProxy(
                            address = address,
                            port = port,
                            updateGooglePay = true
                        )
                    }
                }
            }
        }
    }
}