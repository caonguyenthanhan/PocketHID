//
//  MainView.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import SwiftUI

public struct MainView: View {
    @StateObject private var transport = iOSTransport()
    @StateObject private var tabletController = DrawingTabletController()
    @ObservedObject private var focusLock = FocusLockController.shared
    
    @State private var selectedMode: ControlMode = .keyboard
    @State private var showSettings: Bool = false
    
    public init() {}
    
    public var body: some View {
        GeometryReader { geometry in
            let isLandscape = geometry.size.width > geometry.size.height
            
            ZStack {
                ThemeColors.darkBg.edgesIgnoringSafeArea(.all)
                
                VStack(spacing: 0) {
                    // Top Command Bar
                    TopCommandBarView(
                        transport: transport,
                        onToggleOrientation: {},
                        onOpenSettings: {
                            showSettings = true
                        }
                    )
                    
                    // Landscape Command Deck Tab Bar
                    if isLandscape {
                        DeckModeSwitcherView(selectedMode: $selectedMode)
                            .padding(.top, 4)
                    }
                    
                    // Active Interaction Mode Content
                    Group {
                        switch selectedMode {
                        case .keyboard:
                            KeyboardView(transport: transport)
                        case .mouse:
                            MouseView(transport: transport)
                        case .gamepad:
                            GamepadView(transport: transport)
                        case .presenter:
                            PresenterView(transport: transport)
                        case .oneHand:
                            OneHandView(transport: transport)
                        case .draw:
                            DrawingTabletView(controller: tabletController)
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    
                    // Portrait Bottom Mode Navigation Bar
                    if !isLandscape {
                        ModeNavigationBar(selectedMode: $selectedMode)
                    }
                }
            }
            .sheet(isPresented: $showSettings) {
                SettingsView(transport: transport) {
                    showSettings = false
                }
            }
            .onAppear {
                tabletController.setTransport(transport)
            }
        }
    }
}
