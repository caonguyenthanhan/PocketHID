//
//  DrawingTabletView.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import SwiftUI

public struct DrawingTabletView: View {
    @ObservedObject var controller: DrawingTabletController
    @ObservedObject var focusLock = FocusLockController.shared
    
    @State private var showClearAlert: Bool = false
    
    public init(controller: DrawingTabletController) {
        self.controller = controller
    }
    
    public var body: some View {
        ZStack {
            // Full-bleed Canvas Surface (90-95% viewport dominance)
            Canvas { context, size in
                // Render finished strokes
                for stroke in controller.engine.strokes {
                    var path = Path()
                    guard let first = stroke.points.first else { continue }
                    path.move(to: first.cgPoint)
                    
                    for i in 1..<stroke.points.count {
                        let pt = stroke.points[i]
                        let prev = stroke.points[i - 1]
                        let mid = CGPoint(
                            x: (prev.cgPoint.x + pt.cgPoint.x) / 2.0,
                            y: (prev.cgPoint.y + pt.cgPoint.y) / 2.0
                        )
                        path.addQuadCurve(to: mid, control: prev.cgPoint)
                    }
                    if let last = stroke.points.last {
                        path.addLine(to: last.cgPoint)
                    }
                    
                    let color = Color(hex: stroke.color.hexValue)
                    context.stroke(
                        path,
                        with: .color(color),
                        style: StrokeStyle(lineWidth: CGFloat(stroke.strokeWidth), lineCap: .round, lineJoin: .round)
                    )
                }
                
                // Render active draft stroke
                if let draft = controller.engine.currentDraftStroke {
                    var path = Path()
                    if let first = draft.points.first {
                        path.move(to: first.cgPoint)
                        for pt in draft.points.dropFirst() {
                            path.addLine(to: pt.cgPoint)
                        }
                    }
                    let color = Color(hex: draft.color.hexValue)
                    context.stroke(
                        path,
                        with: .color(color),
                        style: StrokeStyle(lineWidth: CGFloat(draft.strokeWidth), lineCap: .round, lineJoin: .round)
                    )
                }
            }
            .background(ThemeColors.darkBg)
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        let pt = DrawingPoint(x: Float(value.location.x), y: Float(value.location.y))
                        if !controller.isDrawingActive {
                            controller.onTouchDown(point: pt, pointerCount: 1)
                        } else {
                            controller.onTouchMove(point: pt, pointerCount: 1)
                        }
                    }
                    .onEnded { _ in
                        controller.onTouchUp()
                    }
            )
            
            // Top Compact Status Overlays: Focus Indicator & Target Mode Pill
            VStack {
                HStack(spacing: 8) {
                    if focusLock.isLocked {
                        HStack(spacing: 4) {
                            Image(systemName: "lock.fill")
                                .font(.system(size: 10, weight: .bold))
                            Text("FOCUS")
                                .font(.system(size: 10, weight: .black, design: .monospaced))
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .foregroundColor(ThemeColors.darkBg)
                        .background(ThemeColors.cyanAccent)
                        .cornerRadius(12)
                    }
                    
                    Spacer()
                    
                    // Tablet Mode Pill
                    Text("LOCAL DIGITIZER (16-BIT)")
                        .font(.system(size: 9, weight: .bold, design: .monospaced))
                        .foregroundColor(ThemeColors.cyanAccent)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(ThemeColors.darkSurface.opacity(0.8))
                        .cornerRadius(10)
                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(ThemeColors.darkBorder, lineWidth: 1))
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                
                Spacer()
                
                // Floating Toolbar (Collapsed by default: ~5-10% chrome)
                floatingToolbar
                    .padding(.bottom, 12)
            }
        }
        .alert(isPresented: $showClearAlert) {
            Alert(
                title: Text("Clear Drawing Canvas"),
                message: Text("Are you sure you want to delete all strokes?"),
                primaryButton: .destructive(Text("Clear")) {
                    controller.engine.clear()
                },
                secondaryButton: .cancel()
            )
        }
    }
    
    // MARK: - Floating Toolbar
    
    private var floatingToolbar: some View {
        VStack(spacing: 8) {
            // Expanded Secondary Controls (Colors & Sizes)
            if controller.isToolbarExpanded {
                HStack(spacing: 8) {
                    // Pen Sizes
                    ForEach([2.0, 4.0, 8.0, 12.0], id: \.self) { size in
                        Button(action: {
                            controller.engine.activeStrokeWidth = Float(size)
                        }) {
                            Circle()
                                .fill(controller.engine.activeStrokeWidth == Float(size) ? ThemeColors.cyanAccent : ThemeColors.darkBorder)
                                .frame(width: CGFloat(size + 14), height: CGFloat(size + 14))
                        }
                    }
                    
                    Divider().frame(height: 20).background(ThemeColors.darkBorder)
                    
                    // Colors
                    ForEach(DrawingColor.allCases) { color in
                        Button(action: {
                            controller.engine.activeColor = color
                            controller.engine.activeTool = .pen
                        }) {
                            Circle()
                                .fill(Color(hex: color.hexValue))
                                .frame(width: 22, height: 22)
                                .overlay(
                                    Circle()
                                        .stroke(Color.white, lineWidth: controller.engine.activeColor == color ? 2 : 0)
                                )
                        }
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(ThemeColors.darkSurface)
                .cornerRadius(20)
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(ThemeColors.darkBorder, lineWidth: 1))
            }
            
            // Primary Pill Toolbar (Compact)
            HStack(spacing: 12) {
                // Expand / Collapse Chevron
                Button(action: {
                    controller.toggleToolbar()
                }) {
                    Image(systemName: controller.isToolbarExpanded ? "chevron.down" : "paintbrush.fill")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(ThemeColors.cyanAccent)
                }
                
                // Tool Toggle: Pen vs Eraser
                Button(action: {
                    controller.engine.activeTool = controller.engine.activeTool == .pen ? .eraser : .pen
                }) {
                    Image(systemName: controller.engine.activeTool == .eraser ? "eraser.fill" : "pencil.tip")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(controller.engine.activeTool == .eraser ? ThemeColors.accentAmber : ThemeColors.textPrimary)
                }
                
                // Undo
                Button(action: {
                    controller.engine.undo()
                }) {
                    Image(systemName: "arrow.uturn.backward")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(controller.engine.canUndo ? ThemeColors.textPrimary : ThemeColors.textMuted)
                }
                .disabled(!controller.engine.canUndo)
                
                // Redo
                Button(action: {
                    controller.engine.redo()
                }) {
                    Image(systemName: "arrow.uturn.forward")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(controller.engine.canRedo ? ThemeColors.textPrimary : ThemeColors.textMuted)
                }
                .disabled(!controller.engine.canRedo)
                
                // Clear Canvas
                Button(action: {
                    showClearAlert = true
                }) {
                    Image(systemName: "trash")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(ThemeColors.errorContainer)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(ThemeColors.darkSurface)
            .cornerRadius(24)
            .overlay(RoundedRectangle(cornerRadius: 24).stroke(ThemeColors.darkBorder, lineWidth: 1.5))
            .shadow(color: Color.black.opacity(0.4), radius: 8, x: 0, y: 4)
        }
    }
}

// Color helper
extension Color {
    init(hex: UInt32) {
        let red = Double((hex >> 16) & 0xFF) / 255.0
        let green = Double((hex >> 8) & 0xFF) / 255.0
        let blue = Double(hex & 0xFF) / 255.0
        self.init(red: red, green: green, blue: blue)
    }
}
