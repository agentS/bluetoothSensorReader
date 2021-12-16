package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application

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

    override fun observeState(): StateFlow<BluetoothSensorDiscoveryState> = this.state

    override fun observeSideEffect(): Flow<BluetoothSensorDiscoverySideEffect> = this.sideEffect

    override fun dispatch(action: BluetoothSensorDiscoveryAction) {
        val previousState = this.state.value

        val nextState = when (action) {
            is BluetoothSensorDiscoveryAction.DiscoverDevices ->
                if (!previousState.scanning) {
                    this.startBluetoothDiscovery()
                    previousState.copy(scanning = true)
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
        this.bluetoothDiscoverer.startDiscovery(this::onBluetoothDeviceDiscovered)
    }

    private fun onBluetoothDeviceDiscovered(address: String) {
        this.dispatch(BluetoothSensorDiscoveryAction.DeviceDiscovered(address))
    }

    private fun stopBluetoothDiscovery() {

    }
}
