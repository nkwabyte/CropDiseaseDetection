#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * ObjC-visible bridge to ExecuTorch inference.
 * Implemented in ExecuTorchBridge.swift (@objc(ExecuTorchBridge)).
 * KMP cinterop binds to this header at Gradle compile time;
 * the Swift implementation is resolved at iOS runtime via ObjC dispatch.
 */
@interface ExecuTorchBridge : NSObject

+ (instancetype)shared;

/** Load a detection model (.pte) from an absolute path. Returns YES on success. */
- (BOOL)loadDetectionModelAtPath:(NSString *)path;

/** Load the EfficientNet-B2 classifier model. Returns YES on success. */
- (BOOL)loadClassifierModelAtPath:(NSString *)path;

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
 * Returns logits for [Corn, Pepper, Tomato].
 * Returns an empty array on failure.
 */
- (NSArray<NSNumber *> *)runClassificationWithImageData:(NSData *)imageData;

/** Release both models and free memory. */
- (void)releaseModels;

@end

NS_ASSUME_NONNULL_END
