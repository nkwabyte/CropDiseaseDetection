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
            crop: L("Crop"),
            isHealthy: false,
            description: L("No details available"),
            symptoms: L("N/A"),
            causes: L("N/A"),
            effects: L("N/A"),
            prevention: L("N/A"),
            organicMitigation: L("N/A"),
            chemicalMitigation: L("N/A"),
            imageUrl: ""
        )
    }
}

private struct RecommendationCardView: View {
    let det: DetectionResult
    @StateObject private var langMgr = LanguageManager.shared
    
    var body: some View {
        let diseaseName = det.displayName.isEmpty ? L("Unknown Disease") : det.displayName
        let info = DiseaseDatabase.shared.getDiseaseInfo(diseaseName: diseaseName)
        
        return VStack(alignment: .leading, spacing: 14) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(diseaseName)
                        .font(.title3)
                        .fontWeight(.bold)
                    LText("Severity: %@ Match", "\(Int(det.score * 100))%")
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
    @StateObject private var langMgr = LanguageManager.shared
    @State private var selectedLanguageCode: String = KoinHelper.settingsManager.getRecommendationLanguage()
    @State private var showAudioComingSoonAlert: Bool = false
    
    private struct LanguageOption: Hashable {
        let code: String
        let displayName: String
        let flag: String
    }
    
    private let languages: [LanguageOption] = [
        LanguageOption(code: "ENGLISH", displayName: "English", flag: "🇬🇧"),
        LanguageOption(code: "HAUSA", displayName: "Hausa", flag: "🇬🇭"),
        LanguageOption(code: "EWE", displayName: "Ewe (Eʋe)", flag: "🇬🇭"),
        LanguageOption(code: "ASANTE_TWI", displayName: "Asante Twi", flag: "🇬🇭"),
        LanguageOption(code: "GA", displayName: "Ga", flag: "🇬🇭"),
        LanguageOption(code: "FRENCH", displayName: "French (Français)", flag: "🇫🇷")
    ]
    
    public var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // ── Top Header Control Row (Language Selector & Play Audio Button) ─────
                let currentOpt = languages.first(where: { $0.code == selectedLanguageCode }) ?? languages[0]
                
                HStack {
                    // Language Picker Menu
                    Menu {
                        Picker(L("Language"), selection: $selectedLanguageCode) {
                            ForEach(languages, id: \.code) { lang in
                                Text("\(lang.flag) \(lang.displayName)").tag(lang.code)
                            }
                        }
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "globe")
                                .foregroundColor(.green)
                            Text("\(currentOpt.flag) \(currentOpt.displayName)")
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundColor(.primary)
                            Image(systemName: "chevron.down")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color(uiColor: .tertiarySystemFill))
                        .cornerRadius(20)
                    }
                    .onChange(of: selectedLanguageCode) { newLang in
                        let recLang = RecommendationLanguage.from(code: newLang)
                        KoinHelper.settingsManager.setRecommendationLanguage(value: newLang)
                        KoinHelper.appViewModel.setRecommendationLanguage(language: recLang)
                        LanguageManager.shared.setLanguage(recLang.localeCode)
                    }

                    Spacer()

                    // Play Audio Button (Future feature placeholder)
                    Button {
                        showAudioComingSoonAlert = true
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "play.fill")
                                .font(.caption)
                            LText("Play Audio")
                                .font(.subheadline)
                                .fontWeight(.semibold)
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(Color.green)
                        .foregroundColor(.white)
                        .cornerRadius(20)
                    }
                }
                .padding(.horizontal, 4)

                // Translation pending banner if non-English
                if selectedLanguageCode != "ENGLISH" {
                    HStack(spacing: 10) {
                        Image(systemName: "character.bubble.fill")
                            .foregroundColor(.orange)
                        VStack(alignment: .leading, spacing: 2) {
                            LText("%@ %@ — Translation coming soon", currentOpt.flag, currentOpt.displayName)
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundColor(Color(red: 0.75, green: 0.2, blue: 0.05))
                            LText("Content is currently displayed in English.")
                                .font(.caption2)
                                .foregroundColor(.orange)
                        }
                        Spacer()
                    }
                    .padding(12)
                    .background(Color.orange.opacity(0.12))
                    .cornerRadius(12)
                }

                if results.isEmpty {
                    LText("No disease information required.")
                        .foregroundColor(.secondary)
                        .padding()
                } else {
                    let distinctResults: [DetectionResult] = {
                        var dict = [String: DetectionResult]()
                        for det in results {
                            let key = det.displayName.isEmpty ? "\(det.classIndex)" : det.displayName
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
        .navigationTitle(L("Treatment Guidelines"))
        .navigationBarTitleDisplayMode(.inline)
        .alert(isPresented: $showAudioComingSoonAlert) {
            let currentOpt = languages.first(where: { $0.code == selectedLanguageCode }) ?? languages[0]
            return Alert(
                title: Text(L("Audio Playback")),
                message: Text(L("Audio playback in %@ is coming soon!", currentOpt.displayName)),
                dismissButton: .default(Text(L("OK")))
            )
        }
        .onAppear {
            let currentRecLang = KoinHelper.settingsManager.getRecommendationLanguage()
            self.selectedLanguageCode = currentRecLang
            let recLang = RecommendationLanguage.from(code: currentRecLang)
            LanguageManager.shared.setLanguage(recLang.localeCode)
        }
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
    /// English source string, used as the localization key.
    let title: String
    let icon: String
    let text: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(.green)
                LText(title)
                    .font(.headline)
            }
            FormattedBulletListView(content: text, color: .green)
        }
    }
}
