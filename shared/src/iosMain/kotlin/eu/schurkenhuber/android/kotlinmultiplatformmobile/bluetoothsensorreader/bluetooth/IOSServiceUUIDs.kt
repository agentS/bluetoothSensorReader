package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth

object ServiceUUIDs {
    const val ENVIRONMENTAL_SENSING = "181A"
    object EnvironmentalSensingCharacteristicUUIDs {
        const val PRESSURE = "2A6D"
        const val HUMIDITY = "2A6F"
        const val TEMPERATURE = "2A6E"
    }
    const val INCLINATION_SERVICE = "1815"
    object InclinationCharacteristicUUIDs {
        const val INCLINATION = "A3E0C307-18B3-4FE6-BB2B-102FEB860BAE"
    }
}

enum class BluetoothLEServices {
    ENVIRONMENTAL_SENSING,
    AUTOMATION
}
