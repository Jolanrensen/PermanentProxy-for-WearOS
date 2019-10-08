package nl.jolanrensen.permanentproxy

import android.content.Context
import android.org.apache.commons.codec.binary.Base64.encodeBase64String
import android.util.Log
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import com.tananaev.adblib.AdbStream
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.UnknownHostException
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException

/**
 * Created by Jolan Rensen on 21-2-2017.
 * Used https://github.com/cgutman/AdbLibTest as example.
 */

class SendSingleCommand

/**
 * Method to send a given ADB Shell command to a given device.
 *
 * @param context Context of the activity, to get the file store location (might be deprecated)
 * @param ip      IP of the device to connect to
 * @param port    Port of the device with given `ip` to connect to
 * @param command The command to be executed on `ip:port`
 */
@Throws(IOException::class, Exception::class)
constructor(
    private val logs: ArrayList<String>,
    private val context: Context,
    private val ip: String,
    private val port: Int,
    private val command: String,
    private val timeout: Int,
    ctrlC: Boolean,
    private val callBack: (ArrayList<String>?) -> Unit
) {

    private var splitResponses: ArrayList<String>? = null

    val pub = File(context.filesDir, "pub.key")
    val priv = File(context.filesDir, "priv.key")

    var stream: AdbStream
    var adb: AdbConnection

    // This implements the AdbBase64 interface required for AdbCrypto
    val base64Impl: AdbBase64
        get() = AdbBase64 { encodeBase64String(it) }

    init {
        val sock: Socket
        val crypto: AdbCrypto

        // Setup the crypto object required for the AdbConnection
        try {
            crypto = setupCrypto()
        } catch (e: IOException) {
            throw IOException(
                "Couldn't read/write keys from ${pub.path} and ${priv.path}, make sure you gave storage read/write permission.",
                e
            )
        }

        // Connect the socket to the remote host
        logI("Socket connecting at $ip:$port")
        try {
            sock = Socket(ip, port)
        } catch (e: UnknownHostException) {
            throw Exception("$ip is no valid ip address", e)
        } catch (e: ConnectException) {
            throw Exception("Device at $ip:$port has no adb enabled or connection is refused", e)
        } catch (e: NoRouteToHostException) {
            throw Exception("Couldn't find adb device at $ip:$port", e)
        }

        logI("Socket connected, creating AdbConnection...")

        // Construct the AdbConnection object
        adb = AdbConnection.create(sock, crypto)

        // Start the application layer connection process
        logD("Created, ADB connecting...")
        try {
            adb.connect()
        } catch (e: IllegalStateException) {
            logE("ADB already connected...", e)
        } catch (e: InterruptedException) {
            throw Exception("unable to wait for the connection to finish", e)
        } catch (e: IOException) {
            throw IOException("the socket fails while connecting", e)
        }
        logD("ADB connected, opening shell stream...")

        // sending...
        try {
            stream = adb.open("shell:")
        } catch (e: IOException) {
            throw IOException("stream fails while sending the packet", e)
        } catch (e: UnsupportedEncodingException) {
            throw Exception("destination cannot be encoded to UTF-8", e)
        } catch (e: InterruptedException) {
            throw Exception("unable to wait for the connection to finish", e)
        }

        logD("Writing command: $command")

        try {
            if (ctrlC) stream.write(byteArrayOf(0x03))
            stream.write(command + '\n')
        } catch (e: IOException) {
            throw IOException("Couldn't write command, stream fails while sending data, try without CTRL+C", e)
        } catch (e: InterruptedException) {
            throw Exception("Couldn't write command, unable to wait to send data", e)
        }

        logD("Command sent")

        var responses = ""
        var done = false
        var timer = 0

        logD("Getting responses...")
        GlobalScope.launch {
            while (!done) {
                try {
                    timer = 0
                    val responseBytes = stream.read()
                    val response = String(responseBytes, charset("US-ASCII"))
                    responses += response
                } catch (e: InterruptedException) {
                    throw Exception("Couldn't get response, unable to wait for data", e)
                } catch (e: IOException) {
                    logD("Stream stopped")
                }
            }
        }

        GlobalScope.launch {
            // the timer resets on each response and when it reached the limit, it stops reading and continues with the rest
            while (!done) {
                delay(1)
                timer++
                if (timer == timeout) done = true
            }

            logD("response:\n$responses")

            // Trying to split the response on newlines, not waterproof
            splitResponses = ArrayList()
            for (item in responses.split("\\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                splitResponses!!.add(item.trim { it <= ' ' })
            }

            logD("Sending close command and waiting for stream to close")
            stream.close()
            GlobalScope.launch {
                delay(10000)
                if (!stream.isClosed)
                    throw Exception("Stream didn't close after 10 seconds waiting")
            }
            GlobalScope.launch {
                while (!stream.isClosed) {
                }
                logD("Stream closed, closing Adb...")

                try {
                    adb.close()
                } catch (e: IOException) {
                    throw IOException("Couldn't close ADB connection socket", e)
                }
                GlobalScope.launch {
                    delay(10000)
                    if (!sock.isClosed)
                        throw Exception("ADB connection socket didn't close after 10 seconds waiting")
                }
                GlobalScope.launch {
                    while (!sock.isClosed) {
                    }
                    logD("ADB connection socket closed")
                    callBack(splitResponses)
                }
            }
        }
    }

    // This function loads a keypair from the specified files if one exists, and if not,
    // it creates a new keypair and saves it in the specified files
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class, IOException::class)
    private fun setupCrypto(): AdbCrypto {
        logD("keyfiles paths: ${pub.path}, ${priv.path}")
        var c: AdbCrypto? = null

        // Try to load a key pair from the files
        if (pub.exists() && priv.exists()) {
            c = try {
                AdbCrypto.loadAdbKeyPair(base64Impl, priv, pub)
            } catch (e: IOException) {
                // Failed to read from file
                null
            } catch (e: InvalidKeySpecException) {
                // Key spec was invalid
                null
            } catch (e: NoSuchAlgorithmException) {
                // RSA algorithm was unsupported with the crypo packages available
                null
            }
        }

        if (c == null) {
            // We couldn't load a key, so let's generate a new one
            c = AdbCrypto.generateAdbKeyPair(base64Impl)

            // Save it
            c!!.saveAdbKeyPair(priv, pub)
            logI("Generated new keypair")
        } else {
            logI("Loaded existing keypair")
        }

        return c
    }

    private fun logD(message: String) {
        Log.d(Constants.LOGTAG, message)
        logs.add(message)
    }

    private fun logI(message: String) {
        Log.i(Constants.LOGTAG, message)
        logs.add(message)
    }

    private fun logE(message: String, exception: java.lang.Exception) {
        Log.e(Constants.LOGTAG, message, exception)
        logs.add(message)
    }
}
