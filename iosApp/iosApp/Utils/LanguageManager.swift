import SwiftUI

public class LanguageManager: ObservableObject {
    public static let shared = LanguageManager()

    @Published public var currentLanguage: String {
        didSet {
            UserDefaults.standard.set(currentLanguage, forKey: "selected_app_language_code")
        }
    }

    private var translations: [String: [String: String]] = [:]

    private init() {
        self.currentLanguage = UserDefaults.standard.string(forKey: "selected_app_language_code") ?? "en"
        loadTranslations()
    }

    /// Loads the translation table shipped as a raw resource.
    ///
    /// `Localizations.json` is copied into the bundle verbatim. A `.xcstrings`
    /// catalog cannot be used here: Xcode compiles string catalogs into
    /// `.lproj/.strings` at build time, so the source file never reaches the
    /// bundle and every lookup silently falls back to the English key.
    public func loadTranslations() {
        let candidates: [(String, String)] = [
            ("Localizations", "json"),
            ("Localizable", "xcstrings")
        ]
        for (name, ext) in candidates {
            if let url = Bundle.main.url(forResource: name, withExtension: ext),
               let data = try? Data(contentsOf: url) {
                parseJsonData(data)
                if !translations.isEmpty { return }
            }
        }
    }

    private func parseJsonData(_ data: Data) {
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let stringsDict = json["strings"] as? [String: [String: Any]] else {
            return
        }

        var map: [String: [String: String]] = [:]
        for (key, valDict) in stringsDict {
            if let localizations = valDict["localizations"] as? [String: [String: Any]] {
                for (lang, langData) in localizations {
                    if let stringUnit = langData["stringUnit"] as? [String: Any],
                       let value = stringUnit["value"] as? String {
                        if map[lang] == nil { map[lang] = [:] }
                        map[lang]?[key] = value
                    }
                }
            }
        }
        self.translations = map
    }

    public func localize(_ key: String) -> String {
        if currentLanguage == "en" { return key }
        if let translated = translations[currentLanguage]?[key] {
            return translated
        }
        return key
    }

    public func setLanguage(_ code: String) {
        guard code != currentLanguage else { return }
        self.currentLanguage = code
    }
}

/// Localized `String` lookup, for APIs that require a `String` rather than a `View`
/// (navigation titles, alert titles, text field placeholders, picker labels).
///
/// Views calling this must observe `LanguageManager.shared` so they redraw on a
/// language switch — see the `langMgr` property on each screen.
public func L(_ key: String, _ args: CVarArg...) -> String {
    let raw = LanguageManager.shared.localize(key)
    if args.isEmpty { return raw }
    return String(format: raw, arguments: args)
}

/// Localized `Text`. Self-observing, so it redraws on a language switch without
/// the enclosing view having to observe anything.
public struct LText: View {
    private let key: String
    private let args: [CVarArg]
    @StateObject private var langMgr = LanguageManager.shared

    public init(_ key: String, _ args: CVarArg...) {
        self.key = key
        self.args = args
    }

    public var body: some View {
        let raw = langMgr.localize(key)
        if args.isEmpty {
            Text(raw)
        } else {
            Text(String(format: raw, arguments: args))
        }
    }
}
