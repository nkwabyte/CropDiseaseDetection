import SwiftUI

struct ContentView: View {
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
    }
}

