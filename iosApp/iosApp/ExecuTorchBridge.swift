import Foundation
import UIKit
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
            let module = try Module(filePath: path)
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
            let module = try Module(filePath: path)
            try module.load()
            classifierModule = module
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
              let data = preprocessCHW(imageData, width: 640, height: 640,
                                         mean: (0.0, 0.0, 0.0),
                                         std: (1.0, 1.0, 1.0)) else { return [] }
        return runForward(module: module, data: data,
                          shape: [NSNumber(value: Int32(1)), NSNumber(value: Int32(3)), NSNumber(value: Int32(640)), NSNumber(value: Int32(640))],
                          tag: "detection")
    }

    /// EfficientNet-B2: input 260×260, ImageNet normalization.
    /// Output: logits for [Corn, Pepper, Tomato].
    @objc public func runClassification(withImageData imageData: Data) -> [NSNumber] {
        guard let module = classifierModule,
              let data = preprocessCHW(imageData, width: 260, height: 260,
                                         mean: (0.485, 0.456, 0.406),
                                         std: (0.229, 0.224, 0.225)) else { return [] }
        return runForward(module: module, data: data,
                          shape: [NSNumber(value: Int32(1)), NSNumber(value: Int32(3)), NSNumber(value: Int32(260)), NSNumber(value: Int32(260))],
                          tag: "classification")
    }

    @objc public func releaseModels() {
        detectionModule = nil
        classifierModule = nil
    }

    // MARK: - Private helpers

    private func runForward(module: Module, data: Data,
                             shape: [NSNumber], tag: String) -> [NSNumber] {
        do {
            let inputTensor = Tensor(data: data, shape: shape, dataType: .float)
            let outputs = try module.forward(inputTensor)
            guard let outTensor = outputs.first?.tensor else { return [] }
            var result: [NSNumber] = []
            outTensor.bytes { pointer, count, _ in
                let floats = pointer.assumingMemoryBound(to: Float.self)
                result = (0..<count).map { NSNumber(value: floats[$0]) }
            }
            return result
        } catch {
            print("[ExecuTorchBridge] \(tag) inference failed: \(error)")
            return []
        }
    }

    /// Decodes JPEG/PNG bytes, resizes to (width × height), returns CHW float array.
    private func preprocessCHW(_ data: Data, width: Int, height: Int,
                                mean: (Float, Float, Float),
                                std: (Float, Float, Float)) -> Data? {
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
        return alignedData
    }
}
