package nl.jolanrensen.permanentproxy

import android.os.Bundle
import android.support.wearable.activity.WearableActivity

class EnableADBBluetoothActivity : WearableActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enable_adbbluetooth)

        // Enables Always-on
        setAmbientEnabled()
    }
}
