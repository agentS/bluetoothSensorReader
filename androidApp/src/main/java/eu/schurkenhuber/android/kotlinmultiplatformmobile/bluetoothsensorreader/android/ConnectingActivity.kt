package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.BluetoothSensorDiscoveryAction
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.BluetoothSensorDiscoveryStore
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.ConnectionStatus
import kotlinx.coroutines.flow.last
import org.koin.android.ext.android.inject

class ConnectingActivity : ComponentActivity() {
    private val store: BluetoothSensorDiscoveryStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setContent {
            ConnectingIndicator()
        }
    }

    @Composable
    fun ConnectingIndicator() {
        val state = this.store.observeState().collectAsState()

        if (state.value.connectionStatus == ConnectionStatus.CONNECTED) {
            val intent = Intent(this, SensorReadingActivity::class.java)
            this.startActivity(intent)
            println("Started sensor reading activity.")
        } else if (state.value.connectionStatus == ConnectionStatus.DISCONNECTED) {
            val intent = Intent(this, DiscoveryActivity::class.java)
            this.startActivity(intent)
        }
        Text(text = this.applicationContext.getString(R.string.lblConnectiong))
    }
}