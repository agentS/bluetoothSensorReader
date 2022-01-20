package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth

import android.content.*
import android.os.IBinder
import com.badoo.reaktive.observable.Observable
import com.badoo.reaktive.subject.publish.PublishSubject
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.InclinationMeasurement
import java.lang.IllegalArgumentException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AndroidBluetoothSensorAccessor(private val context: Context) : BluetoothSensorAccessor {
    private val connectionStatusSubject = PublishSubject<ConnectionStatus>()
    override val connectionStatus: Observable<ConnectionStatus> = this.connectionStatusSubject

    private val inclinationMeasurementSubject = PublishSubject<InclinationMeasurement>()
    override val inclinationMeasurements: Observable<InclinationMeasurement> = this.inclinationMeasurementSubject

    private var bluetoothLEService: BluetoothLEService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            println("BluetoothLE service connected!")
            this@AndroidBluetoothSensorAccessor.bluetoothLEService =
                (service as BluetoothLEService.LocalBinder).getService()
            this@AndroidBluetoothSensorAccessor.bluetoothLEService?.initialise()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            println("BluetoothLE service disconnected!")
            this@AndroidBluetoothSensorAccessor.bluetoothLEService = null
        }
    }

    init {
        val bluetoothLEServiceIntent = Intent(this.context, BluetoothLEService::class.java)
        val serviceBound = this.context.bindService(
            bluetoothLEServiceIntent,
            this.serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        println("Has the BluetoothLE service successfully been bound? $serviceBound")
    }

    override fun connect(identifier: String) {
        this.context.registerReceiver(this.gattUpdateReceiver, this.createGATTUpdateFilter())
        this@AndroidBluetoothSensorAccessor.bluetoothLEService?.let { service ->
            service.connect(identifier)
        }
    }

    override fun disconnect() {
        try {
            this.context.unregisterReceiver(this.gattUpdateReceiver)
        } catch (exception: IllegalArgumentException) {
            System.err.println(exception)
        }
        try {
            this.context.unbindService(this.serviceConnection)
        } catch (exception: IllegalArgumentException) {
            System.err.println(exception)
        }
    }

    private val gattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLEService.ACTION_GATT_CONNECTED -> {
                    this@AndroidBluetoothSensorAccessor.connectionStatusSubject.onNext(ConnectionStatus.CONNECTED)
                }
                BluetoothLEService.ACTION_GATT_DISCONNECTED -> {
                    this@AndroidBluetoothSensorAccessor.connectionStatusSubject.onNext(ConnectionStatus.DISCONNECTED)
                }
            }
        }
    }

    private fun createGATTUpdateFilter(): IntentFilter {
        return IntentFilter().apply {
            this.addAction(BluetoothLEService.ACTION_GATT_CONNECTED)
            this.addAction(BluetoothLEService.ACTION_GATT_DISCONNECTED)
        }
    }

    override suspend fun fetchPressure(): Double {
        return this.fetchDoubleValue(
            ServiceUUIDs.ENVIRONMENTAL_SENSING,
            ServiceUUIDs.EnvironmentalSensingCharacteristicUUIDs.PRESSURE,
            floatingPointDivisor = 10
        )
    }

    private suspend fun fetchDoubleValue(serviceUUID: String, characteristicUUID: String, floatingPointDivisor: Int = 1): Double = suspendCoroutine { continuation ->
        val gattValueReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                context.unregisterReceiver(this)

                val value = (
                        intent.getDoubleExtra(BluetoothLEService.EXTRA_GATT_FLOAT_VALUE, 0.0)
                        / floatingPointDivisor
                )
                continuation.resume(value)
            }
        }

        this.context.registerReceiver(gattValueReceiver, this.createGATTReadFilter())
        this.bluetoothLEService?.readCharacteristic(serviceUUID, characteristicUUID)
    }

    private fun createGATTReadFilter() = IntentFilter().apply {
        this.addAction(BluetoothLEService.ACTION_GATT_READ)
    }

    override suspend fun fetchTemperature(): Double {
        return this.fetchDoubleValue(
            ServiceUUIDs.ENVIRONMENTAL_SENSING,
            ServiceUUIDs.EnvironmentalSensingCharacteristicUUIDs.TEMPERATURE
        )
    }

    override suspend fun fetchHumidity(): Double {
        return this.fetchDoubleValue(
            ServiceUUIDs.ENVIRONMENTAL_SENSING,
            ServiceUUIDs.EnvironmentalSensingCharacteristicUUIDs.HUMIDITY
        )
    }

    override suspend fun startInclinationMeasuring() {
        val intentFilter = IntentFilter().apply {
            this.addAction(BluetoothLEService.ACTION_GATT_NOTIFICATION)
        }
        this.context.registerReceiver(this.inclinationMeasurementHandler, intentFilter)
        this.bluetoothLEService?.setCharacteristicNotification(
            ServiceUUIDs.INCLINATION_SERVICE,
            ServiceUUIDs.InclinationCharacteristicUUIDs.INCLINATION
        )
    }

    private val inclinationMeasurementHandler = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val counter = intent.getIntExtra(BluetoothLEService.EXTRA_GATT_COUNTER_VALUE, -1)
            val inclination = intent.getDoubleExtra(BluetoothLEService.EXTRA_GATT_FLOAT_VALUE, 0.0)
            this@AndroidBluetoothSensorAccessor.inclinationMeasurementSubject.onNext(
                InclinationMeasurement(counter,  inclination)
            )
        }
    }

    override suspend fun stopInclinationMeasuring() {
        this.context.unregisterReceiver(this.inclinationMeasurementHandler)
        this.bluetoothLEService?.cancelCharacteristicNotification(
            ServiceUUIDs.INCLINATION_SERVICE,
            ServiceUUIDs.InclinationCharacteristicUUIDs.INCLINATION
        )
    }

    override suspend fun startBlinking() {
        TODO("Not yet implemented")
    }

    override suspend fun stopBlinking() {
        TODO("Not yet implemented")
    }
}