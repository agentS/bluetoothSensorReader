package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.databasemodel

object MeasurementTypeIDs {
    const val TEMPERATURE = 0L
    const val HUMIDITY = 1L
    const val ATMOSPHERIC_PRESSURE = 2L

    fun mapToUnit(typeID: Long): String = when (typeID) {
        0L -> "°C"
        1L -> "%"
        2L -> "mbar"
        else -> "unknown unit"
    }
}
