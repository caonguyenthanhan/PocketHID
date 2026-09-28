//
//  PocketAction.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation

/// Semantic Action tree for PocketHID.
/// Decouples user-intent from OS-specific scancodes and HID protocol reports.
public enum PocketAction: Equatable {
    // System Actions
    case showDesktop
    case lockPC
    case taskView
    case quickSettings
    
    // Navigation & Slide Actions
    case nextSlide
    case prevSlide
    case startPresentation
    case resumePresentation
    case exitPresentation
    case pageUp
    case pageDown
    
    // Edit & Clipboard Actions
    case copy
    case cut
    case paste
    case undo
    case redo
    case selectAll
    case save
    
    // Media & Volume Actions
    case playPause
    case nextTrack
    case prevTrack
    case volumeUp
    case volumeDown
    case mute
    
    // Web / One-Hand Actions
    case webBack
    case webForward
    case webRefresh
    case newTab
    case closeTab
    
    // Raw Keys
    case rawKey(code: UInt8, modifiers: UInt8)
    
    public var identifier: String {
        switch self {
        case .showDesktop: return "system.show_desktop"
        case .lockPC: return "system.lock_pc"
        case .taskView: return "system.task_view"
        case .quickSettings: return "system.quick_settings"
        case .nextSlide: return "presenter.next"
        case .prevSlide: return "presenter.prev"
        case .startPresentation: return "presenter.start"
        case .resumePresentation: return "presenter.resume"
        case .exitPresentation: return "presenter.exit"
        case .pageUp: return "nav.page_up"
        case .pageDown: return "nav.page_down"
        case .copy: return "edit.copy"
        case .cut: return "edit.cut"
        case .paste: return "edit.paste"
        case .undo: return "edit.undo"
        case .redo: return "edit.redo"
        case .selectAll: return "edit.select_all"
        case .save: return "edit.save"
        case .playPause: return "media.play_pause"
        case .nextTrack: return "media.next_track"
        case .prevTrack: return "media.prev_track"
        case .volumeUp: return "media.volume_up"
        case .volumeDown: return "media.volume_down"
        case .mute: return "media.mute"
        case .webBack: return "web.back"
        case .webForward: return "web.forward"
        case .webRefresh: return "web.refresh"
        case .newTab: return "web.new_tab"
        case .closeTab: return "web.close_tab"
        case .rawKey(let code, let mod): return "raw.0x\(String(format: "%02X", code))_mod.0x\(String(format: "%02X", mod))"
        }
    }
}
