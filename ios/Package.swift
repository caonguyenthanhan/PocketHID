// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "PocketHID",
    defaultLocalization: "en",
    platforms: [
        .iOS(.v15),
        .macOS(.v12)
    ],
    products: [
        .library(
            name: "PocketHID",
            targets: ["PocketHID"]
        )
    ],
    dependencies: [],
    targets: [
        .target(
            name: "PocketHID",
            dependencies: [],
            path: "PocketHID"
        ),
        .testTarget(
            name: "PocketHIDTests",
            dependencies: ["PocketHID"],
            path: "PocketHIDTests"
        )
    ]
)
