package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth

import com.badoo.reaktive.observable.Observable
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.model.BluetoothDeviceInformation

// This class is only required because the class IOSBluetoothDiscoverer depends on Objective-C-specific functionality and Kotlin/Native does not allow a class to both implement an Objective-C protocol as well as a Kotlin interface
class IOSBluetoothDiscovererAdapter(iosBluetoothAccessor: IOSBluetoothAccessor) : BluetoothDiscoverer {
    private val bluetoothSensorAccessor: IOSBluetoothAccessor = iosBluetoothAccessor

    override val onDeviceDiscovered: Observable<BluetoothDeviceInformation> = this.bluetoothSensorAccessor.onDeviceDiscovered

    override fun startDiscovery() {
        this.bluetoothSensorAccessor.startDiscovery()
    }

    override fun stopDiscovery() {
        this.bluetoothSensorAccessor.stopDiscovery()
    }
}
