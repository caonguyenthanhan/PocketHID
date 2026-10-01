//
//  KeyLegends.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation

public struct KeyLegend: Identifiable, Equatable {
    public var id: String { primary }
    public let primary: String
    public let shifted: String?
    public let keyCode: UInt8
    public let isAlphabetic: Bool
    
    public init(primary: String, shifted: String? = nil, keyCode: UInt8, isAlphabetic: Bool = false) {
        self.primary = primary
        self.shifted = shifted
        self.keyCode = keyCode
        self.isAlphabetic = isAlphabetic
    }
    
    public func resolveActiveLabel(isShiftActive: Bool, isCapsLockActive: Bool) -> String {
        if isAlphabetic {
            let isUpper = isCapsLockActive != isShiftActive // XOR
            return isUpper ? primary.uppercased() : primary.lowercased()
        } else {
            if isShiftActive, let shifted = shifted {
                return shifted
            }
            return primary
        }
    }
}

public struct KeyLegends {
    // Number Row with standard shifted legends
    public static let numberRow: [KeyLegend] = [
        KeyLegend(primary: "`", shifted: "~", keyCode: HidConstants.KEY_GRAVE),
        KeyLegend(primary: "1", shifted: "!", keyCode: HidConstants.KEY_1),
        KeyLegend(primary: "2", shifted: "@", keyCode: HidConstants.KEY_2),
        KeyLegend(primary: "3", shifted: "#", keyCode: HidConstants.KEY_3),
        KeyLegend(primary: "4", shifted: "$", keyCode: HidConstants.KEY_4),
        KeyLegend(primary: "5", shifted: "%", keyCode: HidConstants.KEY_5),
        KeyLegend(primary: "6", shifted: "^", keyCode: HidConstants.KEY_6),
        KeyLegend(primary: "7", shifted: "&", keyCode: HidConstants.KEY_7),
        KeyLegend(primary: "8", shifted: "*", keyCode: HidConstants.KEY_8),
        KeyLegend(primary: "9", shifted: "(", keyCode: HidConstants.KEY_9),
        KeyLegend(primary: "0", shifted: ")", keyCode: HidConstants.KEY_0),
        KeyLegend(primary: "-", shifted: "_", keyCode: HidConstants.KEY_MINUS),
        KeyLegend(primary: "=", shifted: "+", keyCode: HidConstants.KEY_EQUAL)
    ]
    
    // QWERTY Row 1
    public static let row1: [KeyLegend] = [
        KeyLegend(primary: "Q", keyCode: HidConstants.KEY_Q, isAlphabetic: true), // Placeholder scancodes mapped via mapper
        KeyLegend(primary: "W", keyCode: HidConstants.KEY_W, isAlphabetic: true),
        KeyLegend(primary: "E", keyCode: HidConstants.KEY_E, isAlphabetic: true),
        KeyLegend(primary: "R", keyCode: HidConstants.KEY_R, isAlphabetic: true),
        KeyLegend(primary: "T", keyCode: HidConstants.KEY_T, isAlphabetic: true),
        KeyLegend(primary: "Y", keyCode: HidConstants.KEY_Y, isAlphabetic: true),
        KeyLegend(primary: "U", keyCode: HidConstants.KEY_U, isAlphabetic: true),
        KeyLegend(primary: "I", keyCode: HidConstants.KEY_I, isAlphabetic: true),
        KeyLegend(primary: "O", keyCode: HidConstants.KEY_O, isAlphabetic: true),
        KeyLegend(primary: "P", keyCode: HidConstants.KEY_P, isAlphabetic: true),
        KeyLegend(primary: "[", shifted: "{", keyCode: HidConstants.KEY_LEFTBRACE),
        KeyLegend(primary: "]", shifted: "}", keyCode: HidConstants.KEY_RIGHTBRACE),
        KeyLegend(primary: "\\", shifted: "|", keyCode: HidConstants.KEY_BACKSLASH)
    ]
    
    // QWERTY Row 2
    public static let row2: [KeyLegend] = [
        KeyLegend(primary: "A", keyCode: HidConstants.KEY_A, isAlphabetic: true),
        KeyLegend(primary: "S", keyCode: HidConstants.KEY_S, isAlphabetic: true),
        KeyLegend(primary: "D", keyCode: HidConstants.KEY_D, isAlphabetic: true),
        KeyLegend(primary: "F", keyCode: HidConstants.KEY_F, isAlphabetic: true),
        KeyLegend(primary: "G", keyCode: HidConstants.KEY_G, isAlphabetic: true),
        KeyLegend(primary: "H", keyCode: HidConstants.KEY_H, isAlphabetic: true),
        KeyLegend(primary: "J", keyCode: HidConstants.KEY_J, isAlphabetic: true),
        KeyLegend(primary: "K", keyCode: HidConstants.KEY_K, isAlphabetic: true),
        KeyLegend(primary: "L", keyCode: HidConstants.KEY_L, isAlphabetic: true),
        KeyLegend(primary: ";", shifted: ":", keyCode: HidConstants.KEY_SEMICOLON),
        KeyLegend(primary: "'", shifted: "\"", keyCode: HidConstants.KEY_APOSTROPHE)
    ]
    
    // QWERTY Row 3
    public static let row3: [KeyLegend] = [
        KeyLegend(primary: "Z", keyCode: HidConstants.KEY_Z, isAlphabetic: true),
        KeyLegend(primary: "X", keyCode: HidConstants.KEY_X, isAlphabetic: true),
        KeyLegend(primary: "C", keyCode: HidConstants.KEY_C, isAlphabetic: true),
        KeyLegend(primary: "V", keyCode: HidConstants.KEY_V, isAlphabetic: true),
        KeyLegend(primary: "B", keyCode: HidConstants.KEY_B, isAlphabetic: true),
        KeyLegend(primary: "N", keyCode: HidConstants.KEY_N, isAlphabetic: true),
        KeyLegend(primary: "M", keyCode: HidConstants.KEY_M, isAlphabetic: true),
        KeyLegend(primary: ",", shifted: "<", keyCode: HidConstants.KEY_COMMA),
        KeyLegend(primary: ".", shifted: ">", keyCode: HidConstants.KEY_DOT),
        KeyLegend(primary: "/", shifted: "?", keyCode: HidConstants.KEY_SLASH)
    ]
}
