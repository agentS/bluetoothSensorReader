package eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.android

import android.app.Application
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.application.BluetoothSensorDiscoveryStore
import eu.schurkenhuber.android.kotlinmultiplatformmobile.bluetoothsensorreader.bluetooth.AndroidBluetoothDiscoverer
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class App : Application() {
    private val applicationModule = module {
        single { BluetoothSensorDiscoveryStore(AndroidBluetoothDiscoverer(this.get())) }
    }

    override fun onCreate() {
        super.onCreate()
        this.initKoin()
    }

    private fun initKoin() {
        startKoin {
            androidContext(this@App)
            modules(applicationModule)
        }
    }
}
