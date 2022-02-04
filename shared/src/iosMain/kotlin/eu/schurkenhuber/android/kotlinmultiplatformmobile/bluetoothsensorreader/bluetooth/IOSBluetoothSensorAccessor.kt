package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth

import com.badoo.reaktive.observable.Observable
import com.badoo.reaktive.subject.Subject
import com.badoo.reaktive.subject.publish.PublishSubject
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.InclinationMeasurement

class IOSBluetoothSensorAccessor {
    private val connectionStatusSubject: Subject<ConnectionStatus> = PublishSubject()
    val connectionStatus: Observable<ConnectionStatus> = this.connectionStatusSubject

    fun connect(identifier: String) {
        TODO("Not yet implemented")
    }

    suspend fun fetchPressure(): Double {
        TODO("Not yet implemented")
    }

    suspend fun fetchTemperature(): Double {
        TODO("Not yet implemented")
    }

    suspend fun fetchHumidity(): Double {
        TODO("Not yet implemented")
    }

    private val inclinationMeasurementsSubject: Subject<InclinationMeasurement> = PublishSubject()
    val inclinationMeasurements: Observable<InclinationMeasurement> = this.inclinationMeasurementsSubject

    suspend fun startInclinationMeasuring() {
        TODO("Not yet implemented")
    }

    suspend fun stopInclinationMeasuring() {
        TODO("Not yet implemented")
    }

    suspend fun startBlinking() {
        TODO("Not yet implemented")
    }

    suspend fun stopBlinking() {
        TODO("Not yet implemented")
    }

    fun disconnect() {
        TODO("Not yet implemented")
    }
}
