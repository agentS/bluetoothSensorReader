package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application

import com.badoo.reaktive.disposable.Disposable
import com.badoo.reaktive.observable.ObservableObserver
import com.badoo.reaktive.observable.subscribe
import com.badoo.reaktive.subject.Subject
import com.badoo.reaktive.subject.publish.PublishSubject
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.BluetoothDiscoverer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class BluetoothSensorDiscoveryState(
    val scanning: Boolean,
    val discoveredDevices: Set<String>
) : State

sealed class BluetoothSensorDiscoveryAction : Action {
    data class DiscoverDevices(val forceReload: Boolean) : BluetoothSensorDiscoveryAction()
    data class StopDiscovery(val force: Boolean) : BluetoothSensorDiscoveryAction()
    data class DeviceDiscovered(val address: String) : BluetoothSensorDiscoveryAction()
}

sealed class BluetoothSensorDiscoverySideEffect : Effect {}

class BluetoothSensorDiscoveryStore(private val bluetoothDiscoverer: BluetoothDiscoverer)
    : Store<BluetoothSensorDiscoveryState, BluetoothSensorDiscoveryAction, BluetoothSensorDiscoverySideEffect> {
    private val state = MutableStateFlow(BluetoothSensorDiscoveryState(
        scanning = false,
        discoveredDevices = emptySet()
    ))
    private val sideEffect = MutableSharedFlow<BluetoothSensorDiscoverySideEffect>()

    init {
        this.bluetoothDiscoverer.onDeviceDiscovered.subscribe(
            isThreadLocal = false,
            onNext = this::onBluetoothDeviceDiscovered
        )
    }

    override fun observeState(): StateFlow<BluetoothSensorDiscoveryState> = this.state

    override fun observeSideEffect(): Flow<BluetoothSensorDiscoverySideEffect> = this.sideEffect

    override fun dispatch(action: BluetoothSensorDiscoveryAction) {
        val previousState = this.state.value

        val nextState = when (action) {
            is BluetoothSensorDiscoveryAction.DiscoverDevices ->
                if (!previousState.scanning) {
                    this.startBluetoothDiscovery()
                    previousState.copy(scanning = true, discoveredDevices = emptySet())
                } else {
                    previousState
                }
            is BluetoothSensorDiscoveryAction.StopDiscovery ->
                if (previousState.scanning) {
                    this.stopBluetoothDiscovery()
                    previousState.copy(scanning = false)
                } else {
                    previousState
                }
            is BluetoothSensorDiscoveryAction.DeviceDiscovered ->
                previousState.copy(
                    discoveredDevices = previousState.discoveredDevices + action.address // setOf(*previousState.discoveredDevices.toTypedArray(), action.address)
                )
        }

        if (nextState != previousState) {
            println("switching to state $nextState from $previousState")
            this.state.value = nextState
        }
    }

    private fun startBluetoothDiscovery() {
        this.bluetoothDiscoverer.startDiscovery()
    }

    private fun onBluetoothDeviceDiscovered(address: String) {
        this.dispatch(BluetoothSensorDiscoveryAction.DeviceDiscovered(address))
    }

    private fun stopBluetoothDiscovery() {
        this.bluetoothDiscoverer.stopDiscovery()
    }
}
