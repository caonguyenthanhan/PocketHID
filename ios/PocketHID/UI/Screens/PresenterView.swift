//
//  PresenterView.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import SwiftUI

public struct PresenterView: View {
    @ObservedObject var transport: iOSTransport
    @ObservedObject var settings = AppSettings.shared
    
    public init(transport: iOSTransport) {
        self.transport = transport
    }
    
    public var body: some View {
        VStack(spacing: 12) {
            // Top Secondary Controls (Start, Resume, Safe Exit)
            HStack(spacing: 8) {
                presenterActionButton(PocketStrings.presenterStart(settings.language), icon: "play.fill", action: .startPresentation)
                presenterActionButton(PocketStrings.presenterResume(settings.language), icon: "forward.end.fill", action: .resumePresentation)
                
                // Safe Exit with 1.5s hold protection
                PresenterSafeExitButtonView {
                    ActionDispatcher.dispatch(action: .exitPresentation, to: transport, hostOs: settings.hostOs)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            
            // Giant Primary Navigation Buttons: PREVIOUS and NEXT
            HStack(spacing: 12) {
                // Previous Slide
                Button(action: {
                    ActionDispatcher.dispatch(action: .prevSlide, to: transport, hostOs: settings.hostOs)
                    triggerHaptic()
                }) {
                    VStack(spacing: 12) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 38, weight: .bold))
                        Text("PREV")
                            .font(.system(size: 16, weight: .heavy, design: .monospaced))
                    }
                    .foregroundColor(ThemeColors.textPrimary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(ThemeColors.darkSurface)
                    .cornerRadius(16)
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(ThemeColors.darkBorder, lineWidth: 1.5))
                }
                
                // Next Slide (Dominant)
                Button(action: {
                    ActionDispatcher.dispatch(action: .nextSlide, to: transport, hostOs: settings.hostOs)
                    triggerHaptic()
                }) {
                    VStack(spacing: 12) {
                        Image(systemName: "chevron.right")
                            .font(.system(size: 44, weight: .bold))
                        Text("NEXT")
                            .font(.system(size: 18, weight: .heavy, design: .monospaced))
                    }
                    .foregroundColor(ThemeColors.darkBg)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(ThemeColors.cyanAccent)
                    .cornerRadius(16)
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(ThemeColors.cyanAccent, lineWidth: 2))
                }
            }
            .padding(.horizontal, 16)
            
            // Bottom Utility Controls (Black, White, Pointer)
            HStack(spacing: 8) {
                utilityButton("BLACK (B)") {
                    ActionDispatcher.dispatch(action: .rawKey(code: HidConstants.KEY_B, modifiers: 0), to: transport, hostOs: settings.hostOs)
                }
                utilityButton("WHITE (W)") {
                    ActionDispatcher.dispatch(action: .rawKey(code: HidConstants.KEY_W, modifiers: 0), to: transport, hostOs: settings.hostOs)
                }
                utilityButton("PAGE UP") {
                    ActionDispatcher.dispatch(action: .pageUp, to: transport, hostOs: settings.hostOs)
                }
                utilityButton("PAGE DOWN") {
                    ActionDispatcher.dispatch(action: .pageDown, to: transport, hostOs: settings.hostOs)
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 12)
        }
        .background(ThemeColors.darkBg)
    }
    
    // MARK: - Components
    
    private func presenterActionButton(_ title: String, icon: String, action: PocketAction) -> some View {
        Button(action: {
            ActionDispatcher.dispatch(action: action, to: transport, hostOs: settings.hostOs)
            triggerHaptic()
        }) {
            HStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 11, weight: .bold))
                Text(title)
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
            }
            .foregroundColor(ThemeColors.cyanAccent)
            .frame(maxWidth: .infinity, minHeight: 48)
            .background(ThemeColors.darkSurface)
            .cornerRadius(8)
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(ThemeColors.darkBorder, lineWidth: 1))
        }
    }
    
    private func utilityButton(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: {
            action()
            triggerHaptic()
        }) {
            Text(title)
                .font(.system(size: 10, weight: .bold, design: .monospaced))
                .foregroundColor(ThemeColors.textSecondary)
                .frame(maxWidth: .infinity, minHeight: 38)
                .background(ThemeColors.darkSurfaceVariant)
                .cornerRadius(6)
                .overlay(RoundedRectangle(cornerRadius: 6).stroke(ThemeColors.darkBorder, lineWidth: 1))
        }
    }
    
    private func triggerHaptic() {
        if settings.hapticEnabled {
            #if os(iOS)
            let impact = UIImpactFeedbackGenerator(style: .medium)
            impact.impactOccurred()
            #endif
        }
    }
}
