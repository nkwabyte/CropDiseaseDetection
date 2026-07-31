import SwiftUI
import ComposeApp

private struct IdentifiedDetection: Identifiable {
    let id: Int
    let result: DetectionResult
}

public struct DetectionResultView: View {
    public let image: UIImage
    public let imageBytes: [UInt8]
    
    @StateObject private var detectionStateObs = ObservableFlow(KoinHelper.detectionViewModel.detectionState)
    @Environment(\.dismiss) private var dismiss
    @State private var isRecommendationPresented: Bool = false
    @State private var isFlagSheetPresented: Bool = false
    @State private var flagNotes: String = ""
    
    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    let state = detectionStateObs.value
                    
                    // Interactive Bounding Box Canvas
                    BoundingBoxCanvasView(
                        image: image,
                        results: state.results as? [DetectionResult] ?? [],
                        modelWidth: 640,
                        modelHeight: 640
                    )
                    .frame(height: 320)
                    .cornerRadius(16)
                    .shadow(radius: 4)
                    
                    // Diagnostic Status Banner
                    if state.isClassifierRejected {
                        VStack(spacing: 8) {
                            HStack {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .foregroundColor(.orange)
                                Text("Unrecognized Leaf Type")
                                    .font(.headline)
                            }
                            Text("The classifier confidence (\(Int(state.classifierConfidence * 100))%) suggests this is not a supported Corn, Pepper, or Tomato crop.")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        .padding()
                        .background(Color.orange.opacity(0.12))
                        .cornerRadius(12)
                    } else if state.isCropMissMatch {
                        VStack(spacing: 8) {
                            HStack {
                                Image(systemName: "xmark.octagon.fill")
                                    .foregroundColor(.red)
                                Text("Crop Mismatch Detected")
                                    .font(.headline)
                            }
                            Text("Detections found do not match your selected crop category.")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        .padding()
                        .background(Color.red.opacity(0.12))
                        .cornerRadius(12)
                    } else if state.isDetectionSuccessful {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Image(systemName: "checkmark.seal.fill")
                                    .foregroundColor(.green)
                                Text("Detection Summary")
                                    .font(.headline)
                                Spacer()
                                Text("\(state.results.count) Found")
                                    .font(.subheadline)
                                    .fontWeight(.bold)
                                    .foregroundColor(.green)
                            }
                            
                            Divider()
                            
                            let rawList = state.results as? [DetectionResult] ?? []
                            let items = rawList.enumerated().map { IdentifiedDetection(id: $0.offset, result: $0.element) }
                            ForEach(items) { item in
                                let det = item.result
                                HStack {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(det.className ?? "Unknown")
                                            .font(.body)
                                            .fontWeight(.semibold)
                                        Text("Confidence: \(Int(det.score * 100))%")
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                    }
                                    Spacer()
                                    ProgressView(value: Double(det.score))
                                        .frame(width: 80)
                                        .accentColor(det.score > 0.6 ? .green : .orange)
                                }
                            }
                        }
                        .padding()
                        .background(Color(uiColor: .secondarySystemBackground))
                        .cornerRadius(14)
                    } else {
                        VStack(spacing: 8) {
                            Image(systemName: "leaf.circle")
                                .font(.system(size: 40))
                                .foregroundColor(.green)
                            Text("No Disease Detected")
                                .font(.headline)
                            Text("Your crop leaf appears healthy based on current ML thresholds.")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        .padding()
                        .background(Color.green.opacity(0.12))
                        .cornerRadius(12)
                    }
                    
                    // Action Buttons
                    VStack(spacing: 12) {
                        if state.isDetectionSuccessful {
                            Button {
                                isRecommendationPresented = true
                            } label: {
                                Label("View Treatment Guidelines", systemImage: "book.pages.fill")
                                    .font(.headline)
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .padding()
                                    .background(Color.green)
                                    .cornerRadius(14)
                            }
                        }
                        
                        Button {
                            isFlagSheetPresented = true
                        } label: {
                            Label("Report Inaccurate Detection", systemImage: "flag.fill")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("Diagnostic Results")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
            .sheet(isPresented: $isRecommendationPresented) {
                RecommendationView(results: detectionStateObs.value.results as? [DetectionResult] ?? [])
            }
            .sheet(isPresented: $isFlagSheetPresented) {
                VStack(spacing: 16) {
                    Text("Flag Detection Record")
                        .font(.headline)
                    Text("Help improve AI accuracy by submitting details on misclassifications.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    TextEditor(text: $flagNotes)
                        .frame(height: 120)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.secondary.opacity(0.3)))
                    
                    HStack {
                        Button("Cancel") { isFlagSheetPresented = false }
                            .foregroundColor(.secondary)
                        Spacer()
                        Button("Submit Flag") {
                            let settings = KoinHelper.settingsManager
                            let ktByteArray = KotlinByteArray(size: Int32(imageBytes.count))
                            for (idx, b) in imageBytes.enumerated() {
                                ktByteArray.set(index: Int32(idx), value: Int8(bitPattern: b))
                            }
                            KoinHelper.detectionViewModel.flagDetection(
                                imageBytes: ktByteArray,
                                cropName: KoinHelper.appViewModel.appState.value.selectedCrop ?? "Crop",
                                userRole: "User",
                                notes: flagNotes,
                                detectionThreshold: settings.getDetectionThreshold(),
                                iouThreshold: settings.getIouThreshold(),
                                classifierThreshold: settings.getClassifierThreshold()
                            )
                            isFlagSheetPresented = false
                        }
                        .fontWeight(.bold)
                        .foregroundColor(.green)
                    }
                }
                .padding()
                .presentationDetents([.medium])
            }
        }
    }
}
