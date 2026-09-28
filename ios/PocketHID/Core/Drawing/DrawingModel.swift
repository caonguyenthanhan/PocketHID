//
//  DrawingModel.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation
import CoreGraphics

public struct DrawingPoint: Equatable, Codable {
    public let x: Float
    public let y: Float
    public let pressure: Float
    
    public init(x: Float, y: Float, pressure: Float = 1.0) {
        self.x = x
        self.y = y
        self.pressure = pressure
    }
    
    public var cgPoint: CGPoint {
        CGPoint(x: CGFloat(x), y: CGFloat(y))
    }
}

public enum DrawingTool: String, CaseIterable, Identifiable, Codable {
    case pen = "PEN"
    case eraser = "ERASER"
    
    public var id: String { rawValue }
}

public enum DrawingColor: String, CaseIterable, Identifiable, Codable {
    case white = "WHITE"
    case cyan = "CYAN"
    case yellow = "YELLOW"
    case red = "RED"
    case blue = "BLUE"
    
    public var id: String { rawValue }
    
    public var hexValue: UInt32 {
        switch self {
        case .white: return 0xFFFFFFFF
        case .cyan: return 0xFF00E5FF
        case .yellow: return 0xFFFFD600
        case .red: return 0xFFFF1744
        case .blue: return 0xFF2979FF
        }
    }
}

public struct DrawingStroke: Identifiable, Equatable {
    public let id: UUID
    public var points: [DrawingPoint]
    public let color: DrawingColor
    public let strokeWidth: Float
    public let tool: DrawingTool
    
    public init(
        id: UUID = UUID(),
        points: [DrawingPoint] = [],
        color: DrawingColor = .cyan,
        strokeWidth: Float = 4.0,
        tool: DrawingTool = .pen
    ) {
        self.id = id
        self.points = points
        self.color = color
        self.strokeWidth = strokeWidth
        self.tool = tool
    }
}

public enum TabletOrientation: String, CaseIterable, Identifiable, Codable {
    case portrait = "PORTRAIT"
    case landscape = "LANDSCAPE"
    case reverseLandscape = "REVERSE_LANDSCAPE"
    
    public var id: String { rawValue }
}

public enum DrawingTargetMode: String, CaseIterable, Identifiable, Codable {
    case absoluteTablet = "ABSOLUTE_TABLET"
    case relativeMouse = "RELATIVE_MOUSE"
    
    public var id: String { rawValue }
}
