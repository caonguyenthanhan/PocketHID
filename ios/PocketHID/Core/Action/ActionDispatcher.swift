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
            let press = HIDReportBuilder.buildKeyboardReport(keyCodes: [code], modifiers: modifiers)
            _ = transport.sendRawReport(endpoint: press.endpoint, payload: press.payload)

            // Release key (neutral)
            let release = HIDReportBuilder.buildKeyboardReport(keyCodes: [], modifiers: HidConstants.MOD_NONE)
            return transport.sendRawReport(endpoint: release.endpoint, payload: release.payload)
            
        case .consumerKey(let usageCode):
            let report = HIDReportBuilder.buildConsumerClick(usageCode: usageCode)
            return transport.sendRawReport(endpoint: report.endpoint, payload: report.payload)
            
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
