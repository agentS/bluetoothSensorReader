//
//  DiscoveryView.swift
//  iosApp
//
//  Created by Lukas Schörghuber on 04.02.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI
import shared

extension Device : Identifiable {}

struct ArchiveView: ConnectedView {
    struct Props {
        let registeredDevices: [Device]
        
        let onFetchMeasurements: (Int64) -> Void
    }
    
    func map(reduxState: BluetoothSensorDiscoveryState, dispatch: @escaping DispatchFunction) -> Props {
        return Props(
            registeredDevices: reduxState.registeredDevices,
            
            onFetchMeasurements: { deviceID in
                dispatch(BluetoothSensorDiscoveryAction.LoadMeasurementRecords(registeredDeviceID: deviceID))
            }
        )
    }
    
    func body(properties: Props) -> some View {
        List(properties.registeredDevices) { device in
            Button(device.hardware_identifier) {
                properties.onFetchMeasurements(device.id)
            }
        }
    }
}

struct ArchiveView_Previews: PreviewProvider {
    static var previews: some View {
        ArchiveView()
    }
}
