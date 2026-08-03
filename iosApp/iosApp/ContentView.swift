import SwiftUI
import ComposeApp

struct ContentView: View {
    @StateObject private var appStateObs = ObservableFlow(KoinHelper.appViewModel.appState)
    @StateObject private var langMgr = LanguageManager.shared

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
        TabView {
            HomeView()
                .tabItem {
                    Label {
                        Text(langMgr.localize("Diagnostics"))
                    } icon: {
                        Image(systemName: "waveform.path.ecg")
                    }
                }
            
            EncyclopediaView()
                .tabItem {
                    Label {
                        Text(langMgr.localize("Encyclopedia"))
                    } icon: {
                        Image(systemName: "book.fill")
                    }
                }
            
            HistoryView()
                .tabItem {
                    Label {
                        Text(langMgr.localize("History"))
                    } icon: {
                        Image(systemName: "clock.arrow.circlepath")
                    }
                }
            
            SettingsView()
                .tabItem {
                    Label {
                        Text(langMgr.localize("Settings"))
                    } icon: {
                        Image(systemName: "gearshape.fill")
                    }
                }
            
            ProfileView()
                .tabItem {
                    Label {
                        Text(langMgr.localize("Account"))
                    } icon: {
                        Image(systemName: "person.crop.circle.fill")
                    }
                }
        }
        .accentColor(.green)
        .preferredColorScheme(preferredColorScheme)
    }
}

