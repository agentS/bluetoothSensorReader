package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth

interface BluetoothDiscoverer {
    fun startDiscovery(onDeviceDiscovered: (String) -> Unit)
    fun stopDiscovery()
}
