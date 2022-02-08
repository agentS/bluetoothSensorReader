//
//  SensorReadingView.swift
//  iosApp
//
//  Created by Lukas Schörghuber on 07.02.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI
import shared

struct SensorReadingView : ConnectedView {
    struct Props {
        let connectionState: ConnectionStatus
        let environmentReadings: EnvironmentReadings
        
        let onFetchEnvironmentReadings: () -> Void
        let onDisconnect: () -> Void
    }
    
    func map(reduxState: BluetoothSensorDiscoveryState, dispatch: @escaping DispatchFunction) -> Props {
        return Props(
            connectionState: reduxState.connectionStatus,
            environmentReadings: reduxState.environmentReadings,
            
            onFetchEnvironmentReadings: {
                dispatch(BluetoothSensorDiscoveryAction.FetchEnvironmentReadings(force: false))
            },
            onDisconnect: {
                dispatch(BluetoothSensorDiscoveryAction.DisconnectFromSensor(connectionStatus: ConnectionStatus.connected))
            }
        )
    }
    
    private let device: BluetoothDeviceInformation
    
    init(device: BluetoothDeviceInformation) {
        self.device = device
    }
    
    func body(properties: Props) -> some View {
        VStack {
            Text("Connection status: \(properties.connectionState)")
            if (properties.connectionState == .connected) {
                Button("Fetch environment readings", action: properties.onFetchEnvironmentReadings)
                List() {
                    Text("Temperature: \(properties.environmentReadings.temperature)")
                    Text("Humidity: \(properties.environmentReadings.humidity)")
                    Text("Air pressure: \(properties.environmentReadings.pressure)")
                }
                
                Button("Disconnect", action: properties.onDisconnect)
            } else if (properties.connectionState == .connecting) {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle())
            }
        }
    }
}
