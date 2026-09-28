//
//  GamepadView.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import SwiftUI

public struct GamepadView: View {
    @ObservedObject var transport: iOSTransport
    @ObservedObject var settings = AppSettings.shared
    
    public init(transport: iOSTransport) {
        self.transport = transport
    }
    
    public var body: some View {
        VStack(spacing: 8) {
            // Landscape suggestion chip (Non-intrusive)
            HStack(spacing: 6) {
                Image(systemName: "rotate.right.fill")
                    .font(.system(size: 11))
                    .foregroundColor(ThemeColors.accentAmber)
                Text(PocketStrings.gamepadLandscapeHint(settings.language))
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(ThemeColors.textSecondary)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 5)
            .background(ThemeColors.darkSurface)
            .cornerRadius(12)
            
            // Shoulder Buttons & Triggers (LB, LT, RB, RT)
            HStack(spacing: 12) {
                HStack(spacing: 6) {
                    gamepadPillButton("LT", buttonMask: 0x0040)
                    gamepadPillButton("LB", buttonMask: 0x0100)
                }
                
                Spacer()
                
                // System Buttons (Select, Guide, Start)
                HStack(spacing: 8) {
                    gamepadSystemButton("SELECT", buttonMask: 0x0010)
                    gamepadSystemButton("GUIDE", buttonMask: 0x1000)
                    gamepadSystemButton("START", buttonMask: 0x0020)
                }
                
                Spacer()
                
                HStack(spacing: 6) {
                    gamepadPillButton("RB", buttonMask: 0x0200)
                    gamepadPillButton("RT", buttonMask: 0x0080)
                }
            }
            .padding(.horizontal, 16)
            
            Spacer()
            
            // Main Controls Zone: D-Pad (Left) and ABXY (Right)
            HStack(alignment: .center) {
                // D-Pad Cluster
                VStack(spacing: 4) {
                    dpadButton(icon: "chevron.up", mask: 0x0001)
                    HStack(spacing: 4) {
                        dpadButton(icon: "chevron.left", mask: 0x0004)
                        Circle()
                            .fill(ThemeColors.darkBorder)
                            .frame(width: 28, height: 28)
                        dpadButton(icon: "chevron.right", mask: 0x0008)
                    }
                    dpadButton(icon: "chevron.down", mask: 0x0002)
                }
                .frame(width: 130)
                
                Spacer()
                
                // Thumbsticks Zone (Center)
                HStack(spacing: 24) {
                    thumbstickView(label: "LS")
                    thumbstickView(label: "RS")
                }
                
                Spacer()
                
                // ABXY Action Cluster
                VStack(spacing: 6) {
                    abxyButton(label: "Y", color: ThemeColors.accentAmber, mask: 0x0008)
                    HStack(spacing: 18) {
                        abxyButton(label: "X", color: ThemeColors.primaryBlue, mask: 0x0004)
                        abxyButton(label: "B", color: Color.red, mask: 0x0002)
                    }
                    abxyButton(label: "A", color: Color.green, mask: 0x0001)
                }
                .frame(width: 130)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 16)
        }
        .background(ThemeColors.darkBg)
    }
    
    // MARK: - Components
    
    private func dpadButton(icon: String, mask: UInt16) -> some View {
        Button(action: {}) {
            Image(systemName: icon)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(ThemeColors.textPrimary)
                .frame(width: 38, height: 38)
                .background(ThemeColors.darkSurfaceVariant)
                .cornerRadius(8)
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(ThemeColors.darkBorder, lineWidth: 1))
        }
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in dispatchGamepad(buttons: mask) }
                .onEnded { _ in dispatchGamepad(buttons: 0) }
        )
    }
    
    private func abxyButton(label: String, color: Color, mask: UInt16) -> some View {
        Button(action: {}) {
            Text(label)
                .font(.system(size: 14, weight: .black))
                .foregroundColor(color)
                .frame(width: 38, height: 38)
                .background(ThemeColors.darkSurfaceVariant)
                .clipShape(Circle())
                .overlay(Circle().stroke(color.opacity(0.8), lineWidth: 1.5))
        }
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in dispatchGamepad(buttons: mask) }
                .onEnded { _ in dispatchGamepad(buttons: 0) }
        )
    }
    
    private func gamepadPillButton(_ label: String, buttonMask: UInt16) -> some View {
        Button(action: {}) {
            Text(label)
                .font(.system(size: 11, weight: .bold, design: .monospaced))
                .foregroundColor(ThemeColors.cyanAccent)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(ThemeColors.darkSurface)
                .cornerRadius(6)
                .overlay(RoundedRectangle(cornerRadius: 6).stroke(ThemeColors.darkBorder, lineWidth: 1))
        }
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in dispatchGamepad(buttons: buttonMask) }
                .onEnded { _ in dispatchGamepad(buttons: 0) }
        )
    }
    
    private func gamepadSystemButton(_ label: String, buttonMask: UInt16) -> some View {
        Button(action: {}) {
            Text(label)
                .font(.system(size: 9, weight: .bold, design: .monospaced))
                .foregroundColor(ThemeColors.textMuted)
                .padding(.horizontal, 6)
                .padding(.vertical, 4)
                .background(ThemeColors.darkSurface)
                .cornerRadius(4)
        }
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in dispatchGamepad(buttons: buttonMask) }
                .onEnded { _ in dispatchGamepad(buttons: 0) }
        )
    }
    
    private func thumbstickView(label: String) -> some View {
        ZStack {
            Circle()
                .fill(ThemeColors.darkSurface)
                .frame(width: 60, height: 60)
                .overlay(Circle().stroke(ThemeColors.darkBorder, lineWidth: 1.5))
            
            Circle()
                .fill(ThemeColors.darkSurfaceVariant)
                .frame(width: 36, height: 36)
                .overlay(Circle().stroke(ThemeColors.cyanAccent.opacity(0.5), lineWidth: 1))
            
            Text(label)
                .font(.system(size: 9, weight: .bold, design: .monospaced))
                .foregroundColor(ThemeColors.textMuted)
        }
    }
    
    private func dispatchGamepad(buttons: UInt16) {
        _ = transport.sendGamepadReport(
            buttons: buttons,
            leftStickX: 0,
            leftStickY: 0,
            rightStickX: 0,
            rightStickY: 0,
            leftTrigger: 0,
            rightTrigger: 0
        )
        if settings.hapticEnabled {
            #if os(iOS)
            let impact = UIImpactFeedbackGenerator(style: .light)
            impact.impactOccurred()
            #endif
        }
    }
}
