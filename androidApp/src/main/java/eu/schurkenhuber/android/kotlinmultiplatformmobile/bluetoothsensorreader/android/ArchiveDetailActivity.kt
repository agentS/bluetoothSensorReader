package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.BluetoothSensorDiscoveryStore
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.databasemodel.MeasurementTypeIDs
import org.koin.android.ext.android.inject
import java.lang.IllegalArgumentException

class ArchiveDetailActivity : ComponentActivity() {
    companion object {
        const val EXTRA_KEY_REGISTERED_DEVICE_ID = "eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.android.REGISTERED_DEVICE_ID"
    }

    private val store: BluetoothSensorDiscoveryStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setContent {
            this.ArchiveDetailView()
        }
    }

    @Composable
    fun ArchiveDetailView() {
        val state = this@ArchiveDetailActivity.store.observeState().collectAsState()

        LazyColumn {
            items(state.value.measurementRecords, key = { "${it.device_id}-${it.measurement_type_id}-${it.timestamp}" }) { measurement ->
                Text("${measurement.value_} ${MeasurementTypeIDs.mapToUnit(measurement.measurement_type_id)}")
            }
        }
    }
}
