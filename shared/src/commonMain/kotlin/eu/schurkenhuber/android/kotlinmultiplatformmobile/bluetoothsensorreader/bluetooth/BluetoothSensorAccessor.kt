package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth

import com.badoo.reaktive.observable.Observable

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    CLOSED
}

interface BluetoothSensorAccessor {
    val connectionStatus: Observable<ConnectionStatus>
    fun connect(identifier: String)
    fun disconnect()
}