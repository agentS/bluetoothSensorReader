package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application

import com.badoo.reaktive.observable.subscribe
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.BluetoothSensorAccessorDatabase
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.Device
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.Measurement
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.BluetoothDiscoverer
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.BluetoothSensorAccessor
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.ConnectionStatus
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.databasemodel.MeasurementTypeIDs
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.model.BluetoothDeviceInformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

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
    val discoveredDevices: Map<String, BluetoothDeviceInformation>,
    val connectedDevice: BluetoothDeviceInformation?,
    val environmentReadings: EnvironmentReadings,
    val measuringInclination: Boolean,
    val inclination: InclinationMeasurement,
    val registeredDevices: List<Device>,
    val measurementRecords: List<Measurement>
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
    data class LoadRegisteredDevices(val force: Boolean) : BluetoothSensorDiscoveryAction()
    data class LoadMeasurementRecords(val registeredDeviceID: Long) : BluetoothSensorDiscoveryAction()
}

sealed class BluetoothSensorDiscoverySideEffect : Effect {}

class BluetoothSensorDiscoveryStore(
    private val bluetoothDiscoverer: BluetoothDiscoverer,
    private val bluetoothSensorAccessor: BluetoothSensorAccessor,
    private val database: BluetoothSensorAccessorDatabase
)
    :   Store<BluetoothSensorDiscoveryState, BluetoothSensorDiscoveryAction, BluetoothSensorDiscoverySideEffect>,
        CoroutineScope by CoroutineScope(Dispatchers.Main) {

    private val state = MutableStateFlow(BluetoothSensorDiscoveryState(
        scanning = false,
        connectionStatus = ConnectionStatus.DISCONNECTED,
        discoveredDevices = emptyMap(),
        connectedDevice = null,
        EnvironmentReadings(pressure = 0.0, humidity = 0.0, temperature = 0.0),
        measuringInclination = false,
        inclination = InclinationMeasurement(counter = 0, inclination = 0.0),
        registeredDevices = emptyList(),
        measurementRecords = emptyList()
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
                    previousState.copy(scanning = true, discoveredDevices = emptyMap(), connectionStatus = ConnectionStatus.DISCONNECTED)
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
            is BluetoothSensorDiscoveryAction.DeviceDiscovered -> {
                previousState.copy(
                    discoveredDevices = previousState.discoveredDevices + (action.deviceInformation.identifier to action.deviceInformation)
                )
            }
            is BluetoothSensorDiscoveryAction.ConnectToSensor -> {
                this.stopBluetoothDiscovery()
                this.connectToSensor(action.deviceInformation)
                previousState.copy(
                    scanning = false,
                    connectionStatus = ConnectionStatus.CONNECTING,
                    connectedDevice = action.deviceInformation,
                    discoveredDevices = emptyMap()
                )
            }
            is BluetoothSensorDiscoveryAction.ConnectionEstablished -> {
                if (previousState.connectedDevice != null) {
                    this.registerDevice(previousState.connectedDevice);
                } else {
                    throw IllegalStateException("No device for connecting to has been selected.")
                }
                previousState.copy(
                    connectionStatus = ConnectionStatus.CONNECTED
                )
            }
            is BluetoothSensorDiscoveryAction.DisconnectFromSensor -> {
                // TODO: replace with solution that waits for closed BluetoothLE connection
                launch {
                    if (previousState.connectionStatus == ConnectionStatus.CONNECTED) {
                        this@BluetoothSensorDiscoveryStore.stopInclinationMeasuring(previousState.connectedDevice?.identifier ?: "")
                    }
                    if (previousState.connectedDevice != null) {
                        this@BluetoothSensorDiscoveryStore.disconnectFromSensor(previousState.connectedDevice)
                    }
                }
                previousState.copy(
                    connectionStatus = ConnectionStatus.DISCONNECTED,
                    connectedDevice = null,
                    discoveredDevices = emptyMap(),
                    environmentReadings = EnvironmentReadings(pressure = 0.0, humidity = 0.0, temperature = 0.0),
                    measuringInclination = false,
                    inclination = InclinationMeasurement(counter = 0, inclination = 0.0)
                )

            }
            is BluetoothSensorDiscoveryAction.FetchEnvironmentReadings -> {
                launch { this@BluetoothSensorDiscoveryStore.fetchEnvironmentReadings(previousState.connectedDevice?.identifier ?: "") }
                previousState
            }
            is BluetoothSensorDiscoveryAction.EnvironmentReadingsFetched -> {
                if (previousState.connectedDevice != null) {
                    this.storeEnvironmentReadings(previousState.connectedDevice, action.environmentReadings)
                } else {
                    throw IllegalStateException("Could not store the environment readings: No active BluetoothLE connection.")
                }
                previousState.copy(environmentReadings = action.environmentReadings)
            }
            is BluetoothSensorDiscoveryAction.StartInclinationMeasuring -> {
                launch { this@BluetoothSensorDiscoveryStore.startInclinationMeasuring(previousState.connectedDevice?.identifier ?: "") }
                previousState.copy(measuringInclination = true)
            }
            is BluetoothSensorDiscoveryAction.InclinationMeasurementReceived -> {
                previousState.copy(inclination = action.inclination)
            }
            is BluetoothSensorDiscoveryAction.StopInclinationMeasuring -> {
                launch { this@BluetoothSensorDiscoveryStore.stopInclinationMeasuring(previousState.connectedDevice?.identifier ?: "") }
                previousState.copy(measuringInclination = false)
            }
            is BluetoothSensorDiscoveryAction.LoadRegisteredDevices -> {
                val registeredDevices = this.fetchRegisteredDevices()
                previousState.copy(registeredDevices = registeredDevices)
            }
            is BluetoothSensorDiscoveryAction.LoadMeasurementRecords -> {
                previousState.copy(measurementRecords = this.fetchMeasurementRecords(action.registeredDeviceID))
            }
        }

        if (nextState != previousState || action is BluetoothSensorDiscoveryAction.DeviceDiscovered) {
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

    private fun disconnectFromSensor(deviceInformation: BluetoothDeviceInformation) {
        this.bluetoothSensorAccessor.disconnect(deviceInformation.identifier)
    }

    private fun onBluetoothLEConnectionStatusChanged(status: ConnectionStatus) {
        when (status) {
            ConnectionStatus.CONNECTED -> this.dispatch(BluetoothSensorDiscoveryAction.ConnectionEstablished(status))
            ConnectionStatus.DISCONNECTED -> this.dispatch(BluetoothSensorDiscoveryAction.DisconnectFromSensor(status))
            else -> println("Changed Bluetooth LE connection status to $status.")
        }
    }

    private suspend fun startInclinationMeasuring(peripheralUUID: String) {
        this.bluetoothSensorAccessor.startInclinationMeasuring(peripheralUUID)
    }

    private suspend fun stopInclinationMeasuring(peripheralUUID: String) {
        this.bluetoothSensorAccessor.stopInclinationMeasuring(peripheralUUID)
    }

    private fun onInclinationMeasurementReceived(inclination: InclinationMeasurement) {
        this.dispatch(BluetoothSensorDiscoveryAction.InclinationMeasurementReceived(inclination))
    }

    private suspend fun fetchEnvironmentReadings(peripheralUUID: String) {
        val pressure = this.bluetoothSensorAccessor.fetchPressure(peripheralUUID)
        val humidity = this.bluetoothSensorAccessor.fetchHumidity(peripheralUUID)
        val temperature = this.bluetoothSensorAccessor.fetchTemperature(peripheralUUID)
        this.dispatch(BluetoothSensorDiscoveryAction.EnvironmentReadingsFetched(EnvironmentReadings(
            pressure,
            humidity,
            temperature,
        )))
    }

    private fun registerDevice(connectedDevice: BluetoothDeviceInformation) {
        val device = this.database.deviceManagementQueries.findDeviceByHardwareIdentifier(
            connectedDevice.identifier
        ).executeAsOneOrNull()
        if (device == null) {
            this.database.deviceManagementQueries.createDevice(connectedDevice.identifier)
        }
    }

    private fun storeEnvironmentReadings(
        connectedDevice: BluetoothDeviceInformation,
        environmentReadings: EnvironmentReadings
    ) {
        val device = this.database.deviceManagementQueries.findDeviceByHardwareIdentifier(
            connectedDevice.identifier
        ).executeAsOne()

        val timestamp = Clock.System.now().toString()
        val unitsToValues: Map<Long, Double> = mapOf(
            MeasurementTypeIDs.TEMPERATURE to environmentReadings.temperature,
            MeasurementTypeIDs.HUMIDITY to environmentReadings.humidity,
            MeasurementTypeIDs.ATMOSPHERIC_PRESSURE to environmentReadings.pressure
        )
        for ((unitID, measurementValue) in unitsToValues) {
            this.database.measurementQueries.storeMeasurement(
                timestamp, device.id,
                unitID, measurementValue
            )
        }
    }

    private fun fetchRegisteredDevices(): List<Device> =
        this.database.deviceManagementQueries.findAllDevices().executeAsList()

    private fun fetchMeasurementRecords(deviceID: Long): List<Measurement> =
        this.database.measurementQueries.findAllByDeviceID(deviceID).executeAsList()
}
