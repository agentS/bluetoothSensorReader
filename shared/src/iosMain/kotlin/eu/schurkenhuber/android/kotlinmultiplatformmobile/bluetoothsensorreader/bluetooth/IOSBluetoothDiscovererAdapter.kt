package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth

import com.badoo.reaktive.observable.Observable
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.model.BluetoothDeviceInformation

// This class is only required because the class IOSBluetoothDiscoverer depends on Objective-C-specific functionality and Kotlin/Native does not allow a class to both implement an Objective-C protocol as well as a Kotlin interface
class IOSBluetoothDiscovererAdapter : BluetoothDiscoverer {
    private val iosBluetoothDiscoverer: IOSBluetoothDiscoverer

    init {
        this.iosBluetoothDiscoverer = IOSBluetoothDiscoverer()
    }

    override val onDeviceDiscovered: Observable<BluetoothDeviceInformation> = this.iosBluetoothDiscoverer.onDeviceDiscovered

    override fun startDiscovery() {
        this.iosBluetoothDiscoverer.startDiscovery()
    }

    override fun stopDiscovery() {
        this.iosBluetoothDiscoverer.stopDiscovery()
    }
}
