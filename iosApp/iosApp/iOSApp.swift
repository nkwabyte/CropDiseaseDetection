import SwiftUI
import UIKit
import FirebaseCore
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        MainViewControllerKt.doInitKoin()
        #if DEBUG
        HeadlessBenchmark.runIfRequested()
        HeadlessDiagnostics.runIfRequested()
        #endif
        return true
    }
}

#if DEBUG
/// Headless trigger for the publication-protocol benchmark, for scripted runs
/// on a physical device.
///
/// Apple's first-party tooling cannot synthesize touches on a real iPhone
/// (`devicectl` has no input command, `simctl` has none either, and this project
/// has no XCUITest target), so without this the only way to run the benchmark on
/// a device is to tap the Settings button by hand — which is neither repeatable
/// nor scriptable for the per-device runbooks, and cannot be driven from CI.
///
/// This is NOT a second implementation: it calls exactly the same shared
/// `ObjectDetector.runExtendedBenchmark()` the Settings screen's button calls,
/// and the files it reports are the ones that implementation wrote. It is
/// compiled out of Release builds entirely, and is inert unless the app is
/// launched with the explicit argument:
///
///     xcrun devicectl device process launch --console --device <udid> \
///       com.nkwabyte.cropdiseasedetection --run-extended-benchmark
/// DEBUG-only diagnostics for the raw-output transport change (schema v4).
/// Both call shared Kotlin implementations; neither is compiled into Release.
///
///     xcrun simctl launch --console-pty <udid> com.nkwabyte.cropdiseasedetection --run-equivalence-check
///     xcrun simctl launch --console-pty <udid> com.nkwabyte.cropdiseasedetection --run-memory-soak
enum HeadlessDiagnostics {

    private static let tag = "[HeadlessDiagnostics]"

    static func runIfRequested() {
        let args = ProcessInfo.processInfo.arguments
        let wantsEquivalence = args.contains("--run-equivalence-check")
        let wantsSoak = args.contains("--run-memory-soak")
        guard wantsEquivalence || wantsSoak else { return }

        Task { @MainActor in UIApplication.shared.isIdleTimerDisabled = true }
        Task.detached(priority: .userInitiated) {
            defer { Task { @MainActor in UIApplication.shared.isIdleTimerDisabled = false } }
            let detector = KoinHelper.objectDetector
            do {
                try await detector.loadModel()
                try await detector.loadClassifierModel()
            } catch {
                print("\(tag) FAILED to load models: \(error.localizedDescription)")
                return
            }
            // Prefer a real, checksum-locked photograph dropped into the app's
            // Documents directory as `equivalence_input.jpg` — the synthetic
            // striped pattern produces no detections, so it cannot exercise the
            // detection-level half of the comparison. Falls back to the
            // deterministic synthetic image when no file is present.
            let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            let fixture = docs.appendingPathComponent("equivalence_input.jpg")
            let image: Data
            if let onDisk = try? Data(contentsOf: fixture) {
                print("\(tag) using fixture \(fixture.lastPathComponent) (\(onDisk.count) bytes)")
                image = onDisk
            } else if let synthetic = ExecuTorchBridge.shared.syntheticJpegData(withWidth: 1920, height: 1080) {
                print("\(tag) using synthetic 1920x1080 image (no fixture on disk)")
                image = synthetic
            } else {
                print("\(tag) FAILED to obtain a test image")
                return
            }
            let bytes = KotlinByteArray(size: Int32(image.count))
            for (i, b) in image.enumerated() { bytes.set(index: Int32(i), value: Int8(bitPattern: b)) }

            if wantsEquivalence {
                print("\(tag) \(detector.runRawOutputEquivalenceCheck(imageBytes: bytes))")
            }
            if wantsSoak {
                print(detector.runDetectorMemorySoak(imageBytes: bytes, calls: 500, warmup: 20)
                    .split(separator: "\n")
                    .map { "\(tag) \($0)" }
                    .joined(separator: "\n"))
            }
            print("\(tag) DONE")
        }
    }
}

enum HeadlessBenchmark {

    static let launchArgument = "--run-extended-benchmark"

    /// Marker prefix so a console capture can be grepped without ambiguity.
    private static let tag = "[HeadlessBenchmark]"

    static func runIfRequested() {
        guard ProcessInfo.processInfo.arguments.contains(launchArgument) else { return }

        print("\(tag) START warmupRuns=\(BENCHMARK_EXTENDED_WARMUP_RUNS) measuredRuns=\(BENCHMARK_EXTENDED_MEASURED_RUNS)")

        // The protocol runs for minutes. If the screen auto-locks, iOS backgrounds
        // and then kills the app part-way through (SIGKILL), which is what happened
        // on the first device attempt. Hold the idle timer open for the duration.
        Task { @MainActor in UIApplication.shared.isIdleTimerDisabled = true }

        Task.detached(priority: .userInitiated) {
            defer { Task { @MainActor in UIApplication.shared.isIdleTimerDisabled = false } }
            do {
                let detector = KoinHelper.objectDetector
                let export = try await detector.runExtendedBenchmark(
                    warmupRuns: BENCHMARK_EXTENDED_WARMUP_RUNS,
                    measuredRuns: BENCHMARK_EXTENDED_MEASURED_RUNS
                )
                let env = export.deviceEnvironment
                // isEmulator is printed first and on its own line: it is the one
                // field that decides whether a figure may be cited as device
                // performance at all.
                print("\(tag) isEmulator=\(env.isEmulator)")
                print("\(tag) device=\(env.manufacturer) \(env.model) os=\(env.osVersion) build=\(env.buildIdentifier)")
                print("\(tag) schemaVersion=\(export.schemaVersion) notes=\(export.notes.count)")
                for path in detector.lastExtendedBenchmarkFiles {
                    print("\(tag) wrote=\(path)")
                }
                print("\(tag) DONE")
            } catch {
                print("\(tag) FAILED error=\(error.localizedDescription)")
            }
        }
    }
}
#endif

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
