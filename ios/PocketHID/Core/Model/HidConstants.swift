//
//  HidConstants.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation

public struct HidConstants {
    // Modifier Masks
    public static let MOD_NONE: UInt8       = 0x00
    public static let MOD_LEFT_CTRL: UInt8  = 0x01
    public static let MOD_LEFT_SHIFT: UInt8 = 0x02
    public static let MOD_LEFT_ALT: UInt8   = 0x04
    public static let MOD_LEFT_GUI: UInt8   = 0x08
    public static let MOD_RIGHT_CTRL: UInt8 = 0x10
    public static let MOD_RIGHT_SHIFT: UInt8 = 0x20
    public static let MOD_RIGHT_ALT: UInt8  = 0x40
    public static let MOD_RIGHT_GUI: UInt8  = 0x80

    // Common Keys
    public static let KEY_NONE: UInt8       = 0x00
    public static let KEY_A: UInt8          = 0x04
    public static let KEY_C: UInt8          = 0x06
    public static let KEY_D: UInt8          = 0x07
    public static let KEY_L: UInt8          = 0x0F
    public static let KEY_S: UInt8          = 0x16
    public static let KEY_T: UInt8          = 0x17
    public static let KEY_V: UInt8          = 0x19
    public static let KEY_W: UInt8          = 0x1A
    public static let KEY_X: UInt8          = 0x1B
    public static let KEY_Y: UInt8          = 0x1C
    public static let KEY_Z: UInt8          = 0x1D
    
    public static let KEY_1: UInt8          = 0x1E
    public static let KEY_2: UInt8          = 0x1F
    public static let KEY_3: UInt8          = 0x20
    public static let KEY_4: UInt8          = 0x21
    public static let KEY_5: UInt8          = 0x22
    public static let KEY_6: UInt8          = 0x23
    public static let KEY_7: UInt8          = 0x24
    public static let KEY_8: UInt8          = 0x25
    public static let KEY_9: UInt8          = 0x26
    public static let KEY_0: UInt8          = 0x27
    
    public static let KEY_ENTER: UInt8      = 0x28
    public static let KEY_ESC: UInt8        = 0x29
    public static let KEY_BACKSPACE: UInt8  = 0x2A
    public static let KEY_TAB: UInt8        = 0x2B
    public static let KEY_SPACE: UInt8      = 0x2C
    public static let KEY_MINUS: UInt8      = 0x2D
    public static let KEY_EQUAL: UInt8      = 0x2E
    public static let KEY_LEFTBRACE: UInt8  = 0x2F
    public static let KEY_RIGHTBRACE: UInt8 = 0x30
    public static let KEY_BACKSLASH: UInt8  = 0x31
    public static let KEY_SEMICOLON: UInt8  = 0x33
    public static let KEY_APOSTROPHE: UInt8 = 0x34
    public static let KEY_GRAVE: UInt8      = 0x35
    public static let KEY_COMMA: UInt8      = 0x36
    public static let KEY_DOT: UInt8        = 0x37
    public static let KEY_SLASH: UInt8      = 0x38
    public static let KEY_CAPSLOCK: UInt8   = 0x39
    
    // Function Keys
    public static let KEY_F1: UInt8         = 0x3A
    public static let KEY_F2: UInt8         = 0x3B
    public static let KEY_F3: UInt8         = 0x3C
    public static let KEY_F4: UInt8         = 0x3D
    public static let KEY_F5: UInt8         = 0x3E
    public static let KEY_F6: UInt8         = 0x3F
    public static let KEY_F7: UInt8         = 0x40
    public static let KEY_F8: UInt8         = 0x41
    public static let KEY_F9: UInt8         = 0x42
    public static let KEY_F10: UInt8        = 0x43
    public static let KEY_F11: UInt8        = 0x44
    public static let KEY_F12: UInt8        = 0x45
    
    // Navigation Keys
    public static let KEY_PAGEUP: UInt8     = 0x4B
    public static let KEY_DELETE: UInt8     = 0x4C
    public static let KEY_PAGEDOWN: UInt8   = 0x4E
    public static let KEY_RIGHT: UInt8      = 0x4F
    public static let KEY_LEFT: UInt8       = 0x50
    public static let KEY_DOWN: UInt8       = 0x51
    public static let KEY_UP: UInt8         = 0x52
    public static let KEY_HOME: UInt8       = 0x4A
    public static let KEY_END: UInt8        = 0x4D
    
    // Mouse Buttons
    public static let MOUSE_BUTTON_NONE: UInt8   = 0x00
    public static let MOUSE_BUTTON_LEFT: UInt8   = 0x01
    public static let MOUSE_BUTTON_RIGHT: UInt8  = 0x02
    public static let MOUSE_BUTTON_MIDDLE: UInt8 = 0x04
    
    // Consumer Codes
    public static let CONSUMER_PLAY_PAUSE: UInt16 = 0x00CD
    public static let CONSUMER_NEXT_TRACK: UInt16 = 0x00B5
    public static let CONSUMER_PREV_TRACK: UInt16 = 0x00B6
    public static let CONSUMER_VOLUME_UP: UInt16  = 0x00E9
    public static let CONSUMER_VOLUME_DOWN: UInt16 = 0x00EA
    public static let CONSUMER_MUTE: UInt16       = 0x00E2
    
    // Tablet Status Masks (Report ID 5)
    public static let TABLET_STATUS_NONE: UInt8   = 0x00
    public static let TABLET_STATUS_TIP: UInt8    = 0x01
    public static let TABLET_STATUS_IN_RANGE: UInt8 = 0x02
    public static let TABLET_STATUS_ERASER: UInt8 = 0x10
}
