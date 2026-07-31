import SwiftUI
import ComposeApp

private struct IdentifiedDetection: Identifiable {
    let id: Int
    let result: DetectionResult
}

private struct BoxOverlayView: View {
    let item: IdentifiedDetection
    let palette: [Color]
    let cropIcons: [String: String]
    let ox: CGFloat
    let oy: CGFloat
    let displayedW: CGFloat
    let displayedH: CGFloat
    let modelWidth: CGFloat
    let modelHeight: CGFloat
    let canvasW: CGFloat

    var body: some View {
        let result = item.result
        let classIdx = Int(result.classIndex)
        let color = palette[classIdx % palette.count]
        
        let rawBox = result.box
        let x1 = ox + (CGFloat(rawBox.get(index: 0)) / modelWidth) * displayedW
        let y1 = oy + (CGFloat(rawBox.get(index: 1)) / modelHeight) * displayedH
        let x2 = ox + (CGFloat(rawBox.get(index: 2)) / modelWidth) * displayedW
        let y2 = oy + (CGFloat(rawBox.get(index: 3)) / modelHeight) * displayedH
        
        let boxW = max(0, x2 - x1)
        let boxH = max(0, y2 - y1)
        
        let className = result.className ?? "Unknown"
        let cropKey = className.components(separatedBy: " ").first ?? ""
        let icon = cropIcons[cropKey] ?? "🌿"
        let pct = Int(result.score * 100)
        let labelText = "\(icon) \(className) \(pct)%"
        
        return ZStack(alignment: .topLeading) {
            // Semi-transparent box fill
            Rectangle()
                .fill(color.opacity(0.08))
                .frame(width: boxW, height: boxH)
                .position(x: x1 + boxW / 2.0, y: y1 + boxH / 2.0)
            
            // Bounding box border
            Rectangle()
                .stroke(color.opacity(0.85), lineWidth: 2)
                .frame(width: boxW, height: boxH)
                .position(x: x1 + boxW / 2.0, y: y1 + boxH / 2.0)
            
            // Label Pill
            Text(labelText)
                .font(.system(size: 11, weight: .semibold, design: .rounded))
                .foregroundColor(.white)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(
                    Capsule()
                        .fill(color.opacity(0.95))
                )
                .overlay(
                    Capsule()
                        .stroke(Color.white.opacity(0.4), lineWidth: 1)
                )
                .position(x: min(max(x1 + 40, 50), canvasW - 50),
                          y: max(y1 - 12, 16))
        }
    }
}

public struct BoundingBoxCanvasView: View {
    public let image: UIImage
    public let results: [DetectionResult]
    public let modelWidth: CGFloat
    public let modelHeight: CGFloat
    
    public init(image: UIImage, results: [DetectionResult], modelWidth: CGFloat = 640, modelHeight: CGFloat = 640) {
        self.image = image
        self.results = results
        self.modelWidth = modelWidth
        self.modelHeight = modelHeight
    }
    
    private let palette: [Color] = [
        Color(red: 0.91, green: 0.30, blue: 0.24), Color(red: 0.75, green: 0.22, blue: 0.17),
        Color(red: 0.18, green: 0.80, blue: 0.44), Color(red: 0.15, green: 0.68, blue: 0.38),
        Color(red: 0.20, green: 0.60, blue: 0.86), Color(red: 0.16, green: 0.50, blue: 0.73),
        Color(red: 0.61, green: 0.35, blue: 0.71), Color(red: 0.56, green: 0.27, blue: 0.68),
        Color(red: 0.95, green: 0.61, blue: 0.07), Color(red: 0.90, green: 0.49, blue: 0.13),
        Color(red: 0.10, green: 0.74, blue: 0.61), Color(red: 0.09, green: 0.63, blue: 0.52),
        Color(red: 0.20, green: 0.29, blue: 0.37), Color(red: 0.17, green: 0.24, blue: 0.31),
        Color(red: 0.50, green: 0.55, blue: 0.55), Color(red: 0.58, green: 0.65, blue: 0.65),
        Color(red: 0.83, green: 0.33, blue: 0.00), Color(red: 0.75, green: 0.22, blue: 0.17),
        Color(red: 0.15, green: 0.68, blue: 0.38), Color(red: 0.16, green: 0.50, blue: 0.73),
        Color(red: 0.56, green: 0.27, blue: 0.68), Color(red: 0.95, green: 0.61, blue: 0.07),
        Color(red: 0.09, green: 0.63, blue: 0.52)
    ]
    
    private let cropIcons: [String: String] = [
        "Corn": "🌽",
        "Pepper": "🫑",
        "Tomato": "🍅"
    ]
    
    public var body: some View {
        GeometryReader { geometry in
            let canvasW = geometry.size.width
            let canvasH = geometry.size.height
            let imgW = image.size.width
            let imgH = image.size.height
            
            if imgW > 0 && imgH > 0 {
                let scale = max(canvasW / imgW, canvasH / imgH)
                let displayedW = imgW * scale
                let displayedH = imgH * scale
                let ox = (canvasW - displayedW) / 2.0
                let oy = (canvasH - displayedH) / 2.0
                
                let items = results.enumerated().map { IdentifiedDetection(id: $0.offset, result: $0.element) }
                
                ZStack(alignment: .topLeading) {
                    Image(uiImage: image)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: canvasW, height: canvasH)
                        .clipped()
                    
                    ForEach(items) { item in
                        BoxOverlayView(
                            item: item,
                            palette: palette,
                            cropIcons: cropIcons,
                            ox: ox,
                            oy: oy,
                            displayedW: displayedW,
                            displayedH: displayedH,
                            modelWidth: modelWidth,
                            modelHeight: modelHeight,
                            canvasW: canvasW
                        )
                    }
                }
            }
        }
    }
}

