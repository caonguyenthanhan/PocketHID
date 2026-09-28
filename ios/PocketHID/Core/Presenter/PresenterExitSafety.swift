//
//  PresenterExitSafety.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation

/// Pure state machine enforcing a 1.5-second hold before triggering presentation exit.
/// Prevents accidental exit from single taps during high-stress presentations.
public final class PresenterExitSafety {
    public static let requiredHoldDurationMs: Int64 = 1500
    
    public private(set) var pressStartTimeMs: Int64 = 0
    public private(set) var isHolding: Bool = false
    public private(set) var wasTriggered: Bool = false
    
    public init() {}
    
    /// User touches down on EXIT button.
    public func onPressDown(currentTimeMs: Int64) {
        pressStartTimeMs = currentTimeMs
        isHolding = true
        wasTriggered = false
    }
    
    /// User lifts touch.
    /// - Returns: True if held for at least 1500ms, false if released prematurely.
    public func onPressUp(currentTimeMs: Int64) -> Bool {
        guard isHolding else { return false }
        isHolding = false
        let elapsed = currentTimeMs - pressStartTimeMs
        if elapsed >= PresenterExitSafety.requiredHoldDurationMs {
            wasTriggered = true
            return true
        }
        return false
    }
    
    /// Cancels hold (e.g. gesture interrupted, touch canceled).
    public func cancel() {
        isHolding = false
        pressStartTimeMs = 0
        wasTriggered = false
    }
    
    /// Calculates current hold progress [0.0...1.0].
    public func progress(currentTimeMs: Int64) -> Float {
        guard isHolding else { return 0.0 }
        let elapsed = currentTimeMs - pressStartTimeMs
        if elapsed <= 0 { return 0.0 }
        return min(1.0, Float(elapsed) / Float(PresenterExitSafety.requiredHoldDurationMs))
    }
}
