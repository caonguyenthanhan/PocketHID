//
//  FocusLockController.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation
import Combine

/// Single source of truth for Focus Mode / Global Mode Navigation Lock.
///
/// Product Definition:
/// FOCUS = GLOBAL INTERACTION LOCK
/// Once the user has entered any control mode, FOCUS prevents accidental navigation
/// or mode switching, so the user can operate the active controller without jumping to another mode.
///
/// Focus is NOT a general input lock or touch freeze: all valid controls, buttons,
/// keys, and gestures INSIDE the active mode continue executing at 100% full fidelity.
public final class FocusLockController: ObservableObject {
    public static let shared = FocusLockController()
    
    @Published public private(set) var isLocked: Bool = false
    
    public init() {}
    
    /// Toggles the lock state.
    @discardableResult
    public func toggle() -> Bool {
        isLocked.toggle()
        return isLocked
    }
    
    /// Explicitly sets the lock state.
    public func setLocked(_ locked: Bool) {
        isLocked = locked
    }
    
    /// Resets the lock state to OFF (unlocked).
    public func reset() {
        isLocked = false
    }
    
    /// Checks if mode switching is currently allowed.
    public func canSwitchMode() -> Bool {
        return !isLocked
    }
    
    /// Resolves an Int-indexed mode switch request.
    /// If locked, returns `currentMode` (switch rejected).
    /// If unlocked, returns `targetMode` (switch allowed).
    public func resolveModeSwitch(currentMode: Int, targetMode: Int) -> Int {
        return isLocked ? currentMode : targetMode
    }
    
    /// Resolves a `ControlMode` mode switch request.
    public func resolveModeSwitch(currentMode: ControlMode, targetMode: ControlMode) -> ControlMode {
        return isLocked ? currentMode : targetMode
    }
    
    /// Evaluates whether a mode-switch navigation gesture should be allowed.
    public func allowNavigationGesture() -> Bool {
        return !isLocked
    }
    
    /// Checks whether an internal action inside the current mode is permitted.
    /// Always returns true because Focus is a navigation lock, never an input lock.
    public func allowsInternalAction() -> Bool {
        return true
    }
}
