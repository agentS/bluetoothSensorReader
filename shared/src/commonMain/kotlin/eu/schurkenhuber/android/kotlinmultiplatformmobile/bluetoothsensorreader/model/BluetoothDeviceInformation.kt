package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.model

data class BluetoothDeviceInformation(
    val identifier: String,
    val name: String,
    val rssi: Int,
    val macAddress: String?
)
