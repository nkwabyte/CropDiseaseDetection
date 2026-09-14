import SwiftUI
import ComposeApp

struct ContentView: View {
    @StateObject private var appStateObs = ObservableFlow(KoinHelper.appViewModel.appState)
    @StateObject private var langMgr = LanguageManager.shared
    @State private var selectedTab: Int = ContentView.initialTab

    /// Which tab to open on launch. Always Diagnostics in a Release build; a DEBUG
    /// build additionally honours `--open-settings`, so a scripted launch can open
    /// the Settings screen directly for UI verification. Neither the simulator nor
    /// a physical device can be driven by synthetic taps with first-party tooling,
    /// so without this there is no way to screenshot a non-default tab.
    private static var initialTab: Int {
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("--open-settings") { return 3 }
        #endif
        return 0
    }

    private var preferredColorScheme: ColorScheme? {
        switch appStateObs.value.selectedTheme {
        case "Light":
            return .light
        case "Dark":
            return .dark
        default:
            return nil
        }
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            HomeView()
                .tabItem {
                    Label {
                        Text(langMgr.localize("Diagnostics"))
                    } icon: {
                        Image(systemName: "waveform.path.ecg")
                    }
                }
                .tag(0)
            
            EncyclopediaView()
                .tabItem {
                    Label {
                        Text(langMgr.localize("Encyclopedia"))
                    } icon: {
                        Image(systemName: "book.fill")
                    }
                }
                .tag(1)
            
            HistoryView()
                .tabItem {
                    Label {
                        Text(langMgr.localize("History"))
                    } icon: {
                        Image(systemName: "clock.arrow.circlepath")
                    }
                }
                .tag(2)
            
            SettingsView()
                .tabItem {
                    Label {
                        Text(langMgr.localize("Settings"))
                    } icon: {
                        Image(systemName: "gearshape.fill")
                    }
                }
                .tag(3)
            
            ProfileView()
                .tabItem {
                    Label {
                        Text(langMgr.localize("Account"))
                    } icon: {
                        Image(systemName: "person.crop.circle.fill")
                    }
                }
                .tag(4)
        }
        .accentColor(.green)
        .preferredColorScheme(preferredColorScheme)
    }
}

