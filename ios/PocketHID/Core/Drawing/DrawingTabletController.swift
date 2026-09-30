//
//  DrawingTabletController.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation
import Combine

/// High-level coordinator for Graphics Tablet on iOS.
/// Combines local stroke rendering, canonical coordinate transformation,
/// and floating toolbar lifecycle (collapsed by default).
@MainActor
public final class DrawingTabletController: ObservableObject {
    
    public let engine: DrawingEngine
    public let pipeline: DrawingCoordinatePipeline
    private weak var transport: HIDTransport?
    
    @Published public var targetMode: DrawingTargetMode = .absoluteTablet
    @Published public var isToolbarExpanded: Bool = false
    @Published public var isColorPickerExpanded: Bool = false
    
    // Active touch telemetry
    @Published public private(set) var isDrawingActive: Bool = false
    @Published public private(set) var lastTransformedPoint: HidTabletPoint? = nil
    
    public init(
        engine: DrawingEngine = DrawingEngine(),
        pipeline: DrawingCoordinatePipeline = DrawingCoordinatePipeline(),
        transport: HIDTransport? = nil
    ) {
        self.engine = engine
        self.pipeline = pipeline
        self.transport = transport
    }
    
    public func setTransport(_ transport: HIDTransport?) {
        self.transport = transport
    }
    
    public func updateCanvasDimensions(width: Float, height: Float) {
        pipeline.canvasWidth = width
        pipeline.canvasHeight = height
    }
    
    public func updateInsets(left: Float, top: Float, right: Float, bottom: Float) {
        pipeline.insetsLeft = left
        pipeline.insetsTop = top
        pipeline.insetsRight = right
        pipeline.insetsBottom = bottom
    }
    
    public func onTouchDown(point: DrawingPoint, pointerCount: Int = 1) {
        // Multi-touch rejection
        if pointerCount > 1 {
            releaseDrawing()
            return
        }
        
        // Auto-collapse floating toolbar on canvas touch
        if isToolbarExpanded {
            isToolbarExpanded = false
            isColorPickerExpanded = false
        }
        
        isDrawingActive = true
        engine.startStroke(at: point)
        
        let transformed = pipeline.transform(rawPoint: point, tool: engine.activeTool)
        lastTransformedPoint = transformed
        
        if let t = transport {
            let report = HIDReportBuilder.buildTabletReport(status: transformed.status, x: transformed.x, y: transformed.y)
            _ = t.sendRawReport(endpoint: report.endpoint, payload: report.payload)
        }
    }
    
    public func onTouchMove(point: DrawingPoint, pointerCount: Int = 1) {
        guard isDrawingActive, pointerCount == 1 else {
            if pointerCount > 1 {
                releaseDrawing()
            }
            return
        }
        
        engine.addPointToStroke(point)
        
        let transformed = pipeline.transform(rawPoint: point, tool: engine.activeTool)
        lastTransformedPoint = transformed
        
        if let t = transport {
            let report = HIDReportBuilder.buildTabletReport(status: transformed.status, x: transformed.x, y: transformed.y)
            _ = t.sendRawReport(endpoint: report.endpoint, payload: report.payload)
        }
    }
    
    public func onTouchUp() {
        guard isDrawingActive else { return }
        isDrawingActive = false
        engine.endStroke()
        if let t = transport {
            let report = HIDReportBuilder.buildTabletNeutral()
            _ = t.sendRawReport(endpoint: report.endpoint, payload: report.payload)
        }
    }
    
    public func releaseDrawing() {
        isDrawingActive = false
        engine.cancelStroke()
        if let t = transport {
            let report = HIDReportBuilder.buildTabletNeutral()
            _ = t.sendRawReport(endpoint: report.endpoint, payload: report.payload)
        }
    }
    
    public func toggleToolbar() {
        isToolbarExpanded.toggle()
        if !isToolbarExpanded {
            isColorPickerExpanded = false
        }
    }
}
