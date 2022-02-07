//
//  DiscoveryView.swift
//  iosApp
//
//  Created by Lukas Schörghuber on 04.02.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI
import shared

extension BluetoothDeviceInformation : Identifiable {
    public var id: String { self.identifier }
}

struct DiscoveryView: ConnectedView {
    struct Props {
        let discovering: Bool
        let discoveredDevices: [BluetoothDeviceInformation]
        
        let onStartDiscovery: () -> Void
        let onStopDiscovery: () -> Void
    }
    
    func map(state: BluetoothSensorDiscoveryState, dispatch: @escaping DispatchFunction) -> Props {
        return Props(
            discovering: state.scanning,
            discoveredDevices: Array(state.discoveredDevices.values)
                .sorted(by: { leftHandSide, rightHandSide in
                    rightHandSide.rssi < leftHandSide.rssi
                }
            ),
            onStartDiscovery: { dispatch(BluetoothSensorDiscoveryAction.DiscoverDevices(forceReload: false)) },
            onStopDiscovery: { dispatch(BluetoothSensorDiscoveryAction.StopDiscovery(force: true)) }
        )
    }
    
    func body(properties: Props) -> some View {
        VStack {
            if !properties.discovering {
                Button("Start discovery", action: properties.onStartDiscovery)
            } else {
                Button("Stop discovery", action: properties.onStopDiscovery)
            }
            Spacer()
            Text("Discovered devices")
            Spacer()
            List(properties.discoveredDevices) { device in
                Text("\(device.name) (RSSI = \(device.rssi))")
            }
        }
    }
}

struct DiscoveryView_Previews: PreviewProvider {
    static var previews: some View {
        DiscoveryView()
    }
}
