//
//  DrawingCoordinatePipeline.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation

public struct HidTabletPoint: Equatable {
    public let x: UInt16 // 0...32767
    public let y: UInt16 // 0...32767
    public let pressure: UInt16 // 0...8191
    public let status: UInt8 // 0x01: Tip, 0x02: In Range, 0x10: Eraser
    
    public init(x: UInt16, y: UInt16, pressure: UInt16 = 4095, status: UInt8 = HidConstants.TABLET_STATUS_TIP | HidConstants.TABLET_STATUS_IN_RANGE) {
        self.x = x
        self.y = y
        self.pressure = pressure
        self.status = status
    }
}

/// Canonical 8-Stage Coordinate Transformation Pipeline for PocketHID Graphics Tablet.
/// Transforms raw iPhone touch coordinates into true 16-bit absolute HID digitizer space (0...32767).
public final class DrawingCoordinatePipeline {
    
    public var canvasWidth: Float = 1080.0
    public var canvasHeight: Float = 2400.0
    
    public var insetsLeft: Float = 0.0
    public var insetsTop: Float = 0.0
    public var insetsRight: Float = 0.0
    public var insetsBottom: Float = 0.0
    
    public var orientation: TabletOrientation = .portrait
    public var aspectRatioPolicy: AspectRatioPolicy = .fit
    
    public var pcScreenWidth: Float = 1920.0
    public var pcScreenHeight: Float = 1080.0
    
    public var scaleX: Float = 1.0
    public var scaleY: Float = 1.0
    public var offsetX: Float = 0.0
    public var offsetY: Float = 0.0
    
    public init() {}
    
    /// Executes the full 8-stage transformation pipeline.
    public func transform(rawPoint: DrawingPoint, tool: DrawingTool = .pen) -> HidTabletPoint {
        // Stage 1 & 2: Raw Touch to Canvas Local
        let localX = rawPoint.x
        let localY = rawPoint.y
        
        // Stage 3: Safe Area Inset subtraction and clamping
        let usableWidth = max(1.0, canvasWidth - insetsLeft - insetsRight)
        let usableHeight = max(1.0, canvasHeight - insetsTop - insetsBottom)
        
        let clampedX = min(max(0.0, localX - insetsLeft), usableWidth)
        let clampedY = min(max(0.0, localY - insetsTop), usableHeight)
        
        // Stage 4: Orientation normalization
        var normX: Float
        var normY: Float
        
        switch orientation {
        case .portrait:
            normX = clampedX / usableWidth
            normY = clampedY / usableHeight
            
        case .landscape:
            normX = clampedY / usableHeight
            normY = 1.0 - (clampedX / usableWidth)
            
        case .reverseLandscape:
            normX = 1.0 - (clampedY / usableHeight)
            normY = clampedX / usableWidth
        }
        
        // Stage 5 & 6: Aspect Ratio Mapping (FIT vs STRETCH)
        var mappedX = normX
        var mappedY = normY
        
        if aspectRatioPolicy == .fit {
            let phoneRatio = usableWidth / usableHeight
            let pcRatio = pcScreenWidth / pcScreenHeight
            
            if phoneRatio > pcRatio {
                // Phone is wider than PC: letterbox horizontally
                let visibleRatio = pcRatio / phoneRatio
                let pad = (1.0 - visibleRatio) / 2.0
                mappedX = min(max(0.0, (normX - pad) / visibleRatio), 1.0)
            } else {
                // Phone is taller than PC: pillarbox vertically
                let visibleRatio = phoneRatio / pcRatio
                let pad = (1.0 - visibleRatio) / 2.0
                mappedY = min(max(0.0, (normY - pad) / visibleRatio), 1.0)
            }
        }
        
        // Stage 7: Calibration scale & offset
        let calibratedX = min(max(0.0, mappedX * scaleX + offsetX), 1.0)
        let calibratedY = min(max(0.0, mappedY * scaleY + offsetY), 1.0)
        
        // Stage 8: Output Coordinate mapping to 16-bit Logical HID (0...32767)
        let hidX = UInt16(min(32767.0, max(0.0, calibratedX * 32767.0)))
        let hidY = UInt16(min(32767.0, max(0.0, calibratedY * 32767.0)))
        
        let pressureVal = UInt16(min(8191.0, max(0.0, rawPoint.pressure * 8191.0)))
        
        var status = HidConstants.TABLET_STATUS_TIP | HidConstants.TABLET_STATUS_IN_RANGE
        if tool == .eraser {
            status |= HidConstants.TABLET_STATUS_ERASER
        }
        
        return HidTabletPoint(x: hidX, y: hidY, pressure: pressureVal, status: status)
    }
}
