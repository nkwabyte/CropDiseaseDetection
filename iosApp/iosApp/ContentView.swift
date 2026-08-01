import SwiftUI
import ComposeApp

struct ContentView: View {
    @StateObject private var appStateObs = ObservableFlow(KoinHelper.appViewModel.appState)

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
                    Label("Diagnostics", systemImage: "waveform.path.ecg")
                }
            
            EncyclopediaView()
                .tabItem {
                    Label("Encyclopedia", systemImage: "book.fill")
                }
            
            HistoryView()
                .tabItem {
                    Label("History", systemImage: "clock.arrow.circlepath")
                }
            
            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gearshape.fill")
                }
            
            ProfileView()
                .tabItem {
                    Label("Account", systemImage: "person.crop.circle.fill")
                }
        }
        .accentColor(.green)
        .preferredColorScheme(preferredColorScheme)
    }
}

