package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth

import com.badoo.reaktive.observable.Observable
import com.badoo.reaktive.subject.Subject
import com.badoo.reaktive.subject.publish.PublishSubject
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.InclinationMeasurement
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.model.BluetoothDeviceInformation
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import platform.CoreBluetooth.*
import platform.Foundation.*
import platform.darwin.NSObject
import platform.darwin.Ptr
import platform.posix.memcpy

// see https://iqcode.com/code/other/implement-swift-protocol-in-kotlin
class IOSBluetoothAccessor : NSObject(), CBCentralManagerDelegateProtocol, CBPeripheralDelegateProtocol {
    private val centralManager = CBCentralManager(delegate = this, queue = null)
    private val discoveredDevices: MutableMap<String, CBPeripheral> = mutableMapOf()

    private val onDeviceDiscoveredSubject: Subject<BluetoothDeviceInformation> = PublishSubject()
    val onDeviceDiscovered: Observable<BluetoothDeviceInformation> = this.onDeviceDiscoveredSubject

    private val connectionStatusSubject: Subject<ConnectionStatus> = PublishSubject()
    val connectionStatus: Observable<ConnectionStatus> = this.connectionStatusSubject

    private val inclinationMeasurementsSubject: Subject<InclinationMeasurement> = PublishSubject()
    val inclinationMeasurements: Observable<InclinationMeasurement> = this.inclinationMeasurementsSubject

    private var environmentReadingService: CBService? = null
    private var environmentReadingServiceCharacteristics: MutableMap<String, CBCharacteristic> = mutableMapOf()

    private var automationService: CBService? = null
    private var automationServiceCharacteristics: MutableMap<String, CBCharacteristic> = mutableMapOf()

    private var characteristicReadingChannels: MutableMap<String, Channel<Double>> = mutableMapOf()

    fun startDiscovery() {
        this.centralManager.scanForPeripheralsWithServices(serviceUUIDs = null, options = null)
    }

    override fun centralManager(
        central: CBCentralManager,
        didDiscoverPeripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        RSSI: NSNumber
    ) {
        this.discoveredDevices[didDiscoverPeripheral.identifier.UUIDString] = didDiscoverPeripheral
        this.onDeviceDiscoveredSubject.onNext(
            BluetoothDeviceInformation(
                identifier = didDiscoverPeripheral.identifier.UUIDString,
                name = didDiscoverPeripheral.name ?: didDiscoverPeripheral.identifier.UUIDString,
                rssi = RSSI.intValue,
                macAddress = null // this information is not available on iOS
            )
        )
    }

    fun stopDiscovery() {
        this.centralManager.stopScan()
    }

    fun connect(identifier: String) {
        val peripheral = this.lookupPeripheral(identifier)
        this.centralManager.connectPeripheral(peripheral, options = null)
        this.connectionStatusSubject.onNext(ConnectionStatus.CONNECTING)
    }

    private fun lookupPeripheral(identifier: String): CBPeripheral =
        this.discoveredDevices[identifier] ?: throw RuntimeException("The device with the UUID $identifier has not been discovered yet.")

    override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
        didConnectPeripheral.delegate = this
        didConnectPeripheral.discoverServices(serviceUUIDs = listOf(
            CBUUID.UUIDWithString(ServiceUUIDs.ENVIRONMENTAL_SENSING),
            CBUUID.UUIDWithString(ServiceUUIDs.INCLINATION_SERVICE),
        ))
    }

    override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
        peripheral.services?.let { services ->
            for (service in services) {
                if (service is CBService) {
                    when (service.UUID.UUIDString) {
                        ServiceUUIDs.ENVIRONMENTAL_SENSING -> this.environmentReadingService = service
                        ServiceUUIDs.INCLINATION_SERVICE -> this.automationService = service
                    }
                }
            }
            this.environmentReadingService?.let { service ->
                peripheral.discoverCharacteristics(
                    characteristicUUIDs = listOf(
                        CBUUID.UUIDWithString(ServiceUUIDs.EnvironmentalSensingCharacteristicUUIDs.TEMPERATURE),
                        CBUUID.UUIDWithString(ServiceUUIDs.EnvironmentalSensingCharacteristicUUIDs.HUMIDITY),
                        CBUUID.UUIDWithString(ServiceUUIDs.EnvironmentalSensingCharacteristicUUIDs.PRESSURE),
                    ),
                    forService = service
                )
            }
            this.automationService?.let { service ->
                peripheral.discoverCharacteristics(
                    characteristicUUIDs = listOf(
                        CBUUID.UUIDWithString(ServiceUUIDs.InclinationCharacteristicUUIDs.INCLINATION),
                    ),
                    forService = service
                )
            }
        }
    }

    override fun peripheral(
        peripheral: CBPeripheral,
        didDiscoverCharacteristicsForService: CBService,
        error: NSError?
    ) {
        didDiscoverCharacteristicsForService.characteristics?.let { characteristics ->
            for (characteristic in characteristics) {
                if (characteristic is CBCharacteristic) {
                    val characteristicUUID: String = characteristic.UUID.UUIDString
                    when (didDiscoverCharacteristicsForService.UUID.UUIDString) {
                        ServiceUUIDs.ENVIRONMENTAL_SENSING -> this.environmentReadingServiceCharacteristics[characteristicUUID] = characteristic
                        ServiceUUIDs.INCLINATION_SERVICE -> this.automationServiceCharacteristics[characteristicUUID] = characteristic
                    }
                }
            }
            //println("environment reading service characteristics: ${this.environmentReadingServiceCharacteristics}")
            //println("automation service characteristics: ${this.automationService}")
        }
        this.connectionStatusSubject.onNext(ConnectionStatus.CONNECTED)
    }

    suspend fun readCharacteristic(
        peripheralIdentifier: String,
        characteristicUUID: String
    ): Double = coroutineScope {
        val peripheral = this@IOSBluetoothAccessor.lookupPeripheral(peripheralIdentifier)
        val characteristic = this@IOSBluetoothAccessor.lookupCharacteristic(
            BluetoothLEServices.ENVIRONMENTAL_SENSING,
            characteristicUUID
        )

        val channel = Channel<Double>()
        this@IOSBluetoothAccessor.characteristicReadingChannels[characteristicUUID] = channel

        withContext(Dispatchers.Default) {
            peripheral.readValueForCharacteristic(characteristic)
            val result = channel.receive()
            channel.close()
            result
        }
    }

    override fun peripheral(
        peripheral: CBPeripheral,
        didUpdateValueForCharacteristic: CBCharacteristic,
        error: NSError?
    ) {
        val channel = this.lookupCharacteristicReadChannel(didUpdateValueForCharacteristic.UUID.UUIDString)
        didUpdateValueForCharacteristic.value?.let { data ->
            // fuck this shit: https://github.com/Reedyuk/blue-falcon/blob/master/library/src/iosMain/kotlin/dev/bluefalcon/BluetoothCharacteristic.kt
            val bytes = ByteArray(data.length.toInt()).apply {
                usePinned {
                    memcpy(it.addressOf(0), data.bytes, data.length)
                }
            }
            channel.trySend(bytes.getUShortAt(0).toDouble())
        }
    }

    private fun lookupCharacteristicReadChannel(
        characteristicUUID: String
    ) = this.characteristicReadingChannels[characteristicUUID] ?: throw RuntimeException("No result channel found for characteristic $characteristicUUID.")

    private fun lookupCharacteristic(service: BluetoothLEServices, characteristicUUID: String): CBCharacteristic {
        val characeristicMap = when (service) {
            BluetoothLEServices.ENVIRONMENTAL_SENSING -> this.environmentReadingServiceCharacteristics
            BluetoothLEServices.AUTOMATION -> this.automationServiceCharacteristics
        }
        val characteristic = characeristicMap[characteristicUUID]
        if (characteristic != null) {
            return characteristic
        } else {
            throw RuntimeException("No characteristic with UUID $characteristicUUID discovered for service $service.")
        }
    }

    override fun centralManager(
        central: CBCentralManager,
        didDisconnectPeripheral: CBPeripheral,
        error: NSError?
    ) {
        this.connectionStatusSubject.onNext(ConnectionStatus.DISCONNECTED)
        this.environmentReadingService = null
        this.environmentReadingServiceCharacteristics = mutableMapOf()
        this.automationService = null
        this.automationServiceCharacteristics = mutableMapOf()
        this.characteristicReadingChannels = mutableMapOf()
    }

    fun disconnect(identifier: String) {
        val peripheral = this.lookupPeripheral(identifier)
        this.centralManager.cancelPeripheralConnection(peripheral)
    }

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        println("centralManagerDidUpdateState called")
    }
}
