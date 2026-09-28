//
//  MouseView.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import SwiftUI

public struct MouseView: View {
    @ObservedObject var transport: iOSTransport
    @ObservedObject var settings = AppSettings.shared
    
    @State private var isFastScrollActive: Bool = false
    @State private var lastDragLocation: CGPoint? = nil
    
    public init(transport: iOSTransport) {
        self.transport = transport
    }
    
    public var body: some View {
        VStack(spacing: 8) {
            // Trackpad Header Status & Fast Scroll Toggle
            HStack {
                HStack(spacing: 6) {
                    Image(systemName: "hand.draw.fill")
                        .font(.system(size: 11))
                        .foregroundColor(ThemeColors.cyanAccent)
                    Text("PRECISION TRACKPAD")
                        .font(.system(size: 10, weight: .bold, design: .monospaced))
                        .foregroundColor(ThemeColors.textSecondary)
                }
                
                Spacer()
                
                Button(action: {
                    isFastScrollActive.toggle()
                }) {
                    HStack(spacing: 4) {
                        Image(systemName: isFastScrollActive ? "bolt.fill" : "bolt")
                            .font(.system(size: 10))
                        Text("FAST SCROLL")
                            .font(.system(size: 9, weight: .bold, design: .monospaced))
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .foregroundColor(isFastScrollActive ? ThemeColors.darkBg : ThemeColors.cyanAccent)
                    .background(isFastScrollActive ? ThemeColors.cyanAccent : ThemeColors.darkSurface)
                    .cornerRadius(12)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(ThemeColors.cyanAccent, lineWidth: 1))
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 6)
            
            // Multi-Touch Trackpad Surface
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(ThemeColors.darkSurface)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(ThemeColors.darkBorder, lineWidth: 1.5)
                    )
                
                // Subtle crosshair & scroll arrows
                VStack(spacing: 12) {
                    Image(systemName: "arrow.up.and.down")
                        .font(.system(size: 18, weight: .light))
                        .foregroundColor(ThemeColors.textMuted.opacity(0.3))
                    Text("↕ 2-Finger Scroll  •  Tap to Click  •  2-Finger Right Click")
                        .font(.system(size: 11, weight: .medium, design: .monospaced))
                        .foregroundColor(ThemeColors.textMuted.opacity(0.5))
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding(.horizontal, 12)
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        handleDragChanged(value)
                    }
                    .onEnded { value in
                        handleDragEnded(value)
                    }
            )
            
            // Physical Mouse Buttons (Left, Middle, Right)
            HStack(spacing: 8) {
                mouseButton(label: "LEFT CLICK", buttonMask: HidConstants.MOUSE_BUTTON_LEFT)
                mouseButton(label: "MIDDLE", buttonMask: HidConstants.MOUSE_BUTTON_MIDDLE)
                mouseButton(label: "RIGHT CLICK", buttonMask: HidConstants.MOUSE_BUTTON_RIGHT)
            }
            .padding(.horizontal, 12)
            .padding(.bottom, 8)
        }
        .background(ThemeColors.darkBg)
    }
    
    // MARK: - Gestures & Interactions
    
    private func handleDragChanged(_ value: DragGesture.Value) {
        if let last = lastDragLocation {
            let dx = Float(value.location.x - last.x) * settings.mouseSensitivity
            let dy = Float(value.location.y - last.y) * settings.mouseSensitivity
            
            let clampedDx = Int8(min(127, max(-127, Int(dx))))
            let clampedDy = Int8(min(127, max(-127, Int(dy))))
            
            _ = transport.sendMouseMove(dx: clampedDx, dy: clampedDy, buttons: 0, wheel: 0)
        }
        lastDragLocation = value.location
    }
    
    private func handleDragEnded(_ value: DragGesture.Value) {
        lastDragLocation = nil
        let distance = hypot(value.translation.width, value.translation.height)
        if distance < 5 {
            // Tap = Left Click
            _ = transport.sendMouseMove(dx: 0, dy: 0, buttons: HidConstants.MOUSE_BUTTON_LEFT, wheel: 0)
            _ = transport.sendMouseMove(dx: 0, dy: 0, buttons: 0, wheel: 0)
            triggerClickHaptic()
        }
    }
    
    private func mouseButton(label: String, buttonMask: UInt8) -> some View {
        Button(action: {}) {
            Text(label)
                .font(.system(size: 11, weight: .bold, design: .monospaced))
                .foregroundColor(ThemeColors.textPrimary)
                .frame(maxWidth: .infinity, minHeight: 46)
                .background(ThemeColors.darkSurfaceVariant)
                .cornerRadius(8)
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(ThemeColors.darkBorder, lineWidth: 1))
        }
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in
                    _ = transport.sendMouseMove(dx: 0, dy: 0, buttons: buttonMask, wheel: 0)
                }
                .onEnded { _ in
                    _ = transport.sendMouseMove(dx: 0, dy: 0, buttons: 0, wheel: 0)
                    triggerClickHaptic()
                }
        )
    }
    
    private func triggerClickHaptic() {
        if settings.hapticEnabled {
            #if os(iOS)
            let impact = UIImpactFeedbackGenerator(style: .medium)
            impact.impactOccurred()
            #endif
        }
    }
}
