//
//  DeckModeSwitcherView.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import SwiftUI

public struct DeckModeSwitcherView: View {
    @Binding var selectedMode: ControlMode
    @ObservedObject var focusLock = FocusLockController.shared
    
    public init(selectedMode: Binding<ControlMode>) {
        self._selectedMode = selectedMode
    }
    
    public var body: some View {
        HStack(spacing: 4) {
            ForEach(ControlMode.allCases) { mode in
                Button(action: {
                    if focusLock.canSwitchMode() {
                        selectedMode = mode
                        triggerHaptic()
                    }
                }) {
                    Text(mode.title.uppercased())
                        .font(.system(size: 11, weight: selectedMode == mode ? .heavy : .bold, design: .monospaced))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .foregroundColor(selectedMode == mode ? ThemeColors.cyanAccent : (focusLock.isLocked ? ThemeColors.textMuted.opacity(0.4) : ThemeColors.textSecondary))
                        .background(selectedMode == mode ? ThemeColors.darkSurfaceVariant : Color.clear)
                        .cornerRadius(6)
                        .overlay(
                            Rectangle()
                                .fill(selectedMode == mode ? ThemeColors.cyanAccent : Color.clear)
                                .frame(height: 2),
                            alignment: .bottom
                        )
                }
                .buttonStyle(PlainButtonStyle())
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(ThemeColors.darkSurface)
        .cornerRadius(8)
    }
    
    private func triggerHaptic() {
        #if os(iOS)
        let impact = UIImpactFeedbackGenerator(style: .light)
        impact.impactOccurred()
        #endif
    }
}
