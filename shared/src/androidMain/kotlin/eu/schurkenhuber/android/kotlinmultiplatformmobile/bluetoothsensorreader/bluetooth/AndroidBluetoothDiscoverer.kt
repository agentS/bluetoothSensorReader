package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context

class AndroidBluetoothDiscoverer(context: Context) : BluetoothDiscoverer {
    private val bluetoothAdapter: BluetoothAdapter
    private val bluetoothScanner: BluetoothLeScanner
    private var scanning: Boolean = false
    private var onDeviceDiscovered: ((String) -> Unit)? = null

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        this.bluetoothAdapter = bluetoothManager.adapter
        this.bluetoothScanner = this.bluetoothAdapter.bluetoothLeScanner
    }

    override fun startDiscovery(onDeviceDiscovered: (String) -> Unit) {
        this.onDeviceDiscovered = onDeviceDiscovered
        if (!this.scanning) {
            this.scanning = true
            println("Starting BluetoothLE discovery.")
            this.bluetoothScanner.startScan(this.bluetoothLEScanCallback)
        }
    }

    override fun stopDiscovery() {
        if (this.scanning) {
            println("Stopping BluetoothLE discovery.")
            this.onDeviceDiscovered = null
            this.bluetoothScanner.stopScan(this.bluetoothLEScanCallback)
            this.scanning = false
        }
    }

    private val bluetoothLEScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result != null) {
                this@AndroidBluetoothDiscoverer.onDeviceDiscovered?.invoke(result.device.address)
            }
        }
    }
}