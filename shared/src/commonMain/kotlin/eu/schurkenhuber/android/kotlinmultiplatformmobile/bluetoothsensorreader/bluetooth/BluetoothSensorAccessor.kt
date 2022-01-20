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

    suspend fun fetchPressure(): Double
    suspend fun fetchTemperature(): Double
    suspend fun fetchHumidity(): Double

    val inclinationMeasurements: Observable<InclinationMeasurement>
    suspend fun startInclinationMeasuring()
    suspend fun stopInclinationMeasuring()

    suspend fun startBlinking()
    suspend fun stopBlinking()

    fun disconnect()
}

object ServiceUUIDs {
    const val ENVIRONMENTAL_SENSING = "0000181a-0000-1000-8000-00805f9b34fb"
    object EnvironmentalSensingCharacteristicUUIDs {
        const val PRESSURE = "00002a6d-0000-1000-8000-00805f9b34fb"
        const val HUMIDITY = "00002a6f-0000-1000-8000-00805f9b34fb"
        const val TEMPERATURE = "00002a6e-0000-1000-8000-00805f9b34fb"
    }
    const val INCLINATION_SERVICE = "00001815-0000-1000-8000-00805f9b34fb"
    object InclinationCharacteristicUUIDs {
        const val INCLINATION = "a3e0c307-18b3-4fe6-bb2b-102feb860bae"
    }
}
