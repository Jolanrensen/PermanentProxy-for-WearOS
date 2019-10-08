package nl.jolanrensen.permanentproxy

import android.content.Intent
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import kotlinx.android.synthetic.main.activity_main.*
import nl.jolanrensen.permanentproxy.Constants.logD
import nl.jolanrensen.permanentproxy.Constants.logE
import nl.jolanrensen.permanentproxy.Constants.toast
import kotlin.concurrent.thread

class MainActivity : WearableActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val address = "5.9.73.93"
        val port = "8080"

        val command = "settings put global http_proxy $address:$port\n"

        /*
            Turn off proxy with:
            adb shell settings delete global http_proxy
            adb shell settings delete global global_http_proxy_host
            adb shell settings delete global global_http_proxy_port
         */
        // Enables Always-on
        setAmbientEnabled()

        start_proxy.setOnClickListener {
            thread(start = true) {
                val logs = arrayListOf<String>()

                try {
                    SendSingleCommand(
                        logs = logs,
                        context = this,
                        ip = "localhost",
                        port = 5555,
                        command = command,
                        timeout = 150,
                        ctrlC = false
                    ) {
                        logD(it.toString())
                        sendBroadcast(
                            Intent("android.server.checkin.CHECKIN")
                        )
                    }
                } catch (e: Exception) {
                    logE("", e)
                    runOnUiThread {
                        toast(getString(R.string.adb_enabled))
                    }
                }
            }
        }
    }
}
