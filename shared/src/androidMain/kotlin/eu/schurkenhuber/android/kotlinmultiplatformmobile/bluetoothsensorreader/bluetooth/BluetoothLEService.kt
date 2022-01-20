package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import java.lang.RuntimeException
import kotlin.IllegalArgumentException

class BluetoothLEService : Service() {
    companion object {
        const val ACTION_GATT_CONNECTED = "eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_READ = "eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.ACTION_GATT_READ"
        const val EXTRA_GATT_FLOAT_VALUE = "eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.EXTRA_GATT_FLOAT_VALUE"
    }

    private val binder = LocalBinder()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGATT: BluetoothGatt? = null
//    private var connectionState = BluetoothProfile.STATE_DISCONNECTED
    private var services: List<BluetoothGattService>? = null

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
        } ?: run {
            throw RuntimeException("The Bluetooth adapter has not been initialised yet.")
        }
    }

    private val bluetoothGATTCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    this@BluetoothLEService.bluetoothGATT?.let { gatt ->
                        gatt.discoverServices()
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
//                    this@BluetoothLEService.connectionState = BluetoothProfile.STATE_DISCONNECTED
                    this@BluetoothLEService.broadcastUpdate(ACTION_GATT_DISCONNECTED)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                this@BluetoothLEService.connectionState = BluetoothProfile.STATE_CONNECTED
                this@BluetoothLEService.bluetoothGATT?.let { gatt ->
                    this@BluetoothLEService.services = gatt.services
                    for (service in gatt.services) {
                        if (service.uuid.toString() == ServiceUUIDs.ENVIRONMENTAL_SENSING) {
                            for (characteristic in service.characteristics) {
                                println("UUID: ${characteristic.uuid}")
                            }
                        }
                    }
                }
                this@BluetoothLEService.broadcastUpdate(ACTION_GATT_CONNECTED)
            } else {
//                this@BluetoothLEService.connectionState = BluetoothProfile.STATE_DISCONNECTED
                this@BluetoothLEService.broadcastUpdate(ACTION_GATT_DISCONNECTED)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                val intent = Intent(ACTION_GATT_READ)
                val value = try {
                    characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 0).toDouble()
                } catch (exception: NullPointerException) {
                    characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0).toDouble() / 100
                }
                intent.putExtra(EXTRA_GATT_FLOAT_VALUE, value)
                this@BluetoothLEService.sendBroadcast(intent)
            }
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        this.sendBroadcast(intent)
    }

    fun readCharacteristic(serviceUUID: String, characteristicUUID: String) {
        this.bluetoothGATT?.let { gatt ->
            this.services?.let { services ->
                val service = services.find { service -> serviceUUID == service.uuid.toString() }
                if (service != null) {
                    val characteristic = service.characteristics.find { characteristic -> characteristicUUID == characteristic.uuid.toString() }
                    if (characteristic != null) {
                        if (!gatt.readCharacteristic(characteristic)) {
                            throw IllegalStateException("Could not read characteristic ${characteristic.uuid} of service ${service.uuid}.")
                        }
                    } else {
                        throw IllegalArgumentException("The service ${service.uuid} does not have a characteristic with UUID ${characteristicUUID}.")
                    }
                } else {
                    throw IllegalArgumentException("The GATT server does not support a service with UUID ${serviceUUID}.")
                }
            }
        }
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
