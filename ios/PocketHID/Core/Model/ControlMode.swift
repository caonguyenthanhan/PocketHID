//
//  ControlMode.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation

/// Primary interaction modes in PocketHID.
public enum ControlMode: Int, CaseIterable, Identifiable {
    case keyboard = 0
    case mouse = 1
    case gamepad = 2
    case presenter = 3
    case oneHand = 4
    case draw = 5
    
    public var id: Int { rawValue }
    
    public var title: String {
        switch self {
        case .keyboard: return "Keyboard"
        case .mouse: return "Mouse"
        case .gamepad: return "Gamepad"
        case .presenter: return "Presenter"
        case .oneHand: return "1-Hand"
        case .draw: return "Draw"
        }
    }
    
    public var systemIconName: String {
        switch self {
        case .keyboard: return "keyboard"
        case .mouse: return "computermouse"
        case .gamepad: return "gamecontroller"
        case .presenter: return "play.rectangle"
        case .oneHand: return "hand.tap"
        case .draw: return "pencil.and.outline"
        }
    }
}
