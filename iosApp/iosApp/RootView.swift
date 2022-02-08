import SwiftUI
import shared

struct RootView: ConnectedView {
    struct Props {
        let selectedDevice: BluetoothDeviceInformation?
    }
    
    func map(reduxState: BluetoothSensorDiscoveryState, dispatch: @escaping DispatchFunction) -> Props {
        return Props(selectedDevice: reduxState.connectedDevice)
    }
    
    func body(properties: Props) -> some View {
        VStack {
            if let selectedDevice = properties.selectedDevice {
                SensorReadingView(device: selectedDevice)
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
