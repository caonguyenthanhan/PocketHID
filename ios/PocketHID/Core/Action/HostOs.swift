//
//  HostOs.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation

/// Host Operating System target.
public enum HostOs: String, CaseIterable, Identifiable, Codable {
    case windows = "WINDOWS"
    case macOS = "MACOS"
    case linux = "LINUX"
    
    public var id: String { rawValue }
    
    public var displayName: String {
        switch self {
        case .windows: return "Windows"
        case .macOS: return "macOS"
        case .linux: return "Linux"
        }
    }
}
