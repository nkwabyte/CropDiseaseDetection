import SwiftUI
import ComposeApp

public struct HistoryView: View {
    @State private var historyItems: [String] = []
    
    public var body: some View {
        NavigationStack {
            VStack {
                if historyItems.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "clock.arrow.circlepath")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary)
                        Text("No Detection History")
                            .font(.headline)
                        Text("Your past crop disease scans will appear here.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .padding()
                } else {
                    List(historyItems, id: \.self) { item in
                        HStack {
                            Image(systemName: "leaf.fill")
                                .foregroundColor(.green)
                            Text(item)
                        }
                    }
                }
            }
            .navigationTitle("History Log")
        }
    }
}
