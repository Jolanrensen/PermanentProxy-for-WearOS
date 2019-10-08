package nl.jolanrensen.permanentproxy

import android.content.Context
import android.util.Log
import android.widget.Toast

object Constants {
    const val LOGTAG = "PermanentProxy"

    fun logD(message: String) = Log.d(LOGTAG, message)
    fun logI(message: String) = Log.i(LOGTAG, message)
    fun logE(message: String, e: java.lang.Exception) = Log.e(LOGTAG, message, e)

    fun Context.toast(message: String, length: Int = Toast.LENGTH_SHORT) = Toast.makeText(this, message, length).show()
    fun Context.toastLong(message: String) = toast(message, Toast.LENGTH_LONG)
}