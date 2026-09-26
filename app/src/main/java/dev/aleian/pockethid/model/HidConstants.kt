package dev.aleian.pockethid.model

object HidConstants {
    const val REPORT_ID_KEYBOARD: Byte = 1
    const val REPORT_ID_MOUSE: Byte = 2
    const val REPORT_ID_CONSUMER: Byte = 3

    // Combo Report Descriptor (Keyboard + Mouse + Consumer Media)
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
        0xC0.toByte()                 // End Collection
    )

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

    const val KEY_LEFTCTRL: Byte = 0xE0.toByte()
    const val KEY_LEFTSHIFT: Byte = 0xE1.toByte()
    const val KEY_LEFTALT: Byte = 0xE2.toByte()
    const val KEY_LEFTGUI: Byte = 0xE3.toByte()
    const val KEY_RIGHTCTRL: Byte = 0xE4.toByte()
    const val KEY_RIGHTSHIFT: Byte = 0xE5.toByte()
    const val KEY_RIGHTALT: Byte = 0xE6.toByte()
    const val KEY_RIGHTGUI: Byte = 0xE7.toByte()
}
