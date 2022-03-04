package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.dal

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.BluetoothSensorAccessorDatabase

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver = AndroidSqliteDriver(
        BluetoothSensorAccessorDatabase.Schema, this.context, "bluetoothSensorAccessor.db"
    )
}
