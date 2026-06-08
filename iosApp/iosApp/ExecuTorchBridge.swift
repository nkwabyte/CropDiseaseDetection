import Foundation
import UIKit

// REQUIRES: ExecuTorch Swift Package added to this Xcode target.
// Xcode → File → Add Package Dependencies
// URL: https://github.com/pytorch/executorch  tag: 0.6.0
// Product to add: executorch (or ExecuTorch, depending on release)
import ExecuTorch

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
            detectionModule = try Module(filePath: path)
            return true
        } catch {
            print("[ExecuTorchBridge] Detection load failed: \(error)")
            return false
        }
    }

    @objc public func loadClassifierModel(atPath path: String) -> Bool {
        do {
            classifierModule = try Module(filePath: path)
            return true
        } catch {
            print("[ExecuTorchBridge] Classifier load failed: \(error)")
            return false
        }
    }

    // MARK: - Inference

    /// YOLO26: input 640×640, pixel/255 normalization (mean=0, std=1).
    /// Output: raw float array flattened from [1, 27, N].
    @objc public func runDetection(withImageData imageData: Data) -> [NSNumber] {
        guard let module = detectionModule,
              let pixels = preprocessCHW(imageData, width: 640, height: 640,
                                         mean: (0.0, 0.0, 0.0),
                                         std: (1.0, 1.0, 1.0)) else { return [] }
        return runForward(module: module, pixels: pixels,
                          shape: [1, 3, 640, 640], tag: "detection")
    }

    /// EfficientNet-B2: input 260×260, ImageNet normalization.
    /// Output: logits for [Corn, Pepper, Tomato].
    @objc public func runClassification(withImageData imageData: Data) -> [NSNumber] {
        guard let module = classifierModule,
              let pixels = preprocessCHW(imageData, width: 260, height: 260,
                                         mean: (0.485, 0.456, 0.406),
                                         std: (0.229, 0.224, 0.225)) else { return [] }
        return runForward(module: module, pixels: pixels,
                          shape: [1, 3, 260, 260], tag: "classification")
    }

    @objc public func releaseModels() {
        detectionModule = nil
        classifierModule = nil
    }

    // MARK: - Private helpers

    private func runForward(module: Module, pixels: [Float],
                             shape: [Int64], tag: String) -> [NSNumber] {
        do {
            let inputTensor = Tensor(shape: shape, data: pixels)
            let outputs = try module.forward([Value(tensor: inputTensor)])
            guard let outTensor = outputs.first?.toTensor() else { return [] }
            return outTensor.floats.map { NSNumber(value: $0) }
        } catch {
            print("[ExecuTorchBridge] \(tag) inference failed: \(error)")
            return []
        }
    }

    /// Decodes JPEG/PNG bytes, resizes to (width × height), returns CHW float array.
    private func preprocessCHW(_ data: Data, width: Int, height: Int,
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
