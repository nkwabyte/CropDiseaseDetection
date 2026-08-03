import SwiftUI
import ComposeApp

private struct IdentifiedDetection: Identifiable {
    let id: Int
    let result: DetectionResult
}

public struct DetectionResultView: View {
    public let image: UIImage
    public let imageBytes: [UInt8]
    public var onDone: (() -> Void)? = nil
    
    @StateObject private var detectionStateObs = ObservableFlow(KoinHelper.detectionViewModel.detectionState)
    @Environment(\.dismiss) private var dismiss
    @State private var isRecommendationPresented: Bool = false
    @State private var isFlagSheetPresented: Bool = false
    @State private var flagNotes: String = ""
    @StateObject private var langMgr = LanguageManager.shared
    
    public var body: some View {
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
                                LText("Unrecognized Leaf Type")
                                    .font(.headline)
                            }
                            LText("The classifier confidence (%@) suggests this is not a supported Corn, Pepper, or Tomato crop.", "\(Int(state.classifierConfidence * 100))%")
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
                                LText("Crop Mismatch Detected")
                                    .font(.headline)
                            }
                            LText("Detections found do not match your selected crop category.")
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
                                LText("Detection Summary")
                                    .font(.headline)
                                Spacer()
                                LText("%@ Found", "\(state.results.count)")
                                    .font(.subheadline)
                                    .fontWeight(.bold)
                                    .foregroundColor(.green)
                            }
                            
                            Divider()
                            
                            let rawList = state.results as? [DetectionResult] ?? []
                            let distinctList: [DetectionResult] = {
                                var dict = [String: DetectionResult]()
                                for det in rawList {
                                    let key = det.className ?? "\(det.classIndex)"
                                    if let existing = dict[key] {
                                        if det.score > existing.score {
                                            dict[key] = det
                                        }
                                    } else {
                                        dict[key] = det
                                    }
                                }
                                return dict.values.sorted(by: { $0.score > $1.score })
                            }()
                            let items = distinctList.enumerated().map { IdentifiedDetection(id: $0.offset, result: $0.element) }
                            ForEach(items) { item in
                                let det = item.result
                                NavigationLink {
                                    let info = DiseaseDatabase.shared.getDiseaseInfo(diseaseName: det.className ?? L("Unknown"))
                                    DiseaseDetailView(disease: info)
                                } label: {
                                    HStack {
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(det.className ?? L("Unknown"))
                                                .font(.body)
                                                .fontWeight(.semibold)
                                                .foregroundColor(.primary)
                                            LText("Confidence: %@", "\(Int(det.score * 100))%")
                                                .font(.caption)
                                                .foregroundColor(.secondary)
                                        }
                                        Spacer()
                                        ProgressView(value: Double(det.score))
                                            .frame(width: 80)
                                            .accentColor(det.score > 0.6 ? .green : .orange)
                                        Image(systemName: "chevron.right")
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                    }
                                    .contentShape(Rectangle())
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
                            LText("No Disease Detected")
                                .font(.headline)
                            LText("Your crop leaf appears healthy based on current ML thresholds.")
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
                            NavigationLink {
                                RecommendationView(results: detectionStateObs.value.results as? [DetectionResult] ?? [])
                            } label: {
                                Label(L("View Treatment Guidelines"), systemImage: "book.pages.fill")
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
                            Label(L("Report Inaccurate Detection"), systemImage: "flag.fill")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding()
            }
            .navigationTitle(L("Diagnostic Results"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(L("Done")) {
                        onDone?()
                        dismiss()
                    }
                }
            }
            .sheet(isPresented: $isFlagSheetPresented) {
                VStack(spacing: 16) {
                    LText("Flag Detection Record")
                        .font(.headline)
                    LText("Help improve AI accuracy by submitting details on misclassifications.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    TextEditor(text: $flagNotes)
                        .frame(height: 120)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.secondary.opacity(0.3)))
                    
                    HStack {
                        Button(L("Cancel")) { isFlagSheetPresented = false }
                            .foregroundColor(.secondary)
                        Spacer()
                        Button(L("Submit Flag")) {
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
