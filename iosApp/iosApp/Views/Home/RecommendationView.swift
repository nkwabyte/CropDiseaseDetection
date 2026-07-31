import SwiftUI
import ComposeApp

private struct IdentifiedDetection: Identifiable {
    let id: Int
    let result: DetectionResult
}

extension DiseaseDatabase {
    func getDiseaseInfo(diseaseName: String) -> DiseaseInfo {
        let target = diseaseName.lowercased()
        let list = self.diseases as? [DiseaseInfo] ?? []
        if let found = list.first(where: { $0.name.lowercased() == target }) {
            return found
        }
        if let found = list.first(where: { $0.name.lowercased().contains(target) }) {
            return found
        }
        return list.first!
    }
}

private struct RecommendationCardView: View {
    let det: DetectionResult
    
    var body: some View {
        let diseaseName = det.className ?? "Unknown Disease"
        let info = DiseaseDatabase.shared.getDiseaseInfo(diseaseName: diseaseName)
        
        return VStack(alignment: .leading, spacing: 14) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(diseaseName)
                        .font(.title3)
                        .fontWeight(.bold)
                    Text("Severity: \(Int(det.score * 100))% Match")
                        .font(.subheadline)
                        .foregroundColor(.green)
                }
                Spacer()
            }
            
            Divider()
            
            RecommendationSectionView(title: "Symptoms", icon: "cross.case.fill", text: info.symptoms)
            RecommendationSectionView(title: "Organic Management", icon: "leaf.fill", text: info.organicMitigation)
            RecommendationSectionView(title: "Chemical Control", icon: "flask.fill", text: info.chemicalMitigation)
            RecommendationSectionView(title: "Prevention Measures", icon: "shield.fill", text: info.prevention)
        }
        .padding()
        .background(Color(uiColor: .secondarySystemBackground))
        .cornerRadius(16)
    }
}

public struct RecommendationView: View {
    public let results: [DetectionResult]
    @Environment(\.dismiss) private var dismiss
    
    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    if results.isEmpty {
                        Text("No disease information required.")
                            .foregroundColor(.secondary)
                            .padding()
                    } else {
                        let items = results.enumerated().map { IdentifiedDetection(id: $0.offset, result: $0.element) }
                        ForEach(items) { item in
                            RecommendationCardView(det: item.result)
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("Treatment Guidelines")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}

struct RecommendationSectionView: View {
    let title: String
    let icon: String
    let text: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(.green)
                Text(title)
                    .font(.headline)
            }
            Text(text)
                .font(.body)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}
