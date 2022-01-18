package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth

import com.badoo.reaktive.observable.Observable

interface BluetoothDiscoverer {
    val onDeviceDiscovered: Observable<String>
    fun startDiscovery()
    fun stopDiscovery()
}
