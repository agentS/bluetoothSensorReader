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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
        val context = LocalContext.current

        if (state.value.connectionStatus == ConnectionStatus.DISCONNECTED) {
            Toast.makeText(this, R.string.msgBluetoothLEConnectionLost, Toast.LENGTH_LONG).show()
            this.switchToDiscoverActivity()
        }

        Column {
            Text(text = "${stringResource(R.string.lblConnectionStatus)} ${state.value.connectionStatus}")

            Text(text = "${stringResource(R.string.lblInclination)} = ${state.value.inclination.inclination} (${stringResource(R.string.lblCounter)}: ${state.value.inclination.counter})")
            if (!state.value.measuringInclination) {
                Button(onClick = this@SensorReadingActivity::startInclinationMeasurement, Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.btnStartInclinationMeasurement))
                }
            } else {
                Button(onClick = this@SensorReadingActivity::stopInclinationMeasurement, Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.btnStopInclinationMeasurement))
                }
            }

            Text(text = "${stringResource(R.string.lblPressure)} = ${state.value.environmentReadings.pressure} mbar")
            Text(text = "${stringResource(R.string.lblHumidity)} = ${state.value.environmentReadings.humidity} %")
            Text(text = "${stringResource(R.string.lblTemperature)} = ${state.value.environmentReadings.temperature} °C")
            Button(onClick = this@SensorReadingActivity::fetchEnvironmentReadings, Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.btnFetchEnvironmentReadings))
            }
            Button(onClick = this@SensorReadingActivity::disconnect, Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.btnDisconnect))
            }
        }
    }

    fun startInclinationMeasurement() {
        this.store.dispatch(BluetoothSensorDiscoveryAction.StartInclinationMeasuring(force = false))
    }

    fun stopInclinationMeasurement() {
        this.store.dispatch(BluetoothSensorDiscoveryAction.StopInclinationMeasuring(force = false))
    }

    fun fetchEnvironmentReadings() {
        this.store.dispatch(BluetoothSensorDiscoveryAction.FetchEnvironmentReadings(force = false))
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