package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import java.lang.IllegalArgumentException
import java.lang.RuntimeException

class BluetoothLEService : Service() {
    companion object {
        const val ACTION_GATT_CONNECTED = "eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.ACTION_GATT_DISCONNECTED"
    }

    private val binder = LocalBinder()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGATT: BluetoothGatt? = null
    private var connectionState = BluetoothProfile.STATE_DISCONNECTED

    override fun onBind(intent: Intent?) = binder

    inner class LocalBinder : Binder() {
        fun getService() = this@BluetoothLEService
    }

    fun initialise() {
        val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        this.bluetoothAdapter = bluetoothManager.adapter
        if (this.bluetoothAdapter == null) {
            throw RuntimeException("The Bluetooth adapter is not available.")
        }
    }

    fun connect(address: String) {
        this@BluetoothLEService.bluetoothAdapter?.let { bluetooth ->
            val device = bluetooth.getRemoteDevice(address)
            device.createBond()
            this.bluetoothGATT = device.connectGatt(this, false,  this.bluetoothGATTCallback)
            // println("Connected to ${device.address}, bond state: ${device.bondState}")
        } ?: run {
            throw RuntimeException("The Bluetooth adapter has not been initialised yet.")
        }
    }

    private val bluetoothGATTCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    this@BluetoothLEService.connectionState = BluetoothProfile.STATE_CONNECTED
                    this@BluetoothLEService.broadcastUpdate(ACTION_GATT_CONNECTED)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    this@BluetoothLEService.connectionState = BluetoothProfile.STATE_DISCONNECTED
                    this@BluetoothLEService.broadcastUpdate(ACTION_GATT_DISCONNECTED)
                }
            }
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        this.sendBroadcast(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        this.close()
        println("Good Bye Baby! - The Big Bopper")
        return super.onUnbind(intent)
    }

    private fun close() {
        bluetoothGATT?.let { gatt ->
            gatt.close()
            this@BluetoothLEService.bluetoothGATT = null
        }
    }
}
