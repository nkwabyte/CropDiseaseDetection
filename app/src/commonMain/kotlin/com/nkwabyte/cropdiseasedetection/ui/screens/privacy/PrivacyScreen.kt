package com.nkwabyte.cropdiseasedetection.ui.screens.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nkwabyte.cropdiseasedetection.common.navigation.appbar.AppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    modifier: Modifier = Modifier,
    onDrawerButtonClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            AppBar(
                title = {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onDrawerButtonClick = onDrawerButtonClick,
                isHomeScreen = false,
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Responsible AI Lab",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Last Updated: Today",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                PrivacySection(
                    title = "1. Introduction",
                    content = "Welcome to the Responsible Artificial Intelligence Lab (“RAIL,” “we,” “us,” or “our”). We are committed to advancing the field of artificial intelligence through responsible and ethical research. This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you interact with our applications, websites, and services (collectively, our “Services”).\n\nProtecting your privacy is a core component of our mission. We are committed to transparency and security in our data practices. This policy details our commitment to anonymizing data where possible, encrypting your information, and giving you control over its retention.\n\nPlease read this Privacy Policy carefully. By using our Services, you agree to the collection and use of information in accordance with this policy."
                )

                PrivacySection(
                    title = "2. Information We Collect",
                    content = "We may collect information about you in a variety of ways. The information we may collect via our Services includes:\n\na) Personal Data You Provide to Us\nWe collect personally identifiable information that you voluntarily provide to us when you register for an account or interact with our Services. This information includes:\n• Account Credentials: Your username, email address, and a hashed, salted (and therefore unreadable) version of your password.\n• Social Authentication: If you choose to register or log in using a third-party social media service (e.g., Google, GitHub, Microsoft), we will collect the authentication information provided by that service, such as your name and email address. We do not collect or store the password you use for that third-party service.\n\nb) Data Collected Automatically\nWhen you use our Services, we may automatically collect information about your usage, such as:\n• Log and Usage Data: Information about your activity on our Services, including timestamps of your visits, pages viewed, and other system activity. This data is necessary to enforce our data retention policies (e.g., to track account inactivity)."
                )

                PrivacySection(
                    title = "3. How We Use Your Information",
                    content = "We use the information we collect for the following purposes:\n\n• To Enforce Our Policies: To comply with legal obligations and enforce our data retention and deletion policies.\n• To Provide and Manage Your Account: To create your account, authenticate you as a user, and provide you with access to our Services.\n• To Secure Our Services: To monitor for and prevent fraudulent or malicious activity and to ensure the security of your data and our systems.\n• For AI Research and Development: This is the core purpose of our lab. To facilitate this research, all personally identifiable information (PII) is anonymized or aggregated before it is used by our researchers. Your personal login credentials (like your email or password) are never used in our research data sets. Our research focuses on patterns and insights from non-identifiable data.\n• To Communicate With You: To send you administrative information, such as updates to our terms, security alerts, or support messages."
                )

                PrivacySection(
                    title = "4. Data Security",
                    content = "We implement robust administrative, technical, and physical security measures to protect your information. Key security practices include:\n\n• Encryption in Transmission: All data exchanged between your device and our Services is encrypted in transmission using industry-standard Transport Layer Security (TLS/SSL) protocols.\n• Encryption at Rest: Your personal data, including your account credentials, is stored in an encrypted format on our servers.\n• Anonymization: As stated above, any data used for research purposes is anonymized to remove any personal identifiers.\n• Access Controls: Access to raw personal data is strictly limited to essential personnel who require it for system administration and security, and who are bound by strict confidentiality obligations.\n\nWhile we have taken reasonable steps to secure the personal information you provide to us, please be aware that no security measures are perfect or impenetrable, and no method of data transmission can be guaranteed against any interception or other type of misuse."
                )

                PrivacySection(
                    title = "5. Data Retention and Deletion",
                    content = "We believe in data minimization and user control. We store personal information only for as long as it is necessary to fulfill the purposes for which it was collected, or as required by law.\n\na) Policy on Inactivity\nBy default, your account and all associated personal data will be either permanently deleted or securely archived (and put beyond use) after a continuous period of thirty (30) days of inactivity.\n\nInactivity is defined as not logging into or otherwise interacting with our Services. We will attempt to notify you via your registered email address before this 30-day period expires to give you an opportunity to keep your account active.\n\nb) Deletion Upon Request\nYou have the right to request the deletion of your account and all associated personal data at any time. You can make this request by:\n• Using the account deletion settings within our Services (if available).\n• Contacting us directly at the email address provided in the “Contact Us” section below.\n\nWe will process your request in a timely manner, subject to any legal obligations that may require us to retain certain information."
                )

                PrivacySection(
                    title = "6. Your Data Protection Rights",
                    content = "You have certain rights regarding your personal information. Depending on your location, these may include the right to:\n\n• Access: Request a copy of the personal information we hold about you.\n• Rectification: Request that we correct any inaccurate or incomplete information.\n• Erasure (Deletion): Request that we delete your personal information, as detailed in Section 5.\n• Portability: Request that we provide your data in a structured, machine-readable format.\n• Object: Object to our processing of your personal data for certain purposes.\n\nTo exercise any of these rights, please contact us."
                )

                PrivacySection(
                    title = "7. Children’s Privacy",
                    content = "Our Services are not intended for or directed to individuals under the age of 13 (or a higher age if required by applicable law). We do not knowingly collect personal information from children. If we become aware that we have inadvertently collected personal data from a child, we will take steps to delete it as soon as possible."
                )

                PrivacySection(
                    title = "8. Third-Party Links and Services",
                    content = "Our Services may contain links to third-party websites or integrate with third-party services (such as social authentication providers). We are not responsible for the privacy practices or the content of these third-party services. This Privacy Policy does not apply to them. We encourage you to read the privacy policies of any third-party services you use."
                )

                PrivacySection(
                    title = "9. Changes to This Privacy Policy",
                    content = "We may update this Privacy Policy from time to time to reflect changes in our practices or for other operational, legal, or regulatory reasons. We will notify you of any changes by posting the new Privacy Policy on this page and updating the “Last Updated” date at the top.\n\nWe encourage you to review this Privacy Policy periodically to stay informed about how we are protecting your information."
                )

                PrivacySection(
                    title = "10. Contact Us",
                    content = "If you have any questions, concerns, or requests regarding this Privacy Policy or our data practices, please contact us at:\n\nResponsible Artificial Intelligence Lab (RAIL)\nEmail: rail@knust.edu.gh\nAddress: KNUST, College of Engineering, Research Hill, Kumasi, Ghana"
                )

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun PrivacySection(title: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
        )
    }
}