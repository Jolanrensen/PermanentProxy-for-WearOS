package nl.jolanrensen.permanentproxy

import android.os.Bundle
import android.support.wearable.activity.WearableActivity

class OldWatchActivity : WearableActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_old_watch)

        // Enables Always-on
        setAmbientEnabled()
    }
}
