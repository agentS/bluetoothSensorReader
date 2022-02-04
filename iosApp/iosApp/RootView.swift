import SwiftUI
import shared

struct RootView: View {
    @EnvironmentObject var store: ObservableBluetoothSensorDiscoveryStore
    
	var body: some View {
        DiscoveryView()
	}
}

struct RootView_Previews: PreviewProvider {
	static var previews: some View {
		RootView()
	}
}
