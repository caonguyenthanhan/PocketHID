//
//  KeyboardView.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import SwiftUI

public struct KeyboardView: View {
    @ObservedObject var transport: iOSTransport
    @ObservedObject var settings = AppSettings.shared
    
    @State private var isShiftActive: Bool = false
    @State private var isCapsLockActive: Bool = false
    @State private var isCtrlActive: Bool = false
    @State private var isAltActive: Bool = false
    @State private var isGuiActive: Bool = false
    
    public init(transport: iOSTransport) {
        self.transport = transport
    }
    
    public var body: some View {
        VStack(spacing: 6) {
            // Quick Action Toolbar
            HStack(spacing: 6) {
                quickActionButton("UNDO", action: .undo)
                quickActionButton("REDO", action: .redo)
                quickActionButton("COPY", action: .copy)
                quickActionButton("PASTE", action: .paste)
                quickActionButton("SAVE", action: .save)
                
                Spacer()
                
                // LED Caps Lock Indicator
                HStack(spacing: 4) {
                    Circle()
                        .fill(isCapsLockActive ? ThemeColors.cyanAccent : ThemeColors.darkBorder)
                        .frame(width: 6, height: 6)
                    Text("CAPS")
                        .font(.system(size: 9, weight: .bold, design: .monospaced))
                        .foregroundColor(isCapsLockActive ? ThemeColors.cyanAccent : ThemeColors.textMuted)
                }
                .padding(.horizontal, 6)
                .padding(.vertical, 4)
                .background(ThemeColors.darkSurface)
                .cornerRadius(4)
            }
            .padding(.horizontal, 8)
            
            // Dedicated Dual-Legend Number Row
            HStack(spacing: 4) {
                ForEach(KeyLegends.numberRow) { key in
                    keyButton(
                        primary: key.primary,
                        shifted: key.shifted,
                        activeLabel: key.resolveActiveLabel(isShiftActive: isShiftActive, isCapsLockActive: isCapsLockActive)
                    ) {
                        dispatchKey(key.keyCode)
                    }
                }
            }
            .padding(.horizontal, 4)
            
            // QWERTY Row 1
            HStack(spacing: 4) {
                ForEach(KeyLegends.row1) { key in
                    keyButton(
                        primary: key.primary,
                        shifted: key.shifted,
                        activeLabel: key.resolveActiveLabel(isShiftActive: isShiftActive, isCapsLockActive: isCapsLockActive)
                    ) {
                        dispatchKey(key.keyCode)
                    }
                }
            }
            .padding(.horizontal, 4)
            
            // QWERTY Row 2
            HStack(spacing: 4) {
                ForEach(KeyLegends.row2) { key in
                    keyButton(
                        primary: key.primary,
                        shifted: key.shifted,
                        activeLabel: key.resolveActiveLabel(isShiftActive: isShiftActive, isCapsLockActive: isCapsLockActive)
                    ) {
                        dispatchKey(key.keyCode)
                    }
                }
            }
            .padding(.horizontal, 8)
            
            // QWERTY Row 3
            HStack(spacing: 4) {
                // Caps Lock Toggle
                modifierButton("CAPS", isActive: isCapsLockActive) {
                    isCapsLockActive.toggle()
                }
                .frame(width: 44)
                
                ForEach(KeyLegends.row3) { key in
                    keyButton(
                        primary: key.primary,
                        shifted: key.shifted,
                        activeLabel: key.resolveActiveLabel(isShiftActive: isShiftActive, isCapsLockActive: isCapsLockActive)
                    ) {
                        dispatchKey(key.keyCode)
                    }
                }
                
                // Backspace
                actionKeyButton("⌫") {
                    dispatchKey(HidConstants.KEY_BACKSPACE)
                }
                .frame(width: 44)
            }
            .padding(.horizontal, 4)
            
            // Bottom Modifier & Space Row
            HStack(spacing: 4) {
                modifierButton("CTRL", isActive: isCtrlActive) { isCtrlActive.toggle() }
                modifierButton("ALT", isActive: isAltActive) { isAltActive.toggle() }
                modifierButton("WIN", isActive: isGuiActive) { isGuiActive.toggle() }
                modifierButton("SHIFT", isActive: isShiftActive) { isShiftActive.toggle() }
                
                // Spacebar
                Button(action: {
                    dispatchKey(HidConstants.KEY_SPACE)
                }) {
                    Text("SPACE")
                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                        .foregroundColor(ThemeColors.textSecondary)
                        .frame(maxWidth: .infinity, minHeight: 40)
                        .background(ThemeColors.darkSurface)
                        .cornerRadius(6)
                        .overlay(RoundedRectangle(cornerRadius: 6).stroke(ThemeColors.darkBorder, lineWidth: 1))
                }
                
                actionKeyButton("ENTER") {
                    dispatchKey(HidConstants.KEY_ENTER)
                }
                .frame(width: 58)
            }
            .padding(.horizontal, 4)
        }
        .padding(.vertical, 8)
        .background(ThemeColors.darkBg)
    }
    
    // MARK: - Helpers & Subviews
    
    private func keyButton(primary: String, shifted: String?, activeLabel: String, action: @escaping () -> Void) -> some View {
        Button(action: {
            action()
            triggerKeyHaptic()
        }) {
            VStack(spacing: 1) {
                if let shifted = shifted {
                    Text(shifted)
                        .font(.system(size: 8, weight: .medium))
                        .foregroundColor(isShiftActive ? ThemeColors.cyanAccent : ThemeColors.textMuted)
                }
                Text(activeLabel)
                    .font(.system(size: 13, weight: .semibold, design: .monospaced))
                    .foregroundColor(ThemeColors.textPrimary)
            }
            .frame(maxWidth: .infinity, minHeight: 38)
            .background(ThemeColors.darkSurface)
            .cornerRadius(5)
            .overlay(RoundedRectangle(cornerRadius: 5).stroke(ThemeColors.darkBorder, lineWidth: 1))
        }
    }
    
    private func modifierButton(_ label: String, isActive: Bool, action: @escaping () -> Void) -> some View {
        Button(action: {
            action()
            triggerKeyHaptic()
        }) {
            Text(label)
                .font(.system(size: 10, weight: .bold, design: .monospaced))
                .foregroundColor(isActive ? ThemeColors.darkBg : ThemeColors.cyanAccent)
                .frame(minHeight: 40)
                .padding(.horizontal, 6)
                .background(isActive ? ThemeColors.cyanAccent : ThemeColors.darkSurface)
                .cornerRadius(5)
                .overlay(RoundedRectangle(cornerRadius: 5).stroke(ThemeColors.cyanAccent.opacity(0.8), lineWidth: 1))
        }
    }
    
    private func actionKeyButton(_ label: String, action: @escaping () -> Void) -> some View {
        Button(action: {
            action()
            triggerKeyHaptic()
        }) {
            Text(label)
                .font(.system(size: 11, weight: .bold, design: .monospaced))
                .foregroundColor(ThemeColors.textPrimary)
                .frame(maxWidth: .infinity, minHeight: 38)
                .background(ThemeColors.darkSurfaceVariant)
                .cornerRadius(5)
                .overlay(RoundedRectangle(cornerRadius: 5).stroke(ThemeColors.darkBorder, lineWidth: 1))
        }
    }
    
    private func quickActionButton(_ label: String, action: PocketAction) -> some View {
        Button(action: {
            ActionDispatcher.dispatch(action: action, to: transport, hostOs: settings.hostOs)
            triggerKeyHaptic()
        }) {
            Text(label)
                .font(.system(size: 10, weight: .bold, design: .monospaced))
                .foregroundColor(ThemeColors.cyanAccent)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(ThemeColors.darkSurface)
                .cornerRadius(4)
                .overlay(RoundedRectangle(cornerRadius: 4).stroke(ThemeColors.darkBorder, lineWidth: 1))
        }
    }
    
    private func dispatchKey(_ keyCode: UInt8) {
        var mod: UInt8 = 0
        if isCtrlActive { mod |= HidConstants.MOD_LEFT_CTRL }
        if isAltActive { mod |= HidConstants.MOD_LEFT_ALT }
        if isGuiActive { mod |= HidConstants.MOD_LEFT_GUI }
        if isShiftActive { mod |= HidConstants.MOD_LEFT_SHIFT }
        
        ActionDispatcher.execute(plan: .keyStroke(keyCode: keyCode, modifiers: mod), on: transport)
        
        // Consume sticky shift on standard typing
        if isShiftActive {
            isShiftActive = false
        }
    }
    
    private func triggerKeyHaptic() {
        if settings.hapticEnabled {
            #if os(iOS)
            let impact = UIImpactFeedbackGenerator(style: .light)
            impact.impactOccurred()
            #endif
        }
    }
}
