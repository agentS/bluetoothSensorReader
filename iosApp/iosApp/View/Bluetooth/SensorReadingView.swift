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
        let measuringInclination: Bool
        let inclination: InclinationMeasurement
        let environmentReadings: EnvironmentReadings
        
        let onStartInclinationMeasuring: () -> Void
        let onStopInclinationMeasuring: () -> Void
        let onFetchEnvironmentReadings: () -> Void
        let onDisconnect: () -> Void
    }
    
    func map(reduxState: BluetoothSensorDiscoveryState, dispatch: @escaping DispatchFunction) -> Props {
        return Props(
            connectionState: reduxState.connectionStatus,
            measuringInclination: reduxState.measuringInclination,
            inclination: reduxState.inclination,
            environmentReadings: reduxState.environmentReadings,
            
            onStartInclinationMeasuring: {
                dispatch(BluetoothSensorDiscoveryAction.StartInclinationMeasuring(force: false))
            },
            onStopInclinationMeasuring: {
                dispatch(BluetoothSensorDiscoveryAction.StopInclinationMeasuring(force: true))
            },
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
            if properties.connectionState == .connected {
                if !properties.measuringInclination {
                    Button("Start inclination measurement", action: properties.onStartInclinationMeasuring)
                } else {
                    Button("Stop inclination measurement", action: properties.onStopInclinationMeasuring)
                }
                Text("Counter: \(properties.inclination.counter), Inclination: \(properties.inclination.inclination)")
                
                Button("Fetch environment readings", action: properties.onFetchEnvironmentReadings)
                List() {
                    Text("Temperature: \(properties.environmentReadings.temperature) °C")
                    Text("Humidity: \(properties.environmentReadings.humidity) %")
                    Text("Air pressure: \(properties.environmentReadings.pressure) hPa")
                }.listStyle(.inset)
                
                Button("Disconnect", action: properties.onDisconnect)
            } else if properties.connectionState == .connecting {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle())
            }
        }
    }
}
