//
//  ModeNavigationBar.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import SwiftUI

public struct ModeNavigationBar: View {
    @Binding var selectedMode: ControlMode
    @ObservedObject var focusLock = FocusLockController.shared
    
    public init(selectedMode: Binding<ControlMode>) {
        self._selectedMode = selectedMode
    }
    
    public var body: some View {
        HStack(spacing: 0) {
            ForEach(ControlMode.allCases) { mode in
                Button(action: {
                    handleSelect(mode: mode)
                }) {
                    VStack(spacing: 4) {
                        Image(systemName: mode.systemIconName)
                            .font(.system(size: 16, weight: selectedMode == mode ? .bold : .regular))
                            .foregroundColor(iconColor(for: mode))
                        
                        Text(mode.title)
                            .font(.system(size: 10, weight: selectedMode == mode ? .bold : .medium))
                            .foregroundColor(textColor(for: mode))
                            .lineLimit(1)
                            .fixedSize(horizontal: true, vertical: false)
                        
                        // Active cyan indicator bar
                        Rectangle()
                            .fill(selectedMode == mode ? ThemeColors.cyanAccent : Color.clear)
                            .frame(height: 2)
                            .cornerRadius(1)
                            .padding(.horizontal, 8)
                    }
                    .frame(maxWidth: .infinity)
                    .contentShape(Rectangle())
                }
                .buttonStyle(PlainButtonStyle())
            }
        }
        .padding(.vertical, 8)
        .background(ThemeColors.darkSurface)
        .overlay(
            Rectangle()
                .fill(ThemeColors.darkBorder)
                .frame(height: 1),
            alignment: .top
        )
    }
    
    private func handleSelect(mode: ControlMode) {
        if focusLock.canSwitchMode() {
            selectedMode = mode
            triggerHaptic()
        } else {
            // Mode switch blocked by Focus Lock
            triggerWarningHaptic()
        }
    }
    
    private func iconColor(for mode: ControlMode) -> Color {
        if selectedMode == mode {
            return ThemeColors.cyanAccent
        }
        return focusLock.isLocked ? ThemeColors.textMuted.opacity(0.4) : ThemeColors.textSecondary
    }
    
    private func textColor(for mode: ControlMode) -> Color {
        if selectedMode == mode {
            return ThemeColors.textPrimary
        }
        return focusLock.isLocked ? ThemeColors.textMuted.opacity(0.4) : ThemeColors.textSecondary
    }
    
    private func triggerHaptic() {
        #if os(iOS)
        let impact = UIImpactFeedbackGenerator(style: .light)
        impact.impactOccurred()
        #endif
    }
    
    private func triggerWarningHaptic() {
        #if os(iOS)
        let notification = UINotificationFeedbackGenerator()
        notification.notificationOccurred(.warning)
        #endif
    }
}
