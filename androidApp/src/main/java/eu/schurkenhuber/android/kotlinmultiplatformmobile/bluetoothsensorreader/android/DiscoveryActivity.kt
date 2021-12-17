package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.android

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.Greeting
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.BluetoothSensorDiscoveryAction
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.BluetoothSensorDiscoveryStore
import org.koin.android.ext.android.inject

fun greet(): String {
    return Greeting().greeting()
}

class DiscoveryActivity : ComponentActivity() {
    private val store: BluetoothSensorDiscoveryStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DiscoveryScreen()
        }
    }

    @Composable
    fun DiscoveryScreen() {
        val state = this@DiscoveryActivity.store.observeState().collectAsState()
        val discoveryButtonText = remember(state.value.scanning) {
            if (!state.value.scanning) "Start discovery" else "Stop discovery"
        }
        val discoveryButtonHandler = remember(state.value.scanning) {
            if (!state.value.scanning) this@DiscoveryActivity::startDiscovery else this@DiscoveryActivity::stopDiscovery
        }
        val discoveredDevices = remember(state.value.discoveredDevices) { state.value.discoveredDevices.toTypedArray() }

        Column {
            Button(onClick = discoveryButtonHandler, Modifier.fillMaxWidth()) {
                Text(text = discoveryButtonText)
            }

            LazyColumn {
                items(discoveredDevices) { discoveredDevice ->
                    Text(text = "MAC address: $discoveredDevice")
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
}
