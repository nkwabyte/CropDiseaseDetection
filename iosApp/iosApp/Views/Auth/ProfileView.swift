import SwiftUI
import ComposeApp

public struct ProfileView: View {
    @StateObject private var authStateObs = ObservableFlow(KoinHelper.authViewModel.authState)
    @StateObject private var profileStateObs = ObservableFlow(KoinHelper.profileViewModel.userProfileState)
    
    @State private var emailInput: String = ""
    @State private var passwordInput: String = ""
    @State private var isRegistering: Bool = false
    
    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    let user = profileStateObs.value
                    
                    // User Profile Details
                    VStack(spacing: 12) {
                        Image(systemName: "person.crop.circle.fill")
                            .font(.system(size: 72))
                            .foregroundColor(.green)
                        
                        Text(user.userName)
                            .font(.title2)
                            .fontWeight(.bold)
                        
                        if let email = user.userEmail {
                            Text(email)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        
                        HStack(spacing: 16) {
                            VStack {
                                Text(user.detections)
                                    .font(.title3)
                                    .fontWeight(.bold)
                                Text("Detections")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            
                            Divider()
                                .frame(height: 30)
                            
                            VStack {
                                Text(user.successRate)
                                    .font(.title3)
                                    .fontWeight(.bold)
                                    .foregroundColor(.green)
                                Text("Success Rate")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding(.top, 4)
                        
                        Text("Role: \(user.role.name)")
                            .font(.caption)
                            .fontWeight(.semibold)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Capsule().fill(Color.green.opacity(0.15)))
                            .foregroundColor(.green)
                    }
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(Color(uiColor: .secondarySystemBackground))
                    .cornerRadius(16)
                    
                    // Login / Registration Form Section
                    VStack(spacing: 16) {
                        Image(systemName: "lock.shield.fill")
                            .font(.system(size: 40))
                            .foregroundColor(.green)
                        
                        Text(isRegistering ? "Register Account" : "Account Sign In")
                            .font(.title3)
                            .fontWeight(.bold)
                        
                        VStack(spacing: 12) {
                            TextField("Email Address", text: $emailInput)
                                .keyboardType(.emailAddress)
                                .autocapitalization(.none)
                                .padding()
                                .background(Color(uiColor: .secondarySystemBackground))
                                .cornerRadius(10)
                            
                            SecureField("Password", text: $passwordInput)
                                .padding()
                                .background(Color(uiColor: .secondarySystemBackground))
                                .cornerRadius(10)
                        }
                        
                        Button {
                            if isRegistering {
                                KoinHelper.authViewModel.registerWithEmail(email: emailInput, pass: passwordInput, userName: "Farmer", role: UserRole.farmer)
                            } else {
                                KoinHelper.authViewModel.loginWithEmail(email: emailInput, pass: passwordInput)
                            }
                        } label: {
                            Text(isRegistering ? "Sign Up" : "Sign In")
                                .font(.headline)
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.green)
                                .cornerRadius(14)
                        }
                        
                        Button {
                            isRegistering.toggle()
                        } label: {
                            Text(isRegistering ? "Already have an account? Sign In" : "Don't have an account? Sign Up")
                                .font(.subheadline)
                                .foregroundColor(.green)
                        }
                    }
                    .padding()
                    .background(Color(uiColor: .secondarySystemBackground))
                    .cornerRadius(16)
                }
                .padding()
            }
            .navigationTitle("Account")
        }
    }
}

