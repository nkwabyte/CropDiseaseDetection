#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * ObjC-visible bridge to ExecuTorch inference.
 * Implemented in ExecuTorchBridge.swift (@objc(ExecuTorchBridge)).
 * KMP cinterop binds to this header at Gradle compile time;
 * the Swift implementation is resolved at iOS runtime via ObjC dispatch.
 *
 * IMPORTANT: every @objc method added to ExecuTorchBridge.swift MUST be
 * declared here too, or Kotlin/Native's cinterop simply will not see it —
 * unlike normal ObjC/Swift interop, there is no runtime fallback. This header
 * had fallen out of sync with the Swift file before 2026-09-14 (the
 * runLatencyBenchmarkFor* methods added for the Phase 3 quick benchmark were
 * never added here), which would have made the iOS build fail the moment
 * anyone actually tried it — see the 2026-09-14 worklog entry that found and
 * fixed this. Declarations below are grouped to match the Swift file's
 * `// MARK:` sections.
 */
@interface ExecuTorchBridge : NSObject

+ (instancetype)shared;

// MARK: - Load

/** Load a detection model (.pte) from an absolute path. Returns YES on success. */
- (BOOL)loadDetectionModelAtPath:(NSString *)path;

/** Load the EfficientNet-B2 classifier model. Returns YES on success. */
- (BOOL)loadClassifierModelAtPath:(NSString *)path;

// MARK: - Inference



/**
 * Run EfficientNet-B2 classification on JPEG/PNG image bytes.
 * Returns logits for [Corn, Pepper, Tomato, Other].
 * Returns an empty array on failure.
 */
- (NSArray<NSNumber *> *)runClassificationWithImageData:(NSData *)imageData;

/**
 * Dimensions of the image AFTER EXIF orientation is applied, plus the
 * orientation itself. Returns a flat array:
 *   [widthPx, heightPx, exifOrientation(1-8), orientationApplied(1/0)]
 * or an empty array if the bytes cannot be decoded. Runs no model.
 *
 * The Android counterpart is ImageOrientation.orientedSize(); both report the
 * upright image's dimensions, which is what makes the two platforms' results
 * comparable for the same bytes.
 */
- (NSArray<NSNumber *> *)orientedImageSizeWithImageData:(NSData *)imageData;

/** Release both models and free memory. */
- (void)releaseModels;

// MARK: - Stage-timed inference (publication-protocol extended benchmark, added 2026-09-14)
//
// Each returns a flat array: [imageDecodeMs, orientationCorrectionMs,
// preprocessMs, inferenceMs, available(1/0), ...rawModelOutput...]. See the
// Swift implementation's doc comments for exactly what each index means and
// why timing and real inference happen in one call.

- (NSArray<NSNumber *> *)runClassificationStageTimedWithImageData:(NSData *)imageData;

/**
 * Detector with per-stage timing, returning its raw output as a CONTIGUOUS
 * Float32 buffer rather than a boxed NSNumber array.
 *
 * Boxing the full output tensor cost ~226,800 NSNumber objects per call for
 * YOLO26's [1, 27, 8400] — about 11.7 MB retained per call, which drove the
 * extended benchmark past 1.5 GB and got the app killed by jetsam on a physical
 * iPhone. The buffer form is a single ~907 KB copy.
 *
 * Returned keys (all present whenever `available` is 1):
 *   available        NSNumber(BOOL)    0 when no output was produced
 *   imageDecodeMs    NSNumber(double)
 *   orientationMs    NSNumber(double)
 *   preprocessMs     NSNumber(double)
 *   inferenceMs      NSNumber(double)  model forward only
 *   outputTransferMs NSNumber(double)  tensor -> [Float] -> NSData copies
 *   count            NSNumber(NSInteger) Float32 element count in `output`
 *   output           NSData            count * 4 bytes, native byte order
 *
 * `output` is a COPY and is owned by the caller — it never aliases
 * ExecuTorch-owned tensor memory, which is not guaranteed to outlive the call.
 * Callers must validate `output.length == count * 4` before decoding.
 */
- (NSDictionary<NSString *, id> *)runDetectionStageTimedBufferWithImageData:(NSData *)imageData;

/** Stretch-preprocessed (RT-DETR) counterpart. Same keys; boxes are left
 *  normalized to 0…1 with no box rewriting applied. */
- (NSDictionary<NSString *, id> *)runDetectionStretchedStageTimedBufferWithImageData:(NSData *)imageData
                                                                           inputSize:(NSInteger)inputSize;

/**
 * Diagnostic A/B of the two raw-output transports from ONE forward pass.
 * Keys: available (NSNumber BOOL), count (NSNumber), boxed (NSArray<NSNumber *>),
 * buffer (NSData, Float32 native byte order). Both carry the same post-
 * unletterbox data, so a caller can prove the NSData transport is lossless.
 * Diagnostic only — production never calls this.
 */
- (NSDictionary<NSString *, id> *)runDetectionEquivalenceProbeWithImageData:(NSData *)imageData
                                                                isLetterbox:(BOOL)isLetterbox
                                                                  inputSize:(NSInteger)inputSize;

// MARK: - Latency benchmarking (quick developer-button protocol)

/**
 * Runs the classifier `warmupRuns` times (discarded) then `measuredRuns` times
 * (timed via CFAbsoluteTimeGetCurrent()), each on a fresh synthetic 260×260
 * JPEG. Returns per-run wall-clock milliseconds for the measured runs only.
 */
- (NSArray<NSNumber *> *)runLatencyBenchmarkForClassifierWithWarmupRuns:(NSInteger)warmupRuns
                                                             measuredRuns:(NSInteger)measuredRuns;

/**
 * Same protocol as the classifier benchmark, but for the detector at a given
 * synthetic capture resolution (imageWidth×imageHeight, before the model's own
 * inputSize resize/letterbox).
 */
- (NSArray<NSNumber *> *)runLatencyBenchmarkForDetectorWithInputSize:(NSInteger)inputSize
                                                          isLetterbox:(BOOL)isLetterbox
                                                            imageWidth:(NSInteger)imageWidth
                                                           imageHeight:(NSInteger)imageHeight
                                                            warmupRuns:(NSInteger)warmupRuns
                                                          measuredRuns:(NSInteger)measuredRuns;

// MARK: - Publication-protocol utilities (device/build/resource metrics, hashing; added 2026-09-14)

/** Monotonic milliseconds since an arbitrary reference point (not wall-clock). */
- (double)monotonicNowMs;

- (NSString *)sha256HexOfFileAtPath:(NSString *)path;

- (NSString *)sha256HexOfData:(NSData *)data;

/** Process-level (not per-thread) user+system CPU time via getrusage(). */
- (double)processCpuTimeMs;

/** mach_task_basic_info.resident_size — RSS-like, not a PSS equivalent. */
- (uint64_t)residentMemoryBytes;

/** Sum of file sizes under the app's own bundle — a best-effort app-size proxy. */
- (int64_t)installedAppSizeBytes;

- (BOOL)isRunningOnSimulator;

- (NSString *)cpuArchitecture;

/** Raw hardware identifier (e.g. "iPhone16,2"), more specific than UIDevice.model. */
- (NSString *)deviceModelIdentifier;

/**
 * Deterministic synthetic JPEG (striped pattern, not a photo) at the given
 * resolution — the same generator the quick benchmark uses internally,
 * exposed so the extended benchmark's Kotlin side can request identical
 * images for its manifest/stage timing rather than duplicating the generator.
 */
- (nullable NSData *)syntheticJpegDataWithWidth:(NSInteger)width height:(NSInteger)height;

@end

NS_ASSUME_NONNULL_END
