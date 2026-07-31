import SwiftUI
import ComposeApp

public struct SettingsView: View {
    @StateObject private var appStateObs = ObservableFlow(KoinHelper.appViewModel.appState)
    
    @State private var selectedTheme: String = "System Default"
    @State private var selectedLanguage: String = "ENGLISH"
    @State private var selectedDetectionModel: String = "YOLO26"
    @State private var detectionThreshold: Float = 0.10
    @State private var iouThreshold: Float = 0.10
    @State private var classifierThreshold: Float = 0.55
    @State private var appVersion: String = ""
    
    private let themes = ["System Default", "Light", "Dark"]
    
    private struct LanguageOption: Hashable {
        let code: String
        let displayName: String
    }
    
    private let languages: [LanguageOption] = [
        LanguageOption(code: "ENGLISH", displayName: "English 🇬🇧"),
        LanguageOption(code: "HAUSA", displayName: "Hausa 🇬🇭"),
        LanguageOption(code: "EWE", displayName: "Ewe (Eʋe) 🇬🇭"),
        LanguageOption(code: "ASANTE_TWI", displayName: "Asante Twi 🇬🇭"),
        LanguageOption(code: "GA", displayName: "Ga 🇬🇭")
    ]
    
    public var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("Disease Detection Model"), footer: Text("YOLO26 is currently active and optimized for on-device ExecuTorch inference. Faster R-CNN and Vision Transformer (ViT) models are under training and will be supported upon release.")) {
                    Picker("Detection Model", selection: $selectedDetectionModel) {
                        Text("YOLO26 (Active)").tag("YOLO26")
                        Text("Faster R-CNN (Under Training)").tag("FasterRCNN")
                        Text("Vision Transformer (Under Training)").tag("VisionTransformer")
                    }
                    .onChange(of: selectedDetectionModel) { newModel in
                        KoinHelper.appViewModel.setSelectedDetectionModel(model: newModel)
                    }
                }

                Section(header: Text("Appearance")) {
                    Picker("Theme Mode", selection: $selectedTheme) {
                        ForEach(themes, id: \.self) { theme in
                            Text(theme).tag(theme)
                        }
                    }
                    .onChange(of: selectedTheme) { newTheme in
                        KoinHelper.appViewModel.setSelectedTheme(theme: newTheme)
                    }
                }
                
                Section(header: Text("Localization")) {
                    Picker("Recommendation Language", selection: $selectedLanguage) {
                        ForEach(languages, id: \.code) { lang in
                            Text(lang.displayName).tag(lang.code)
                        }
                    }
                    .onChange(of: selectedLanguage) { newLang in
                        KoinHelper.settingsManager.setRecommendationLanguage(value: newLang)
                        KoinHelper.appViewModel.setRecommendationLanguage(language: RecommendationLanguage.from(code: newLang))
                    }
                }
                
                Section(header: Text("ExecuTorch ML Thresholds"), footer: Text("Adjust detection confidence and IoU overlap sensitivity for YOLO26 & EfficientNet models.")) {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("Detection Confidence Threshold")
                            Spacer()
                            Text("\(Int(detectionThreshold * 100))%")
                                .fontWeight(.bold)
                                .foregroundColor(.green)
                        }
                        Slider(value: $detectionThreshold, in: 0.05...0.90, step: 0.05) { _ in
                            KoinHelper.settingsManager.setDetectionThreshold(value: detectionThreshold)
                        }
                    }
                    
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("IoU Overlap Threshold")
                            Spacer()
                            Text("\(Int(iouThreshold * 100))%")
                                .fontWeight(.bold)
                                .foregroundColor(.green)
                        }
                        Slider(value: $iouThreshold, in: 0.05...0.90, step: 0.05) { _ in
                            KoinHelper.settingsManager.setIouThreshold(value: iouThreshold)
                        }
                    }
                    
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("Classifier Acceptance Threshold")
                            Spacer()
                            Text("\(Int(classifierThreshold * 100))%")
                                .fontWeight(.bold)
                                .foregroundColor(.green)
                        }
                        Slider(value: $classifierThreshold, in: 0.10...0.95, step: 0.05) { _ in
                            KoinHelper.settingsManager.setClassifierThreshold(value: classifierThreshold)
                        }
                    }
                }
                
                Section(header: Text("About & Support")) {
                    NavigationLink {
                        AboutView()
                    } label: {
                        Label("About Plant Disease Detector", systemImage: "info.circle.fill")
                    }
                    
                    NavigationLink {
                        HelpView()
                    } label: {
                        Label("Help & Support", systemImage: "questionmark.circle.fill")
                    }
                }

                Section(header: Text("App Info")) {
                    HStack {
                        Text("App Version")
                        Spacer()
                        Text(appVersion)
                            .foregroundColor(.secondary)
                    }
                }
            }
            .navigationTitle("Settings")
            .onAppear {
                let settings = KoinHelper.settingsManager
                self.selectedTheme = appStateObs.value.selectedTheme
                self.selectedLanguage = settings.getRecommendationLanguage()
                self.selectedDetectionModel = settings.getDetectionModel()
                self.detectionThreshold = settings.getDetectionThreshold()
                self.iouThreshold = settings.getIouThreshold()
                self.classifierThreshold = settings.getClassifierThreshold()
                self.appVersion = settings.getAppVersion()
            }
        }
    }
}

extension RecommendationLanguage {
    static func from(code: String) -> RecommendationLanguage {
        switch code {
        case "HAUSA": return .hausa
        case "EWE": return .ewe
        case "ASANTE_TWI": return .asanteTwi
        case "GA": return .ga
        default: return .english
        }
    }
}
