import Foundation
import UIKit
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
            let module = try Module(filePath: path, loadMode: .mmap)
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
            let module = try Module(filePath: path, loadMode: .mmap)
            try module.load()
            classifierModule = module
            return true
        } catch {
            print("[ExecuTorchBridge] Classifier load failed: \(error)")
            return false
        }
    }

    // MARK: - Inference

    /// YOLO26: input 640×640, letterboxed with gray (114, 114, 114) background, pixel/255 normalization (mean=0, std=1).
    /// Output: raw float array flattened from [1, 27, N] with bounding boxes un-letterboxed to original aspect ratio.
    @objc public func runDetection(withImageData imageData: Data) -> [NSNumber] {
        guard let module = detectionModule,
              let prep = preprocessCHW(imageData, width: 640, height: 640,
                                        mean: (0.0, 0.0, 0.0),
                                        std: (1.0, 1.0, 1.0),
                                        isLetterbox: true) else { return [] }
        
        var rawOutput = runForward(module: module, data: prep.data,
                                  shape: [NSNumber(value: Int32(1)), NSNumber(value: Int32(3)), NSNumber(value: Int32(640)), NSNumber(value: Int32(640))],
                                  tag: "detection")
        if rawOutput.isEmpty { return [] }
        
        let numClasses = 23
        let rowStride = numClasses + 4
        let n = rawOutput.count / rowStride
        if n <= 0 { return rawOutput }
        
        let scale = prep.scale
        let padLeft = prep.padLeft
        let padTop = prep.padTop
        let origWidth = prep.origWidth
        let origHeight = prep.origHeight
        
        for i in 0..<n {
            let cx = rawOutput[0 * n + i].floatValue
            let cy = rawOutput[1 * n + i].floatValue
            let w  = rawOutput[2 * n + i].floatValue
            let h  = rawOutput[3 * n + i].floatValue
            
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
            
            rawOutput[0 * n + i] = NSNumber(value: newCx)
            rawOutput[1 * n + i] = NSNumber(value: newCy)
            rawOutput[2 * n + i] = NSNumber(value: newW)
            rawOutput[3 * n + i] = NSNumber(value: newH)
        }
        
        return rawOutput
    }

    /// EfficientNet-B2: input 260×260, ImageNet normalization.
    /// Output: logits for [Corn, Pepper, Tomato].
    @objc public func runClassification(withImageData imageData: Data) -> [NSNumber] {
        guard let module = classifierModule,
              let prep = preprocessCHW(imageData, width: 260, height: 260,
                                        mean: (0.485, 0.456, 0.406),
                                        std: (0.229, 0.224, 0.225),
                                        isLetterbox: false) else { return [] }
        return runForward(module: module, data: prep.data,
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

    /// Decodes JPEG/PNG bytes, resizes to (width × height) with optional letterboxing, returns CHW float array and preprocessing metadata.
    private func preprocessCHW(_ data: Data, width: Int, height: Int,
                                mean: (Float, Float, Float),
                                std: (Float, Float, Float),
                                isLetterbox: Bool = false) -> PreprocessResult? {
        guard let rawImage = UIImage(data: data) else { return nil }
        let uiImage = rawImage.fixOrientation()

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

