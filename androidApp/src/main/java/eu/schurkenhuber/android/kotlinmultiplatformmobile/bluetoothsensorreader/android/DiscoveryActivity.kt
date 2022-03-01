package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.Greeting
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.BluetoothSensorDiscoveryAction
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.BluetoothSensorDiscoveryStore
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.model.BluetoothDeviceInformation
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject

class DiscoveryActivity : ComponentActivity() {
    private val store: BluetoothSensorDiscoveryStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setContent {
            DiscoveryScreen()
        }
    }

    @Composable
    fun DiscoveryScreen() {
        val state = this@DiscoveryActivity.store.observeState().collectAsState()
        val discoveredDevices = remember(state.value.discoveredDevices) {
            state.value.discoveredDevices.values
                .sortedByDescending { device -> device.rssi }
                .toTypedArray()
        }

        Column {
            if (!state.value.scanning) {
                Button(onClick = this@DiscoveryActivity::startDiscovery, Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.btnStartDiscovery))
                }
            } else {
                Button(onClick = this@DiscoveryActivity::stopDiscovery, Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.btnStopDiscovery))
                }
            }

            LazyColumn {
                items(discoveredDevices, key = { it.identifier }) { discoveredDevice ->
                    Button(onClick = { this@DiscoveryActivity.connectToSensor(discoveredDevice) }, Modifier.fillMaxWidth()) {
                        Text(text = "${discoveredDevice.name}, RSSI: ${discoveredDevice.rssi}")
                    }
                }
            }
        }
    }

    fun startDiscovery() {
        this.store.dispatch(BluetoothSensorDiscoveryAction.DiscoverDevices(forceReload = false))
    }

    fun stopDiscovery() {
        this.store.dispatch(BluetoothSensorDiscoveryAction.StopDiscovery(force = true))
    }

    fun connectToSensor(deviceInformation: BluetoothDeviceInformation) {
        this.store.dispatch(BluetoothSensorDiscoveryAction.ConnectToSensor(deviceInformation))
        val intent = Intent(this, ConnectingActivity::class.java)
        this.startActivity(intent)
    }
}
