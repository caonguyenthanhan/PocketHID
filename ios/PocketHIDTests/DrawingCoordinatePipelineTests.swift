//
//  DrawingCoordinatePipelineTests.swift
//  PocketHIDTests
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import XCTest
@testable import PocketHID

final class DrawingCoordinatePipelineTests: XCTestCase {

    func testCornersMappingInPortraitStretch() {
        let pipeline = DrawingCoordinatePipeline()
        pipeline.canvasWidth = 1000
        pipeline.canvasHeight = 2000
        pipeline.orientation = .portrait
        pipeline.aspectRatioPolicy = .stretch

        // 1. Top-Left (0, 0) -> (0, 0)
        let tl = pipeline.transform(rawPoint: DrawingPoint(x: 0, y: 0))
        XCTAssertEqual(tl.x, 0)
        XCTAssertEqual(tl.y, 0)

        // 2. Bottom-Right (1000, 2000) -> (32767, 32767)
        let br = pipeline.transform(rawPoint: DrawingPoint(x: 1000, y: 2000))
        XCTAssertEqual(br.x, 32767)
        XCTAssertEqual(br.y, 32767)

        // 3. Center (500, 1000) -> approx (16383, 16383)
        let center = pipeline.transform(rawPoint: DrawingPoint(x: 500, y: 1000))
        XCTAssertEqual(Double(center.x), 16383.5, accuracy: 2.0)
        XCTAssertEqual(Double(center.y), 16383.5, accuracy: 2.0)
    }

    func testLandscapeOrientationRotation() {
        let pipeline = DrawingCoordinatePipeline()
        pipeline.canvasWidth = 2000
        pipeline.canvasHeight = 1000
        pipeline.orientation = .landscape
        pipeline.aspectRatioPolicy = .stretch

        // In landscape, top-left on phone maps to rotated coordinate
        let pt = pipeline.transform(rawPoint: DrawingPoint(x: 0, y: 0))
        XCTAssertEqual(pt.x, 0)
        XCTAssertEqual(pt.y, 32767)
    }

    func testEraserStatusMask() {
        let pipeline = DrawingCoordinatePipeline()
        let penPoint = pipeline.transform(rawPoint: DrawingPoint(x: 100, y: 100), tool: .pen)
        XCTAssertEqual(penPoint.status & HidConstants.TABLET_STATUS_ERASER, 0)

        let eraserPoint = pipeline.transform(rawPoint: DrawingPoint(x: 100, y: 100), tool: .eraser)
        XCTAssertEqual(eraserPoint.status & HidConstants.TABLET_STATUS_ERASER, HidConstants.TABLET_STATUS_ERASER)
    }
}
