//
//  ActionDispatcher.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation

/// Dispatches execution plans onto an active HIDTransport instance.
public struct ActionDispatcher {
    
    @discardableResult
    public static func dispatch(
        action: PocketAction,
        to transport: HIDTransport?,
        hostOs: HostOs = .windows
    ) -> Bool {
        guard let transport = transport else { return false }
        let plan = ActionResolver.resolve(action: action, for: hostOs)
        return execute(plan: plan, on: transport)
    }
    
    @discardableResult
    public static func execute(plan: ActionExecutionPlan, on transport: HIDTransport) -> Bool {
        switch plan {
        case .keyStroke(let code, let modifiers):
            // Press key
            _ = transport.sendKeyboardReport(keyCodes: [code], modifiers: modifiers)
            // Release key (neutral)
            return transport.sendKeyboardReport(keyCodes: [], modifiers: HidConstants.MOD_NONE)
            
        case .consumerKey(let usageCode):
            return transport.sendConsumerClick(usageCode: usageCode)
            
        case .sequence(let plans):
            var success = true
            for p in plans {
                if !execute(plan: p, on: transport) {
                    success = false
                }
            }
            return success
            
        case .none:
            return true
        }
    }
}
