import SwiftUI
import ComposeApp

private struct IdentifiedDetection: Identifiable {
    let id: Int
    let result: DetectionResult
}

// Section headings, the banner, the audio notice and the disclaimer all come
// from `RecommendationStrings` in commonMain — do not re-declare them here.
// Swift cannot add a member the imported Kotlin type already exposes, and a
// second copy drifts from the shared one within a release.
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
    let selectedLanguageCode: String
    @StateObject private var langMgr = LanguageManager.shared
    
    var body: some View {
        let diseaseName = det.displayName.isEmpty ? L("Unknown Disease") : det.displayName
        let canonical = DiseaseDatabase.shared.getDiseaseInfo(diseaseName: diseaseName)
        let recLang = RecommendationLanguage.from(code: selectedLanguageCode)
        // Body text in the selected language; falls back to English per-disease.
        let info = DiseaseTranslations.shared.localized(disease: canonical, lang: recLang)
        let strings = RecommendationStrings.shared

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

            if canonical.isHealthy {
                RecommendationSectionView(title: strings.healthyTitle(lang: recLang), icon: "checkmark.seal.fill", text: info.prevention)
                RecommendationSectionView(title: strings.healthyOrganicTitle(lang: recLang), icon: "leaf.fill", text: info.organicMitigation)
                RecommendationSectionView(title: strings.healthyChemicalTitle(lang: recLang), icon: "flask.fill", text: info.chemicalMitigation)
            } else {
                RecommendationSectionView(title: strings.symptomsTitle(lang: recLang), icon: "cross.case.fill", text: info.symptoms)
                RecommendationSectionView(title: strings.causeTitle(lang: recLang), icon: "exclamationmark.triangle.fill", text: info.causes)
                RecommendationSectionView(title: strings.effectsTitle(lang: recLang), icon: "chart.line.downtrend.xyaxis", text: info.effects)
                RecommendationSectionView(title: strings.organicControlTitle(lang: recLang), icon: "leaf.fill", text: info.organicMitigation)
                RecommendationSectionView(title: strings.chemicalControlTitle(lang: recLang), icon: "flask.fill", text: info.chemicalMitigation)
                RecommendationSectionView(title: strings.preventionTitle(lang: recLang), icon: "shield.fill", text: info.prevention)
            }
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
                let recLang = RecommendationLanguage.from(code: selectedLanguageCode)
                
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
                        let rLang = RecommendationLanguage.from(code: newLang)
                        KoinHelper.settingsManager.setRecommendationLanguage(value: newLang)
                        KoinHelper.appViewModel.setRecommendationLanguage(language: rLang)
                        LanguageManager.shared.setLanguage(rLang.localeCode)
                    }

                    Spacer()

                    // Play Audio Button
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

                // Translation banner if non-English. It only claims a full
                // translation when every disease on screen actually has one —
                // otherwise it says the headings are translated and the body
                // is still English.
                if selectedLanguageCode != "ENGLISH" {
                    let strings = RecommendationStrings.shared
                    let allBodiesTranslated = results.allSatisfy { det in
                        let name = det.displayName.isEmpty ? L("Unknown Disease") : det.displayName
                        let info = DiseaseDatabase.shared.getDiseaseInfo(diseaseName: name)
                        return DiseaseTranslations.shared.hasTranslation(id: info.id, lang: recLang)
                    }
                    HStack(spacing: 10) {
                        Image(systemName: "character.bubble.fill")
                            .foregroundColor(allBodiesTranslated ? .green : .orange)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("\(currentOpt.flag) \(currentOpt.displayName) — " + (allBodiesTranslated
                                ? strings.bannerLocalizedTitle(lang: recLang)
                                : strings.bannerHeadingsOnlyTitle(lang: recLang)))
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundColor(allBodiesTranslated
                                    ? Color(red: 0.1, green: 0.45, blue: 0.15)
                                    : Color(red: 0.75, green: 0.2, blue: 0.05))
                            Text(allBodiesTranslated
                                ? strings.bannerLocalizedBody(lang: recLang)
                                : strings.bannerHeadingsOnlyBody(lang: recLang))
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                    }
                    .padding(12)
                    .background((allBodiesTranslated ? Color.green : Color.orange).opacity(0.12))
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
                        RecommendationCardView(det: item.result, selectedLanguageCode: selectedLanguageCode)
                    }

                    // Advisory disclaimer — same shared copy as Android.
                    Text(RecommendationStrings.shared.disclaimer(lang: recLang))
                        .font(.caption2)
                        .foregroundColor(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                        .padding(.top, 4)
                }
            }
            .padding()
        }
        .navigationTitle(L("Treatment Guidelines"))
        .navigationBarTitleDisplayMode(.inline)
        .alert(isPresented: $showAudioComingSoonAlert) {
            let recLang = RecommendationLanguage.from(code: selectedLanguageCode)
            return Alert(
                title: Text(L("Audio Playback")),
                message: Text(RecommendationStrings.shared.audioNotice(lang: recLang)),
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
    /// Display text, already resolved for the selected recommendation language
    /// by `RecommendationStrings`. Not a localization key — do not pass it
    /// through `L()`, or a heading that happens to collide with a catalogue key
    /// would be translated a second time.
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
