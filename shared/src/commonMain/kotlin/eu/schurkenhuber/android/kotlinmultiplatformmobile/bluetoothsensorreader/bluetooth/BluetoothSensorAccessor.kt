package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth

import com.badoo.reaktive.observable.Observable
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.InclinationMeasurement

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    DISCOVERING_SERVICES,
    CONNECTED,
    CLOSED,
}

interface BluetoothSensorAccessor {
    val connectionStatus: Observable<ConnectionStatus>
    fun connect(identifier: String)

    suspend fun fetchPressure(peripheralIdentifier: String): Double
    suspend fun fetchTemperature(peripheralIdentifier: String): Double
    suspend fun fetchHumidity(peripheralIdentifier: String): Double

    val inclinationMeasurements: Observable<InclinationMeasurement>
    suspend fun startInclinationMeasuring(peripheralIdentifier: String)
    suspend fun stopInclinationMeasuring(peripheralIdentifier: String)

    suspend fun startBlinking(peripheralIdentifier: String)
    suspend fun stopBlinking(peripheralIdentifier: String)

    fun disconnect(identifier: String)
}
