import Foundation
import Combine
import ComposeApp

/// Swift ObservableObject wrapper for SKIE-generated Kotlin StateFlow properties using Swift AsyncSequence.
/// Enables seamless SwiftUI `@StateObject` and `@ObservedObject` state bindings.
@MainActor
public final class ObservableFlow<T: AnyObject>: ObservableObject {
    @Published public private(set) var value: T
    private var task: Task<Void, Never>?

    public init(_ flow: SkieSwiftStateFlow<T>) {
        self.value = flow.value
        self.task = Task { [weak self] in
            for await newValue in flow {
                guard let self = self else { break }
                self.value = newValue
            }
        }
    }

    deinit {
        task?.cancel()
    }
}

