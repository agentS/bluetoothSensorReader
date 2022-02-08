import SwiftUI
import shared

@main
class iOSApp: App {
    let store: ObservableBluetoothSensorDiscoveryStore
    
    required init() {
        let bluetoothAccessor = IOSBluetoothUtilities.companion.createIOSBluetoothAccessor()
        self.store = ObservableBluetoothSensorDiscoveryStore(store: BluetoothSensorDiscoveryStore(
            bluetoothDiscoverer: IOSBluetoothDiscovererAdapter(iosBluetoothAccessor: bluetoothAccessor),
            bluetoothSensorAccessor: IOSBluetoothSensorAccessorAdapter(iosBluetoothAccessor: bluetoothAccessor)
        ))
    }
    
	var body: some Scene {
		WindowGroup {
            RootView().environmentObject(self.store)
		}
	}
}

class ObservableBluetoothSensorDiscoveryStore : ObservableObject {
    @Published public var reduxState: BluetoothSensorDiscoveryState = BluetoothSensorDiscoveryState(
        scanning: false,
        connectionStatus: .disconnected,
        discoveredDevices: [:],
        connectedDevice: nil,
        environmentReadings: EnvironmentReadings(pressure: 0.0, humidity: 0.0, temperature: 0.0),
        measuringInclination: false,
        inclination: InclinationMeasurement(counter: 0, inclination: 0.0)
    )
    @Published public var sideEffect: BluetoothSensorDiscoverySideEffect?
    
    private let store: BluetoothSensorDiscoveryStore
    private var stateWatcher: Closeable?
    private var sideEffectWatcher: Closeable?
    
    init(store: BluetoothSensorDiscoveryStore) {
        self.store = store
        self.stateWatcher = self.store.watchState().watch { [weak self] reduxState in
            self?.reduxState = reduxState
        }
        self.sideEffectWatcher = self.store.watchSideEffect().watch { [weak self] sideEffect in
            self?.sideEffect = sideEffect
        }
    }
    
    public func dispatch(action: BluetoothSensorDiscoveryAction) {
        self.store.dispatch(action: action)
    }
    
    deinit {
        self.stateWatcher?.close()
        self.sideEffectWatcher?.close()
    }
}

public typealias DispatchFunction = (BluetoothSensorDiscoveryAction) -> ()

public protocol ConnectedView : View {
    associatedtype Props
    associatedtype V : View
    
    func map(reduxState: BluetoothSensorDiscoveryState, dispatch: @escaping DispatchFunction) -> Props
    func body(properties: Props) -> V
}

public extension ConnectedView {
    func render(reduxState: BluetoothSensorDiscoveryState, dispatch: @escaping DispatchFunction) -> V {
        let properties = map(reduxState: reduxState, dispatch: dispatch)
        return body(properties: properties)
    }
    
    var body: StoreConnector<V> {
        return StoreConnector(content: render)
    }
}

public struct StoreConnector<V: View>: View {
    @EnvironmentObject var store: ObservableBluetoothSensorDiscoveryStore
    let content: (BluetoothSensorDiscoveryState, @escaping DispatchFunction) -> V
    
    public var body: V {
        return content(store.reduxState, store.dispatch)
    }
}
