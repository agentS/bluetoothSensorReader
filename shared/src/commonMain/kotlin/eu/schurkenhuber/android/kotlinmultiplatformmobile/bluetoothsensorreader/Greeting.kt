package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader

class Greeting {
    fun greeting(): String {
        return "Hello, ${Platform().platform}!"
    }
}