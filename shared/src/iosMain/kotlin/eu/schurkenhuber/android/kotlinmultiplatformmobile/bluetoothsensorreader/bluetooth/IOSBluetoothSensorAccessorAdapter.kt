package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth

import com.badoo.reaktive.observable.Observable
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.InclinationMeasurement

class IOSBluetoothSensorAccessorAdapter(iosBluetoothAccessor: IOSBluetoothAccessor) : BluetoothSensorAccessor {
    private val bluetoothSensorAccessor: IOSBluetoothAccessor = iosBluetoothAccessor

    override val connectionStatus: Observable<ConnectionStatus> = this.bluetoothSensorAccessor.connectionStatus

    override fun connect(identifier: String) {
        println("Connecting to device $identifier...")
        this.bluetoothSensorAccessor.connect(identifier)
    }

    override suspend fun fetchPressure(peripheralIdentifier: String): Double {
        return this.bluetoothSensorAccessor.readCharacteristic(
            peripheralIdentifier,
            ServiceUUIDs.EnvironmentalSensingCharacteristicUUIDs.PRESSURE
        ) / 10f
    }

    override suspend fun fetchTemperature(peripheralIdentifier: String): Double {
        return this.bluetoothSensorAccessor.readCharacteristic(
            peripheralIdentifier,
            ServiceUUIDs.EnvironmentalSensingCharacteristicUUIDs.TEMPERATURE
        ) / 100f
    }

    override suspend fun fetchHumidity(peripheralIdentifier: String): Double {
        return this.bluetoothSensorAccessor.readCharacteristic(
            peripheralIdentifier,
            ServiceUUIDs.EnvironmentalSensingCharacteristicUUIDs.HUMIDITY
        ) / 100f
    }

    override val inclinationMeasurements: Observable<InclinationMeasurement> = this.bluetoothSensorAccessor.inclinationMeasurements

    override suspend fun startInclinationMeasuring(peripheralIdentifier: String) {
        this.bluetoothSensorAccessor.subscribeToCharacteristic(
            peripheralIdentifier,
            ServiceUUIDs.InclinationCharacteristicUUIDs.INCLINATION
        )
    }

    override suspend fun stopInclinationMeasuring(peripheralIdentifier: String) {
        this.bluetoothSensorAccessor.unsubscribeFromCharacteristic(
            peripheralIdentifier,
            ServiceUUIDs.InclinationCharacteristicUUIDs.INCLINATION
        )
    }

    override suspend fun startBlinking(peripheralIdentifier: String) {
        TODO("Not yet implemented")
    }

    override suspend fun stopBlinking(peripheralIdentifier: String) {
        TODO("Not yet implemented")
    }

    override fun disconnect(identifier: String) {
        this.bluetoothSensorAccessor.disconnect(identifier)
    }
}
