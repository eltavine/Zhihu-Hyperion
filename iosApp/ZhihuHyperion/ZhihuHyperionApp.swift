// Zhihu-Hyperion - Free & Ad-Free Zhihu client for all platforms.
// Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
// Co-author: eltavine <me@eltavine.com>
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published by
// the Free Software Foundation (version 3 only).
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

import SwiftUI
import UIKit
import ZhihuHyperionKit

/// The whole interface is the shared Compose UI; this file only hosts it in a SwiftUI scene.
@main
struct ZhihuHyperionApp: App {
    init() {
        MainViewControllerKt.startZhihuApp()
    }

    var body: some Scene {
        WindowGroup {
            ComposeHostView()
                // Compose applies the safe area and keyboard insets itself through WindowInsets.
                .ignoresSafeArea()
        }
    }
}

private struct ComposeHostView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
