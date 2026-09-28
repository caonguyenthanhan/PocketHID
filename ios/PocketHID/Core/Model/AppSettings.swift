//
//  AppSettings.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation
import Combine

public enum AppLanguage: String, CaseIterable, Identifiable, Codable {
    case english = "en"
    case vietnamese = "vi"
    
    public var id: String { rawValue }
    
    public var displayName: String {
        switch self {
        case .english: return "English"
        case .vietnamese: return "Tiếng Việt"
        }
    }
}

public enum AspectRatioPolicy: String, CaseIterable, Identifiable, Codable {
    case fit = "FIT"
    case stretch = "STRETCH"
    
    public var id: String { rawValue }
}

public final class AppSettings: ObservableObject {
    public static let shared = AppSettings()
    
    @Published public var language: AppLanguage = .english
    @Published public var hapticEnabled: Bool = true
    @Published public var hostOs: HostOs = .windows
    
    // Mouse
    @Published public var mouseSensitivity: Float = 1.0
    @Published public var fastScrollEnabled: Bool = false
    
    // Gamepad
    @Published public var gamepadDeadzone: Float = 0.15
    
    // Drawing
    @Published public var drawingSensitivity: Float = 1.0
    @Published public var aspectRatioPolicy: AspectRatioPolicy = .fit
    @Published public var pcScreenWidth: Float = 1920.0
    @Published public var pcScreenHeight: Float = 1080.0
    
    public init() {}
}
