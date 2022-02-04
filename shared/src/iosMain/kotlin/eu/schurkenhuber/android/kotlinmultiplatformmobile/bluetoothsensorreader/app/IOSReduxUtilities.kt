package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.app

import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.BluetoothSensorDiscoveryStore

fun BluetoothSensorDiscoveryStore.watchState() = this.observeState().wrap()
fun BluetoothSensorDiscoveryStore.watchSideEffect() = this.observeSideEffect().wrap()
