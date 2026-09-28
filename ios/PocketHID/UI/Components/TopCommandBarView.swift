//
//  TopCommandBarView.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import SwiftUI

public struct TopCommandBarView: View {
    @ObservedObject var focusLock = FocusLockController.shared
    @ObservedObject var transport: iOSTransport
    var onToggleOrientation: () -> Void = {}
    var onOpenSettings: () -> Void = {}
    
    public init(
        transport: iOSTransport,
        onToggleOrientation: @escaping () -> Void = {},
        onOpenSettings: @escaping () -> Void = {}
    ) {
        self.transport = transport
        self.onToggleOrientation = onToggleOrientation
        self.onOpenSettings = onOpenSettings
    }
    
    public var body: some View {
        HStack(spacing: 8) {
            // Connection & Capability Status Pill
            HStack(spacing: 6) {
                Circle()
                    .fill(statusColor)
                    .frame(width: 8, height: 8)
                
                Text(statusText)
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .foregroundColor(ThemeColors.textPrimary)
                    .lineLimit(1)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(ThemeColors.darkSurface)
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(ThemeColors.darkBorder, lineWidth: 1)
            )
            
            Spacer()
            
            // Global Focus Mode Button (Single Source of Truth)
            Button(action: {
                focusLock.toggle()
                triggerHaptic()
            }) {
                HStack(spacing: 5) {
                    Image(systemName: focusLock.isLocked ? "lock.fill" : "lock.open")
                        .font(.system(size: 10, weight: .bold))
                    Text("FOCUS")
                        .font(.system(size: 11, weight: .black, design: .monospaced))
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .foregroundColor(focusLock.isLocked ? ThemeColors.darkBg : ThemeColors.cyanAccent)
                .background(focusLock.isLocked ? ThemeColors.cyanAccent : ThemeColors.darkSurface)
                .cornerRadius(16)
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(ThemeColors.cyanAccent, lineWidth: 1)
                )
            }
            
            // Quick Orientation Toggle
            Button(action: onToggleOrientation) {
                Image(systemName: "arrow.triangle.2.circlepath")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(ThemeColors.textSecondary)
                    .padding(7)
                    .background(ThemeColors.darkSurface)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(ThemeColors.darkBorder, lineWidth: 1))
            }
            
            // Settings Button
            Button(action: onOpenSettings) {
                Image(systemName: "gearshape.fill")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(ThemeColors.textSecondary)
                    .padding(7)
                    .background(ThemeColors.darkSurface)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(ThemeColors.darkBorder, lineWidth: 1))
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(ThemeColors.darkBg)
    }
    
    private var statusColor: Color {
        switch transport.status {
        case .connected:
            return Color.green
        case .connecting:
            return ThemeColors.cyanAccent
        case .unavailable:
            return ThemeColors.accentAmber
        case .notConnected, .disconnected, .disconnecting:
            return ThemeColors.textMuted
        case .error:
            return ThemeColors.errorContainer
        }
    }
    
    private var statusText: String {
        switch transport.status {
        case .connected(let name):
            return "Connected: \(name)"
        case .connecting(let name):
            return "Connecting \(name ?? "")…"
        case .unavailable:
            return "Direct HID: Unavailable"
        case .notConnected, .disconnected:
            return "Not Connected"
        case .disconnecting:
            return "Disconnecting…"
        case .error(let msg):
            return "Error: \(msg)"
        }
    }
    
    private func triggerHaptic() {
        #if os(iOS)
        let impact = UIImpactFeedbackGenerator(style: .medium)
        impact.impactOccurred()
        #endif
    }
}
