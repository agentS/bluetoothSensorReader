package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import java.lang.RuntimeException
import java.util.*
import kotlin.IllegalArgumentException


class BluetoothLEService : Service() {
    companion object {
        const val ACTION_GATT_CONNECTED = "eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_READ = "eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.ACTION_GATT_READ"
        const val EXTRA_GATT_FLOAT_VALUE = "eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.EXTRA_GATT_FLOAT_VALUE"
        const val ACTION_GATT_NOTIFICATION = "eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.ACTION_GATT_NOTIFICATION"
        const val EXTRA_GATT_COUNTER_VALUE = "eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.EXTRA_GATT_COUNTER_VALUE"
        private val NOTIFICATION_CHARACTERISTIC_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
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
//                    for (service in gatt.services) {
//                        println("service UUID: ${service.uuid}")
//                        for (characteristic in service.characteristics) {
//                            println("characteristic UUID: ${characteristic.uuid}")
//                        }
//                    }
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

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            if (characteristic != null) {
                val intent = Intent(ACTION_GATT_NOTIFICATION)
                intent.putExtra(EXTRA_GATT_COUNTER_VALUE, characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0))
                intent.putExtra(EXTRA_GATT_FLOAT_VALUE, characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 2).toDouble())
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
            val characteristic = this.lookupCharacteristic(serviceUUID, characteristicUUID)
            if (!gatt.readCharacteristic(characteristic)) {
                throw IllegalStateException("Could not read characteristic ${characteristicUUID} of service ${serviceUUID}.")
            }
        }
    }

    private fun lookupCharacteristic(serviceUUID: String, characteristicUUID: String): BluetoothGattCharacteristic {
        this.services?.let { services ->
            val service = services.find { service -> serviceUUID == service.uuid.toString() }
            if (service != null) {
                val characteristic =
                    service.characteristics.find { characteristic -> characteristicUUID == characteristic.uuid.toString() }
                if (characteristic != null) {
                    return characteristic
                }
                else {
                    throw IllegalArgumentException("The service ${service.uuid} does not have a characteristic with UUID ${characteristicUUID}.")
                }
            } else {
                throw IllegalArgumentException("The GATT server does not support a service with UUID ${serviceUUID}.")
            }
        }
        throw IllegalArgumentException("Service discovery has not been performed yet.")
    }

    fun setCharacteristicNotification(serviceUUID: String, characteristicUUID: String) {
        val characteristic = this.lookupCharacteristic(serviceUUID, characteristicUUID)
        if (!this.toggleCharacteristicNotificationStatus(characteristic, enabled = true)) {
            throw IllegalStateException("Could not setup a notification for characteristic $characteristicUUID of service $serviceUUID.")
        }
    }

    private fun toggleCharacteristicNotificationStatus(characteristic: BluetoothGattCharacteristic, enabled: Boolean): Boolean {
        this.bluetoothGATT?.let { gatt ->
            val notificationDescriptor = characteristic.getDescriptor(NOTIFICATION_CHARACTERISTIC_UUID)
            val notificationDescriptorTargetValue = if (enabled) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            notificationDescriptor.value = notificationDescriptorTargetValue
            gatt.writeDescriptor(notificationDescriptor)
            return gatt.setCharacteristicNotification(characteristic, enabled)
        }
        throw IllegalStateException("The BluetoothLE connection has not been established yet.")
    }

    fun cancelCharacteristicNotification(serviceUUID: String, characteristicUUID: String) {
        val characteristic = this.lookupCharacteristic(serviceUUID, characteristicUUID)
        if (!this.toggleCharacteristicNotificationStatus(characteristic, enabled = false)) {
            throw IllegalStateException("Could not teardown a notification for characteristic $characteristicUUID of service $serviceUUID.")
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        this.close()
//        println("Good Bye Baby! - The Big Bopper")
        return super.onUnbind(intent)
    }

    private fun close() {
        bluetoothGATT?.let { gatt ->
            gatt.close()
            this@BluetoothLEService.bluetoothGATT = null
        }
    }
}
