//
//  PresenterSafeExitButtonView.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import SwiftUI

public struct PresenterSafeExitButtonView: View {
    var onSafeExit: () -> Void
    
    @State private var safety = PresenterExitSafety()
    @State private var progress: Float = 0.0
    @State private var showWarning: Bool = false
    @State private var timer: Timer? = nil
    
    public init(onSafeExit: @escaping () -> Void) {
        self.onSafeExit = onSafeExit
    }
    
    public var body: some View {
        Button(action: {}) {
            ZStack {
                // Background & Progress Fill
                RoundedRectangle(cornerRadius: 8)
                    .fill(ThemeColors.errorContainer.opacity(0.15))
                
                // Progress Bar overlay
                GeometryReader { geo in
                    Rectangle()
                        .fill(ThemeColors.errorContainer.opacity(0.35))
                        .frame(width: geo.size.width * CGFloat(progress))
                }
                .cornerRadius(8)
                
                // Text label
                HStack(spacing: 6) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 13, weight: .bold))
                    Text(showWarning ? "HOLD 1.5s!" : "EXIT (ESC)")
                        .font(.system(size: 12, weight: .bold, design: .monospaced))
                }
                .foregroundColor(showWarning ? ThemeColors.accentAmber : ThemeColors.errorContainer)
            }
            .frame(height: 48)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(showWarning ? ThemeColors.accentAmber : ThemeColors.errorContainer, lineWidth: 1.5)
            )
        }
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in
                    if !safety.isHolding {
                        startHold()
                    }
                }
                .onEnded { _ in
                    endHold()
                }
        )
    }
    
    private func startHold() {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        safety.onPressDown(currentTimeMs: now)
        showWarning = false
        
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { _ in
            let current = Int64(Date().timeIntervalSince1970 * 1000)
            progress = safety.progress(currentTimeMs: current)
            if progress >= 1.0 {
                // Triggered!
                triggerCompletionHaptic()
                _ = safety.onPressUp(currentTimeMs: current)
                timer?.invalidate()
                timer = nil
                progress = 0.0
                onSafeExit()
            }
        }
    }
    
    private func endHold() {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        timer?.invalidate()
        timer = nil
        
        let didComplete = safety.onPressUp(currentTimeMs: now)
        if !didComplete && progress > 0.05 && progress < 1.0 {
            // Premature release -> Show warning
            showWarning = true
            triggerWarningHaptic()
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                showWarning = false
            }
        }
        progress = 0.0
    }
    
    private func triggerCompletionHaptic() {
        #if os(iOS)
        let impact = UIImpactFeedbackGenerator(style: .heavy)
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
