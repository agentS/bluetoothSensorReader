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
        let onConnectToDevice: (BluetoothDeviceInformation) -> Void
        let onFetchRegisteredDevices: () -> Void
    }
    
    func map(reduxState: BluetoothSensorDiscoveryState, dispatch: @escaping DispatchFunction) -> Props {
        return Props(
            discovering: reduxState.scanning,
            discoveredDevices: Array(reduxState.discoveredDevices.values)
                .sorted(by: { leftHandSide, rightHandSide in
                    rightHandSide.rssi < leftHandSide.rssi
                }
            ),
            onStartDiscovery: { dispatch(BluetoothSensorDiscoveryAction.DiscoverDevices(forceReload: false)) },
            onStopDiscovery: { dispatch(BluetoothSensorDiscoveryAction.StopDiscovery(force: true)) },
            onConnectToDevice: { deviceInformation in
                dispatch(BluetoothSensorDiscoveryAction.ConnectToSensor(deviceInformation: deviceInformation))
            },
            onFetchRegisteredDevices: { dispatch(BluetoothSensorDiscoveryAction.LoadRegisteredDevices(force: false)) }
        )
    }
    
    func body(properties: Props) -> some View {
        VStack {
            Button("Archive", action: properties.onFetchRegisteredDevices)
                .padding(.top, 12)
            if !properties.discovering {
                Button("Start discovery", action: properties.onStartDiscovery)
            } else {
                Button("Stop discovery", action: properties.onStopDiscovery)
            }
            List(properties.discoveredDevices) { device in
                Button("\(device.name) (RSSI = \(device.rssi))") {
                    properties.onConnectToDevice(device)
                }
            }
        }
    }
}

struct DiscoveryView_Previews: PreviewProvider {
    static var previews: some View {
        DiscoveryView()
    }
}
