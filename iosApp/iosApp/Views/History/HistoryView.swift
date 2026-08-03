import SwiftUI
import ComposeApp

public struct HistoryView: View {
    @StateObject private var stateObs = ObservableFlow(KoinHelper.historyViewModel.historyState)
    @State private var selectedRecord: DetectionRecord? = nil
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
                            HistoryRecordRow(record: record)
                                .contentShape(Rectangle())
                                .onTapGesture {
                                    selectedRecord = record
                                }
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
            .sheet(item: $selectedRecord) { record in
                HistoryDetailSheet(record: record, onDelete: { pendingDeletion = record })
            }
            // Deleting only hides the scan; the record is retained, so the wording promises
            // exactly that and nothing stronger.
            .alert(L("Delete Scan"), isPresented: isDeleteAlertPresented) {
                Button(L("Delete"), role: .destructive) {
                    if let record = pendingDeletion {
                        KoinHelper.historyViewModel.deleteRecord(record: record)
                    }
                    selectedRecord = nil
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
        primaryResult?.className ?? L("No Detection")
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

private struct HistoryDetailSheet: View {
    @Environment(\.dismiss) private var dismiss
    let record: DetectionRecord
    var onDelete: (() -> Void)? = nil
    @StateObject private var langMgr = LanguageManager.shared

    private var formattedDate: String {
        let date = Date(timeIntervalSince1970: Double(record.timestamp) / 1000.0)
        let formatter = DateFormatter()
        formatter.dateStyle = .long
        formatter.timeStyle = .medium
        return formatter.string(from: date)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // Image Header
                    ZStack {
                        RoundedRectangle(cornerRadius: 16)
                            .fill(Color(uiColor: .secondarySystemBackground))
                            .frame(height: 220)

                        if record.imageUrl.hasPrefix("data:image"),
                           let commaIndex = record.imageUrl.firstIndex(of: ","),
                           let data = Data(base64Encoded: String(record.imageUrl[record.imageUrl.index(after: commaIndex)...])),
                           let uiImage = UIImage(data: data) {
                            Image(uiImage: uiImage)
                                .resizable()
                                .scaledToFit()
                                .frame(height: 220)
                                .cornerRadius(16)
                        } else if let url = URL(string: record.imageUrl) {
                            AsyncImage(url: url) { image in
                                image
                                    .resizable()
                                    .scaledToFit()
                            } placeholder: {
                                ProgressView()
                            }
                            .frame(height: 220)
                            .cornerRadius(16)
                        }
                    }

                    // Metadata Card
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            LText("Crop Category")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            Spacer()
                            LText(record.cropName.capitalized)
                                .font(.subheadline)
                                .fontWeight(.bold)
                                .foregroundColor(.primary)
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

                        Divider()

                        HStack {
                            LText("Model Backend")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            Spacer()
                            Text(record.modelName ?? L("Unknown"))
                                .font(.subheadline)
                                .fontWeight(.medium)
                                .foregroundColor(.primary)
                        }
                    }
                    .padding()
                    .background(Color(uiColor: .secondarySystemBackground))
                    .cornerRadius(16)

                    // Detections List
                    LText("Detected Conditions (%@)", "\(record.matchingResults.count)")
                        .font(.headline)
                        .fontWeight(.bold)

                    ForEach(Array(record.matchingResults.enumerated()), id: \.offset) { _, result in
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(result.className ?? L("Unknown"))
                                    .font(.body)
                                    .fontWeight(.semibold)
                                LText("Score: %@", "\(Int(result.score * 100))%")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }

                            Spacer()

                            Text("\(Int(result.score * 100))%")
                                .font(.headline)
                                .fontWeight(.bold)
                                .foregroundColor(.green)
                        }
                        .padding()
                        .background(Color(uiColor: .secondarySystemBackground))
                        .cornerRadius(12)
                    }
                }
                .padding()
            }
            .navigationTitle(L("Scan Details"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L("Done")) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .destructiveAction) {
                    Button(role: .destructive) {
                        dismiss()
                        onDelete?()
                    } label: {
                        Image(systemName: "trash")
                    }
                }
            }
        }
    }
}
