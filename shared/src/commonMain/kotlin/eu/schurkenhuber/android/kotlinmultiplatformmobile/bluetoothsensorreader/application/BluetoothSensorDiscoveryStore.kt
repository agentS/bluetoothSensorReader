package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application

import com.badoo.reaktive.observable.subscribe
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.BluetoothDiscoverer
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.BluetoothSensorAccessor
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.ConnectionStatus
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.model.BluetoothDeviceInformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class EnvironmentReadings(
    val pressure: Double,
    val humidity: Double,
    val temperature: Double
)

data class InclinationMeasurement(
    val counter: Int,
    val inclination: Double
)

data class BluetoothSensorDiscoveryState(
    val scanning: Boolean,
    val connectionStatus: ConnectionStatus,
    val discoveredDevices: Set<BluetoothDeviceInformation>,
    val connectedDevice: BluetoothDeviceInformation?,
    val environmentReadings: EnvironmentReadings,
    val measuringInclination: Boolean,
    val inclination: InclinationMeasurement
) : State

sealed class BluetoothSensorDiscoveryAction : Action {
    data class DiscoverDevices(val forceReload: Boolean) : BluetoothSensorDiscoveryAction()
    data class StopDiscovery(val force: Boolean) : BluetoothSensorDiscoveryAction()
    data class DeviceDiscovered(val deviceInformation: BluetoothDeviceInformation) : BluetoothSensorDiscoveryAction()
    data class ConnectToSensor(val deviceInformation: BluetoothDeviceInformation) : BluetoothSensorDiscoveryAction()
    data class ConnectionEstablished(val connectionStatus: ConnectionStatus) : BluetoothSensorDiscoveryAction()
    data class DisconnectFromSensor(val connectionStatus: ConnectionStatus) : BluetoothSensorDiscoveryAction()
    data class FetchEnvironmentReadings(val force: Boolean) : BluetoothSensorDiscoveryAction()
    data class EnvironmentReadingsFetched(val environmentReadings: EnvironmentReadings) : BluetoothSensorDiscoveryAction()
    data class StartInclinationMeasuring(val force: Boolean) : BluetoothSensorDiscoveryAction()
    data class InclinationMeasurementReceived(val inclination: InclinationMeasurement) : BluetoothSensorDiscoveryAction()
    data class StopInclinationMeasuring(val force: Boolean) : BluetoothSensorDiscoveryAction()
}

sealed class BluetoothSensorDiscoverySideEffect : Effect {}

class BluetoothSensorDiscoveryStore(
    private val bluetoothDiscoverer: BluetoothDiscoverer,
    private val bluetoothSensorAccessor: BluetoothSensorAccessor
)
    :   Store<BluetoothSensorDiscoveryState, BluetoothSensorDiscoveryAction, BluetoothSensorDiscoverySideEffect>,
        CoroutineScope by CoroutineScope(Dispatchers.Main) {

    private val state = MutableStateFlow(BluetoothSensorDiscoveryState(
        scanning = false,
        connectionStatus = ConnectionStatus.DISCONNECTED,
        discoveredDevices = emptySet(),
        connectedDevice = null,
        EnvironmentReadings(pressure = 0.0, humidity = 0.0, temperature = 0.0),
        measuringInclination = false,
        inclination = InclinationMeasurement(counter = 0, inclination = 0.0)
    ))
    private val sideEffect = MutableSharedFlow<BluetoothSensorDiscoverySideEffect>()

    init {
        this.bluetoothDiscoverer.onDeviceDiscovered.subscribe(
            isThreadLocal = false,
            onNext = this::onBluetoothDeviceDiscovered
        )
        this.bluetoothSensorAccessor.connectionStatus.subscribe(
            isThreadLocal = false,
            onNext = this::onBluetoothLEConnectionStatusChanged
        )
        this.bluetoothSensorAccessor.inclinationMeasurements.subscribe(
            isThreadLocal = false,
            onNext = this::onInclinationMeasurementReceived
        )
    }

    override fun observeState(): StateFlow<BluetoothSensorDiscoveryState> = this.state

    override fun observeSideEffect(): Flow<BluetoothSensorDiscoverySideEffect> = this.sideEffect

    override fun dispatch(action: BluetoothSensorDiscoveryAction) {
        val previousState = this.state.value

        val nextState = when (action) {
            is BluetoothSensorDiscoveryAction.DiscoverDevices ->
                if (!previousState.scanning) {
                    this.startBluetoothDiscovery()
                    previousState.copy(scanning = true, discoveredDevices = emptySet(), connectionStatus = ConnectionStatus.DISCONNECTED)
                } else {
                    previousState
                }
            is BluetoothSensorDiscoveryAction.StopDiscovery ->
                if (previousState.scanning) {
                    this.stopBluetoothDiscovery()
                    previousState.copy(scanning = false, connectionStatus = ConnectionStatus.DISCONNECTED)
                } else {
                    previousState
                }
            is BluetoothSensorDiscoveryAction.DeviceDiscovered ->
                previousState.copy(
                    discoveredDevices = previousState.discoveredDevices + action.deviceInformation // setOf(*previousState.discoveredDevices.toTypedArray(), action.address)
                )
            is BluetoothSensorDiscoveryAction.ConnectToSensor -> {
                this.stopBluetoothDiscovery()
                this.connectToSensor(action.deviceInformation)
                previousState.copy(
                    scanning = false,
                    connectionStatus = ConnectionStatus.CONNECTING,
                    connectedDevice = action.deviceInformation,
                    discoveredDevices = emptySet()
                )
            }
            is BluetoothSensorDiscoveryAction.ConnectionEstablished -> {
                previousState.copy(
                    connectionStatus = ConnectionStatus.CONNECTED
                )
            }
            is BluetoothSensorDiscoveryAction.DisconnectFromSensor -> {
                this.disconnectFromSensor()
                previousState.copy(
                    connectionStatus = ConnectionStatus.DISCONNECTED,
                    connectedDevice = null,
                    discoveredDevices = emptySet()
                )
            }
            is BluetoothSensorDiscoveryAction.FetchEnvironmentReadings -> {
                launch { this@BluetoothSensorDiscoveryStore.fetchEnvironmentReadings() }
                previousState
            }
            is BluetoothSensorDiscoveryAction.EnvironmentReadingsFetched -> {
                previousState.copy(environmentReadings = action.environmentReadings)
            }
            is BluetoothSensorDiscoveryAction.StartInclinationMeasuring -> {
                launch { this@BluetoothSensorDiscoveryStore.startInclinationMeasuring() }
                previousState.copy(measuringInclination = true)
            }
            is BluetoothSensorDiscoveryAction.InclinationMeasurementReceived -> {
                previousState.copy(inclination = action.inclination)
            }
            is BluetoothSensorDiscoveryAction.StopInclinationMeasuring -> {
                launch { this@BluetoothSensorDiscoveryStore.stopInclinationMeasuring() }
                previousState.copy(measuringInclination = false)
            }
        }

        if (nextState != previousState) {
            println("switching to state $nextState from $previousState")
            this.state.value = nextState
        }
    }

    private fun startBluetoothDiscovery() {
        this.bluetoothDiscoverer.startDiscovery()
    }

    private fun onBluetoothDeviceDiscovered(deviceInformation: BluetoothDeviceInformation) {
        this.dispatch(BluetoothSensorDiscoveryAction.DeviceDiscovered(deviceInformation))
    }

    private fun stopBluetoothDiscovery() {
        this.bluetoothDiscoverer.stopDiscovery()
    }

    private fun connectToSensor(deviceInformation: BluetoothDeviceInformation) {
        this.bluetoothSensorAccessor.connect(deviceInformation.identifier)
    }

    private fun disconnectFromSensor() {
        this.bluetoothSensorAccessor.disconnect()
    }

    private fun onBluetoothLEConnectionStatusChanged(status: ConnectionStatus) {
        when (status) {
            ConnectionStatus.CONNECTED -> this.dispatch(BluetoothSensorDiscoveryAction.ConnectionEstablished(status))
            ConnectionStatus.DISCONNECTED -> this.dispatch(BluetoothSensorDiscoveryAction.DisconnectFromSensor(status))
            else -> println("Changed Bluetooth LE connection status to $status.")
        }
    }

    private suspend fun startInclinationMeasuring() {
        this.bluetoothSensorAccessor.startInclinationMeasuring()
    }

    private suspend fun stopInclinationMeasuring() {
        this.bluetoothSensorAccessor.stopInclinationMeasuring()
    }

    private fun onInclinationMeasurementReceived(inclination: InclinationMeasurement) {
        this.dispatch(BluetoothSensorDiscoveryAction.InclinationMeasurementReceived(inclination))
    }

    private suspend fun fetchEnvironmentReadings() {
        val pressure = this.bluetoothSensorAccessor.fetchPressure()
        val humidity = this.bluetoothSensorAccessor.fetchHumidity()
        val temperature = this.bluetoothSensorAccessor.fetchTemperature()
        this.dispatch(BluetoothSensorDiscoveryAction.EnvironmentReadingsFetched(EnvironmentReadings(
            pressure,
            humidity,
            temperature,
        )))
    }
}
