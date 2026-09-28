//
//  SettingsView.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import SwiftUI

public struct SettingsView: View {
    @ObservedObject var settings = AppSettings.shared
    @ObservedObject var transport: iOSTransport
    var onDismiss: () -> Void
    
    public init(transport: iOSTransport, onDismiss: @escaping () -> Void) {
        self.transport = transport
        self.onDismiss = onDismiss
    }
    
    public var body: some View {
        NavigationView {
            ZStack {
                ThemeColors.darkBg.edgesIgnoringSafeArea(.all)
                
                ScrollView {
                    VStack(spacing: 20) {
                        // Platform Capability & Architecture Gate Notice
                        capabilityNoticeCard
                        
                        // General Settings
                        settingsSection("GENERAL") {
                            // Language
                            HStack {
                                Text("Language")
                                    .foregroundColor(ThemeColors.textPrimary)
                                Spacer()
                                Picker("Language", selection: $settings.language) {
                                    ForEach(AppLanguage.allCases) { lang in
                                        Text(lang.displayName).tag(lang)
                                    }
                                }
                                .pickerStyle(MenuPickerStyle())
                            }
                            
                            Divider().background(ThemeColors.darkBorder)
                            
                            // Haptics
                            Toggle("Haptic Feedback", isOn: $settings.hapticEnabled)
                                .foregroundColor(ThemeColors.textPrimary)
                            
                            Divider().background(ThemeColors.darkBorder)
                            
                            // Target Host OS
                            HStack {
                                Text("Target Host OS")
                                    .foregroundColor(ThemeColors.textPrimary)
                                Spacer()
                                Picker("Host OS", selection: $settings.hostOs) {
                                    ForEach(HostOs.allCases) { os in
                                        Text(os.displayName).tag(os)
                                    }
                                }
                                .pickerStyle(MenuPickerStyle())
                            }
                        }
                        
                        // Mouse Settings
                        settingsSection("MOUSE & TRACKPAD") {
                            VStack(alignment: .leading, spacing: 8) {
                                HStack {
                                    Text("Sensitivity")
                                        .foregroundColor(ThemeColors.textPrimary)
                                    Spacer()
                                    Text(String(format: "%.1fx", settings.mouseSensitivity))
                                        .font(.system(size: 12, design: .monospaced))
                                        .foregroundColor(ThemeColors.cyanAccent)
                                }
                                Slider(value: $settings.mouseSensitivity, in: 0.5...3.0, step: 0.1)
                                    .accentColor(ThemeColors.cyanAccent)
                            }
                        }
                        
                        // Drawing Settings
                        settingsSection("GRAPHICS TABLET") {
                            HStack {
                                Text("Aspect Ratio Policy")
                                    .foregroundColor(ThemeColors.textPrimary)
                                Spacer()
                                Picker("Policy", selection: $settings.aspectRatioPolicy) {
                                    ForEach(AspectRatioPolicy.allCases) { policy in
                                        Text(policy.rawValue).tag(policy)
                                    }
                                }
                                .pickerStyle(SegmentedPickerStyle())
                                .frame(width: 160)
                            }
                        }
                        
                        // Telemetry Summary
                        settingsSection("TELEMETRY (LOCAL DISPATCH)") {
                            HStack {
                                Text("Local Actions Dispatched")
                                    .foregroundColor(ThemeColors.textSecondary)
                                Spacer()
                                Text("\(transport.totalLocalActionsDispatched)")
                                    .font(.system(size: 13, weight: .bold, design: .monospaced))
                                    .foregroundColor(ThemeColors.cyanAccent)
                            }
                            Divider().background(ThemeColors.darkBorder)
                            HStack {
                                Text("Last Output Action")
                                    .foregroundColor(ThemeColors.textSecondary)
                                Spacer()
                                Text(transport.lastDispatchedLabel)
                                    .font(.system(size: 11, design: .monospaced))
                                    .foregroundColor(ThemeColors.textPrimary)
                            }
                        }
                    }
                    .padding(16)
                }
            }
            .navigationBarTitle(PocketStrings.settingsTitle(settings.language), displayMode: .inline)
            .navigationBarItems(
                trailing: Button("Done") {
                    onDismiss()
                }
                .foregroundColor(ThemeColors.cyanAccent)
            )
        }
    }
    
    private var capabilityNoticeCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 6) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundColor(ThemeColors.accentAmber)
                Text("iOS Bluetooth Architecture Gate")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(ThemeColors.textPrimary)
            }
            
            Text(transport.capability.technicalNotes)
                .font(.system(size: 11))
                .foregroundColor(ThemeColors.textSecondary)
                .lineSpacing(3)
        }
        .padding(14)
        .background(ThemeColors.darkSurface)
        .cornerRadius(12)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(ThemeColors.accentAmber.opacity(0.5), lineWidth: 1))
    }
    
    private func settingsSection<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.system(size: 11, weight: .bold, design: .monospaced))
                .foregroundColor(ThemeColors.textMuted)
                .padding(.horizontal, 4)
            
            VStack(spacing: 12) {
                content()
            }
            .padding(14)
            .background(ThemeColors.darkSurface)
            .cornerRadius(12)
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(ThemeColors.darkBorder, lineWidth: 1))
        }
    }
}
