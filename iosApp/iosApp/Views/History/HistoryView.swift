import SwiftUI
import ComposeApp

public struct HistoryView: View {
    @StateObject private var stateObs = ObservableFlow(KoinHelper.historyViewModel.historyState)
    @State private var pendingDeletion: DetectionRecord? = nil
    @StateObject private var langMgr = LanguageManager.shared

    /// Derived so the record being confirmed and the alert's visibility cannot drift apart.
    private var isDeleteAlertPresented: Binding<Bool> {
        Binding(get: { pendingDeletion != nil },
                set: { if !$0 { pendingDeletion = nil } })
    }

    public init() {}

    public var body: some View {
        NavigationStack {
            Group {
                if stateObs.value.isLoading {
                    VStack(spacing: 12) {
                        ProgressView()
                            .scaleEffect(1.2)
                        LText("Loading Detection History...")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if stateObs.value.isGuest {
                    GuestHistoryLockView()
                } else if stateObs.value.records.isEmpty {
                    EmptyHistoryView()
                } else {
                    // A List rather than a LazyVStack so rows get the standard swipe-to-delete
                    // gesture; the row chrome is stripped back so the cards still read as cards.
                    List {
                        if stateObs.value.isRefreshing {
                            ProgressView()
                                .frame(maxWidth: .infinity)
                                .listRowSeparator(.hidden)
                                .listRowBackground(Color.clear)
                        }

                        ForEach(stateObs.value.records, id: \.id) { record in
                            NavigationLink(destination: HistoryDetailView(record: record, onDelete: {
                                pendingDeletion = record
                            })) {
                                HistoryRecordRow(record: record)
                            }
                            .buttonStyle(.plain)
                            .listRowSeparator(.hidden)
                            .listRowBackground(Color.clear)
                            .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
                            .swipeActions(edge: .trailing) {
                                Button(role: .destructive) {
                                    pendingDeletion = record
                                } label: {
                                    Label(L("Delete"), systemImage: "trash")
                                }
                            }
                        }
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                    .refreshable {
                        KoinHelper.historyViewModel.refresh()
                    }
                }
            }
            .navigationTitle(L("Scan History"))
            .onAppear {
                KoinHelper.historyViewModel.loadHistory()
            }
            // Deleting only hides the scan; the record is retained, so the wording promises
            // exactly that and nothing stronger.
            .alert(L("Delete Scan"), isPresented: isDeleteAlertPresented) {
                Button(L("Delete"), role: .destructive) {
                    if let record = pendingDeletion {
                        KoinHelper.historyViewModel.deleteRecord(record: record)
                    }
                    pendingDeletion = nil
                }
                Button(L("Cancel"), role: .cancel) { pendingDeletion = nil }
            } message: {
                LText("Remove this scan from your history? It will no longer appear on this page.")
            }
        }
    }
}

extension DetectionRecord: @retroactive Identifiable {
    /// Includes the image because a queued scan and its uploaded twin can otherwise
    /// collide, and duplicate ids break ForEach.
    public var id: String {
        docId ?? "\(timestamp)_\(cropName)_\(imageUrl.hashValue)"
    }
}

private struct GuestHistoryLockView: View {
    @State private var showAuthSheet = false

    var body: some View {
        VStack(spacing: 20) {
            ZStack {
                Circle()
                    .fill(Color.green.opacity(0.12))
                    .frame(width: 80, height: 80)
                Image(systemName: "lock.fill")
                    .font(.system(size: 36))
                    .foregroundColor(.green)
            }

            LText("History is Locked")
                .font(.title2)
                .fontWeight(.bold)

            VStack(spacing: 12) {
                LText("Scans made as a guest are not synced to the cloud. Sign in or register to keep an archive of your diagnostics, sync data, and manage your crops.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)

                Button {
                    showAuthSheet = true
                } label: {
                    LText("Sign In to Unlock History")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.green)
                        .cornerRadius(14)
                }
                .padding(.top, 8)
            }
            .padding()
            .background(Color(uiColor: .secondarySystemBackground))
            .cornerRadius(20)
            .padding(.horizontal)
        }
        .padding()
        .sheet(isPresented: $showAuthSheet) {
            ProfileView()
        }
    }
}

private struct EmptyHistoryView: View {
    var body: some View {
        VStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(Color.green.opacity(0.12))
                    .frame(width: 80, height: 80)
                Image(systemName: "leaf.fill")
                    .font(.system(size: 36))
                    .foregroundColor(.green)
            }

            LText("No Scan History")
                .font(.title2)
                .fontWeight(.bold)

            LText("You haven't scanned any crops for diseases yet. Use the disease detector on a leaf to add diagnosed records to your cloud history archive.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
        }
        .padding()
    }
}

private struct HistoryRecordRow: View {
    let record: DetectionRecord
    @StateObject private var langMgr = LanguageManager.shared

    private var primaryResult: DetectionResult? {
        record.matchingResults.max(by: { $0.score < $1.score })
    }

    private var diseaseLabel: String {
        primaryResult.map { $0.displayName.isEmpty ? L("No Detection") : $0.displayName } ?? L("No Detection")
    }

    private var confidenceText: String {
        if let score = primaryResult?.score {
            return "\(Int(score * 100))%"
        }
        return "0%"
    }

    private var isHealthy: Bool {
        diseaseLabel.lowercased().contains("healthy")
    }

    private var cropEmoji: String {
        switch record.cropName.lowercased() {
        case "corn": return "🌽"
        case "tomato": return "🍅"
        case "pepper": return "🫑"
        default: return "🌱"
        }
    }

    private var formattedDate: String {
        let date = Date(timeIntervalSince1970: Double(record.timestamp) / 1000.0)
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }

    var body: some View {
        HStack(spacing: 14) {
            // Leaf Thumbnail Image
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color(uiColor: .tertiarySystemBackground))
                    .frame(width: 72, height: 72)

                if record.imageUrl.hasPrefix("data:image"),
                   let commaIndex = record.imageUrl.firstIndex(of: ","),
                   let data = Data(base64Encoded: String(record.imageUrl[record.imageUrl.index(after: commaIndex)...])),
                   let uiImage = UIImage(data: data) {
                    Image(uiImage: uiImage)
                        .resizable()
                        .scaledToFill()
                        .frame(width: 72, height: 72)
                        .cornerRadius(12)
                } else if let url = URL(string: record.imageUrl) {
                    AsyncImage(url: url) { image in
                        image
                            .resizable()
                            .scaledToFill()
                    } placeholder: {
                        ProgressView()
                    }
                    .frame(width: 72, height: 72)
                    .cornerRadius(12)
                } else {
                    Image(systemName: "photo.fill")
                        .foregroundColor(.secondary)
                }
            }

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text("\(cropEmoji) \(L(record.cropName.capitalized).uppercased())")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.green)

                    Spacer()

                    Text(formattedDate)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }

                Text(diseaseLabel)
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)
                    .lineLimit(1)

                HStack {
                    LText(isHealthy ? "Healthy" : "Diseased")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(isHealthy ? .green : .red)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background((isHealthy ? Color.green : Color.red).opacity(0.12))
                        .cornerRadius(6)

                    Spacer()

                    LText("Confidence: %@", confidenceText)
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(12)
        .background(Color(uiColor: .secondarySystemBackground))
        .cornerRadius(16)
    }
}

private struct DistinctDiseaseItem: Identifiable {
    let id: String
    let diseaseName: String
    let count: Int
    let maxScore: Float
    let sampleResult: DetectionResult
}

private struct RecordImageView: View {
    let imageUrl: String
    let visibleBoxes: [DetectionResult]
    @State private var loadedImage: UIImage? = nil

    var body: some View {
        ZStack {
            if let image = loadedImage {
                BoundingBoxCanvasView(
                    image: image,
                    results: visibleBoxes,
                    modelWidth: 640,
                    modelHeight: 640
                )
            } else {
                ZStack {
                    RoundedRectangle(cornerRadius: 16)
                        .fill(Color(uiColor: .secondarySystemBackground))
                    ProgressView()
                }
            }
        }
        .frame(height: 320)
        .cornerRadius(16)
        .shadow(radius: 4)
        .onAppear {
            loadImage()
        }
        .onChange(of: imageUrl) { _ in
            loadImage()
        }
    }

    private func loadImage() {
        if imageUrl.hasPrefix("data:image"),
           let commaIndex = imageUrl.firstIndex(of: ","),
           let data = Data(base64Encoded: String(imageUrl[imageUrl.index(after: commaIndex)...])),
           let uiImg = UIImage(data: data) {
            self.loadedImage = uiImg
            return
        }
        if let url = URL(string: imageUrl) {
            URLSession.shared.dataTask(with: url) { data, _, _ in
                if let data = data, let uiImg = UIImage(data: data) {
                    DispatchQueue.main.async {
                        self.loadedImage = uiImg
                    }
                }
            }.resume()
        }
    }
}

public struct HistoryDetailView: View {
    @Environment(\.dismiss) private var dismiss
    let record: DetectionRecord
    var onDelete: (() -> Void)? = nil

    @StateObject private var langMgr = LanguageManager.shared
    @State private var boxConfidence: Float = 0.10
    @State private var boxFloor: Float = 0.10
    @State private var selectedDiseaseFilter: String? = nil

    private var allResults: [DetectionResult] {
        (record.matchingResults as? [DetectionResult]) ?? []
    }

    private var distinctDiseaseNames: [String] {
        var names = [String]()
        for det in allResults {
            let name = det.displayName.isEmpty ? L("Unknown Disease") : det.displayName
            if !names.contains(name) {
                names.append(name)
            }
        }
        return names
    }

    private var visibleBoxes: [DetectionResult] {
        allResults.filter { det in
            let scorePass = det.score >= boxConfidence
            if let filter = selectedDiseaseFilter {
                let name = det.displayName.isEmpty ? L("Unknown Disease") : det.displayName
                return scorePass && name == filter
            }
            return scorePass
        }
    }

    private var distinctDiseases: [DistinctDiseaseItem] {
        var dict = [String: (count: Int, maxScore: Float, sample: DetectionResult)]()
        for det in allResults {
            let key = det.displayName.isEmpty ? L("Unknown Disease") : det.displayName
            if let existing = dict[key] {
                let newMax = max(existing.maxScore, det.score)
                let newSample = det.score > existing.maxScore ? det : existing.sample
                dict[key] = (count: existing.count + 1, maxScore: newMax, sample: newSample)
            } else {
                dict[key] = (count: 1, maxScore: det.score, sample: det)
            }
        }
        return dict.map { (key, val) in
            DistinctDiseaseItem(
                id: key,
                diseaseName: key,
                count: val.count,
                maxScore: val.maxScore,
                sampleResult: val.sample
            )
        }.sorted(by: { $0.count > $1.count })
    }

    private var isSupportedCrop: Bool {
        if record.isCropMismatch { return false }
        let c = record.cropName.lowercased()
        if c == "other" || c.isEmpty { return false }
        return c.contains("corn") || c.contains("tomato") || c.contains("pepper")
    }

    private var formattedDate: String {
        let date = Date(timeIntervalSince1970: Double(record.timestamp) / 1000.0)
        let formatter = DateFormatter()
        formatter.dateStyle = .long
        formatter.timeStyle = .medium
        return formatter.string(from: date)
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Interactive Bounding Box Canvas on Image
                RecordImageView(imageUrl: record.imageUrl, visibleBoxes: visibleBoxes)

                // Box Confidence Threshold & Disease Filter Row
                if !allResults.isEmpty {
                    VStack(spacing: 8) {
                        // Disease Filter Chips Row
                        if distinctDiseaseNames.count > 1 {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    Button {
                                        selectedDiseaseFilter = nil
                                    } label: {
                                        HStack(spacing: 4) {
                                            Image(systemName: "line.3.horizontal.decrease.circle.fill")
                                            LText("All (%@)", "\(allResults.count)")
                                        }
                                        .font(.caption)
                                        .fontWeight(.bold)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(selectedDiseaseFilter == nil ? Color.green : Color(uiColor: .tertiarySystemFill))
                                        .foregroundColor(selectedDiseaseFilter == nil ? .white : .primary)
                                        .cornerRadius(16)
                                    }

                                    ForEach(distinctDiseaseNames, id: \.self) { diseaseName in
                                        let count = allResults.filter { ($0.displayName.isEmpty ? L("Unknown Disease") : $0.displayName) == diseaseName }.count
                                        let isSelected = selectedDiseaseFilter == diseaseName
                                        
                                        Button {
                                            if isSelected {
                                                selectedDiseaseFilter = nil
                                            } else {
                                                selectedDiseaseFilter = diseaseName
                                            }
                                        } label: {
                                            HStack(spacing: 4) {
                                                Text(diseaseName)
                                                Text("(\(count))")
                                                    .font(.caption2)
                                                    .opacity(0.8)
                                            }
                                            .font(.caption)
                                            .fontWeight(.bold)
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 6)
                                            .background(isSelected ? Color.green : Color(uiColor: .tertiarySystemFill))
                                            .foregroundColor(isSelected ? .white : .primary)
                                            .cornerRadius(16)
                                        }
                                    }
                                }
                            }
                        }

                        HStack {
                            LText("Box confidence")
                                .font(.subheadline)
                                .fontWeight(.semibold)
                            Spacer()
                            Text("\(visibleBoxes.count) of \(allResults.count)  ·  \(Int(boxConfidence * 100))%")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Slider(
                            value: $boxConfidence,
                            in: boxFloor...1.0
                        )
                    }
                    .padding(.horizontal, 4)
                }

                // Diagnostic Metadata Card
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        LText("Crop Category")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        Spacer()
                        Text(isSupportedCrop ? record.cropName.capitalized : "\(record.cropName.capitalized) (Unsupported Crop)")
                            .font(.subheadline)
                            .fontWeight(.bold)
                            .foregroundColor(isSupportedCrop ? .primary : .orange)
                    }

                    Divider()

                    HStack {
                        LText("Timestamp")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        Spacer()
                        Text(formattedDate)
                            .font(.subheadline)
                            .foregroundColor(.primary)
                    }
                }
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(16)

                // Distinct Detected Diseases Section
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Image(systemName: isSupportedCrop ? "checkmark.seal.fill" : "exclamationmark.triangle.fill")
                            .foregroundColor(isSupportedCrop ? .green : .orange)
                        LText("Detected Conditions")
                            .font(.headline)
                            .fontWeight(.bold)
                        Spacer()
                        if isSupportedCrop {
                            LText("%@ Distinct", "\(distinctDiseases.count)")
                                .font(.subheadline)
                                .fontWeight(.bold)
                                .foregroundColor(.green)
                        } else {
                            LText("Unsupported Crop")
                                .font(.subheadline)
                                .fontWeight(.bold)
                                .foregroundColor(.orange)
                        }
                    }

                    Divider()

                    if !isSupportedCrop {
                        HStack(alignment: .top, spacing: 10) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(.orange)
                                .font(.subheadline)
                            VStack(alignment: .leading, spacing: 4) {
                                LText("Not a Supported Target Crop")
                                    .font(.subheadline)
                                    .fontWeight(.bold)
                                    .foregroundColor(.orange)
                                LText("This scanned image is not identified as one of our supported target crops (Corn, Tomato, or Pepper). Disease detection models are optimized for target crops only.")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding(10)
                        .background(Color.orange.opacity(0.12))
                        .cornerRadius(10)
                    } else if distinctDiseases.isEmpty {
                        HStack {
                            Image(systemName: "leaf.fill")
                                .foregroundColor(.green)
                            LText("No Disease Detected")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        .padding(.vertical, 8)
                    } else {
                        ForEach(distinctDiseases) { item in
                            NavigationLink {
                                let info = DiseaseDatabase.shared.getDiseaseInfo(diseaseName: item.diseaseName)
                                DiseaseDetailView(disease: info)
                            } label: {
                                HStack(spacing: 12) {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(item.diseaseName)
                                            .font(.body)
                                            .fontWeight(.bold)
                                            .foregroundColor(.primary)

                                        LText("Detected %d time%@  ·  Max Match: %@",
                                              item.count,
                                              item.count > 1 ? "s" : "",
                                              "\(Int(item.maxScore * 100))%")
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                    }

                                    Spacer()

                                    Text("\(Int(item.maxScore * 100))%")
                                        .font(.subheadline)
                                        .fontWeight(.bold)
                                        .foregroundColor(.green)

                                    Image(systemName: "chevron.right")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                .padding(12)
                                .background(Color(uiColor: .tertiarySystemBackground))
                                .cornerRadius(12)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(16)

                // Treatment Guidelines Action Button
                if !allResults.isEmpty {
                    NavigationLink {
                        RecommendationView(results: allResults)
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
            }
            .padding()
        }
        .navigationTitle(L("Scan Details"))
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            let minVal = allResults.map(\.score).min() ?? 0.10
            let floor = min(max(minVal, 0.05), 0.95)
            boxFloor = floor
            boxConfidence = floor
        }
        .toolbar {
            if let onDelete = onDelete {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(role: .destructive) {
                        onDelete()
                        dismiss()
                    } label: {
                        Image(systemName: "trash")
                            .foregroundColor(.red)
                    }
                }
            }
        }
    }
}

