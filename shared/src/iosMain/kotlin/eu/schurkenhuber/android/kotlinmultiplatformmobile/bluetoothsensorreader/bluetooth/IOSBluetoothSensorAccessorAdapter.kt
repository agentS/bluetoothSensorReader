package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth

import com.badoo.reaktive.observable.Observable
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.InclinationMeasurement

class IOSBluetoothSensorAccessorAdapter : BluetoothSensorAccessor {
    private val blueoothSensorAccessor: IOSBluetoothSensorAccessor

    init {
        this.blueoothSensorAccessor = IOSBluetoothSensorAccessor()
    }

    override val connectionStatus: Observable<ConnectionStatus> = this.blueoothSensorAccessor.connectionStatus

    override fun connect(identifier: String) {
        this.blueoothSensorAccessor.connect(identifier)
    }

    override suspend fun fetchPressure(): Double {
        return this.blueoothSensorAccessor.fetchPressure()
    }

    override suspend fun fetchTemperature(): Double {
        return this.blueoothSensorAccessor.fetchTemperature()
    }

    override suspend fun fetchHumidity(): Double {
        return this.blueoothSensorAccessor.fetchHumidity()
    }

    override val inclinationMeasurements: Observable<InclinationMeasurement> = this.blueoothSensorAccessor.inclinationMeasurements

    override suspend fun startInclinationMeasuring() {
        this.blueoothSensorAccessor.startInclinationMeasuring()
    }

    override suspend fun stopInclinationMeasuring() {
        this.blueoothSensorAccessor.stopInclinationMeasuring()
    }

    override suspend fun startBlinking() {
        this.blueoothSensorAccessor.startBlinking()
    }

    override suspend fun stopBlinking() {
        this.blueoothSensorAccessor.stopBlinking()
    }

    override fun disconnect() {
        this.blueoothSensorAccessor.disconnect()
    }
}
