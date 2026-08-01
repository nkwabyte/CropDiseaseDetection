import SwiftUI

public struct PrivacyView: View {
    public init() {}

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Responsible AI Lab")
                        .font(.title)
                        .fontWeight(.bold)
                        .foregroundColor(.green)

                    Text("Last Updated: Today")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.bottom, 4)

                PrivacySectionView(
                    title: "1. Introduction",
                    content: """
                    Welcome to the Responsible Artificial Intelligence Lab (“RAIL,” “we,” “us,” or “our”). We are committed to advancing the field of artificial intelligence through responsible and ethical research. This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you interact with our applications, websites, and services (collectively, our “Services”).

                    Protecting your privacy is a core component of our mission. We are committed to transparency and security in our data practices. This policy details our commitment to anonymizing data where possible, encrypting your information, and giving you control over its retention.

                    Please read this Privacy Policy carefully. By using our Services, you agree to the collection and use of information in accordance with this policy.
                    """
                )

                PrivacySectionView(
                    title: "2. Information We Collect",
                    content: """
                    We may collect information about you in a variety of ways. The information we may collect via our Services includes:

                    a) Personal Data You Provide to Us
                    We collect personally identifiable information that you voluntarily provide to us when you register for an account or interact with our Services. This information includes:
                    • Account Credentials: Your username, email address, and a hashed, salted (and therefore unreadable) version of your password.
                    • Social Authentication: If you choose to register or log in using a third-party social media service (e.g., Google, GitHub, Microsoft), we will collect the authentication information provided by that service, such as your name and email address. We do not collect or store the password you use for that third-party service.

                    b) Data Collected Automatically
                    When you use our Services, we may automatically collect information about your usage, such as:
                    • Log and Usage Data: Information about your activity on our Services, including timestamps of your visits, pages viewed, and other system activity. This data is necessary to enforce our data retention policies (e.g., to track account inactivity).
                    """
                )

                PrivacySectionView(
                    title: "3. How We Use Your Information",
                    content: """
                    We use the information we collect for the following purposes:

                    • To Enforce Our Policies: To comply with legal obligations and enforce our data retention and deletion policies.
                    • To Provide and Manage Your Account: To create your account, authenticate you as a user, and provide you with access to our Services.
                    • To Secure Our Services: To monitor for and prevent fraudulent or malicious activity and to ensure the security of your data and our systems.
                    • For AI Research and Development: This is the core purpose of our lab. To facilitate this research, all personally identifiable information (PII) is anonymized or aggregated before it is used by our researchers. Your personal login credentials (like your email or password) are never used in our research data sets. Our research focuses on patterns and insights from non-identifiable data.
                    • To Communicate With You: To send you administrative information, such as updates to our terms, security alerts, or support messages.
                    """
                )

                PrivacySectionView(
                    title: "4. Data Security",
                    content: """
                    We implement robust administrative, technical, and physical security measures to protect your information. Key security practices include:

                    • Encryption in Transmission: All data exchanged between your device and our Services is encrypted in transmission using industry-standard Transport Layer Security (TLS/SSL) protocols.
                    • Encryption at Rest: Your personal data, including your account credentials, is stored in an encrypted format on our servers.
                    • Anonymization: As stated above, any data used for research purposes is anonymized to remove any personal identifiers.
                    • Access Controls: Access to raw personal data is strictly limited to essential personnel who require it for system administration and security, and who are bound by strict confidentiality obligations.

                    While we have taken reasonable steps to secure the personal information you provide to us, please be aware that no security measures are perfect or impenetrable, and no method of data transmission can be guaranteed against any interception or other type of misuse.
                    """
                )

                PrivacySectionView(
                    title: "5. Data Retention and Deletion",
                    content: """
                    We believe in data minimization and user control. We store personal information only for as long as it is necessary to fulfill the purposes for which it was collected, or as required by law.

                    a) Policy on Inactivity
                    By default, your account and all associated personal data will be either permanently deleted or securely archived (and put beyond use) after a continuous period of thirty (30) days of inactivity.

                    Inactivity is defined as not logging into or otherwise interacting with our Services. We will attempt to notify you via your registered email address before this 30-day period expires to give you an opportunity to keep your account active.

                    b) Deletion Upon Request
                    You have the right to request the deletion of your account and all associated personal data at any time. You can make this request by:
                    • Using the account deletion settings within our Services (if available).
                    • Contacting us directly at the email address provided in the “Contact Us” section below.

                    We will process your request in a timely manner, subject to any legal obligations that may require us to retain certain information.
                    """
                )

                PrivacySectionView(
                    title: "6. Your Data Protection Rights",
                    content: """
                    You have certain rights regarding your personal information. Depending on your location, these may include the right to:

                    • Access: Request a copy of the personal information we hold about you.
                    • Rectification: Request that we correct any inaccurate or incomplete information.
                    • Erasure (Deletion): Request that we delete your personal information, as detailed in Section 5.
                    • Portability: Request that we provide your data in a structured, machine-readable format.
                    • Object: Object to our processing of your personal data for certain purposes.

                    To exercise any of these rights, please contact us.
                    """
                )

                PrivacySectionView(
                    title: "7. Children’s Privacy",
                    content: """
                    Our Services are not intended for or directed to individuals under the age of 13 (or a higher age if required by applicable law). We do not knowingly collect personal information from children. If we become aware that we have inadvertently collected personal data from a child, we will take steps to delete it as soon as possible.
                    """
                )

                PrivacySectionView(
                    title: "8. Third-Party Links and Services",
                    content: """
                    Our Services may contain links to third-party websites or integrate with third-party services (such as social authentication providers). We are not responsible for the privacy practices or the content of these third-party services. This Privacy Policy does not apply to them. We encourage you to read the privacy policies of any third-party services you use.
                    """
                )

                PrivacySectionView(
                    title: "9. Changes to This Privacy Policy",
                    content: """
                    We may update this Privacy Policy from time to time to reflect changes in our practices or for other operational, legal, or regulatory reasons. We will notify you of any changes by posting the new Privacy Policy on this page and updating the “Last Updated” date at the top.

                    We encourage you to review this Privacy Policy periodically to stay informed about how we are protecting your information.
                    """
                )

                PrivacyContactCardView()
            }
            .padding()
        }
        .navigationTitle("Privacy Policy")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct PrivacySectionView: View {
    let title: String
    let content: String

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.headline)
                .fontWeight(.bold)
                .foregroundColor(.green)

            FormattedPrivacyContentView(content: content)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(uiColor: .secondarySystemBackground))
        .cornerRadius(16)
    }
}

struct FormattedPrivacyContentView: View {
    let content: String

    private var lines: [String] {
        content.components(separatedBy: "\n")
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            ForEach(lines.indices, id: \.self) { idx in
                let line = lines[idx]

                if let range = line.range(of: "^[a-z]\\)\\s*", options: .regularExpression) {
                    let badge = String(line[..<range.upperBound]).trimmingCharacters(in: .whitespaces)
                    let text = String(line[range.upperBound...]).trimmingCharacters(in: .whitespaces)

                    HStack(spacing: 8) {
                        Text(badge)
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(.green)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.green.opacity(0.15))
                            .cornerRadius(6)

                        Text(text)
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundColor(.primary)
                    }
                    .padding(.top, 4)

                } else if line.range(of: "^[•\\-]\\s*", options: .regularExpression) != nil {
                    let cleanLine = line.replacingOccurrences(of: "^[•\\-]\\s*", with: "", options: .regularExpression)

                    HStack(alignment: .top, spacing: 10) {
                        Circle()
                            .fill(Color.green)
                            .frame(width: 6, height: 6)
                            .padding(.top, 7)

                        if let colonRange = cleanLine.range(of: ":") {
                            let bulletTitle = String(cleanLine[..<colonRange.lowerBound]).trimmingCharacters(in: .whitespaces)
                            let bulletDesc = String(cleanLine[colonRange.upperBound...]).trimmingCharacters(in: .whitespaces)

                            (Text(bulletTitle + ": ").fontWeight(.bold).foregroundColor(.primary) +
                             Text(bulletDesc).foregroundColor(.secondary))
                                .font(.subheadline)
                                .lineSpacing(4)
                        } else {
                            Text(cleanLine)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                                .lineSpacing(4)
                        }
                    }

                } else {
                    Text(line)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .lineSpacing(4)
                }
            }
        }
    }
}

struct PrivacyContactCardView: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("10. Contact Us")
                .font(.headline)
                .fontWeight(.bold)
                .foregroundColor(.green)

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
                PrivacyContactInfoRow(
                    icon: "phone.fill",
                    title: "Phone",
                    value: "+233 20 753 4396",
                    url: "tel:+233207534396"
                )
                PrivacyContactInfoRow(
                    icon: "envelope.fill",
                    title: "Email",
                    value: "rail@knust.edu.gh",
                    url: "mailto:rail@knust.edu.gh"
                )
                PrivacyContactInfoRow(
                    icon: "mappin.and.ellipse",
                    title: "Location",
                    value: "College of Engineering, Research Hill, KNUST, Kumasi, Ghana",
                    url: nil
                )
                PrivacyContactInfoRow(
                    icon: "globe",
                    title: "Website",
                    value: "rail.knust.edu.gh",
                    url: "https://rail.knust.edu.gh"
                )
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(uiColor: .secondarySystemBackground))
        .cornerRadius(16)
    }
}

private struct PrivacyContactInfoRow: View {
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

