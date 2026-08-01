import SwiftUI

public struct HelpView: View {
    @Environment(\.openURL) private var openURL
    
    public init() {}
    
    private let socialLinks: [SocialLinkItem] = [
        SocialLinkItem(
            name: "Facebook",
            handle: "@RAILKNUST",
            icon: "hand.thumbsup.fill",
            color: Color(red: 0.11, green: 0.46, blue: 0.93),
            url: "https://facebook.com/railknust"
        ),
        SocialLinkItem(
            name: "Twitter / X",
            handle: "@RAILKNUST",
            icon: "message.fill",
            color: Color(red: 0.11, green: 0.63, blue: 0.95),
            url: "https://twitter.com/railknust"
        ),
        SocialLinkItem(
            name: "LinkedIn",
            handle: "RAIL KNUST",
            icon: "briefcase.fill",
            color: Color(red: 0.04, green: 0.45, blue: 0.71),
            url: "https://linkedin.com/company/rail-knust"
        ),
        SocialLinkItem(
            name: "YouTube",
            handle: "RAIL KNUST Channel",
            icon: "play.tv.fill",
            color: Color(red: 0.90, green: 0.13, blue: 0.13),
            url: "https://youtube.com/@railknust"
        ),
        SocialLinkItem(
            name: "Official Website",
            handle: "rail.knust.edu.gh",
            icon: "globe",
            color: .green,
            url: "https://rail.knust.edu.gh"
        )
    ]
    
    public var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Header Banner
                VStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(Color.green.opacity(0.15))
                            .frame(width: 88, height: 88)
                        
                        Image(systemName: "questionmark.bubble.fill")
                            .font(.system(size: 40))
                            .foregroundColor(.green)
                    }
                    
                    Text("Help & Community Support")
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    Text("Connect with RAIL KNUST for research updates, extension guidance, and feedback.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }
                .padding(.top, 12)
                
                // Guidance Instructions Card
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Image(systemName: "lifepreserver.fill")
                            .foregroundColor(.green)
                        Text("How to Use Diagnostics")
                            .font(.headline)
                    }
                    
                    VStack(alignment: .leading, spacing: 8) {
                        GuideStepRow(step: "1", title: "Select Crop Category", desc: "Choose Corn 🌽, Pepper 🫑, or Tomato 🍅 from the Home screen.")
                        GuideStepRow(step: "2", title: "Capture Clear Leaf Photo", desc: "Ensure good lighting and place a single leaf centered in the frame.")
                        GuideStepRow(step: "3", title: "Review Bounding Boxes & Score", desc: "Examine detected disease spots and confidence meters.")
                        GuideStepRow(step: "4", title: "View Treatment Guidelines", desc: "Tap 'View Recommendations' for organic and chemical treatment advice.")
                    }
                }
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(16)
                
                // Contact Us Card
                VStack(alignment: .leading, spacing: 14) {
                    HStack {
                        Image(systemName: "envelope.badge.fill")
                            .foregroundColor(.green)
                        Text("Contact Us")
                            .font(.headline)
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text("Responsible Artificial Intelligence Lab (RAIL)")
                            .font(.subheadline)
                            .fontWeight(.bold)
                            .foregroundColor(.primary)

                        Text("Kwame Nkrumah University of Science and Technology (KNUST)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }

                    Divider()

                    VStack(alignment: .leading, spacing: 12) {
                        ContactInfoRow(
                            icon: "phone.fill",
                            title: "Phone",
                            value: "+233 20 753 4396",
                            url: "tel:+233207534396"
                        )
                        ContactInfoRow(
                            icon: "envelope.fill",
                            title: "Email",
                            value: "rail@knust.edu.gh",
                            url: "mailto:rail@knust.edu.gh"
                        )
                        ContactInfoRow(
                            icon: "mappin.and.ellipse",
                            title: "Location",
                            value: "College of Engineering, Research Hill, KNUST, Kumasi, Ghana",
                            url: nil
                        )
                        ContactInfoRow(
                            icon: "globe",
                            title: "Website",
                            value: "rail.knust.edu.gh",
                            url: "https://rail.knust.edu.gh"
                        )
                    }
                }
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(16)

                // Social Media Connections Section
                VStack(alignment: .leading, spacing: 14) {
                    HStack {
                        Image(systemName: "person.2.wave.2.fill")
                            .foregroundColor(.green)
                        Text("Connect on Social Media")
                            .font(.headline)
                    }
                    
                    VStack(spacing: 12) {
                        ForEach(socialLinks) { link in
                            Button {
                                if let url = URL(string: link.url) {
                                    openURL(url)
                                }
                            } label: {
                                HStack(spacing: 14) {
                                    ZStack {
                                        RoundedRectangle(cornerRadius: 10)
                                            .fill(link.color.opacity(0.15))
                                            .frame(width: 44, height: 44)
                                        
                                        Image(systemName: link.icon)
                                            .font(.title3)
                                            .foregroundColor(link.color)
                                    }
                                    
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(link.name)
                                            .font(.headline)
                                            .foregroundColor(.primary)
                                        Text(link.handle)
                                            .font(.subheadline)
                                            .foregroundColor(.secondary)
                                    }
                                    
                                    Spacer()
                                    
                                    Image(systemName: "arrow.up.right")
                                        .font(.system(size: 14, weight: .bold))
                                        .foregroundColor(.secondary)
                                }
                                .padding(10)
                                .background(Color(uiColor: .tertiarySystemBackground))
                                .cornerRadius(12)
                            }
                        }
                    }
                }
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(16)
            }
            .padding()
        }
        .navigationTitle("Help & Support")
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct SocialLinkItem: Identifiable {
    let id = UUID()
    let name: String
    let handle: String
    let icon: String
    let color: Color
    let url: String
}

private struct GuideStepRow: View {
    let step: String
    let title: String
    let desc: String
    
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            ZStack {
                Circle()
                    .fill(Color.green)
                    .frame(width: 24, height: 24)
                Text(step)
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            }
            
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                Text(desc)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }
}

private struct ContactInfoRow: View {
    @Environment(\.openURL) private var openURL
    let icon: String
    let title: String
    let value: String
    let url: String?

    var body: some View {
        Button {
            if let urlStr = url, let targetURL = URL(string: urlStr) {
                openURL(targetURL)
            }
        } label: {
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(Color.green.opacity(0.15))
                        .frame(width: 32, height: 32)
                    Image(systemName: icon)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.green)
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(value)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.primary)
                }

                Spacer()

                if url != nil {
                    Image(systemName: "arrow.up.right")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(.secondary)
                }
            }
        }
        .buttonStyle(.plain)
    }
}
