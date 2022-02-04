package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth

import platform.CoreBluetooth.CBCentralManager
import com.badoo.reaktive.observable.Observable
import com.badoo.reaktive.subject.Subject
import com.badoo.reaktive.subject.publish.PublishSubject
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.model.BluetoothDeviceInformation
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBPeripheral
import platform.Foundation.NSNumber
import platform.darwin.NSObject

// see https://iqcode.com/code/other/implement-swift-protocol-in-kotlin
class IOSBluetoothDiscoverer : NSObject(), CBCentralManagerDelegateProtocol {
    private val centralManager = CBCentralManager(delegate = this, queue = null)

    private val onDeviceDiscoveredSubject: Subject<BluetoothDeviceInformation> = PublishSubject()
    val onDeviceDiscovered: Observable<BluetoothDeviceInformation> = this.onDeviceDiscoveredSubject

    fun startDiscovery() {
        this.centralManager.scanForPeripheralsWithServices(serviceUUIDs = null, options = null)
    }

    override fun centralManager(
        central: CBCentralManager,
        didDiscoverPeripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        RSSI: NSNumber
    ) {
        println("Discovered peripheral: ${didDiscoverPeripheral.name} (UUID: ${didDiscoverPeripheral.identifier}) (signal strength: ${RSSI})")
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

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        println("centralManagerDidUpdateState called")
    }
}