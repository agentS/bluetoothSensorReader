import SwiftUI
import shared

struct RootView: ConnectedView {
    struct Props {
        let selectedDevice: BluetoothDeviceInformation?
        let registeredDevices: [Device]
        let measurementRecords: [shared.Measurement]
    }
    
    func map(reduxState: BluetoothSensorDiscoveryState, dispatch: @escaping DispatchFunction) -> Props {
        return Props(
            selectedDevice: reduxState.connectedDevice,
            registeredDevices: reduxState.registeredDevices,
            measurementRecords: reduxState.measurementRecords
        )
    }
    
    func body(properties: Props) -> some View {
        VStack {
            if let selectedDevice = properties.selectedDevice {
                SensorReadingView(device: selectedDevice)
            } else if properties.measurementRecords.count > 0 {
                ArchiveMeasurementView()
            } else if properties.registeredDevices.count > 0 {
                ArchiveView()
            } else {
                DiscoveryView()
            }
        }
	}
}

struct RootView_Previews: PreviewProvider {
	static var previews: some View {
		RootView()
	}
}
