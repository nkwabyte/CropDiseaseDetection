import SwiftUI
import PhotosUI
import ComposeApp

public struct HomeView: View {
    @StateObject private var appStateObs = ObservableFlow(KoinHelper.appViewModel.appState)
    @StateObject private var detectionStateObs = ObservableFlow(KoinHelper.detectionViewModel.detectionState)
    
    @State private var selectedPhotoItem: PhotosPickerItem? = nil
    @State private var selectedUIImage: UIImage? = nil
    @State private var selectedByteArray: [UInt8]? = nil
    @State private var isCameraPresented: Bool = false
    @State private var isResultPresented: Bool = false
    @State private var isCropPickerPresented: Bool = false
    @State private var isCropAlertPresented: Bool = false
    @StateObject private var langMgr = LanguageManager.shared
    
    // The first element is the CANONICAL crop id (SupportedCrop.canonicalLabel),
    // not display text: it is what the detection pipeline routes on, and LText()
    // translates it for display. Keeping the two apart is what stops routing from
    // depending on the interface language.
    private let crops = [
        ("Corn", "🌽", "Maize Crops"),
        ("Pepper", "🫑", "Bell Peppers"),
        ("Tomato", "🍅", "Tomato Leaves")
    ]
    
    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Header Banner
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            LText("Crop Disease Detector")
                                .font(.title2)
                                .fontWeight(.bold)
                            LText("AI-powered diagnostic assistant")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        Image(systemName: "leaf.fill")
                            .font(.system(size: 32))
                            .foregroundColor(.green)
                    }
                    .padding()
                    .background(Color(uiColor: .secondarySystemBackground))
                    .cornerRadius(16)
                    
                    // Selected Crop Card
                    VStack(alignment: .leading, spacing: 12) {
                        LText("Select Target Crop")
                            .font(.headline)
                        
                        HStack(spacing: 12) {
                            ForEach(crops, id: \.0) { cropName, icon, subtitle in
                                let isSelected = (appStateObs.value.selectedCrop == cropName)
                                Button {
                                    KoinHelper.appViewModel.setSelectedCrop(crop: cropName, cropId: cropName)
                                } label: {
                                    VStack(spacing: 6) {
                                        Text(icon)
                                            .font(.system(size: 28))
                                        LText(cropName)
                                            .font(.subheadline)
                                            .fontWeight(.semibold)
                                            .foregroundColor(isSelected ? .white : .primary)
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 14)
                                    .background(
                                        RoundedRectangle(cornerRadius: 12)
                                            .fill(isSelected ? Color.green : Color(uiColor: .secondarySystemBackground))
                                    )
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12)
                                            .stroke(isSelected ? Color.green : Color.clear, lineWidth: 2)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Image Preview / Drop Area
                    VStack(spacing: 16) {
                        if let image = selectedUIImage {
                            ZStack(alignment: .topTrailing) {
                                Image(uiImage: image)
                                    .resizable()
                                    .aspectRatio(contentMode: .fit)
                                    .frame(maxHeight: 280)
                                    .cornerRadius(16)
                                    .shadow(radius: 4)
                                
                                Button {
                                    selectedUIImage = nil
                                    selectedByteArray = nil
                                    KoinHelper.detectionViewModel.reset()
                                } label: {
                                    Image(systemName: "xmark.circle.fill")
                                        .font(.title)
                                        .foregroundColor(.white)
                                        .shadow(radius: 2)
                                }
                                .padding(8)
                            }
                        } else {
                            VStack(spacing: 12) {
                                Image(systemName: "camera.macro")
                                    .font(.system(size: 48))
                                    .foregroundColor(.green)
                                LText("No Crop Photo Selected")
                                    .font(.headline)
                                LText("Take a photo or upload an image of an affected leaf")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .multilineTextAlignment(.center)
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 200)
                            .background(
                                RoundedRectangle(cornerRadius: 16)
                                    .stroke(style: StrokeStyle(lineWidth: 2, dash: [6]))
                                    .foregroundColor(Color.secondary.opacity(0.4))
                            )
                        }
                        
                        // Image Source Action Buttons
                        HStack(spacing: 16) {
                            PhotosPicker(selection: $selectedPhotoItem, matching: .images) {
                                Label(L("Gallery"), systemImage: "photo.on.rectangle")
                                    .font(.headline)
                                    .frame(maxWidth: .infinity)
                                    .padding()
                                    .background(Color.blue.opacity(0.15))
                                    .foregroundColor(.blue)
                                    .cornerRadius(12)
                            }
                            
                            Button {
                                isCameraPresented = true
                            } label: {
                                Label(L("Camera"), systemImage: "camera.fill")
                                    .font(.headline)
                                    .frame(maxWidth: .infinity)
                                    .padding()
                                    .background(Color.green.opacity(0.15))
                                    .foregroundColor(.green)
                                    .cornerRadius(12)
                            }
                        }
                    }
                    
                    // Run Detection Action Button
                    if selectedUIImage != nil {
                        Button {
                            let selectedCrop = appStateObs.value.selectedCrop
                            if selectedCrop == nil || selectedCrop?.isEmpty == true {
                                isCropAlertPresented = true
                                return
                            }
                            if let bytes = selectedByteArray, let crop = selectedCrop {
                                let ktByteArray = KotlinByteArray(size: Int32(bytes.count))
                                for (idx, byte) in bytes.enumerated() {
                                    ktByteArray.set(index: Int32(idx), value: Int8(bitPattern: byte))
                                }
                                // The canonical id, never the displayed name. The
                                // image dimensions are no longer passed in: the
                                // pipeline reports the authoritative post-orientation
                                // size and the box coordinate space itself.
                                let cropId = appStateObs.value.selectedCropId ?? crop
                                KoinHelper.detectionViewModel.detect(imageBytes: ktByteArray, selectedCropId: cropId)
                                isResultPresented = true
                            }
                        } label: {
                            HStack {
                                if detectionStateObs.value.isDetecting {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                        .padding(.trailing, 8)
                                    LText("Analyzing Crop Health...")
                                } else {
                                    Image(systemName: "waveform.path.ecg")
                                    LText("Run Disease Diagnostics")
                                }
                            }
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.green)
                            .cornerRadius(14)
                            .shadow(radius: 2)
                        }
                        .disabled(detectionStateObs.value.isDetecting)
                    }
                }
                .padding()
            }
            .navigationTitle(L("Diagnostics"))
            .onChange(of: selectedPhotoItem) { newItem in
                Task {
                    if let data = try? await newItem?.loadTransferable(type: Data.self),
                       let uiImg = UIImage(data: data) {
                        await MainActor.run {
                            self.selectedUIImage = uiImg
                            self.selectedByteArray = Array(data)
                        }
                    }
                }
            }
            .navigationDestination(isPresented: $isResultPresented) {
                if let uiImg = selectedUIImage, let bytes = selectedByteArray {
                    DetectionResultView(
                        image: uiImg,
                        imageBytes: bytes,
                        onDone: {
                            clearHomeScreen()
                        }
                    )
                }
            }
            .onChange(of: isResultPresented) { isPresented in
                if !isPresented {
                    clearHomeScreen()
                }
            }
            .sheet(isPresented: $isCameraPresented) {
                CameraPickerView { image, data in
                    self.selectedUIImage = image
                    self.selectedByteArray = data
                }
            }
            .alert(L("Select Target Crop"), isPresented: $isCropAlertPresented) {
                Button(L("OK"), role: .cancel) { }
            } message: {
                LText("Please select a target crop type (Corn, Pepper, or Tomato) before running disease diagnostics.")
            }
        }
    }
    
    private func clearHomeScreen() {
        selectedPhotoItem = nil
        selectedUIImage = nil
        selectedByteArray = nil
        isResultPresented = false
        KoinHelper.detectionViewModel.reset()
        KoinHelper.appViewModel.reset()
    }
}

struct CameraPickerView: UIViewControllerRepresentable {
    let onImagePicked: (UIImage, [UInt8]) -> Void
    @Environment(\.dismiss) private var dismiss
    
    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = UIImagePickerController.isSourceTypeAvailable(.camera) ? .camera : .photoLibrary
        picker.delegate = context.coordinator
        return picker
    }
    
    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: CameraPickerView
        init(_ parent: CameraPickerView) { self.parent = parent }
        
        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
            if let image = info[.originalImage] as? UIImage,
               let data = image.jpegData(compressionQuality: 0.8) {
                parent.onImagePicked(image, Array(data))
            }
            parent.dismiss()
        }
        
        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.dismiss()
        }
    }
}
