//
//  OneHandView.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import SwiftUI

public enum OneHandSubMode: String, CaseIterable, Identifiable {
    case web = "WEB"
    case video = "VIDEO"
    
    public var id: String { rawValue }
}

public struct OneHandView: View {
    @ObservedObject var transport: iOSTransport
    @ObservedObject var settings = AppSettings.shared
    
    @State private var subMode: OneHandSubMode = .web
    @State private var isRightHanded: Bool = true
    
    public init(transport: iOSTransport) {
        self.transport = transport
    }
    
    public var body: some View {
        VStack(spacing: 12) {
            // Mode Switcher (WEB vs VIDEO) & Handedness toggle
            HStack {
                Picker("Submode", selection: $subMode) {
                    ForEach(OneHandSubMode.allCases) { mode in
                        Text(mode.rawValue).tag(mode)
                    }
                }
                .pickerStyle(SegmentedPickerStyle())
                .frame(width: 180)
                
                Spacer()
                
                Button(action: {
                    isRightHanded.toggle()
                }) {
                    HStack(spacing: 4) {
                        Image(systemName: isRightHanded ? "hand.point.right.fill" : "hand.point.left.fill")
                            .font(.system(size: 11))
                        Text(isRightHanded ? "R-Hand" : "L-Hand")
                            .font(.system(size: 10, weight: .bold, design: .monospaced))
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 5)
                    .foregroundColor(ThemeColors.cyanAccent)
                    .background(ThemeColors.darkSurface)
                    .cornerRadius(12)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(ThemeColors.darkBorder, lineWidth: 1))
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 6)
            
            Spacer()
            
            // Thumb-Optimized Interactive Arc Zone
            if subMode == .web {
                webControlsArc
            } else {
                videoControlsArc
            }
            
            Spacer()
        }
        .background(ThemeColors.darkBg)
    }
    
    // MARK: - Web Controls
    
    private var webControlsArc: some View {
        VStack(spacing: 16) {
            // Virtual Scroll Roller Zone
            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .fill(ThemeColors.darkSurface)
                    .frame(height: 120)
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(ThemeColors.darkBorder, lineWidth: 1.5))
                
                VStack(spacing: 6) {
                    Image(systemName: "circle.circle")
                        .font(.system(size: 24))
                        .foregroundColor(ThemeColors.cyanAccent)
                    Text("↕ THUMB SCROLL ROLLER")
                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                        .foregroundColor(ThemeColors.textSecondary)
                }
            }
            .padding(.horizontal, 24)
            .gesture(
                DragGesture()
                    .onChanged { value in
                        let deltaY = Int8(min(127, max(-127, -Int(value.translation.height / 10))))
                        _ = transport.sendMouseMove(dx: 0, dy: 0, buttons: 0, wheel: deltaY)
                    }
            )
            
            // Primary Browser Action Buttons
            HStack(spacing: 12) {
                oneHandButton("BACK", icon: "chevron.left") {
                    ActionDispatcher.dispatch(action: .webBack, to: transport, hostOs: settings.hostOs)
                }
                oneHandButton("REFRESH", icon: "arrow.clockwise") {
                    ActionDispatcher.dispatch(action: .webRefresh, to: transport, hostOs: settings.hostOs)
                }
                oneHandButton("FORWARD", icon: "chevron.right") {
                    ActionDispatcher.dispatch(action: .webForward, to: transport, hostOs: settings.hostOs)
                }
            }
            .padding(.horizontal, 20)
        }
    }
    
    // MARK: - Video Controls
    
    private var videoControlsArc: some View {
        VStack(spacing: 16) {
            // Large Play/Pause Toggle
            Button(action: {
                ActionDispatcher.dispatch(action: .playPause, to: transport, hostOs: settings.hostOs)
                triggerHaptic()
            }) {
                ZStack {
                    Circle()
                        .fill(ThemeColors.darkSurface)
                        .frame(width: 100, height: 100)
                        .overlay(Circle().stroke(ThemeColors.cyanAccent, lineWidth: 2))
                    
                    Image(systemName: "playpause.fill")
                        .font(.system(size: 36, weight: .bold))
                        .foregroundColor(ThemeColors.cyanAccent)
                }
            }
            
            // Volume & Seek Buttons
            HStack(spacing: 16) {
                oneHandButton("VOL -", icon: "speaker.minus.fill") {
                    ActionDispatcher.dispatch(action: .volumeDown, to: transport, hostOs: settings.hostOs)
                }
                oneHandButton("MUTE", icon: "speaker.slash.fill") {
                    ActionDispatcher.dispatch(action: .mute, to: transport, hostOs: settings.hostOs)
                }
                oneHandButton("VOL +", icon: "speaker.plus.fill") {
                    ActionDispatcher.dispatch(action: .volumeUp, to: transport, hostOs: settings.hostOs)
                }
            }
            .padding(.horizontal, 20)
        }
    }
    
    private func oneHandButton(_ label: String, icon: String, action: @escaping () -> Void) -> some View {
        Button(action: {
            action()
            triggerHaptic()
        }) {
            VStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.system(size: 16, weight: .bold))
                Text(label)
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
            }
            .foregroundColor(ThemeColors.textPrimary)
            .frame(maxWidth: .infinity, minHeight: 64)
            .background(ThemeColors.darkSurfaceVariant)
            .cornerRadius(12)
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(ThemeColors.darkBorder, lineWidth: 1))
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
