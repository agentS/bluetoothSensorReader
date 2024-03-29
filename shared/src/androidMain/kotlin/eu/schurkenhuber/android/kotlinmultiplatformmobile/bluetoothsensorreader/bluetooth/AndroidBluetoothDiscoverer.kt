package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.badoo.reaktive.observable.publish
import com.badoo.reaktive.subject.Subject
import com.badoo.reaktive.subject.publish.PublishSubject
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.model.BluetoothDeviceInformation

class AndroidBluetoothDiscoverer(context: Context) : BluetoothDiscoverer {
    private val bluetoothAdapter: BluetoothAdapter
    private val bluetoothScanner: BluetoothLeScanner
    private var scanning: Boolean = false
    private val onDeviceDiscoveredSubject: Subject<BluetoothDeviceInformation> = PublishSubject()
    override val onDeviceDiscovered = onDeviceDiscoveredSubject

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        this.bluetoothAdapter = bluetoothManager.adapter
        this.bluetoothScanner = this.bluetoothAdapter.bluetoothLeScanner
    }

    override fun startDiscovery() {
        if (!this.scanning) {
            this.scanning = true
            println("Starting BluetoothLE discovery.")
            this.bluetoothScanner.startScan(this.bluetoothLEScanCallback)
        }
    }

    override fun stopDiscovery() {
        if (this.scanning) {
            println("Stopping BluetoothLE discovery.")
            this.bluetoothScanner.stopScan(this.bluetoothLEScanCallback)
            this.scanning = false
        }
    }

    private val bluetoothLEScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result != null) {
                this@AndroidBluetoothDiscoverer.onDeviceDiscoveredSubject.onNext(
                    BluetoothDeviceInformation(
                        result.device.address,
                        result.device.name ?: result.device.address,
                        result.rssi,
                        result.device.address
                    )
                )

            }
        }
    }
}