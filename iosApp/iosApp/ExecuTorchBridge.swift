import Foundation
import UIKit

// REQUIRES: ExecuTorch Swift Package added to this Xcode target.
// Xcode → File → Add Package Dependencies
// URL: https://github.com/pytorch/executorch  branch: swiftpm-1.3.1
// Products to add: executorch, backend_xnnpack, kernels_optimized
import ExecuTorch

// @objc(ExecuTorchBridge) makes the ObjC class name exactly "ExecuTorchBridge",
// matching ExecuTorchBridge.h so KMP cinterop resolves it at runtime.
@objc(ExecuTorchBridge)
public class ExecuTorchBridge: NSObject {
    private struct DetectionPreprocessMeta {
        let originalWidth: Int
        let originalHeight: Int
        let scale: Float
        let padLeft: Float
        let padTop: Float
    }

    @objc public static let shared = ExecuTorchBridge()
    private override init() { super.init() }

    private var detectionModule: Module?
    private var classifierModule: Module?
    private var detectionErrorMessage: String?
    private var classificationErrorMessage: String?
    private var detectionPreprocessMeta = DetectionPreprocessMeta(
        originalWidth: 0,
        originalHeight: 0,
        scale: 0,
        padLeft: 0,
        padTop: 0
    )

    // MARK: - Load

    @objc public func loadDetectionModel(atPath path: String) -> Bool {
        do {
            let module = try Module(filePath: path)
            try module.load("forward")
            detectionModule = module
            detectionErrorMessage = nil
            return true
        } catch {
            let message = "[ExecuTorchBridge] Detection load failed: \(error)"
            detectionErrorMessage = message
            print(message)
            return false
        }
    }

    @objc public func loadClassifierModel(atPath path: String) -> Bool {
        do {
            let module = try Module(filePath: path)
            try module.load("forward")
            classifierModule = module
            classificationErrorMessage = nil
            return true
        } catch {
            let message = "[ExecuTorchBridge] Classifier load failed: \(error)"
            classificationErrorMessage = message
            print(message)
            return false
        }
    }

    // MARK: - Inference

    /// YOLO26: input 640×640, pixel/255 normalization (mean=0, std=1).
    /// Output: raw float array flattened from [1, 27, N].
    @objc public func runDetection(withImageData imageData: Data) -> [NSNumber] {
        guard let module = detectionModule,
              let (pixels, meta) = preprocessDetectionCHW(imageData, width: 640, height: 640) else {
            detectionErrorMessage = "[ExecuTorchBridge] detection preprocessing failed: unable to decode or letterbox input image"
            print(detectionErrorMessage!)
            return []
        }
        detectionPreprocessMeta = meta
        return runForward(module: module, pixels: pixels,
                          shape: [1, 3, 640, 640], tag: "detection")
    }

    /// EfficientNet-B2: input 260×260, ImageNet normalization.
    /// Output: logits for [Corn, Pepper, Tomato].
    @objc public func runClassification(withImageData imageData: Data) -> [NSNumber] {
        guard let module = classifierModule,
              let pixels = preprocessResizedCHW(imageData, width: 260, height: 260,
                                                mean: (0.485, 0.456, 0.406),
                                                std: (0.229, 0.224, 0.225)) else {
            classificationErrorMessage = "[ExecuTorchBridge] classification preprocessing failed: unable to decode input image"
            print(classificationErrorMessage!)
            return []
        }
        return runForward(module: module, pixels: pixels,
                          shape: [1, 3, 260, 260], tag: "classification")
    }

    @objc public func releaseModels() {
        detectionModule = nil
        classifierModule = nil
        detectionErrorMessage = nil
        classificationErrorMessage = nil
        detectionPreprocessMeta = DetectionPreprocessMeta(
            originalWidth: 0,
            originalHeight: 0,
            scale: 0,
            padLeft: 0,
            padTop: 0
        )
    }

    @objc public func lastDetectionErrorMessage() -> String? {
        detectionErrorMessage
    }

    @objc public func lastClassificationErrorMessage() -> String? {
        classificationErrorMessage
    }

    @objc public func lastDetectionOriginalWidth() -> NSNumber {
        NSNumber(value: detectionPreprocessMeta.originalWidth)
    }

    @objc public func lastDetectionOriginalHeight() -> NSNumber {
        NSNumber(value: detectionPreprocessMeta.originalHeight)
    }

    @objc public func lastDetectionScale() -> NSNumber {
        NSNumber(value: detectionPreprocessMeta.scale)
    }

    @objc public func lastDetectionPadLeft() -> NSNumber {
        NSNumber(value: detectionPreprocessMeta.padLeft)
    }

    @objc public func lastDetectionPadTop() -> NSNumber {
        NSNumber(value: detectionPreprocessMeta.padTop)
    }

    // MARK: - Private helpers

    private func runForward(module: Module, pixels: [Float],
                             shape: [Int64], tag: String) -> [NSNumber] {
        do {
            let inputTensor = Tensor(pixels, shape: shape.map(Int.init))
            let outputTensor = try Tensor<Float>(module.forward(inputTensor))
            let outputScalars = outputTensor.scalars()
            if tag == "detection" {
                detectionErrorMessage = nil
            } else {
                classificationErrorMessage = nil
            }
            return outputScalars.map { NSNumber(value: $0) }
        } catch {
            let message = "[ExecuTorchBridge] \(tag) inference failed for shape \(shape): \(error)"
            if tag == "detection" {
                detectionErrorMessage = message
            } else {
                classificationErrorMessage = message
            }
            print(message)
            return []
        }
    }

    /// Decodes JPEG/PNG bytes, resizes to (width × height), returns CHW float array.
    private func preprocessResizedCHW(_ data: Data, width: Int, height: Int,
                                      mean: (Float, Float, Float),
                                      std: (Float, Float, Float)) -> [Float]? {
        guard let uiImage = UIImage(data: data),
              let cgImage = uiImage.cgImage else { return nil }

        var rawBytes = [UInt8](repeating: 0, count: width * height * 4)
        guard let ctx = CGContext(
            data: &rawBytes,
            width: width, height: height,
            bitsPerComponent: 8,
            bytesPerRow: width * 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue
        ) else { return nil }
        ctx.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))
        return rgbaToCHW(rawBytes, width: width, height: height, mean: mean, std: std)
    }

    private func preprocessDetectionCHW(_ data: Data, width: Int, height: Int)
    -> ([Float], DetectionPreprocessMeta)? {
        guard let uiImage = UIImage(data: data),
              let cgImage = uiImage.cgImage else { return nil }

        let originalWidth = cgImage.width
        let originalHeight = cgImage.height
        let scale = min(Float(width) / Float(originalWidth), Float(height) / Float(originalHeight))
        let scaledWidth = Int((Float(originalWidth) * scale).rounded())
        let scaledHeight = Int((Float(originalHeight) * scale).rounded())
        let padLeft = Float(width - scaledWidth) / 2.0
        let padTop = Float(height - scaledHeight) / 2.0

        var rawBytes = [UInt8](repeating: 114, count: width * height * 4)
        for index in stride(from: 3, to: rawBytes.count, by: 4) {
            rawBytes[index] = 255
        }
        guard let ctx = CGContext(
            data: &rawBytes,
            width: width, height: height,
            bitsPerComponent: 8,
            bytesPerRow: width * 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue
        ) else { return nil }
        ctx.setFillColor(red: 114.0 / 255.0, green: 114.0 / 255.0, blue: 114.0 / 255.0, alpha: 1.0)
        ctx.fill(CGRect(x: 0, y: 0, width: width, height: height))
        ctx.interpolationQuality = .high
        ctx.draw(
            cgImage,
            in: CGRect(
                x: CGFloat(padLeft),
                y: CGFloat(padTop),
                width: scaledWidth,
                height: scaledHeight
            )
        )

        let meta = DetectionPreprocessMeta(
            originalWidth: originalWidth,
            originalHeight: originalHeight,
            scale: scale,
            padLeft: padLeft,
            padTop: padTop
        )
        let chw = rgbaToCHW(
            rawBytes,
            width: width,
            height: height,
            mean: (0.0, 0.0, 0.0),
            std: (1.0, 1.0, 1.0)
        )
        return (chw, meta)
    }

    private func rgbaToCHW(_ rawBytes: [UInt8], width: Int, height: Int,
                           mean: (Float, Float, Float),
                           std: (Float, Float, Float)) -> [Float] {
        let n = width * height
        var chw = [Float](repeating: 0, count: 3 * n)
        for i in 0..<n {
            chw[0 * n + i] = (Float(rawBytes[i * 4])     / 255.0 - mean.0) / std.0
            chw[1 * n + i] = (Float(rawBytes[i * 4 + 1]) / 255.0 - mean.1) / std.1
            chw[2 * n + i] = (Float(rawBytes[i * 4 + 2]) / 255.0 - mean.2) / std.2
        }
        return chw
    }
}
