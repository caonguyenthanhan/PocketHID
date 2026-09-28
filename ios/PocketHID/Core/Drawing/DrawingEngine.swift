//
//  DrawingEngine.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation
import Combine

/// Pure local drawing state machine for iOS.
/// Ensures the drawing canvas remains 100% responsive and functional
/// with smooth local preview, stroke history, and eraser interaction.
public final class DrawingEngine: ObservableObject {
    public static let maxHistory: Int = 50
    
    @Published public private(set) var strokes: [DrawingStroke] = []
    @Published public private(set) var currentDraftStroke: DrawingStroke? = nil
    
    @Published public var activeTool: DrawingTool = .pen
    @Published public var activeColor: DrawingColor = .cyan
    @Published public var activeStrokeWidth: Float = 4.0
    
    private var undoStack: [[DrawingStroke]] = []
    private var redoStack: [[DrawingStroke]] = []
    
    public init() {}
    
    public var canUndo: Bool {
        !undoStack.isEmpty
    }
    
    public var canRedo: Bool {
        !redoStack.isEmpty
    }
    
    public func startStroke(at point: DrawingPoint) {
        if activeTool == .eraser {
            eraseAt(point: point, radius: activeStrokeWidth * 2.0)
            return
        }
        
        currentDraftStroke = DrawingStroke(
            points: [point],
            color: activeColor,
            strokeWidth: activeStrokeWidth,
            tool: activeTool
        )
    }
    
    public func addPointToStroke(_ point: DrawingPoint) {
        if activeTool == .eraser {
            eraseAt(point: point, radius: activeStrokeWidth * 2.0)
            return
        }
        
        guard var draft = currentDraftStroke else { return }
        draft.points.append(point)
        currentDraftStroke = draft
    }
    
    public func endStroke() {
        guard let draft = currentDraftStroke else { return }
        if !draft.points.isEmpty {
            pushUndoSnapshot()
            strokes.append(draft)
        }
        currentDraftStroke = nil
    }
    
    public func cancelStroke() {
        currentDraftStroke = nil
    }
    
    public func eraseAt(point: DrawingPoint, radius: Float) {
        let rSquared = radius * radius
        let initialCount = strokes.count
        
        let filtered = strokes.filter { stroke in
            // Check if any point in the stroke lies within the eraser radius
            for pt in stroke.points {
                let dx = pt.x - point.x
                let dy = pt.y - point.y
                if (dx * dx + dy * dy) <= rSquared {
                    return false // Delete entire stroke if intersected
                }
            }
            return true
        }
        
        if filtered.count != initialCount {
            pushUndoSnapshot()
            strokes = filtered
        }
    }
    
    public func undo() {
        guard let previous = undoStack.popLast() else { return }
        redoStack.append(strokes)
        strokes = previous
    }
    
    public func redo() {
        guard let next = redoStack.popLast() else { return }
        undoStack.append(strokes)
        strokes = next
    }
    
    public func clear() {
        guard !strokes.isEmpty else { return }
        pushUndoSnapshot()
        strokes.removeAll()
    }
    
    private func pushUndoSnapshot() {
        undoStack.append(strokes)
        if undoStack.count > DrawingEngine.maxHistory {
            undoStack.removeFirst()
        }
        redoStack.removeAll()
    }
}
