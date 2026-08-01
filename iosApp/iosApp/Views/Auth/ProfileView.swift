import SwiftUI
import FirebaseAuth
import ComposeApp

struct ProfileView: View {
    @StateObject private var authStateObs = ObservableFlow(KoinHelper.authViewModel.authState)
    @StateObject private var profileStateObs = ObservableFlow(KoinHelper.profileViewModel.userProfileState)

    @State private var emailInput: String = ""
    @State private var passwordInput: String = ""
    @State private var userNameInput: String = ""
    @State private var selectedRole: UserRole = .farmer
    @State private var isRegistering: Bool = false

    @State private var isAuthLoading: Bool = false
    @State private var authErrorMessage: String? = nil
    @State private var currentUser: FirebaseAuth.User? = Auth.auth().currentUser
    @State private var statusToastMessage: String? = nil

    // Data & Privacy Dialogs
    @State private var showClearCacheAlert: Bool = false
    @State private var showDeleteDataAlert: Bool = false
    @State private var showDeleteAccountAlert: Bool = false

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    if let msg = statusToastMessage {
                        toastBanner(msg: msg)
                    }

                    if let user = currentUser {
                        loggedInUserCard(user: user)
                    } else {
                        guestAuthCard
                    }
                    
                    privacySection
                }
                .padding()
            }
            .navigationTitle("Account")
            .onAppear {
                self.currentUser = Auth.auth().currentUser
            }
            .onChange(of: authStateObs.value) { state in
                handleAuthStateChange(state: state)
            }
        }
    }

    private func handleAuthStateChange(state: Any) {
        if state is AuthState.Loading {
            isAuthLoading = true
            authErrorMessage = nil
        } else if let error = state as? AuthState.Error {
            isAuthLoading = false
            authErrorMessage = error.message
        } else if state is AuthState.Success {
            isAuthLoading = false
            authErrorMessage = nil
            emailInput = ""
            passwordInput = ""
            userNameInput = ""
            self.currentUser = Auth.auth().currentUser
        } else {
            isAuthLoading = false
        }
    }

    // ── Toast Banner ──────────────────────────────────────────
    @ViewBuilder
    private func toastBanner(msg: String) -> some View {
        HStack {
            Image(systemName: "checkmark.circle.fill")
                .foregroundColor(.green)
            Text(msg)
                .font(.subheadline)
                .foregroundColor(.primary)
            Spacer()
            Button {
                statusToastMessage = nil
            } label: {
                Image(systemName: "xmark")
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color.green.opacity(0.15))
        .cornerRadius(12)
    }

    // ── Logged In User View ──────────────────────────────────
    @ViewBuilder
    private func loggedInUserCard(user: FirebaseAuth.User) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "person.crop.circle.fill")
                .font(.system(size: 80))
                .foregroundColor(.green)
            
            VStack(spacing: 4) {
                Text(user.displayName?.isEmpty == false ? user.displayName! : (profileStateObs.value.userName.isEmpty ? "Farmer" : profileStateObs.value.userName))
                    .font(.title2)
                    .fontWeight(.bold)
                
                if let email = user.email {
                    Text(email)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
            
            HStack(spacing: 24) {
                VStack {
                    Text(profileStateObs.value.detections)
                        .font(.title3)
                        .fontWeight(.bold)
                    Text("Detections")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Divider()
                    .frame(height: 32)
                
                VStack {
                    Text(profileStateObs.value.successRate)
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(.green)
                    Text("Success Rate")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .padding(.vertical, 8)
            .padding(.horizontal, 16)
            .background(Color(uiColor: .systemBackground))
            .cornerRadius(12)
            
            Button(role: .destructive) {
                try? Auth.auth().signOut()
                KoinHelper.authViewModel.resetState()
                KoinHelper.profileViewModel.resetProfile()
                self.currentUser = nil
            } label: {
                Label("Sign Out", systemImage: "rectangle.portrait.and.arrow.right")
                    .font(.headline)
                    .foregroundColor(.red)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.red.opacity(0.1))
                    .cornerRadius(14)
            }
            .padding(.top, 4)
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(Color(uiColor: .secondarySystemBackground))
        .cornerRadius(20)
    }

    // ── Guest & Sign In / Register View ──────────────────────
    @ViewBuilder
    private var guestAuthCard: some View {
        VStack(spacing: 16) {
            // Guest Banner
            VStack(spacing: 8) {
                Image(systemName: "person.badge.shield.checkmark.fill")
                    .font(.system(size: 44))
                    .foregroundColor(.green)
                
                Text("Guest Farmer")
                    .font(.title3)
                    .fontWeight(.bold)
                
                Text("Sign in or create an account to save your disease detection history and sync across devices.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding()
            .frame(maxWidth: .infinity)
            .background(Color.green.opacity(0.08))
            .cornerRadius(16)
            
            // Form Card
            VStack(spacing: 16) {
                Picker("Auth Mode", selection: $isRegistering) {
                    Text("Sign In").tag(false)
                    Text("Register").tag(true)
                }
                .pickerStyle(.segmented)
                .padding(.bottom, 8)
                
                if let errorMsg = authErrorMessage {
                    HStack {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(.red)
                        Text(errorMsg)
                            .font(.caption)
                            .foregroundColor(.red)
                        Spacer()
                    }
                    .padding()
                    .background(Color.red.opacity(0.12))
                    .cornerRadius(10)
                }
                
                VStack(spacing: 12) {
                    if isRegistering {
                        TextField("Full Name", text: $userNameInput)
                            .autocapitalization(.words)
                            .padding()
                            .background(Color(uiColor: .secondarySystemBackground))
                            .cornerRadius(10)
                    }

                    TextField("Email Address", text: $emailInput)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                        .padding()
                        .background(Color(uiColor: .secondarySystemBackground))
                        .cornerRadius(10)
                    
                    SecureField("Password", text: $passwordInput)
                        .padding()
                        .background(Color(uiColor: .secondarySystemBackground))
                        .cornerRadius(10)

                    if isRegistering {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Role")
                                .font(.caption)
                                .fontWeight(.semibold)
                                .foregroundColor(.secondary)
                            
                            Picker("Select Role", selection: $selectedRole) {
                                Text("Farmer 🧑‍🌾").tag(UserRole.farmer)
                                Text("Field Agent 🕵️").tag(UserRole.fieldAgent)
                            }
                            .pickerStyle(.segmented)
                        }
                    }
                }
                
                Button {
                    authErrorMessage = nil
                    let email = emailInput.trimmingCharacters(in: .whitespacesAndNewlines)
                    let pass = passwordInput.trimmingCharacters(in: .whitespacesAndNewlines)
                    
                    if email.isEmpty || pass.isEmpty {
                        authErrorMessage = "Please fill in all email and password fields."
                        return
                    }
                    
                    if isRegistering {
                        let name = userNameInput.trimmingCharacters(in: .whitespacesAndNewlines)
                        let finalName = name.isEmpty ? (selectedRole == .fieldAgent ? "Field Agent" : "Farmer") : name
                        KoinHelper.authViewModel.registerWithEmail(
                            email: email,
                            pass: pass,
                            userName: finalName,
                            role: selectedRole
                        )
                    } else {
                        KoinHelper.authViewModel.loginWithEmail(email: email, pass: pass)
                    }
                } label: {
                    HStack {
                        if isAuthLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .padding(.trailing, 6)
                        }
                        Text(isRegistering ? "Create Account" : "Sign In")
                            .font(.headline)
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.green)
                    .cornerRadius(14)
                }
                .disabled(isAuthLoading)
            }
            .padding()
            .background(Color(uiColor: .secondarySystemBackground))
            .cornerRadius(16)
        }
    }

    // ── Data & Privacy Management Section ────────────────────
    @ViewBuilder
    private var privacySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Data & Privacy Management")
                .font(.headline)
                .foregroundColor(.primary)
                .padding(.leading, 4)
            
            VStack(spacing: 0) {
                // Privacy Policy Row
                NavigationLink {
                    PrivacyView()
                } label: {
                    HStack(spacing: 14) {
                        Image(systemName: "lock.shield.fill")
                            .font(.title3)
                            .foregroundColor(.blue)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Privacy Policy")
                                .font(.body)
                                .fontWeight(.medium)
                                .foregroundColor(.primary)
                            Text("Read how RAIL protects your data and privacy")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding()
                }
                
                Divider()
                    .padding(.leading, 48)

                // Clear Cache Row
                Button {
                    showClearCacheAlert = true
                } label: {
                    HStack(spacing: 14) {
                        Image(systemName: "sparkles")
                            .font(.title3)
                            .foregroundColor(.green)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Clear Cache")
                                .font(.body)
                                .fontWeight(.medium)
                                .foregroundColor(.primary)
                            Text("Free up local storage space on device")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding()
                }
                .alert("Clear Cache", isPresented: $showClearCacheAlert) {
                    Button("Clear", role: .destructive) {
                        statusToastMessage = "Cache cleared successfully."
                    }
                    Button("Cancel", role: .cancel) {}
                } message: {
                    Text("Are you sure you want to clear local cache? This will free up storage space on your device.")
                }
                
                Divider()
                    .padding(.leading, 48)
                
                // Anonymize My Data Row
                Button {
                    showDeleteDataAlert = true
                } label: {
                    HStack(spacing: 14) {
                        Image(systemName: "hand.raised.slash.fill")
                            .font(.title3)
                            .foregroundColor(.orange)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Anonymize My Data")
                                .font(.body)
                                .fontWeight(.medium)
                                .foregroundColor(.orange)
                            Text("Anonymize your data for research")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding()
                }
                .alert("Anonymize My Data", isPresented: $showDeleteDataAlert) {
                    Button("Proceed", role: .destructive) {
                        KoinHelper.profileViewModel.deleteUserData()
                        statusToastMessage = "Your data has been successfully anonymized."
                    }
                    Button("Cancel", role: .cancel) {}
                } message: {
                    Text("Are you sure you want to anonymize your data? This will unlink your identity from your previous scans. Your scans will become completely anonymous and will only be used to help train our AI models for the benefit of all farmers. This action cannot be undone.")
                }
                
                if currentUser != nil {
                    Divider()
                        .padding(.leading, 48)
                    
                    // Delete Account Row
                    Button {
                        showDeleteAccountAlert = true
                    } label: {
                        HStack(spacing: 14) {
                            Image(systemName: "person.crop.circle.badge.xmark")
                                .font(.title3)
                                .foregroundColor(.red)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Delete Account")
                                    .font(.body)
                                    .fontWeight(.medium)
                                    .foregroundColor(.red)
                                Text("Permanently delete account and profile data")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                    }
                    .alert("Delete Account", isPresented: $showDeleteAccountAlert) {
                        Button("Delete Account", role: .destructive) {
                            KoinHelper.authViewModel.deleteAccount()
                        }
                        Button("Cancel", role: .cancel) {}
                    } message: {
                        Text("Are you sure you want to permanently delete your account? This action cannot be undone and all your account information will be permanently removed.")
                    }
                }
            }
            .background(Color(uiColor: .secondarySystemBackground))
            .cornerRadius(16)
        }
        .padding(.top, 8)
    }
}
