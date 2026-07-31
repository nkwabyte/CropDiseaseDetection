import SwiftUI
import ComposeApp

public struct EncyclopediaView: View {
    @State private var searchText: String = ""
    @State private var selectedCropFilter: String = "All"
    
    private let cropFilters = ["All", "Corn", "Pepper", "Tomato"]
    
    public var body: some View {
        NavigationStack {
            VStack(spacing: 12) {
                // Filter Chips
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(cropFilters, id: \.self) { filter in
                            let isSelected = (selectedCropFilter == filter)
                            Button {
                                selectedCropFilter = filter
                            } label: {
                                Text(filter)
                                    .font(.subheadline)
                                    .fontWeight(.semibold)
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 8)
                                    .background(
                                        Capsule()
                                            .fill(isSelected ? Color.green : Color(uiColor: .secondarySystemBackground))
                                    )
                                    .foregroundColor(isSelected ? .white : .primary)
                            }
                        }
                    }
                    .padding(.horizontal)
                }
                
                // Disease List
                let allDiseases = (DiseaseDatabase.shared.diseases as? [DiseaseInfo]) ?? []
                let filteredDiseases = allDiseases.filter { disease in
                    let matchesSearch = searchText.isEmpty || disease.name.localizedCaseInsensitiveContains(searchText)
                    let matchesCrop = (selectedCropFilter == "All") || disease.crop.equalsIgnoreCase(selectedCropFilter)
                    return matchesSearch && matchesCrop
                }
                
                List(filteredDiseases, id: \.name) { disease in
                    NavigationLink {
                        DiseaseDetailView(disease: disease)
                    } label: {
                        HStack(spacing: 14) {
                            AsyncImage(url: URL(string: disease.imageUrl)) { phase in
                                switch phase {
                                case .success(let image):
                                    image
                                        .resizable()
                                        .aspectRatio(contentMode: .fill)
                                case .failure, .empty:
                                    ZStack {
                                        Color.green.opacity(0.12)
                                        Image(systemName: "leaf.fill")
                                            .font(.title3)
                                            .foregroundColor(.green)
                                    }
                                @unknown default:
                                    Color.green.opacity(0.12)
                                }
                            }
                            .frame(width: 60, height: 60)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            
                            VStack(alignment: .leading, spacing: 4) {
                                Text(disease.name)
                                    .font(.headline)
                                
                                HStack(spacing: 8) {
                                    Text(disease.crop)
                                        .font(.caption)
                                        .fontWeight(.bold)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 3)
                                        .background(Capsule().fill(Color.green.opacity(0.12)))
                                        .foregroundColor(.green)
                                    
                                    Text(disease.isHealthy ? "Healthy" : "Disease")
                                        .font(.caption)
                                        .fontWeight(.bold)
                                        .foregroundColor(disease.isHealthy ? .green : .red)
                                }
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }
                .listStyle(.plain)
            }
            .navigationTitle("Encyclopedia")
            .searchable(text: $searchText, prompt: "Search plant diseases...")
        }
    }
}

struct DiseaseDetailView: View {
    let disease: DiseaseInfo
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                // Disease Image Banner
                AsyncImage(url: URL(string: disease.imageUrl)) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    case .failure, .empty:
                        ZStack {
                            Rectangle()
                                .fill(Color.green.opacity(0.12))
                            Image(systemName: "leaf.fill")
                                .font(.system(size: 48))
                                .foregroundColor(.green)
                        }
                    @unknown default:
                        EmptyView()
                    }
                }
                .frame(height: 220)
                .frame(maxWidth: .infinity)
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .shadow(color: Color.black.opacity(0.1), radius: 6, x: 0, y: 3)
                
                VStack(alignment: .leading, spacing: 6) {
                    Text(disease.name)
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    if !disease.localName.isEmpty {
                        Text(disease.localName)
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(.secondary)
                    }
                    
                    HStack(spacing: 8) {
                        Text("Target Crop: \(disease.crop)")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundColor(.green)
                        
                        Spacer()
                        
                        Text(disease.isHealthy ? "✓ Healthy" : "⚠️ Disease")
                            .font(.caption)
                            .fontWeight(.bold)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(Capsule().fill(disease.isHealthy ? Color.green.opacity(0.15) : Color.red.opacity(0.15)))
                            .foregroundColor(disease.isHealthy ? .green : .red)
                    }
                }
                
                Divider()
                
                VStack(alignment: .leading, spacing: 14) {
                    DetailSectionView(title: "Overview", icon: "info.circle.fill", text: disease.description_)
                    DetailSectionView(title: "Symptoms", icon: "cross.case.fill", text: disease.symptoms)
                    DetailSectionView(title: "Organic Treatment", icon: "leaf.fill", text: disease.organicMitigation)
                    DetailSectionView(title: "Chemical Treatment", icon: "flask.fill", text: disease.chemicalMitigation)
                    DetailSectionView(title: "Prevention Measures", icon: "shield.fill", text: disease.prevention)
                }
            }
            .padding()
        }
        .navigationTitle(disease.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct DetailSectionView: View {
    let title: String
    let icon: String
    let text: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(.green)
                Text(title)
                    .font(.headline)
            }
            Text(text)
                .font(.body)
                .foregroundColor(.secondary)
        }
    }
}

extension String {
    func equalsIgnoreCase(_ other: String) -> Bool {
        self.compare(other, options: .caseInsensitive) == .orderedSame
    }
}
