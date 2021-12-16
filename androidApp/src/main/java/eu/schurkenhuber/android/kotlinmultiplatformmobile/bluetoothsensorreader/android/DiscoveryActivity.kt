package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.Greeting
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.BluetoothSensorDiscoveryAction
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.BluetoothSensorDiscoveryStore
import org.koin.android.ext.android.inject

fun greet(): String {
    return Greeting().greeting()
}

class DiscoveryActivity : AppCompatActivity() {
    private val store: BluetoothSensorDiscoveryStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discovery)
    }

    fun startDiscovery(view: View) {
        println("Starting BluetoothLE discovery.")
        this.store.dispatch(BluetoothSensorDiscoveryAction.DiscoverDevices(forceReload = false))
    }
}
