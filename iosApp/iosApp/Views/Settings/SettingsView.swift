import SwiftUI
import UIKit
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
    @StateObject private var langMgr = LanguageManager.shared
    
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
        LanguageOption(code: "GA", displayName: "Ga 🇬🇭"),
        LanguageOption(code: "FRENCH", displayName: "French (Français) 🇫🇷")
    ]
    
    public var body: some View {
        NavigationStack {
            Form {
                DetectionModelSection(selectedDetectionModel: $selectedDetectionModel)

                Section(header: LText("Appearance")) {
                    Picker(L("Theme Mode"), selection: $selectedTheme) {
                        ForEach(themes, id: \.self) { theme in
                            LText(theme).tag(theme)
                        }
                    }
                    .onChange(of: selectedTheme) { newTheme in
                        KoinHelper.appViewModel.setSelectedTheme(theme: newTheme)
                    }
                }
                
                Section(header: LText("Localization")) {
                    Picker(L("Language"), selection: $selectedLanguage) {
                        ForEach(languages, id: \.code) { lang in
                            Text(lang.displayName).tag(lang.code)
                        }
                    }
                    .onChange(of: selectedLanguage) { newLang in
                        let recLang = RecommendationLanguage.from(code: newLang)
                        KoinHelper.settingsManager.setRecommendationLanguage(value: newLang)
                        KoinHelper.appViewModel.setRecommendationLanguage(language: recLang)
                        LanguageManager.shared.setLanguage(recLang.localeCode)
                    }
                }
                
                Section(header: LText("ExecuTorch ML Thresholds"), footer: LText("Adjust detection confidence and IoU overlap sensitivity for YOLO26 & EfficientNet models.")) {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            LText("Detection Confidence Threshold")
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
                            LText("IoU Overlap Threshold")
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
                            LText("Classifier Acceptance Threshold")
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
                
                DeveloperBenchmarkSection()

                Section(header: LText("About & Support")) {
                    NavigationLink {
                        AboutView()
                    } label: {
                        Label {
                            LText("About Plant Disease Detector")
                        } icon: {
                            Image(systemName: "info.circle.fill")
                        }
                    }
                    
                    NavigationLink {
                        HelpView()
                    } label: {
                        Label {
                            LText("Help & Support")
                        } icon: {
                            Image(systemName: "questionmark.circle.fill")
                        }
                    }
                }

                Section(header: LText("App Info")) {
                    HStack {
                        LText("App Version")
                        Spacer()
                        Text(appVersion)
                            .foregroundColor(.secondary)
                    }
                }
            }
            .navigationTitle(L("Settings"))
            .onAppear {
                let settings = KoinHelper.settingsManager
                self.selectedTheme = appStateObs.value.selectedTheme
                let currentRecLang = settings.getRecommendationLanguage()
                self.selectedLanguage = currentRecLang
                let recLang = RecommendationLanguage.from(code: currentRecLang)
                LanguageManager.shared.setLanguage(recLang.localeCode)
                // The view model resolves the stored id to a model that is actually
                // on the device (a deleted download falls back to the default).
                self.selectedDetectionModel = appStateObs.value.selectedDetectionModel
                self.detectionThreshold = settings.getDetectionThreshold()
                self.iouThreshold = settings.getIouThreshold()
                self.classifierThreshold = settings.getClassifierThreshold()
                self.appVersion = settings.getAppVersion()
            }
            .onChange(of: appStateObs.value.selectedTheme) { newTheme in
                self.selectedTheme = newTheme
            }
            // A finished download switches models from the view model; mirror it here.
            .onChange(of: appStateObs.value.selectedDetectionModel) { newModel in
                self.selectedDetectionModel = newModel
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
        case "FRENCH": return .french
        default: return .english
        }
    }

    var localeCode: String {
        switch self {
        case .hausa: return "ha"
        case .ewe: return "ee"
        case .asanteTwi: return "tw"
        case .ga: return "ga"
        case .french: return "fr"
        default: return "en"
        }
    }
}

// MARK: - Developer benchmark
//
// The iOS app renders this native SwiftUI settings screen (see ContentView's
// TabView), NOT the shared Compose `SettingsScreen.kt`. `MainViewController()`
// — the Compose entry point — is never hosted anywhere in the iOS target; only
// `MainViewControllerKt.doInitKoin()` is called, from the AppDelegate. So the
// benchmark controls added to the Compose settings screen were unreachable on
// iOS, and this section is their native equivalent.
//
// No benchmark logic lives in Swift. Each action is a single call into the
// shared `ObjectDetector`, and the files offered for sharing are the exact ones
// that implementation wrote — not a re-serialization of the returned object.

/// Drives one benchmark run and exposes its progress, outcome and exported
/// files to SwiftUI.
@MainActor
final class BenchmarkRunner: ObservableObject {

    enum Phase: Equatable {
        case idle
        case running(label: String)
        case finished(summary: String)
        case failed(message: String)
    }

    @Published private(set) var phase: Phase = .idle
    /// Real elapsed seconds. The shared implementation reports no incremental
    /// progress, so this is shown instead of a fabricated percentage.
    @Published private(set) var elapsedSeconds: Int = 0
    /// File URLs from the shared implementation's own `last*BenchmarkFile(s)`.
    @Published private(set) var exportFiles: [URL] = []
    /// Mirrors `deviceEnvironment.isEmulator` from the last extended export —
    /// surfaced in the UI because a figure captured on a simulator must never be
    /// cited as device performance.
    @Published private(set) var lastRunWasSimulator: Bool?

    private var ticker: Task<Void, Never>?

    var isRunning: Bool {
        if case .running = phase { return true }
        return false
    }

    func runQuickBenchmark() {
        guard !isRunning else { return }
        start(label: "Running latency benchmark…")
        Task {
            do {
                let results = try await KoinHelper.objectDetector.runLatencyBenchmark()
                let detector = KoinHelper.objectDetector
                let files = [detector.lastLatencyBenchmarkFile]
                    .compactMap { $0 }
                    .map { URL(fileURLWithPath: $0) }
                finish(
                    summary: results.isEmpty
                        ? "Benchmark produced no measurements — load a model first, then try again."
                        : results
                            .map { "\($0.stage): \(Self.ms($0.stats.meanMs)) mean" }
                            .joined(separator: "  •  "),
                    files: files,
                    isSimulator: nil
                )
            } catch {
                fail("Latency benchmark failed: \(error.localizedDescription)")
            }
        }
    }

    func runExtendedBenchmark() {
        guard !isRunning else { return }
        start(label: "Running extended benchmark…")
        Task {
            do {
                let export = try await KoinHelper.objectDetector.runExtendedBenchmark(
                    warmupRuns: BENCHMARK_EXTENDED_WARMUP_RUNS,
                    measuredRuns: BENCHMARK_EXTENDED_MEASURED_RUNS
                )
                let detector = KoinHelper.objectDetector
                finish(
                    summary: Self.summarize(export),
                    files: detector.lastExtendedBenchmarkFiles.map { URL(fileURLWithPath: $0) },
                    isSimulator: export.deviceEnvironment.isEmulator
                )
            } catch {
                fail("Extended benchmark failed: \(error.localizedDescription)")
            }
        }
    }

    // MARK: Phase transitions

    private func start(label: String) {
        phase = .running(label: label)
        elapsedSeconds = 0
        exportFiles = []
        lastRunWasSimulator = nil
        // The protocol runs for minutes with the screen untouched. Without this
        // the display auto-locks, iOS backgrounds the app, and a CPU-bound
        // background task is killed (SIGKILL) part-way through — which looks
        // exactly like a crash and leaves no crash report behind.
        UIApplication.shared.isIdleTimerDisabled = true
        ticker?.cancel()
        ticker = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                guard let self, self.isRunning else { return }
                self.elapsedSeconds += 1
            }
        }
    }

    private func finish(summary: String, files: [URL], isSimulator: Bool?) {
        ticker?.cancel()
        ticker = nil
        UIApplication.shared.isIdleTimerDisabled = false
        // Never offer a file the filesystem does not actually have.
        exportFiles = files.filter { FileManager.default.fileExists(atPath: $0.path) }
        lastRunWasSimulator = isSimulator
        phase = .finished(summary: summary)
    }

    private func fail(_ message: String) {
        ticker?.cancel()
        ticker = nil
        UIApplication.shared.isIdleTimerDisabled = false
        exportFiles = []
        phase = .failed(message: message)
    }

    // MARK: Display formatting (presentation only — no measurement logic)

    private static func ms(_ value: Double) -> String {
        value.isFinite ? String(format: "%.1f ms", value) : "\(value)"
    }

    private static func summarize(_ export: ComposeApp.BenchmarkExport) -> String {
        let env = export.deviceEnvironment
        var lines: [String] = [
            "schema v\(export.schemaVersion) · \(env.manufacturer) \(env.model) · \(env.osVersion)",
            "build \(env.buildIdentifier)"
        ]

        // One line per routing path. These are deliberately not combined: only
        // the accepted path includes detector inference, so a single "end-to-end"
        // number across paths would be meaningless.
        let paths = export.endToEndByPath.isEmpty
            ? [export.endToEnd.path: export.endToEnd]
            : export.endToEndByPath
        for key in paths.keys.sorted() {
            guard let e2e = paths[key] else { continue }
            lines.append(
                "\(key): mean \(ms(e2e.stats.meanMs)), p95 \(ms(e2e.stats.p95Ms)), "
                + "n=\(e2e.stats.n) · detector ran \(e2e.detectorExecutedCount)/"
                + "\(e2e.measuredRuns)"
            )
        }
        lines.append("\(export.notes.count) caveat(s) in the export — read notes before citing any figure")
        return lines.joined(separator: "\n")
    }
}

/// The "Developer — Benchmark" section, mirroring the shared Compose settings
/// screen's two actions and adding direct JSON/CSV sharing.
/// Detector picker. Bundled models switch immediately; a model whose weights are too
/// large to bundle asks whether to download it or stay on the default, then shows
/// download progress here. Mirrors the Android `ModelPicker.kt`.
struct DetectionModelSection: View {
    @Binding var selectedDetectionModel: String
    @StateObject private var downloadsObs = ObservableFlow(KoinHelper.modelDownloadManager.downloads)
    @StateObject private var langMgr = LanguageManager.shared
    @State private var pendingDownload: DetectionModelSpec?
    @State private var showDownloadsUnavailable = false

    private var manager: ModelDownloadManager { KoinHelper.modelDownloadManager }
    private var downloadable: [DetectionModelSpec] {
        DetectionModelCatalog.shared.all.filter { $0.requiresDownload }
    }

    var body: some View {
        Section(header: LText("Disease Detection Model")) {
            Picker(selection: $selectedDetectionModel) {
                ForEach(DetectionModelCatalog.shared.all, id: \.id) { spec in
                    row(for: spec).tag(spec.id)
                }
            } label: {
                LText("Detection Model")
            }
            .pickerStyle(.navigationLink)
            .onChange(of: selectedDetectionModel) { newModel in
                if manager.isReady(modelId: newModel) {
                    KoinHelper.appViewModel.setSelectedDetectionModel(model: newModel)
                    return
                }
                // Not on the device yet: stay on the current model and ask.
                let spec = DetectionModelCatalog.shared.byId(id: newModel)
                selectedDetectionModel = KoinHelper.appViewModel.appState.value.selectedDetectionModel
                if !(downloadsObs.value.stateOf(modelId: newModel) is ModelDownloadState.Downloading) {
                    pendingDownload = spec
                }
            }

            ForEach(downloadable, id: \.id) { spec in
                status(for: spec)
            }
        }
        .alert(
            L("Download %@?", pendingDownload?.displayName ?? ""),
            isPresented: Binding(get: { pendingDownload != nil }, set: { if !$0 { pendingDownload = nil } }),
            presenting: pendingDownload
        ) { spec in
            Button(L("Download")) {
                if ModelHosting.shared.DOWNLOADS_ENABLED {
                    KoinHelper.appViewModel.downloadDetectionModel(model: spec.id)
                } else {
                    showDownloadsUnavailable = true
                }
            }
            Button(L("Use Default (%@)", DetectionModelCatalog.shared.default_.displayName)) {
                KoinHelper.appViewModel.useDefaultDetectionModel()
                selectedDetectionModel = DetectionModelCatalog.shared.default_.id
            }
            Button(L("Cancel"), role: .cancel) {}
        } message: { spec in
            Text(L("For better performance, you can download and try the %@ model. Its weights are too large to ship with the app, so they are downloaded once and kept on this device. Use Wi-Fi if you can.", spec.displayName))
        }
        // Shown instead of downloading until a host for the weights is chosen.
        .alert(L("Feature Not Available Yet"), isPresented: $showDownloadsUnavailable) {
            Button(L("OK"), role: .cancel) {}
        } message: {
            Text(L("Downloading additional models is not available yet. We are preparing these models and will enable downloads in a future update. You can keep using the built-in models in the meantime."))
        }
    }

    @ViewBuilder
    private func row(for spec: DetectionModelSpec) -> some View {
        let state = downloadsObs.value.stateOf(modelId: spec.id)
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(spec.displayName)
                Group {
                    if !spec.requiresDownload {
                        LText("Built in")
                    } else if state is ModelDownloadState.Downloaded {
                        LText("Downloaded")
                    } else if state is ModelDownloadState.Downloading {
                        LText("Downloading…")
                    } else {
                        LText("Download to use")
                    }
                }
                .font(.caption)
                .foregroundColor(.secondary)
            }
            Spacer()
            if spec.requiresDownload && !(state is ModelDownloadState.Downloaded) {
                Image(systemName: "icloud.and.arrow.down")
                    .foregroundColor(.green)
            }
        }
    }

    @ViewBuilder
    private func status(for spec: DetectionModelSpec) -> some View {
        let state = downloadsObs.value.stateOf(modelId: spec.id)
        if let downloading = state as? ModelDownloadState.Downloading {
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        LText("Downloading %@", spec.displayName)
                            .font(.subheadline.weight(.semibold))
                        Text(Self.progressText(downloading))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    Spacer()
                    Button {
                        manager.cancel(modelId: spec.id)
                    } label: {
                        Image(systemName: "xmark.circle.fill").foregroundColor(.secondary)
                    }
                    .buttonStyle(.borderless)
                    .accessibilityLabel(L("Cancel download"))
                }
                if let fraction = downloading.fraction?.floatValue {
                    ProgressView(value: fraction)
                } else {
                    ProgressView().frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        } else if let failed = state as? ModelDownloadState.Failed {
            HStack(alignment: .top) {
                Image(systemName: "exclamationmark.triangle.fill").foregroundColor(.red)
                LText("%@ download failed: %@", spec.displayName, failed.message)
                    .font(.caption)
                    .foregroundColor(.red)
                Spacer()
                Button(L("Retry")) {
                    KoinHelper.appViewModel.downloadDetectionModel(model: spec.id)
                }
                .buttonStyle(.borderless)
                Button {
                    manager.dismissError(modelId: spec.id)
                } label: {
                    Image(systemName: "xmark").foregroundColor(.secondary)
                }
                .buttonStyle(.borderless)
            }
        } else if state is ModelDownloadState.Downloaded {
            HStack {
                Image(systemName: "checkmark.circle.fill").foregroundColor(.green)
                Text(spec.displayName + " · ") + Text(L("Downloaded"))
                Spacer()
                Button(role: .destructive) {
                    KoinHelper.appViewModel.deleteDownloadedModel(model: spec.id)
                } label: {
                    Image(systemName: "trash")
                }
                .buttonStyle(.borderless)
                .accessibilityLabel(L("Delete downloaded model"))
            }
            .font(.subheadline)
        }
    }

    private static func progressText(_ d: ModelDownloadState.Downloading) -> String {
        let mb = { (bytes: Int64) in String(format: "%.1f MB", Double(bytes) / 1_048_576) }
        if let total = d.totalBytes?.int64Value {
            return "\(mb(d.downloadedBytes)) / \(mb(total))"
        }
        return mb(d.downloadedBytes)
    }
}

struct DeveloperBenchmarkSection: View {

    @StateObject private var runner = BenchmarkRunner()

    var body: some View {
        Section {
            Button {
                runner.runQuickBenchmark()
            } label: {
                row(
                    icon: "speedometer",
                    title: L("Run Latency Benchmark"),
                    subtitle: L("Quick sanity check — 10 warm-up + 50 measured runs, CSV to app storage.")
                )
            }
            .disabled(runner.isRunning)

            Button {
                runner.runExtendedBenchmark()
            } label: {
                row(
                    icon: "chart.bar.doc.horizontal",
                    title: L("Run Extended Benchmark (Publication Protocol)"),
                    subtitle: L("Full protocol — \(BENCHMARK_EXTENDED_WARMUP_RUNS) warm-up + \(BENCHMARK_EXTENDED_MEASURED_RUNS) measured runs per condition. Takes a while.")
                )
            }
            .disabled(runner.isRunning)

            switch runner.phase {
            case .idle:
                EmptyView()

            case .running(let label):
                HStack(spacing: 10) {
                    ProgressView()
                    VStack(alignment: .leading, spacing: 2) {
                        LText(label).font(.subheadline)
                        // Elapsed time, not a fake progress bar: the shared
                        // implementation reports no intermediate progress.
                        Text("\(runner.elapsedSeconds)s elapsed")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

            case .failed(let message):
                Label {
                    Text(message)
                        .font(.caption)
                        .foregroundColor(.red)
                } icon: {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundColor(.red)
                }

            case .finished(let summary):
                VStack(alignment: .leading, spacing: 8) {
                    Label {
                        LText("Benchmark complete").font(.subheadline).fontWeight(.semibold)
                    } icon: {
                        Image(systemName: "checkmark.circle.fill").foregroundColor(.green)
                    }

                    if let isSimulator = runner.lastRunWasSimulator {
                        // Stated in the UI, not just in the export's notes: the
                        // single most common way these numbers get misused is
                        // being quoted as physical-device performance.
                        Label {
                            Text(isSimulator
                                 ? "Simulator run — NOT device performance"
                                 : "Physical device (isEmulator = false)")
                                .font(.caption)
                                .fontWeight(.semibold)
                        } icon: {
                            Image(systemName: isSimulator ? "desktopcomputer" : "iphone")
                        }
                        .foregroundColor(isSimulator ? .orange : .green)
                    }

                    Text(summary)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .textSelection(.enabled)

                    if runner.exportFiles.isEmpty {
                        LText("No export files were written.")
                            .font(.caption2)
                            .foregroundColor(.orange)
                    } else {
                        ForEach(runner.exportFiles, id: \.self) { url in
                            ShareLink(item: url) {
                                Label(url.lastPathComponent, systemImage: "square.and.arrow.up")
                                    .font(.caption)
                            }
                        }
                        if runner.exportFiles.count > 1 {
                            ShareLink(items: runner.exportFiles) {
                                Label {
                                    LText("Share all export files")
                                } icon: {
                                    Image(systemName: "square.and.arrow.up.on.square")
                                }
                                .font(.caption)
                            }
                        }
                    }
                }
                .padding(.vertical, 2)
            }
        } header: {
            LText("Developer — Benchmark")
        } footer: {
            LText("Runs the app's own in-process instrumentation against the loaded ExecuTorch models. Timing comes from monotonic clocks inside the shared implementation; nothing here measures UI or automation overhead.")
        }
    }

    private func row(icon: String, title: String, subtitle: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(.green)
                .frame(width: 24)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .foregroundColor(.primary)
                Text(subtitle)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Spacer()
        }
    }
}
