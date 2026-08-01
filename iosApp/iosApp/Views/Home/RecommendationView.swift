import SwiftUI
import ComposeApp

private struct IdentifiedDetection: Identifiable {
    let id: Int
    let result: DetectionResult
}

extension DiseaseDatabase {
    func getDiseaseInfo(diseaseName: String) -> DiseaseInfo {
        let target = diseaseName.lowercased()
        let list = (self.diseases as? [DiseaseInfo]) ?? []
        if let found = list.first(where: { $0.name.lowercased() == target }) {
            return found
        }
        if let found = list.first(where: { $0.name.lowercased().contains(target) }) {
            return found
        }
        if let first = list.first {
            return first
        }
        return DiseaseInfo(
            id: 0,
            name: diseaseName,
            localName: "",
            crop: "Crop",
            isHealthy: false,
            description: "No details available",
            symptoms: "N/A",
            causes: "N/A",
            effects: "N/A",
            prevention: "N/A",
            organicMitigation: "N/A",
            chemicalMitigation: "N/A",
            imageUrl: ""
        )
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
    
    public var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                if results.isEmpty {
                    Text("No disease information required.")
                        .foregroundColor(.secondary)
                        .padding()
                } else {
                    let distinctResults: [DetectionResult] = {
                        var dict = [String: DetectionResult]()
                        for det in results {
                            let key = det.className ?? "\(det.classIndex)"
                            if let existing = dict[key] {
                                if det.score > existing.score {
                                    dict[key] = det
                                }
                            } else {
                                dict[key] = det
                            }
                        }
                        return dict.values.sorted(by: { $0.score > $1.score })
                    }()
                    let items = distinctResults.enumerated().map { IdentifiedDetection(id: $0.offset, result: $0.element) }
                    ForEach(items) { item in
                        RecommendationCardView(det: item.result)
                    }
                }
            }
            .padding()
        }
        .navigationTitle("Treatment Guidelines")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct FormattedBulletListView: View {
    let content: String
    var color: Color = .green
    
    private var lines: [String] {
        content.components(separatedBy: "\n")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            ForEach(lines.indices, id: \.self) { idx in
                let line = lines[idx]
                let cleanLine = line
                    .trimmingCharacters(in: .whitespaces)
                    .replacingOccurrences(of: "^[•\\-\\*]\\s*", with: "", options: .regularExpression)
                
                HStack(alignment: .top, spacing: 10) {
                    Circle()
                        .fill(color)
                        .frame(width: 7, height: 7)
                        .padding(.top, 6)
                    
                    if let colonRange = cleanLine.range(of: ":") {
                        let title = String(cleanLine[..<colonRange.lowerBound]).trimmingCharacters(in: .whitespaces)
                        let desc = String(cleanLine[colonRange.upperBound...]).trimmingCharacters(in: .whitespaces)
                        
                        (Text(title + ": ").fontWeight(.bold).foregroundColor(.primary) +
                         Text(desc).foregroundColor(.secondary))
                            .font(.body)
                            .lineSpacing(4)
                    } else {
                        Text(cleanLine)
                            .font(.body)
                            .foregroundColor(.secondary)
                            .lineSpacing(4)
                    }
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
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(.green)
                Text(title)
                    .font(.headline)
            }
            FormattedBulletListView(content: text, color: .green)
        }
    }
}
