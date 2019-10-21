package nl.jolanrensen.permanentproxy

import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.view.View
import androidx.core.view.isVisible
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponse.*
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.SkuDetailsParams
import kotlinx.android.synthetic.main.activity_donate.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.jolanrensen.permanentproxy.Constants.logD
import nl.jolanrensen.permanentproxy.Constants.logE
import nl.jolanrensen.permanentproxy.Constants.toast
import nl.jolanrensen.permanentproxy.Constants.toastLong

class DonateActivity : WearableActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donate)

        // Enables Always-on
        setAmbientEnabled()

        var billingClient: BillingClient? = null
        billingClient = BillingClient.newBuilder(this)
            .setListener { responseCode, purchases ->
                logD("on purchases updated response: $responseCode, purchases: $purchases")
                when (responseCode) {
                    FEATURE_NOT_SUPPORTED -> Unit
                    SERVICE_DISCONNECTED -> Unit
                    OK -> {
                        if (purchases != null) {
                            finish()
                            toastLong(
                                getString(R.string.donation_successful)
                            )
                            for (purchase in purchases) { // should only be 1
                                billingClient!!.consumeAsync(purchase.purchaseToken) { responseCode, _ ->
                                    if (responseCode == OK)
                                        logD("purchase consumed")
                                }
                            }
                        }
                    }
                    USER_CANCELED -> Unit
                    SERVICE_UNAVAILABLE -> Unit
                    BILLING_UNAVAILABLE -> Unit
                    ITEM_UNAVAILABLE -> Unit
                    DEVELOPER_ERROR -> Unit
                    ERROR -> Unit
                    ITEM_ALREADY_OWNED -> Unit
                    ITEM_NOT_OWNED -> Unit
                }
            }
            .build()

        var tries = 0
        val billingClientStateListener = object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                logE("Billing service disconnected")
                if (tries == 10) {
                    runOnUiThread {
                        toast(getString(R.string.couldnt_connect_to_play))
                    }
                }
                val listener = this
                GlobalScope.launch(Dispatchers.Default) {
                    delay(100)
                    billingClient.startConnection(listener)
                    tries++
                }
            }

            override fun onBillingSetupFinished(@BillingClient.BillingResponse responseCode: Int) {
                logD("Billing setup finished")
                // get purchases
                val skuList = arrayListOf(
                    "thank_you",
                    "big_thank_you",
                    "bigger_thank_you",
                    "biggest_thank_you"
                )
                billingClient.querySkuDetailsAsync(
                    SkuDetailsParams.newBuilder()
                        .setSkusList(skuList)
                        .setType(BillingClient.SkuType.INAPP)
                        .build()
                ) { responseCode, skuDetailsList ->
                    logD("Billing sku details received, $responseCode, $skuDetailsList")
                    if (responseCode == OK && skuDetailsList != null) {
                        for (skuDetails in skuDetailsList) {
                            val sku = skuDetails.sku
                            val price = skuDetails.price
                            runOnUiThread {
                                when (sku) {
                                    "thank_you" -> thank_you_price.text = price
                                    "big_thank_you" -> big_thank_you_price.text = price
                                    "bigger_thank_you" -> bigger_thank_you_price.text = price
                                    "biggest_thank_you" -> biggest_thank_you_price.text = price
                                }
                            }
                        }
                    } else {
                        billing_not_working.isVisible = true
                        billing_not_working2.isVisible = true
                    }
                }

                val doPurchase = View.OnClickListener {
                    logD("Billing onclick $it")
                    billingClient!!.launchBillingFlow(
                        this@DonateActivity,
                        BillingFlowParams.newBuilder()
                            .setSku(
                                when (it.id) {
                                    thank_you_card.id -> "thank_you"
                                    big_thank_you_card.id -> "big_thank_you"
                                    bigger_thank_you_card.id -> "bigger_thank_you"
                                    biggest_thank_you_card.id -> "biggest_thank_you"
                                    else -> null
                                }
                            )
                            .setType(BillingClient.SkuType.INAPP)
                            .build()
                    )
                }

                thank_you_card.setOnClickListener(doPurchase)
                big_thank_you_card.setOnClickListener(doPurchase)
                bigger_thank_you_card.setOnClickListener(doPurchase)
                biggest_thank_you_card.setOnClickListener(doPurchase)
            }
        }
        billingClient.startConnection(billingClientStateListener)
    }
}
