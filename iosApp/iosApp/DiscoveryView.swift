//
//  DiscoveryView.swift
//  iosApp
//
//  Created by Lukas Schörghuber on 04.02.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI
import shared

// TODO: replace with an extension to the class `BluetoothDeviceInformation` which implements the protocol `Identifiable`
// this should be fairly easy
struct IOSBluetoothDeviceInformation : Identifiable {
    let identifier: String
    let name: String
    let rssi: Int
    
    var id: String { self.identifier }
    
    init(
        identifier: String,
        name: String,
        rssi: Int
    ) {
        self.identifier = identifier
        self.name = name
        self.rssi = rssi
    }
}
// end TODO

struct DiscoveryView: ConnectedView {
    struct Props {
        let discovering: Bool
        let discoveredDevices: [IOSBluetoothDeviceInformation]
        
        let onStartDiscovery: () -> Void
        let onStopDiscovery: () -> Void
    }
    
    func map(state: BluetoothSensorDiscoveryState, dispatch: @escaping DispatchFunction) -> Props {
        return Props(
            discovering: state.scanning,
            discoveredDevices: Array(
                state.discoveredDevices.values.map { device in
                    IOSBluetoothDeviceInformation(identifier: device.identifier, name: device.name, rssi: Int(device.rssi))
                }
            ).sorted(by: { leftHandSide, rightHandSide in
                rightHandSide.rssi < leftHandSide.rssi
            }),
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
