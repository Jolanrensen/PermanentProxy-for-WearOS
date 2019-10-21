package nl.jolanrensen.permanentproxy

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import kotlinx.android.synthetic.main.activity_request_permission.*
import nl.jolanrensen.permanentproxy.Constants.logE
import nl.jolanrensen.permanentproxy.Constants.toastLong
import kotlin.concurrent.thread

class RequestPermissionActivity : WearableActivity() {

    @Volatile
    var currentADBProcess: SendSingleCommand? = null
    @Volatile
    var stop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_permission)

        cancel.setOnClickListener {
            finish()
        }

        thread(start = true) {
            val logs = arrayListOf<String>()
            try {
                currentADBProcess = SendSingleCommand(
                    logs = logs,
                    context = this,
                    ip = "localhost",
                    port = Constants.PORT,
                    command = "pm grant \\\nnl.jolanrensen.permanentproxy \\\nandroid.permission.WRITE_SECURE_SETTINGS",
                    timeout = 2000,
                    ctrlC = false
                ) {
                    currentADBProcess = null
                    Constants.logD(it.toString())
                    runOnUiThread {
                        if (checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS")
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            toastLong(getString(R.string.permission_granted))
                            setResult(Activity.RESULT_OK)
                        } else {
                            toastLong(getString(R.string.something_wrong))
                        }
                        finish()
                    }
                }
            } catch (e: Exception) {
                currentADBProcess = null
                logE("$logs", e)
                runOnUiThread {
                    toastLong(getString(R.string.something_wrong))
                    finish()
                }
            }
        }

        // useless loading bar to show google the app is doing something
        thread(start = true) {
            var s = 0
            while (s <= 20 && !stop) {
                loading_bar.progress = ((s / 20f) * 100f).toInt()
                s++
                Thread.sleep(1000)
            }
        }
    }

    override fun onStop() {
        currentADBProcess?.cancel()
        stop = true
        logE("requesting permission canceled")
        super.onStop()
    }
}