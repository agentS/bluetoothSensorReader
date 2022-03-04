package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.dal

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.BluetoothSensorAccessorDatabase

actual class DriverFactory {
    actual fun createDriver(): SqlDriver = NativeSqliteDriver(
        BluetoothSensorAccessorDatabase.Schema, "bluetoothSensorAccessor.db"
    )
}