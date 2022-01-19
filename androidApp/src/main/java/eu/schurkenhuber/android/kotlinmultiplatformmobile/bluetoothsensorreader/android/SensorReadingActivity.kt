package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.BluetoothSensorDiscoveryAction
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.BluetoothSensorDiscoveryStore
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.ConnectionStatus
import org.koin.android.ext.android.inject

class SensorReadingActivity : ComponentActivity() {
    private val store: BluetoothSensorDiscoveryStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setContent {
            this.SensorReadingFragment()
        }
    }

    @Composable
    fun SensorReadingFragment() {
        val state = this.store.observeState().collectAsState()

        if (state.value.connectionStatus == ConnectionStatus.DISCONNECTED) {
            Toast.makeText(this, R.string.msgBluetoothLEConnectionLost, Toast.LENGTH_LONG).show()
            this.switchToDiscoverActivity()
        }

        Column {
            Text(text = "Connection status: ${state.value.connectionStatus}")
            Button(onClick = this@SensorReadingActivity::disconnect, Modifier.fillMaxWidth()) {
                Text(text = "Disconnect")
            }
        }
    }

    fun disconnect() {
        this.store.dispatch(BluetoothSensorDiscoveryAction.DisconnectFromSensor(ConnectionStatus.DISCONNECTED))
        this.switchToDiscoverActivity()
    }

    private fun switchToDiscoverActivity() {
        val intent = Intent(this, DiscoveryActivity::class.java)
        this.startActivity(intent)
    }
}