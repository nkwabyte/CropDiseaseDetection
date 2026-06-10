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

/** Load the YOLO26 detection model. Returns YES on success. */
- (BOOL)loadDetectionModelAtPath:(NSString *)path;

/** Load the EfficientNet-B2 classifier model. Returns YES on success. */
- (BOOL)loadClassifierModelAtPath:(NSString *)path;

/**
 * Run YOLO26 detection on JPEG/PNG image bytes.
 * Returns the raw output float array (shape [1 × 27 × N] flattened).
 * Returns an empty array on failure.
 */
- (NSArray<NSNumber *> *)runDetectionWithImageData:(NSData *)imageData;

/**
 * Run EfficientNet-B2 classification on JPEG/PNG image bytes.
 * Returns logits for [Corn, Pepper, Tomato].
 * Returns an empty array on failure.
 */
- (NSArray<NSNumber *> *)runClassificationWithImageData:(NSData *)imageData;

/** Release both models and free memory. */
- (void)releaseModels;

/** Returns the last native detection inference error, if any. */
- (nullable NSString *)lastDetectionErrorMessage;

/** Returns the last native classification inference error, if any. */
- (nullable NSString *)lastClassificationErrorMessage;

/** Metadata from the latest detection preprocessing pass. */
- (NSNumber *)lastDetectionOriginalWidth;
- (NSNumber *)lastDetectionOriginalHeight;
- (NSNumber *)lastDetectionScale;
- (NSNumber *)lastDetectionPadLeft;
- (NSNumber *)lastDetectionPadTop;

@end

NS_ASSUME_NONNULL_END
