//
//  ArchiveMeasurementsView.swift
//  iosApp
//
//  Created by Lukas Schörghuber on 04.03.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI
import shared

extension shared.Measurement : Identifiable {
    public var id: String { "\(self.device_id)-\(self.measurement_type_id)-\(self.timestamp)" }
}

struct ArchiveMeasurementView: ConnectedView {
    struct Props {
        let measurementRecords: [shared.Measurement]
    }
    
    func map(reduxState: BluetoothSensorDiscoveryState, dispatch: @escaping DispatchFunction) -> Props {
        return Props(
            measurementRecords: reduxState.measurementRecords
        )
    }
    
    private let _this = MeasurementTypeIDs()
    
    func body(properties: Props) -> some View {
        List(properties.measurementRecords) { measurement in
            Text("\(measurement.value_) \(MeasurementTypeIDs.mapToUnit(self._this)(typeID: measurement.measurement_type_id))")
        }
    }
}
