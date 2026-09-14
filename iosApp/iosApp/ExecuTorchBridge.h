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
 * Run YOLO26 detection on JPEG/PNG image bytes. Letterboxes to 640×640 and
 * un-letterboxes the returned boxes back to the original aspect ratio.
 * Returns the raw output float array (shape [1 × 27 × N] flattened).
 * Returns an empty array on failure.
 */
- (NSArray<NSNumber *> *)runDetectionWithImageData:(NSData *)imageData;

/**
 * Run a stretch-preprocessed detector (RT-DETR) on JPEG/PNG image bytes.
 * Returns the raw output float array (shape [1 × numQueries × (4 + numClasses)]
 * flattened) with boxes left normalized to 0…1 and no box rewriting applied.
 * Returns an empty array on failure.
 */
- (NSArray<NSNumber *> *)runDetectionStretchedWithImageData:(NSData *)imageData
                                                  inputSize:(NSInteger)inputSize;

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

- (NSArray<NSNumber *> *)runDetectionStageTimedWithImageData:(NSData *)imageData;

- (NSArray<NSNumber *> *)runDetectionStretchedStageTimedWithImageData:(NSData *)imageData
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
