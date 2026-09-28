//
//  ActionResolver.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation

public enum ActionExecutionPlan: Equatable {
    case keyStroke(keyCode: UInt8, modifiers: UInt8)
    case consumerKey(usageCode: UInt16)
    case sequence([ActionExecutionPlan])
    case none
}

/// Resolves semantic actions to OS-specific scancodes and modifier plans.
public struct ActionResolver {
    
    public static func resolve(action: PocketAction, for hostOs: HostOs) -> ActionExecutionPlan {
        switch action {
        // System Actions
        case .showDesktop:
            switch hostOs {
            case .windows, .linux:
                return .keyStroke(keyCode: HidConstants.KEY_D, modifiers: HidConstants.MOD_LEFT_GUI)
            case .macOS:
                return .keyStroke(keyCode: HidConstants.KEY_F11, modifiers: HidConstants.MOD_NONE)
            }
            
        case .lockPC:
            switch hostOs {
            case .windows:
                return .keyStroke(keyCode: HidConstants.KEY_L, modifiers: HidConstants.MOD_LEFT_GUI)
            case .macOS:
                return .keyStroke(keyCode: HidConstants.KEY_Q, modifiers: HidConstants.MOD_LEFT_GUI | HidConstants.MOD_LEFT_CTRL)
            case .linux:
                return .keyStroke(keyCode: HidConstants.KEY_L, modifiers: HidConstants.MOD_LEFT_GUI)
            }
            
        case .taskView:
            switch hostOs {
            case .windows:
                return .keyStroke(keyCode: HidConstants.KEY_TAB, modifiers: HidConstants.MOD_LEFT_GUI)
            case .macOS:
                return .keyStroke(keyCode: HidConstants.KEY_UP, modifiers: HidConstants.MOD_LEFT_CTRL)
            case .linux:
                return .keyStroke(keyCode: HidConstants.KEY_TAB, modifiers: HidConstants.MOD_LEFT_ALT)
            }
            
        case .quickSettings:
            return .keyStroke(keyCode: HidConstants.KEY_A, modifiers: HidConstants.MOD_LEFT_GUI)
            
        // Navigation & Presenter
        case .nextSlide:
            return .keyStroke(keyCode: HidConstants.KEY_RIGHT, modifiers: HidConstants.MOD_NONE)
            
        case .prevSlide:
            return .keyStroke(keyCode: HidConstants.KEY_LEFT, modifiers: HidConstants.MOD_NONE)
            
        case .startPresentation:
            return .keyStroke(keyCode: HidConstants.KEY_F5, modifiers: HidConstants.MOD_NONE)
            
        case .resumePresentation:
            return .keyStroke(keyCode: HidConstants.KEY_F5, modifiers: HidConstants.MOD_LEFT_SHIFT)
            
        case .exitPresentation:
            return .keyStroke(keyCode: HidConstants.KEY_ESC, modifiers: HidConstants.MOD_NONE)
            
        case .pageUp:
            return .keyStroke(keyCode: HidConstants.KEY_PAGEUP, modifiers: HidConstants.MOD_NONE)
            
        case .pageDown:
            return .keyStroke(keyCode: HidConstants.KEY_PAGEDOWN, modifiers: HidConstants.MOD_NONE)
            
        // Edit Actions
        case .copy:
            let mod = hostOs == .macOS ? HidConstants.MOD_LEFT_GUI : HidConstants.MOD_LEFT_CTRL
            return .keyStroke(keyCode: HidConstants.KEY_C, modifiers: mod)
            
        case .cut:
            let mod = hostOs == .macOS ? HidConstants.MOD_LEFT_GUI : HidConstants.MOD_LEFT_CTRL
            return .keyStroke(keyCode: HidConstants.KEY_X, modifiers: mod)
            
        case .paste:
            let mod = hostOs == .macOS ? HidConstants.MOD_LEFT_GUI : HidConstants.MOD_LEFT_CTRL
            return .keyStroke(keyCode: HidConstants.KEY_V, modifiers: mod)
            
        case .undo:
            let mod = hostOs == .macOS ? HidConstants.MOD_LEFT_GUI : HidConstants.MOD_LEFT_CTRL
            return .keyStroke(keyCode: HidConstants.KEY_Z, modifiers: mod)
            
        case .redo:
            if hostOs == .macOS {
                return .keyStroke(keyCode: HidConstants.KEY_Z, modifiers: HidConstants.MOD_LEFT_GUI | HidConstants.MOD_LEFT_SHIFT)
            } else {
                return .keyStroke(keyCode: HidConstants.KEY_Y, modifiers: HidConstants.MOD_LEFT_CTRL)
            }
            
        case .selectAll:
            let mod = hostOs == .macOS ? HidConstants.MOD_LEFT_GUI : HidConstants.MOD_LEFT_CTRL
            return .keyStroke(keyCode: HidConstants.KEY_A, modifiers: mod)
            
        case .save:
            let mod = hostOs == .macOS ? HidConstants.MOD_LEFT_GUI : HidConstants.MOD_LEFT_CTRL
            return .keyStroke(keyCode: HidConstants.KEY_S, modifiers: mod)
            
        // Media Actions
        case .playPause:
            return .consumerKey(usageCode: HidConstants.CONSUMER_PLAY_PAUSE)
            
        case .nextTrack:
            return .consumerKey(usageCode: HidConstants.CONSUMER_NEXT_TRACK)
            
        case .prevTrack:
            return .consumerKey(usageCode: HidConstants.CONSUMER_PREV_TRACK)
            
        case .volumeUp:
            return .consumerKey(usageCode: HidConstants.CONSUMER_VOLUME_UP)
            
        case .volumeDown:
            return .consumerKey(usageCode: HidConstants.CONSUMER_VOLUME_DOWN)
            
        case .mute:
            return .consumerKey(usageCode: HidConstants.CONSUMER_MUTE)
            
        // Web Actions
        case .webBack:
            if hostOs == .macOS {
                return .keyStroke(keyCode: HidConstants.KEY_LEFTBRACE, modifiers: HidConstants.MOD_LEFT_GUI)
            } else {
                return .keyStroke(keyCode: HidConstants.KEY_LEFT, modifiers: HidConstants.MOD_LEFT_ALT)
            }
            
        case .webForward:
            if hostOs == .macOS {
                return .keyStroke(keyCode: HidConstants.KEY_RIGHTBRACE, modifiers: HidConstants.MOD_LEFT_GUI)
            } else {
                return .keyStroke(keyCode: HidConstants.KEY_RIGHT, modifiers: HidConstants.MOD_LEFT_ALT)
            }
            
        case .webRefresh:
            return hostOs == .macOS
                ? .keyStroke(keyCode: HidConstants.KEY_R, modifiers: HidConstants.MOD_LEFT_GUI)
                : .keyStroke(keyCode: HidConstants.KEY_F5, modifiers: HidConstants.MOD_NONE)
                
        case .newTab:
            let mod = hostOs == .macOS ? HidConstants.MOD_LEFT_GUI : HidConstants.MOD_LEFT_CTRL
            return .keyStroke(keyCode: HidConstants.KEY_T, modifiers: mod)
            
        case .closeTab:
            let mod = hostOs == .macOS ? HidConstants.MOD_LEFT_GUI : HidConstants.MOD_LEFT_CTRL
            return .keyStroke(keyCode: HidConstants.KEY_W, modifiers: mod)
            
        case .rawKey(let code, let mod):
            return .keyStroke(keyCode: code, modifiers: mod)
        }
    }
}
