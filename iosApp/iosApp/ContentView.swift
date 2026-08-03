import SwiftUI
import ComposeApp

struct ContentView: View {
    @StateObject private var appStateObs = ObservableFlow(KoinHelper.appViewModel.appState)
    @AppStorage("selected_app_language_code") private var appLangCode: String = "en"

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
                    Label(LocalizedStringKey("Diagnostics"), systemImage: "waveform.path.ecg")
                }
            
            EncyclopediaView()
                .tabItem {
                    Label(LocalizedStringKey("Encyclopedia"), systemImage: "book.fill")
                }
            
            HistoryView()
                .tabItem {
                    Label(LocalizedStringKey("History"), systemImage: "clock.arrow.circlepath")
                }
            
            SettingsView()
                .tabItem {
                    Label(LocalizedStringKey("Settings"), systemImage: "gearshape.fill")
                }
            
            ProfileView()
                .tabItem {
                    Label(LocalizedStringKey("Account"), systemImage: "person.crop.circle.fill")
                }
        }
        .accentColor(.green)
        .preferredColorScheme(preferredColorScheme)
        .environment(\.locale, Locale(identifier: appLangCode))
    }
}

