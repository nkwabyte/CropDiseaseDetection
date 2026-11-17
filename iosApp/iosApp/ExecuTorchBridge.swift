import Foundation
import UIKit
import Darwin
import CryptoKit
import ExecuTorch

private struct PreprocessResult {
    let data: Data
    let scale: Float
    let padLeft: Float
    let padTop: Float
    let origWidth: Float
    let origHeight: Float
}

extension UIImage {
    func fixOrientation() -> UIImage {
        if imageOrientation == .up { return self }
        UIGraphicsBeginImageContextWithOptions(size, false, 1.0)
        draw(in: CGRect(origin: .zero, size: size))
        let normalizedImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return normalizedImage ?? self
    }
}

// @objc(ExecuTorchBridge) makes the ObjC class name exactly "ExecuTorchBridge",
// matching ExecuTorchBridge.h so KMP cinterop resolves it at runtime.
@objc(ExecuTorchBridge)
public class ExecuTorchBridge: NSObject {

    @objc public static let shared = ExecuTorchBridge()
    private override init() { super.init() }

    private var detectionModule: Module?
    private var classifierModule: Module?

    // MARK: - Load

    @objc public func loadDetectionModel(atPath path: String) -> Bool {
        do {
            let module = Module(filePath: path, loadMode: .mmap)
            try module.load()
            detectionModule = module
            return true
        } catch {
            print("[ExecuTorchBridge] Detection load failed: \(error)")
            return false
        }
    }

    @objc public func loadClassifierModel(atPath path: String) -> Bool {
        do {
            let module = Module(filePath: path, loadMode: .mmap)
            try module.load()
            classifierModule = module
            return true
        } catch {
            print("[ExecuTorchBridge] Classifier load failed: \(error)")
            return false
        }
    }

    // MARK: - Inference
    //
    // runDetection / runDetectionStretched / runClassification are unchanged in
    // behavior from before this revision: each is now a thin composition of the
    // private stage-helpers below (decodeImageOrNil, correctOrientation,
    // resizeAndNormalize, unletterboxBoxes, runForward), extracted so the
    // *StageTimed methods further down can time each step individually without
    // touching what gets computed. Same inputs in, same outputs out.



    /// EfficientNet-B2: input 260×260, ImageNet normalization.
    /// Output: logits for [Corn, Pepper, Tomato, Other] — the caller rejects on
    /// argmax == Other, and keeps a confidence floor on the three crop classes.
    @objc public func runClassification(withImageData imageData: Data) -> [NSNumber] {
        guard let module = classifierModule,
              let prep = preprocessCHW(imageData, width: 260, height: 260,
                                        mean: (0.485, 0.456, 0.406),
                                        std: (0.229, 0.224, 0.225),
                                        isLetterbox: false) else { return [] }
        return runForward(module: module, data: prep.data,
                          shape: [1, 3, 260, 260],
                          tag: "classification")
    }

    /// Dimensions of the upright image plus its EXIF orientation, without
    /// running any model. Returns
    /// `[widthPx, heightPx, exifOrientation, orientationApplied]`, or `[]` if
    /// the bytes cannot be decoded.
    ///
    /// Derived from the CGImage's stored pixel dimensions and the decoded
    /// orientation, swapping width and height for the four orientations that
    /// transpose the image — the identical rule Android's
    /// `ImageOrientation.orientedSize()` applies to the EXIF tag, so the two
    /// platforms agree on what "upright" means for the same bytes.
    @objc public func orientedImageSize(withImageData imageData: Data) -> [NSNumber] {
        guard let image = decodeImageOrNil(imageData), let cgImage = image.cgImage else { return [] }

        let storedWidth = cgImage.width
        let storedHeight = cgImage.height
        let exif = Self.exifValue(for: image.imageOrientation)
        let transposes = (exif >= 5 && exif <= 8)

        return [
            NSNumber(value: transposes ? storedHeight : storedWidth),
            NSNumber(value: transposes ? storedWidth : storedHeight),
            NSNumber(value: exif),
            NSNumber(value: image.imageOrientation == .up ? 0 : 1),
        ]
    }

    /// UIImage.Orientation -> the EXIF tag value that produces it. The mapping is
    /// the standard one, and is what lets an iOS export be compared against an
    /// Android export tagged with the raw EXIF value.
    private static func exifValue(for orientation: UIImage.Orientation) -> Int {
        switch orientation {
        case .up:            return 1
        case .upMirrored:    return 2
        case .down:          return 3
        case .downMirrored:  return 4
        case .leftMirrored:  return 5
        case .right:         return 6
        case .rightMirrored: return 7
        case .left:          return 8
        @unknown default:    return 1
        }
    }

    @objc public func releaseModels() {
        detectionModule = nil
        classifierModule = nil
    }

    // MARK: - Stage-timed inference (publication-protocol extended benchmark)
    //
    // Added 2026-09-14. Each method below calls the EXACT SAME private helpers
    // as its non-timed counterpart above, in the same order — decodeImageOrNil,
    // correctOrientation, resizeAndNormalize, runForward, and (for the
    // letterboxed detector) unletterboxBoxes. Nothing here changes what gets
    // computed; it only brackets each step with DispatchTime.now().uptimeNanoseconds,
    // a monotonic tick counter (unaffected by wall-clock/NTP adjustments, unlike
    // CFAbsoluteTimeGetCurrent() used by the pre-existing quick-benchmark methods
    // below, which are left untouched).
    //
    // Return shape: a flat [NSNumber] array —
    //   [0] imageDecodeMs, [1] orientationCorrectionMs, [2] preprocessMs,
    //   [3] inferenceMs, [4] available (1.0 success / 0.0 failed at some step),
    //   [5...] the raw model output (empty if [4] == 0), so Kotlin still gets a
    //   real result to postprocess (softmax/argmax for the classifier, box
    //   decode + NMS for the detector — both already implemented in Kotlin in
    //   iosMain/ObjectDetector.kt) — a single call does both timing and real
    //   work, rather than running inference twice.

    @objc public func runClassificationStageTimed(withImageData imageData: Data) -> [NSNumber] {
        let header0: [NSNumber] = [0, 0, 0, 0, 0]
        guard let module = classifierModule else { return header0 }

        let tDecode0 = monotonicNowNanos()
        let rawImage = decodeImageOrNil(imageData)
        let tDecode1 = monotonicNowNanos()
        guard let rawImage = rawImage else {
            return [msBetween(tDecode0, tDecode1), 0, 0, 0, 0]
        }

        let tOrient0 = monotonicNowNanos()
        let uiImage = correctOrientation(rawImage)
        let tOrient1 = monotonicNowNanos()

        let tPre0 = monotonicNowNanos()
        let prep = resizeAndNormalize(uiImage, width: 260, height: 260,
                                       mean: (0.485, 0.456, 0.406), std: (0.229, 0.224, 0.225),
                                       isLetterbox: false)
        let tPre1 = monotonicNowNanos()
        guard let prep = prep else {
            return [msBetween(tDecode0, tDecode1), msBetween(tOrient0, tOrient1), msBetween(tPre0, tPre1), 0, 0]
        }

        let tInfer0 = monotonicNowNanos()
        let output = runForward(module: module, data: prep.data, shape: [1, 3, 260, 260], tag: "classification-stage-timed")
        let tInfer1 = monotonicNowNanos()

        var result: [NSNumber] = [
            msBetween(tDecode0, tDecode1),
            msBetween(tOrient0, tOrient1),
            msBetween(tPre0, tPre1),
            msBetween(tInfer0, tInfer1),
            output.isEmpty ? 0 : 1,
        ]
        result.append(contentsOf: output)
        return result
    }

    /// Detector (letterboxed, YOLO26) with per-stage timing and its raw output
    /// returned as a contiguous `Float32` buffer.
    ///
    /// Dictionary keys — all present whenever `available` is 1:
    ///   available        NSNumber(Bool)   0 when no output was produced
    ///   imageDecodeMs    NSNumber(Double)
    ///   orientationMs    NSNumber(Double)
    ///   preprocessMs     NSNumber(Double)
    ///   inferenceMs      NSNumber(Double) model forward only
    ///   outputTransferMs NSNumber(Double) tensor -> [Float] -> NSData copies
    ///   count            NSNumber(Int)    Float32 element count in `output`
    ///   output           NSData           count * 4 bytes, native byte order
    ///
    /// `output` is a copy; the caller owns it and it does not alias ExecuTorch
    /// memory. `count` is what the caller validates the byte length against.
    @objc public func runDetectionStageTimedBuffer(withImageData imageData: Data) -> [String: Any] {
        guard let module = detectionModule else { return Self.unavailableBuffer() }

        let tDecode0 = monotonicNowNanos()
        let rawImage = decodeImageOrNil(imageData)
        let tDecode1 = monotonicNowNanos()
        guard let rawImage = rawImage else {
            return Self.unavailableBuffer(imageDecodeMs: msDouble(tDecode0, tDecode1))
        }

        let tOrient0 = monotonicNowNanos()
        let uiImage = correctOrientation(rawImage)
        let tOrient1 = monotonicNowNanos()

        let tPre0 = monotonicNowNanos()
        let prep = resizeAndNormalize(uiImage, width: 640, height: 640,
                                       mean: (0.0, 0.0, 0.0), std: (1.0, 1.0, 1.0),
                                       isLetterbox: true)
        let tPre1 = monotonicNowNanos()
        guard let prep = prep else {
            return Self.unavailableBuffer(imageDecodeMs: msDouble(tDecode0, tDecode1),
                                          orientationMs: msDouble(tOrient0, tOrient1),
                                          preprocessMs: msDouble(tPre0, tPre1))
        }

        guard var raw = runForwardRaw(module: module, data: prep.data,
                                      shape: [1, 3, 640, 640], tag: "detection-stage-timed") else {
            return Self.unavailableBuffer(imageDecodeMs: msDouble(tDecode0, tDecode1),
                                          orientationMs: msDouble(tOrient0, tOrient1),
                                          preprocessMs: msDouble(tPre0, tPre1))
        }

        let tBox0 = monotonicNowNanos()
        unletterboxBoxes(&raw.floats, numClasses: 23,
                         scale: prep.scale, padLeft: prep.padLeft, padTop: prep.padTop,
                         origWidth: prep.origWidth, origHeight: prep.origHeight)
        let data = raw.floats.withUnsafeBufferPointer { Data(buffer: $0) }
        let tBox1 = monotonicNowNanos()

        return [
            "available": NSNumber(value: true),
            "imageDecodeMs": NSNumber(value: msDouble(tDecode0, tDecode1)),
            "orientationMs": NSNumber(value: msDouble(tOrient0, tOrient1)),
            "preprocessMs": NSNumber(value: msDouble(tPre0, tPre1)),
            "inferenceMs": NSNumber(value: raw.forwardMs),
            "outputTransferMs": NSNumber(value: raw.materializeMs + msDouble(tBox0, tBox1)),
            "count": NSNumber(value: raw.floats.count),
            "output": data,
        ]
    }

    /// Stretch-preprocessed detector (RT-DETR) counterpart of
    /// `runDetectionStageTimedBuffer`. Boxes are left normalized to 0…1 and no
    /// box rewriting is applied, exactly as before.
    @objc public func runDetectionStretchedStageTimedBuffer(withImageData imageData: Data,
                                                            inputSize: Int) -> [String: Any] {
        guard let module = detectionModule else { return Self.unavailableBuffer() }

        let tDecode0 = monotonicNowNanos()
        let rawImage = decodeImageOrNil(imageData)
        let tDecode1 = monotonicNowNanos()
        guard let rawImage = rawImage else {
            return Self.unavailableBuffer(imageDecodeMs: msDouble(tDecode0, tDecode1))
        }

        let tOrient0 = monotonicNowNanos()
        let uiImage = correctOrientation(rawImage)
        let tOrient1 = monotonicNowNanos()

        let tPre0 = monotonicNowNanos()
        let prep = resizeAndNormalize(uiImage, width: inputSize, height: inputSize,
                                       mean: (0.0, 0.0, 0.0), std: (1.0, 1.0, 1.0),
                                       isLetterbox: false)
        let tPre1 = monotonicNowNanos()
        guard let prep = prep else {
            return Self.unavailableBuffer(imageDecodeMs: msDouble(tDecode0, tDecode1),
                                          orientationMs: msDouble(tOrient0, tOrient1),
                                          preprocessMs: msDouble(tPre0, tPre1))
        }

        guard let raw = runForwardRaw(module: module, data: prep.data,
                                      shape: [1, 3, inputSize, inputSize],
                                      tag: "detection-stretched-stage-timed") else {
            return Self.unavailableBuffer(imageDecodeMs: msDouble(tDecode0, tDecode1),
                                          orientationMs: msDouble(tOrient0, tOrient1),
                                          preprocessMs: msDouble(tPre0, tPre1))
        }

        let tCopy0 = monotonicNowNanos()
        let data = raw.floats.withUnsafeBufferPointer { Data(buffer: $0) }
        let tCopy1 = monotonicNowNanos()

        return [
            "available": NSNumber(value: true),
            "imageDecodeMs": NSNumber(value: msDouble(tDecode0, tDecode1)),
            "orientationMs": NSNumber(value: msDouble(tOrient0, tOrient1)),
            "preprocessMs": NSNumber(value: msDouble(tPre0, tPre1)),
            "inferenceMs": NSNumber(value: raw.forwardMs),
            "outputTransferMs": NSNumber(value: raw.materializeMs + msDouble(tCopy0, tCopy1)),
            "count": NSNumber(value: raw.floats.count),
            "output": data,
        ]
    }

    /// Diagnostic A/B of the two raw-output transports, from ONE forward pass.
    ///
    /// Returns the SAME post-unletterbox float data both ways — `boxed` as the
    /// `[NSNumber]` array the bridge used to return, and `buffer` as the
    /// contiguous `Float32` NSData it returns now — so a caller can prove the
    /// transport change is lossless without model nondeterminism confounding the
    /// comparison. Used only by the equivalence check; production never calls it.
    @objc public func runDetectionEquivalenceProbe(withImageData imageData: Data,
                                                   isLetterbox: Bool,
                                                   inputSize: Int) -> [String: Any] {
        guard let module = detectionModule,
              let prep = preprocessCHW(imageData,
                                       width: isLetterbox ? 640 : inputSize,
                                       height: isLetterbox ? 640 : inputSize,
                                       mean: (0.0, 0.0, 0.0), std: (1.0, 1.0, 1.0),
                                       isLetterbox: isLetterbox),
              var raw = runForwardRaw(module: module, data: prep.data,
                                      shape: [1, 3, isLetterbox ? 640 : inputSize, isLetterbox ? 640 : inputSize],
                                      tag: "equivalence-probe")
        else { return ["available": NSNumber(value: false)] }

        if isLetterbox {
            unletterboxBoxes(&raw.floats, numClasses: 23,
                             scale: prep.scale, padLeft: prep.padLeft, padTop: prep.padTop,
                             origWidth: prep.origWidth, origHeight: prep.origHeight)
        }

        return [
            "available": NSNumber(value: true),
            "count": NSNumber(value: raw.floats.count),
            "boxed": raw.floats.map { NSNumber(value: $0) },
            "buffer": raw.floats.withUnsafeBufferPointer { Data(buffer: $0) },
        ]
    }

    /// A well-formed "nothing was produced" reply, so the caller never has to
    /// distinguish a missing key from a zero.
    private static func unavailableBuffer(imageDecodeMs: Double = 0,
                                          orientationMs: Double = 0,
                                          preprocessMs: Double = 0) -> [String: Any] {
        return [
            "available": NSNumber(value: false),
            "imageDecodeMs": NSNumber(value: imageDecodeMs),
            "orientationMs": NSNumber(value: orientationMs),
            "preprocessMs": NSNumber(value: preprocessMs),
            "inferenceMs": NSNumber(value: 0.0),
            "outputTransferMs": NSNumber(value: 0.0),
            "count": NSNumber(value: 0),
            "output": Data(),
        ]
    }

    // MARK: - Latency benchmarking (quick developer-button protocol, unchanged)
    //
    // Added for Phase 3 of the accompanying research project (mobile performance
    // evaluation). The README's existing "~18ms"/"~24ms"/"sub-100ms" latency claims
    // had zero instrumentation behind them; these two methods are what actually
    // measure it, on the real device, through the real ExecuTorch runtime — not a
    // simulator. Synthetic images only: legitimate for latency (compute cost is
    // driven by tensor shape, not pixel content), never used for accuracy.
    //
    // Left exactly as originally written (including its CFAbsoluteTimeGetCurrent()
    // timing, which is wall-clock-based rather than a true monotonic tick counter —
    // see the note on the *StageTimed methods above, which use a genuinely
    // monotonic clock instead) to avoid any churn on already-verified code. Use
    // the extended benchmark (runClassificationStageTimed / runDetection*StageTimed
    // / monotonicNowMs) for anything that will be cited.

    /// Runs the classifier `warmupRuns` times (discarded, lets caches/JIT/thermal
    /// state settle) then `measuredRuns` times (timed), each on a fresh synthetic
    /// 260×260 JPEG. Returns per-run wall-clock milliseconds for the measured runs
    /// only, in run order.
    @objc public func runLatencyBenchmarkForClassifier(withWarmupRuns warmupRuns: Int, measuredRuns: Int) -> [NSNumber] {
        guard classifierModule != nil,
              let imageData = Self.syntheticJpegData(width: 260, height: 260) else { return [] }

        for _ in 0..<warmupRuns {
            _ = runClassification(withImageData: imageData)
        }

        var latenciesMs: [NSNumber] = []
        latenciesMs.reserveCapacity(measuredRuns)
        for _ in 0..<measuredRuns {
            let t0 = CFAbsoluteTimeGetCurrent()
            _ = runClassification(withImageData: imageData)
            let t1 = CFAbsoluteTimeGetCurrent()
            latenciesMs.append(NSNumber(value: (t1 - t0) * 1000.0))
        }
        return latenciesMs
    }

    /// Same protocol as the classifier benchmark, but for the detector at a given
    /// synthetic capture resolution (`imageWidth`×`imageHeight`, before the model's
    /// own `inputSize` resize/letterbox) — decode and preprocessing cost scale with
    /// input resolution, so this is swept across plausible camera-capture sizes
    /// rather than measured at only the model's fixed input size.
    @objc public func runLatencyBenchmarkForDetector(withInputSize inputSize: Int, isLetterbox: Bool, imageWidth: Int, imageHeight: Int, warmupRuns: Int, measuredRuns: Int) -> [NSNumber] {
        guard detectionModule != nil,
              let imageData = Self.syntheticJpegData(width: imageWidth, height: imageHeight) else { return [] }

        func runOnce() {
            if isLetterbox {
                _ = runDetectionStageTimedBuffer(withImageData: imageData)
            } else {
                _ = runDetectionStretchedStageTimedBuffer(withImageData: imageData, inputSize: inputSize)
            }
        }

        for _ in 0..<warmupRuns {
            runOnce()
        }

        var latenciesMs: [NSNumber] = []
        latenciesMs.reserveCapacity(measuredRuns)
        for _ in 0..<measuredRuns {
            let t0 = CFAbsoluteTimeGetCurrent()
            runOnce()
            let t1 = CFAbsoluteTimeGetCurrent()
            latenciesMs.append(NSNumber(value: (t1 - t0) * 1000.0))
        }
        return latenciesMs
    }

    /// ObjC-visible wrapper over the deterministic synthetic-image generator, so
    /// the Kotlin extended benchmark can request the SAME images the quick
    /// benchmark builds internally instead of carrying its own generator.
    ///
    /// This method is declared in ExecuTorchBridge.h and was called by
    /// `iosMain/ObjectDetector.kt`, but no `@objc` implementation existed — the
    /// generator below is `private static`, which is invisible to the
    /// Objective-C runtime. cinterop binds against the header and cannot detect
    /// that, so the call compiled and linked cleanly and then died at runtime
    /// with `unrecognized selector sent to instance` the first time the extended
    /// benchmark ran on a device. Verify header/implementation agreement against
    /// Xcode's generated `iosApp-Swift.h`, never against the selector strings in
    /// the linked binary: a selector merely *referenced* by a call site appears
    /// there too, so that check cannot tell a missing implementation from a
    /// present one.
    @objc public func syntheticJpegData(withWidth width: Int, height: Int) -> Data? {
        return Self.syntheticJpegData(width: width, height: height)
    }

    /// Builds a deterministic synthetic JPEG (a striped pattern, not a photo of
    /// anything real) at the given resolution.
    private static func syntheticJpegData(width: Int, height: Int) -> Data? {
        let size = CGSize(width: width, height: height)
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1.0
        format.opaque = true
        let renderer = UIGraphicsImageRenderer(size: size, format: format)
        let stripe = max(1, width / 32)
        let image = renderer.image { context in
            var x = 0
            var stripeIndex = 0
            while x < width {
                let color: UIColor = stripeIndex % 2 == 0
                    ? UIColor(red: 60.0 / 255.0, green: 140.0 / 255.0, blue: 60.0 / 255.0, alpha: 1.0)
                    : UIColor(red: 160.0 / 255.0, green: 200.0 / 255.0, blue: 160.0 / 255.0, alpha: 1.0)
                color.setFill()
                context.fill(CGRect(x: x, y: 0, width: stripe, height: height))
                x += stripe
                stripeIndex += 1
            }
        }
        return image.jpegData(compressionQuality: 0.9)
    }

    // MARK: - Publication-protocol utilities (device/build/resource metrics, hashing)
    //
    // Added 2026-09-14 so the extended benchmark's device/model/resource metadata
    // can be collected without hand-rolling C interop on the Kotlin/Native side.

    /// A monotonic clock in nanoseconds since an arbitrary reference point (NOT
    /// wall-clock time — unaffected by clock/NTP adjustments), for the Kotlin side
    /// to time cold-loads, end-to-end runs, and Kotlin-side postprocessing.
    @objc public func monotonicNowMs() -> Double {
        return Double(monotonicNowNanos()) / 1_000_000.0
    }

    @objc public func sha256Hex(ofFileAtPath path: String) -> String {
        guard let data = FileManager.default.contents(atPath: path) else { return "" }
        return sha256Hex(ofData: data)
    }

    @objc public func sha256Hex(ofData data: Data) -> String {
        let digest = SHA256.hash(data: data)
        return digest.map { String(format: "%02x", $0) }.joined()
    }

    /// Process-level (not per-thread — iOS does not expose an equivalent to
    /// Android's Debug.threadCpuTimeNanos() without private APIs) user+system CPU
    /// time via getrusage(). A CPU-utilization proxy, not a true per-thread figure;
    /// see the extended benchmark's notes for why this differs from the Android
    /// metric it sits next to in the same export schema.
    @objc public func processCpuTimeMs() -> Double {
        var usage = rusage()
        getrusage(RUSAGE_SELF, &usage)
        let userMs = Double(usage.ru_utime.tv_sec) * 1000.0 + Double(usage.ru_utime.tv_usec) / 1000.0
        let sysMs = Double(usage.ru_stime.tv_sec) * 1000.0 + Double(usage.ru_stime.tv_usec) / 1000.0
        return userMs + sysMs
    }

    /// Resident memory (mach_task_basic_info.resident_size) — conceptually closer
    /// to RSS than to Android's PSS (iOS exposes no PSS-equivalent to third-party
    /// code), so it is not directly comparable across platforms. Flagged as such
    /// in the extended benchmark's notes.
    @objc public func residentMemoryBytes() -> UInt64 {
        var info = mach_task_basic_info()
        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size / MemoryLayout<integer_t>.size)
        let kerr: kern_return_t = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: Int(count)) {
                task_info(mach_task_self_, task_flavor_t(MACH_TASK_BASIC_INFO), $0, &count)
            }
        }
        return kerr == KERN_SUCCESS ? info.resident_size : 0
    }

    /// Sum of file sizes under the app's own bundle — the closest same-effort
    /// equivalent to Android's base-APK-file-size figure. Excludes on-device
    /// install-time optimizations, App Thinning slicing already applied by the
    /// App Store, and any data the app has written since install, so — like the
    /// Android figure it sits next to — this is a best-effort under-count, not a
    /// true "installed size" as Settings > General > iPhone Storage would report.
    @objc public func installedAppSizeBytes() -> Int64 {
        let bundleURL = Bundle.main.bundleURL
        guard let enumerator = FileManager.default.enumerator(
            at: bundleURL, includingPropertiesForKeys: [.fileSizeKey], options: [], errorHandler: nil
        ) else { return 0 }
        var total: Int64 = 0
        for case let fileURL as URL in enumerator {
            if let sizeValue = try? fileURL.resourceValues(forKeys: [.fileSizeKey]).fileSize {
                total += Int64(sizeValue)
            }
        }
        return total
    }

    @objc public func isRunningOnSimulator() -> Bool {
        #if targetEnvironment(simulator)
        return true
        #else
        return false
        #endif
    }

    @objc public func cpuArchitecture() -> String {
        #if arch(arm64)
        return "arm64"
        #elseif arch(x86_64)
        return "x86_64"
        #else
        return "unknown"
        #endif
    }

    /// The raw hardware identifier (e.g. "iPhone16,2"), more specific than
    /// UIDevice.currentDevice.model ("iPhone"). Empty string on failure.
    @objc public func deviceModelIdentifier() -> String {
        var systemInfo = utsname()
        uname(&systemInfo)
        let mirror = Mirror(reflecting: systemInfo.machine)
        return mirror.children.reduce("") { partial, element in
            guard let value = element.value as? Int8, value != 0 else { return partial }
            return partial + String(UnicodeScalar(UInt8(value)))
        }
    }

    // MARK: - Private helpers

    private func monotonicNowNanos() -> UInt64 {
        return DispatchTime.now().uptimeNanoseconds
    }

    /// Elapsed milliseconds between two monotonic timestamps, unboxed.
    private func msDouble(_ start: UInt64, _ end: UInt64) -> Double {
        return Double(end - start) / 1_000_000.0
    }

    /// Elapsed milliseconds between two monotonic timestamps, boxed as NSNumber.
    ///
    /// Every caller puts the value straight into an `[NSNumber]` result array for
    /// Kotlin, and Swift will not implicitly bridge a `Double` into one, so the
    /// boxing happens here rather than at eighteen call sites.
    private func msBetween(_ start: UInt64, _ end: UInt64) -> NSNumber {
        return NSNumber(value: Double(end - start) / 1_000_000.0)
    }

    /// One detector forward pass, with its output materialized into a contiguous
    /// `Float32` buffer instead of ~226,800 boxed `NSNumber` objects.
    ///
    /// The copy out of the tensor is deliberate and is NOT optional: ExecuTorch
    /// owns that memory and does not guarantee it outlives this scope, so a
    /// no-copy `Data(bytesNoCopy:)` wrapper would hand Kotlin a buffer that can
    /// dangle. One ~907 KB copy is the correct trade.
    ///
    /// `forwardMs` and `materializeMs` are returned separately so the caller can
    /// report the output transfer on its own while still including it in the
    /// comparable detector total — the boxing this replaces was previously inside
    /// the measured span, so dropping it silently would have made iOS look faster
    /// for free.
    private struct RawTensorOutput {
        var floats: [Float]
        let forwardMs: Double
        let materializeMs: Double
    }

    private func runForwardRaw(module: Module, data: Data,
                               shape: [Int], tag: String) -> RawTensorOutput? {
        do {
            let inputTensor = Tensor<Float>(data: data, shape: shape)
            let t0 = monotonicNowNanos()
            let outputs = try module.forward(inputTensor)
            let t1 = monotonicNowNanos()
            guard let outTensor: Tensor<Float> = outputs.first?.tensor() else {
                print("[ExecuTorchBridge] \(tag) produced no float tensor output")
                return nil
            }
            // Array(buffer) copies element-wise out of ExecuTorch-owned memory.
            let floats: [Float] = outTensor.withUnsafeBytes { buffer in Array(buffer) }
            let t2 = monotonicNowNanos()
            return RawTensorOutput(
                floats: floats,
                forwardMs: msDouble(t0, t1),
                materializeMs: msDouble(t1, t2)
            )
        } catch {
            print("[ExecuTorchBridge] \(tag) inference failed: \(error)")
            return nil
        }
    }

    private func runForward(module: Module, data: Data,
                             shape: [Int], tag: String) -> [NSNumber] {
        do {
            let inputTensor = Tensor<Float>(data: data, shape: shape)
            let outputs = try module.forward(inputTensor)
            guard let outTensor: Tensor<Float> = outputs.first?.tensor() else {
                print("[ExecuTorchBridge] \(tag) produced no float tensor output")
                return []
            }
            return outTensor.withUnsafeBytes { buffer in
                buffer.map { NSNumber(value: $0) }
            }
        } catch {
            print("[ExecuTorchBridge] \(tag) inference failed: \(error)")
            return []
        }
    }

    /// Un-letterboxes a YOLO-layout [1, 4+numClasses, N] raw output's boxes back to
    /// original-aspect-ratio 640×640 canvas space, in place, and returns it — the
    /// exact box-rewrite loop `runDetection` used to inline, extracted so
    /// `runDetectionStageTimed` can call the identical logic.
    /// Same box-rewrite loop as before, operating on the contiguous buffer in
    /// place. The arithmetic is unchanged — only the container is.
    private func unletterboxBoxes(_ output: inout [Float], numClasses: Int,
                                   scale: Float, padLeft: Float, padTop: Float,
                                   origWidth: Float, origHeight: Float) {
        let rowStride = numClasses + 4
        let n = output.count / rowStride
        if n <= 0 { return }

        for i in 0..<n {
            let cx = output[0 * n + i]
            let cy = output[1 * n + i]
            let w  = output[2 * n + i]
            let h  = output[3 * n + i]

            let x1Lb = cx - w / 2.0
            let y1Lb = cy - h / 2.0
            let x2Lb = cx + w / 2.0
            let y2Lb = cy + h / 2.0

            let x1 = min(max(0.0, ((x1Lb - padLeft) / scale / origWidth) * 640.0), 640.0)
            let y1 = min(max(0.0, ((y1Lb - padTop) / scale / origHeight) * 640.0), 640.0)
            let x2 = min(max(0.0, ((x2Lb - padLeft) / scale / origWidth) * 640.0), 640.0)
            let y2 = min(max(0.0, ((y2Lb - padTop) / scale / origHeight) * 640.0), 640.0)

            let newCx = (x1 + x2) / 2.0
            let newCy = (y1 + y2) / 2.0
            let newW  = max(0.0, x2 - x1)
            let newH  = max(0.0, y2 - y1)

            output[0 * n + i] = newCx
            output[1 * n + i] = newCy
            output[2 * n + i] = newW
            output[3 * n + i] = newH
        }
    }

    private func decodeImageOrNil(_ data: Data) -> UIImage? {
        return UIImage(data: data)
    }

    private func correctOrientation(_ image: UIImage) -> UIImage {
        return image.fixOrientation()
    }

    /// Decodes JPEG/PNG bytes, resizes to (width × height) with optional letterboxing, returns CHW float array and preprocessing metadata.
    private func preprocessCHW(_ data: Data, width: Int, height: Int,
                                mean: (Float, Float, Float),
                                std: (Float, Float, Float),
                                isLetterbox: Bool = false) -> PreprocessResult? {
        guard let rawImage = decodeImageOrNil(data) else { return nil }
        let uiImage = correctOrientation(rawImage)
        return resizeAndNormalize(uiImage, width: width, height: height, mean: mean, std: std, isLetterbox: isLetterbox)
    }

    /// The resize/letterbox + pixel-normalization half of preprocessCHW, extracted
    /// so the *StageTimed methods can time it separately from decode/orientation.
    private func resizeAndNormalize(_ uiImage: UIImage, width: Int, height: Int,
                                     mean: (Float, Float, Float),
                                     std: (Float, Float, Float),
                                     isLetterbox: Bool) -> PreprocessResult? {
        let origW = Float(uiImage.size.width)
        let origH = Float(uiImage.size.height)
        if origW <= 0 || origH <= 0 { return nil }

        let targetW = Float(width)
        let targetH = Float(height)

        let drawRect: CGRect
        let scale: Float
        let padLeft: Float
        let padTop: Float

        if isLetterbox {
            scale = min(targetW / origW, targetH / origH)
            let scaledW = origW * scale
            let scaledH = origH * scale
            padLeft = (targetW - scaledW) / 2.0
            padTop = (targetH - scaledH) / 2.0
            drawRect = CGRect(x: CGFloat(padLeft), y: CGFloat(padTop), width: CGFloat(scaledW), height: CGFloat(scaledH))
        } else {
            scale = 1.0
            padLeft = 0.0
            padTop = 0.0
            drawRect = CGRect(x: 0, y: 0, width: CGFloat(targetW), height: CGFloat(targetH))
        }

        let format = UIGraphicsImageRendererFormat()
        format.scale = 1.0
        format.opaque = true

        let renderer = UIGraphicsImageRenderer(size: CGSize(width: CGFloat(targetW), height: CGFloat(targetH)), format: format)
        let resizedImage = renderer.image { context in
            if isLetterbox {
                UIColor(red: 114.0 / 255.0, green: 114.0 / 255.0, blue: 114.0 / 255.0, alpha: 1.0).setFill()
                context.fill(CGRect(x: 0, y: 0, width: CGFloat(targetW), height: CGFloat(targetH)))
            }
            uiImage.draw(in: drawRect)
        }

        guard let cgImage = resizedImage.cgImage else { return nil }

        var rawBytes = [UInt8](repeating: 0, count: width * height * 4)
        guard let ctx = CGContext(
            data: &rawBytes,
            width: width, height: height,
            bitsPerComponent: 8,
            bytesPerRow: width * 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGBitmapInfo.byteOrder32Big.rawValue | CGImageAlphaInfo.noneSkipLast.rawValue
        ) else { return nil }

        ctx.draw(cgImage, in: CGRect(x: 0, y: 0, width: CGFloat(width), height: CGFloat(height)))

        let n = width * height
        let byteCount = 3 * n * MemoryLayout<Float>.stride

        let ptr = UnsafeMutableRawPointer.allocate(byteCount: byteCount, alignment: 16)
        let floatPtr = ptr.bindMemory(to: Float.self, capacity: 3 * n)

        for i in 0..<n {
            floatPtr[0 * n + i] = (Float(rawBytes[i * 4])     / 255.0 - mean.0) / std.0
            floatPtr[1 * n + i] = (Float(rawBytes[i * 4 + 1]) / 255.0 - mean.1) / std.1
            floatPtr[2 * n + i] = (Float(rawBytes[i * 4 + 2]) / 255.0 - mean.2) / std.2
        }

        let alignedData = Data(bytesNoCopy: ptr, count: byteCount, deallocator: .custom { p, _ in
            p.deallocate()
        })

        return PreprocessResult(
            data: alignedData,
            scale: scale,
            padLeft: padLeft,
            padTop: padTop,
            origWidth: origW,
            origHeight: origH
        )
    }
}
