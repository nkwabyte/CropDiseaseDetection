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
                            Image(systemName: "leaf.fill")
                                .font(.title2)
                                .foregroundColor(.green)
                                .padding(10)
                                .background(Color.green.opacity(0.12))
                                .clipShape(Circle())
                            
                            VStack(alignment: .leading, spacing: 4) {
                                Text(disease.name)
                                    .font(.headline)
                                Text("Crop: \(disease.crop)")
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
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
                VStack(alignment: .leading, spacing: 6) {
                    Text(disease.name)
                        .font(.title2)
                        .fontWeight(.bold)
                    Text("Target Crop: \(disease.crop)")
                        .font(.subheadline)
                        .foregroundColor(.green)
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
