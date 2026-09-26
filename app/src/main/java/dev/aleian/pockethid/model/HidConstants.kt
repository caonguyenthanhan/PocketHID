package dev.aleian.pockethid.model

object HidConstants {
    const val REPORT_ID_KEYBOARD: Byte = 1
    const val REPORT_ID_MOUSE: Byte = 2
    const val REPORT_ID_CONSUMER: Byte = 3
    const val REPORT_ID_GAMEPAD: Byte = 4

    // Combo Report Descriptor (Keyboard + Mouse + Consumer Media + Gamepad)
    val COMBO_REPORT_DESCRIPTOR = byteArrayOf(
        // Report ID 1 — Keyboard (8 byte: modifiers, reserved, 6 keycodes)
        0x05.toByte(), 0x01.toByte(), 0x09.toByte(), 0x06.toByte(), 0xA1.toByte(), 0x01.toByte(), 0x85.toByte(), 0x01.toByte(),
        0x05.toByte(), 0x07.toByte(), 0x19.toByte(), 0xE0.toByte(), 0x29.toByte(), 0xE7.toByte(), 0x15.toByte(), 0x00.toByte(), 0x25.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x01.toByte(), 0x95.toByte(), 0x08.toByte(), 0x81.toByte(), 0x02.toByte(), // 8 modifier bits
        0x95.toByte(), 0x01.toByte(), 0x75.toByte(), 0x08.toByte(), 0x81.toByte(), 0x01.toByte(), // reserved byte
        0x95.toByte(), 0x06.toByte(), 0x75.toByte(), 0x08.toByte(), 0x15.toByte(), 0x00.toByte(), 0x25.toByte(), 0x65.toByte(),
        0x05.toByte(), 0x07.toByte(), 0x19.toByte(), 0x00.toByte(), 0x29.toByte(), 0x65.toByte(), 0x81.toByte(), 0x00.toByte(), // 6 key array
        0xC0.toByte(),
        // Report ID 2 — Mouse (4 byte: buttons, dx, dy, wheel)
        0x05.toByte(), 0x01.toByte(), 0x09.toByte(), 0x02.toByte(), 0xA1.toByte(), 0x01.toByte(), 0x85.toByte(), 0x02.toByte(), 0x09.toByte(), 0x01.toByte(), 0xA1.toByte(), 0x00.toByte(),
        0x05.toByte(), 0x09.toByte(), 0x19.toByte(), 0x01.toByte(), 0x29.toByte(), 0x03.toByte(), 0x15.toByte(), 0x00.toByte(), 0x25.toByte(), 0x01.toByte(),
        0x95.toByte(), 0x03.toByte(), 0x75.toByte(), 0x01.toByte(), 0x81.toByte(), 0x02.toByte(), // 3 buttons
        0x95.toByte(), 0x01.toByte(), 0x75.toByte(), 0x05.toByte(), 0x81.toByte(), 0x01.toByte(), // padding
        0x05.toByte(), 0x01.toByte(), 0x09.toByte(), 0x30.toByte(), 0x09.toByte(), 0x31.toByte(), 0x09.toByte(), 0x38.toByte(),
        0x15.toByte(), 0x81.toByte(), 0x25.toByte(), 0x7F.toByte(), 0x75.toByte(), 0x08.toByte(), 0x95.toByte(), 0x03.toByte(), 0x81.toByte(), 0x06.toByte(), // X, Y, wheel relative
        0xC0.toByte(), 0xC0.toByte(),
        // Report ID 3 — Consumer Control (Media)
        0x05.toByte(), 0x0C.toByte(), // Usage Page (Consumer)
        0x09.toByte(), 0x01.toByte(), // Usage (Consumer Control)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x03.toByte(), //   Report ID (3)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x03.toByte(), // Logical Maximum (0x03FF)
        0x19.toByte(), 0x00.toByte(), //   Usage Minimum (0)
        0x2A.toByte(), 0xFF.toByte(), 0x03.toByte(), // Usage Maximum (0x03FF)
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x75.toByte(), 0x10.toByte(), //   Report Size (16)
        0x81.toByte(), 0x00.toByte(), //   Input (Data, Array)
        0xC0.toByte(),                // End Collection
        // Report ID 4 — Gamepad (13 bytes: 16 buttons, 1 hat switch, 4 axes, 2 triggers)
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x05.toByte(), // Usage (Gamepad)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x04.toByte(), //   Report ID (4)
        // 16 Digital Buttons
        0x05.toByte(), 0x09.toByte(), //   Usage Page (Button)
        0x19.toByte(), 0x01.toByte(), //   Usage Minimum (Button 1)
        0x29.toByte(), 0x10.toByte(), //   Usage Maximum (Button 16)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(), //   Report Size (1)
        0x95.toByte(), 0x10.toByte(), //   Report Count (16)
        0x81.toByte(), 0x02.toByte(), //   Input (Data, Var, Abs)
        // Hat Switch (D-Pad, 4 bits + 4 bits padding = 1 byte)
        0x05.toByte(), 0x01.toByte(), //   Usage Page (Generic Desktop)
        0x09.toByte(), 0x39.toByte(), //   Usage (Hat switch)
        0x15.toByte(), 0x01.toByte(), //   Logical Minimum (1)
        0x25.toByte(), 0x08.toByte(), //   Logical Maximum (8)
        0x35.toByte(), 0x00.toByte(), //   Physical Minimum (0)
        0x46.toByte(), 0x3B.toByte(), 0x01.toByte(), // Physical Maximum (315)
        0x65.toByte(), 0x14.toByte(), //   Unit (Eng Rot: Angular Pos)
        0x75.toByte(), 0x04.toByte(), //   Report Size (4)
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x81.toByte(), 0x42.toByte(), //   Input (Data, Var, Abs, Null State)
        0x75.toByte(), 0x04.toByte(), //   Report Size (4) - padding
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x81.toByte(), 0x03.toByte(), //   Input (Const, Var, Abs)
        // Reset Physical Items and Unit to avoid polluting following analog axes
        0x65.toByte(), 0x00.toByte(), //   Unit (None)
        0x35.toByte(), 0x00.toByte(), //   Physical Minimum (0)
        0x45.toByte(), 0x00.toByte(), //   Physical Maximum (0)
        // Left Stick (X, Y) wrapped in Physical Pointer Collection
        0x05.toByte(), 0x01.toByte(), //   Usage Page (Generic Desktop)
        0x09.toByte(), 0x01.toByte(), //   Usage (Pointer)
        0xA1.toByte(), 0x00.toByte(), //   Collection (Physical)
        0x09.toByte(), 0x30.toByte(), //     Usage (X)
        0x09.toByte(), 0x31.toByte(), //     Usage (Y)
        0x16.toByte(), 0x00.toByte(), 0x80.toByte(), //   Logical Minimum (-32768)
        0x26.toByte(), 0xFF.toByte(), 0x7F.toByte(), //   Logical Maximum (32767)
        0x75.toByte(), 0x10.toByte(), //     Report Size (16)
        0x95.toByte(), 0x02.toByte(), //     Report Count (2)
        0x81.toByte(), 0x02.toByte(), //     Input (Data, Var, Abs)
        0xC0.toByte(),                 //   End Collection (Physical)
        // Right Stick (Z, Rz) wrapped in Physical Pointer Collection
        0x05.toByte(), 0x01.toByte(), //   Usage Page (Generic Desktop)
        0x09.toByte(), 0x01.toByte(), //   Usage (Pointer)
        0xA1.toByte(), 0x00.toByte(), //   Collection (Physical)
        0x09.toByte(), 0x32.toByte(), //     Usage (Z)
        0x09.toByte(), 0x35.toByte(), //     Usage (Rz)
        0x16.toByte(), 0x00.toByte(), 0x80.toByte(), //   Logical Minimum (-32768)
        0x26.toByte(), 0xFF.toByte(), 0x7F.toByte(), //   Logical Maximum (32767)
        0x75.toByte(), 0x10.toByte(), //     Report Size (16)
        0x95.toByte(), 0x02.toByte(), //     Report Count (2)
        0x81.toByte(), 0x02.toByte(), //     Input (Data, Var, Abs)
        0xC0.toByte(),                 //   End Collection (Physical)
        // 2 Analog Triggers (LT as Rx, RT as Ry: 0..255)
        0x05.toByte(), 0x01.toByte(), //   Usage Page (Generic Desktop)
        0x09.toByte(), 0x33.toByte(), //   Usage (Rx)
        0x09.toByte(), 0x34.toByte(), //   Usage (Ry)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(), //   Logical Maximum (255)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x95.toByte(), 0x02.toByte(), //   Report Count (2)
        0x81.toByte(), 0x02.toByte(), //   Input (Data, Var, Abs)
        0xC0.toByte()                 // End Collection (Application)
    )

    const val GAMEPAD_REPORT_LENGTH: Int = 13
    const val GAMEPAD_BUTTON_COUNT: Int = 16
    const val GAMEPAD_AXES_COUNT: Int = 6

    // Gamepad Buttons (16-bit bitmask)
    const val GAMEPAD_BTN_A: Int = 1 shl 0
    const val GAMEPAD_BTN_B: Int = 1 shl 1
    const val GAMEPAD_BTN_X: Int = 1 shl 2
    const val GAMEPAD_BTN_Y: Int = 1 shl 3
    const val GAMEPAD_BTN_LB: Int = 1 shl 4
    const val GAMEPAD_BTN_RB: Int = 1 shl 5
    const val GAMEPAD_BTN_BACK: Int = 1 shl 6
    const val GAMEPAD_BTN_START: Int = 1 shl 7
    const val GAMEPAD_BTN_GUIDE: Int = 1 shl 8
    const val GAMEPAD_BTN_L3: Int = 1 shl 9
    const val GAMEPAD_BTN_R3: Int = 1 shl 10
    const val GAMEPAD_BTN_DPAD_UP: Int = 1 shl 11
    const val GAMEPAD_BTN_DPAD_DOWN: Int = 1 shl 12
    const val GAMEPAD_BTN_DPAD_LEFT: Int = 1 shl 13
    const val GAMEPAD_BTN_DPAD_RIGHT: Int = 1 shl 14

    // Gamepad Hat Switch (D-Pad 8-way directional values)
    const val GAMEPAD_HAT_CENTERED: Byte = 0
    const val GAMEPAD_HAT_UP: Byte = 1
    const val GAMEPAD_HAT_UP_RIGHT: Byte = 2
    const val GAMEPAD_HAT_RIGHT: Byte = 3
    const val GAMEPAD_HAT_DOWN_RIGHT: Byte = 4
    const val GAMEPAD_HAT_DOWN: Byte = 5
    const val GAMEPAD_HAT_DOWN_LEFT: Byte = 6
    const val GAMEPAD_HAT_LEFT: Byte = 7
    const val GAMEPAD_HAT_UP_LEFT: Byte = 8

    fun calculateHatSwitch(up: Boolean, down: Boolean, left: Boolean, right: Boolean): Byte {
        val u = up && !down
        val d = down && !up
        val l = left && !right
        val r = right && !left

        return when {
            u && r -> GAMEPAD_HAT_UP_RIGHT
            d && r -> GAMEPAD_HAT_DOWN_RIGHT
            d && l -> GAMEPAD_HAT_DOWN_LEFT
            u && l -> GAMEPAD_HAT_UP_LEFT
            u -> GAMEPAD_HAT_UP
            d -> GAMEPAD_HAT_DOWN
            l -> GAMEPAD_HAT_LEFT
            r -> GAMEPAD_HAT_RIGHT
            else -> GAMEPAD_HAT_CENTERED
        }
    }

    // Consumer Usages (Media)
    const val CONSUMER_PLAY_PAUSE = 0x00CD
    const val CONSUMER_SCAN_NEXT = 0x00B5
    const val CONSUMER_SCAN_PREV = 0x00B6
    const val CONSUMER_STOP = 0x00B7
    const val CONSUMER_VOLUME_UP = 0x00E9
    const val CONSUMER_VOLUME_DOWN = 0x00EA
    const val CONSUMER_MUTE = 0x00E2

    // Modifier Masks for Keyboard Report
    const val MOD_LEFT_CTRL: Byte = 0x01
    const val MOD_LEFT_SHIFT: Byte = 0x02
    const val MOD_LEFT_ALT: Byte = 0x04
    const val MOD_LEFT_GUI: Byte = 0x08 // Win key / Cmd
    const val MOD_RIGHT_CTRL: Byte = 0x10
    const val MOD_RIGHT_SHIFT: Byte = 0x20
    const val MOD_RIGHT_ALT: Byte = 0x40
    const val MOD_RIGHT_GUI: Byte = 0x80.toByte()

    // Mouse Buttons
    const val MOUSE_BUTTON_NONE: Byte = 0x00
    const val MOUSE_BUTTON_LEFT: Byte = 0x01
    const val MOUSE_BUTTON_RIGHT: Byte = 0x02
    const val MOUSE_BUTTON_MIDDLE: Byte = 0x04

    // Keyboard Keycodes (HID Usage Page 0x07)
    const val KEY_NONE: Byte = 0x00
    const val KEY_A: Byte = 0x04
    const val KEY_B: Byte = 0x05
    const val KEY_C: Byte = 0x06
    const val KEY_D: Byte = 0x07
    const val KEY_E: Byte = 0x08
    const val KEY_F: Byte = 0x09
    const val KEY_G: Byte = 0x0A
    const val KEY_H: Byte = 0x0B
    const val KEY_I: Byte = 0x0C
    const val KEY_J: Byte = 0x0D
    const val KEY_K: Byte = 0x0E
    const val KEY_L: Byte = 0x0F
    const val KEY_M: Byte = 0x10
    const val KEY_N: Byte = 0x11
    const val KEY_O: Byte = 0x12
    const val KEY_P: Byte = 0x13
    const val KEY_Q: Byte = 0x14
    const val KEY_R: Byte = 0x15
    const val KEY_S: Byte = 0x16
    const val KEY_T: Byte = 0x17
    const val KEY_U: Byte = 0x18
    const val KEY_V: Byte = 0x19
    const val KEY_W: Byte = 0x1A
    const val KEY_X: Byte = 0x1B
    const val KEY_Y: Byte = 0x1C
    const val KEY_Z: Byte = 0x1D

    const val KEY_1: Byte = 0x1E
    const val KEY_2: Byte = 0x1F
    const val KEY_3: Byte = 0x20
    const val KEY_4: Byte = 0x21
    const val KEY_5: Byte = 0x22
    const val KEY_6: Byte = 0x23
    const val KEY_7: Byte = 0x24
    const val KEY_8: Byte = 0x25
    const val KEY_9: Byte = 0x26
    const val KEY_0: Byte = 0x27

    const val KEY_ENTER: Byte = 0x28
    const val KEY_ESC: Byte = 0x29
    const val KEY_BACKSPACE: Byte = 0x2A
    const val KEY_TAB: Byte = 0x2B
    const val KEY_SPACE: Byte = 0x2C
    const val KEY_MINUS: Byte = 0x2D
    const val KEY_EQUAL: Byte = 0x2E
    const val KEY_LEFTBRACE: Byte = 0x2F
    const val KEY_RIGHTBRACE: Byte = 0x30
    const val KEY_BACKSLASH: Byte = 0x31
    const val KEY_SEMICOLON: Byte = 0x33
    const val KEY_APOSTROPHE: Byte = 0x34
    const val KEY_GRAVE: Byte = 0x35
    const val KEY_COMMA: Byte = 0x36
    const val KEY_DOT: Byte = 0x37
    const val KEY_SLASH: Byte = 0x38
    const val KEY_CAPSLOCK: Byte = 0x39

    const val KEY_F1: Byte = 0x3A
    const val KEY_F2: Byte = 0x3B
    const val KEY_F3: Byte = 0x3C
    const val KEY_F4: Byte = 0x3D
    const val KEY_F5: Byte = 0x3E
    const val KEY_F6: Byte = 0x3F
    const val KEY_F7: Byte = 0x40
    const val KEY_F8: Byte = 0x41
    const val KEY_F9: Byte = 0x42
    const val KEY_F10: Byte = 0x43
    const val KEY_F11: Byte = 0x44
    const val KEY_F12: Byte = 0x45

    const val KEY_PRINTSCREEN: Byte = 0x46
    const val KEY_SCROLLLOCK: Byte = 0x47
    const val KEY_PAUSE: Byte = 0x48
    const val KEY_INSERT: Byte = 0x49
    const val KEY_HOME: Byte = 0x4A
    const val KEY_PAGEUP: Byte = 0x4B
    const val KEY_DELETE: Byte = 0x4C
    const val KEY_END: Byte = 0x4D
    const val KEY_PAGEDOWN: Byte = 0x4E
    const val KEY_RIGHT: Byte = 0x4F
    const val KEY_LEFT: Byte = 0x50
    const val KEY_DOWN: Byte = 0x51
    const val KEY_UP: Byte = 0x52

    const val KEY_NUMLOCK: Byte = 0x53
    const val KEY_KPSLASH: Byte = 0x54
    const val KEY_KPASTERISK: Byte = 0x55
    const val KEY_KPMINUS: Byte = 0x56
    const val KEY_KPPLUS: Byte = 0x57
    const val KEY_KPENTER: Byte = 0x58
    const val KEY_KP1: Byte = 0x59
    const val KEY_KP2: Byte = 0x5A
    const val KEY_KP3: Byte = 0x5B
    const val KEY_KP4: Byte = 0x5C
    const val KEY_KP5: Byte = 0x5D
    const val KEY_KP6: Byte = 0x5E
    const val KEY_KP7: Byte = 0x5F
    const val KEY_KP8: Byte = 0x60
    const val KEY_KP9: Byte = 0x61
    const val KEY_KP0: Byte = 0x62
    const val KEY_KPDOT: Byte = 0x63
    const val KEY_MENU: Byte = 0x65
    const val KEY_KPEQUAL: Byte = 0x67

    const val KEY_LEFTCTRL: Byte = 0xE0.toByte()
    const val KEY_LEFTSHIFT: Byte = 0xE1.toByte()
    const val KEY_LEFTALT: Byte = 0xE2.toByte()
    const val KEY_LEFTGUI: Byte = 0xE3.toByte()
    const val KEY_RIGHTCTRL: Byte = 0xE4.toByte()
    const val KEY_RIGHTSHIFT: Byte = 0xE5.toByte()
    const val KEY_RIGHTALT: Byte = 0xE6.toByte()
    const val KEY_RIGHTGUI: Byte = 0xE7.toByte()
}
