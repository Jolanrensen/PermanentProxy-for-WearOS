package nl.jolanrensen.permanentproxy

import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import kotlinx.android.synthetic.main.activity_donate.*
import nl.jolanrensen.permanentproxy.Constants.launchUrlOnPhone

class DonateActivity : WearableActivity() {

    private fun getPaypalLink(amount: Double) = "https://paypal.me/jolanrensen/$amount"

    val thankYou = 0.99
    val bigThankYou = 1.99
    val biggerThankYou = 4.99
    val biggestThankYou = 9.99

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donate)

        // Enables Always-on
        setAmbientEnabled()


        thank_you_price.text = getString(R.string._dollar, thankYou.toString())
        thank_you_card.setOnClickListener {
            launchUrlOnPhone(
                getPaypalLink(thankYou)
            )
        }

        big_thank_you_price.text = getString(R.string._dollar, bigThankYou.toString())
        big_thank_you_card.setOnClickListener {
            launchUrlOnPhone(
                getPaypalLink(bigThankYou)
            )
        }

        bigger_thank_you_price.text = getString(R.string._dollar, biggerThankYou.toString())
        bigger_thank_you_card.setOnClickListener {
            launchUrlOnPhone(
                getPaypalLink(biggerThankYou)
            )
        }

        biggest_thank_you_price.text = getString(R.string._dollar, biggestThankYou.toString())
        biggest_thank_you_card.setOnClickListener {
            launchUrlOnPhone(
                getPaypalLink(biggestThankYou)
            )
        }
    }
}
