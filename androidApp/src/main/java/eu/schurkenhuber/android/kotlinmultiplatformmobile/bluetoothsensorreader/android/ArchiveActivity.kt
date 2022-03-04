package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.Device
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.BluetoothSensorDiscoveryAction
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.BluetoothSensorDiscoveryStore
import org.koin.android.ext.android.inject

class ArchiveActivity : ComponentActivity() {
    private val store: BluetoothSensorDiscoveryStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            this.ArchiveDeviceList()
        }
    }

    @Composable
    fun ArchiveDeviceList() {
        val state = this@ArchiveActivity.store.observeState().collectAsState()

        LazyColumn {
            items(state.value.registeredDevices, key = { it.id }) { registeredDevice ->
                Button(
                    onClick = { this@ArchiveActivity.switchToArchiveDetail(registeredDevice) },
                    Modifier.fillMaxWidth()
                ) {
                    Text(registeredDevice.hardware_identifier)
                }
            }
        }
    }

    private fun switchToArchiveDetail(registeredDevice: Device) {
        this.store.dispatch(BluetoothSensorDiscoveryAction.LoadMeasurementRecords(registeredDevice.id))

        val extras = Bundle()
        extras.putLong(ArchiveDetailActivity.EXTRA_KEY_REGISTERED_DEVICE_ID, registeredDevice.id)
        val intent = Intent(this, ArchiveDetailActivity::class.java)
        this.startActivity(intent, extras)
    }
}
