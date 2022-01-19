package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth

import com.badoo.reaktive.observable.Observable
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.model.BluetoothDeviceInformation

interface BluetoothDiscoverer {
    val onDeviceDiscovered: Observable<BluetoothDeviceInformation>
    fun startDiscovery()
    fun stopDiscovery()
}
