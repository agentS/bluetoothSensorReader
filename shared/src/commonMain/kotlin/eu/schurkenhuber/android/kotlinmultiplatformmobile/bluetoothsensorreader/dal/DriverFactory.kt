package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.dal

import com.squareup.sqldelight.db.SqlDriver
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.BluetoothSensorAccessorDatabase

expect class DriverFactory {
    fun createDriver() : SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): BluetoothSensorAccessorDatabase {
    val driver = driverFactory.createDriver()
    return BluetoothSensorAccessorDatabase(driver)
}
