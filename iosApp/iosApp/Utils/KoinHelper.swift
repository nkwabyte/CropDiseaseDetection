import Foundation
import ComposeApp

public struct KoinHelper {
    public static var appViewModel: AppViewModel {
        KoinDependencies.shared.getAppViewModel()
    }
    
    public static var authViewModel: AuthViewModel {
        KoinDependencies.shared.getAuthViewModel()
    }
    
    public static var detectionViewModel: DetectionViewModel {
        KoinDependencies.shared.getDetectionViewModel()
    }
    
    public static var profileViewModel: ProfileViewModel {
        KoinDependencies.shared.getProfileViewModel()
    }
    
    public static var settingsManager: SettingsManager {
        KoinDependencies.shared.getSettingsManager()
    }

    public static var historyViewModel: HistoryViewModel {
        KoinDependencies.shared.getHistoryViewModel()
    }

    /// The same singleton detector the DetectionViewModel uses, so the Settings
    /// screen's benchmarks time the models the app actually has loaded.
    public static var objectDetector: ObjectDetector {
        KoinDependencies.shared.getObjectDetector()
    }
}
