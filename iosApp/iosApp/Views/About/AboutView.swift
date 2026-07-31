import SwiftUI
import ComposeApp

public struct AboutView: View {
    @Environment(\.dismiss) private var dismiss
    
    public init() {}
    
    public var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Header Branding Card
                VStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(Color.green.opacity(0.15))
                            .frame(width: 100, height: 100)
                        
                        Image(systemName: "leaf.fill")
                            .font(.system(size: 48))
                            .foregroundColor(.green)
                    }
                    
                    Text("Crop Disease Detection")
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    Text("On-Device AI Diagnostics for Agriculture")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    Text("Version \(KoinHelper.settingsManager.getAppVersion())")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 4)
                        .background(Capsule().fill(Color.green.opacity(0.12)))
                        .foregroundColor(.green)
                }
                .padding(.top, 12)
                
                // Mission & Overview Card
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Image(systemName: "info.circle.fill")
                            .foregroundColor(.green)
                        Text("Project Overview")
                            .font(.headline)
                    }
                    
                    Text("The Plant Disease Detector application provides real-time, offline crop disease identification for farmers and agricultural extension officers across Ghana and West Africa.")
                        .font(.body)
                        .foregroundColor(.secondary)
                        .lineSpacing(4)
                    
                    Text("Powered by Meta ExecuTorch on-device neural networks (YOLO26 object detection and EfficientNet-B2 classification), the app diagnoses disease conditions in Corn 🌽, Pepper 🫑, and Tomato 🍅 crops without requiring an active internet connection.")
                        .font(.body)
                        .foregroundColor(.secondary)
                        .lineSpacing(4)
                }
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(16)
                
                // Partners & Sponsors Card
                VStack(alignment: .leading, spacing: 14) {
                    HStack {
                        Image(systemName: "building.2.fill")
                            .foregroundColor(.green)
                        Text("Research Partners & Support")
                            .font(.headline)
                    }
                    
                    VStack(alignment: .leading, spacing: 12) {
                        HStack(spacing: 14) {
                            Image("rail_logo")
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 52, height: 52)
                                .padding(4)
                                .background(Color.white)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .shadow(color: Color.black.opacity(0.08), radius: 4, x: 0, y: 2)
                            
                            VStack(alignment: .leading, spacing: 2) {
                                Text("RAIL KNUST")
                                    .font(.headline)
                                Text("Responsible AI Lab, KNUST")
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                            }
                        }
                        
                        Text("Developed under the Responsible AI Lab (RAIL) at Kwame Nkrumah University of Science and Technology, Kumasi, Ghana. Dedicated to advancing ethical, inclusive, and practical AI solutions for African agriculture.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .lineSpacing(3)
                        
                        Divider()
                            .padding(.vertical, 2)
                        
                        Text("Partner Institutions")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundColor(.primary)
                        
                        Image("rail_partners")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(maxWidth: .infinity)
                            .frame(height: 130)
                            .padding(8)
                            .background(Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .shadow(color: Color.black.opacity(0.06), radius: 4, x: 0, y: 2)
                    }
                }
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(16)
            }
            .padding()
        }
        .navigationTitle("About")
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct AboutSpecRow: View {
    let label: String
    let value: String
    
    var body: some View {
        HStack {
            Text(label)
                .font(.subheadline)
                .foregroundColor(.primary)
            Spacer()
            Text(value)
                .font(.caption)
                .fontWeight(.medium)
                .foregroundColor(.secondary)
        }
    }
}
