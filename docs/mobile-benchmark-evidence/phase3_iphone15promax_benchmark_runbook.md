# Phase 3 Physical-Device Benchmark Runbook — iPhone 15 Pro Max (secondary target)

Status: written 2026-09-14. NOT YET EXECUTED. Nothing in this file is a
measurement. Companion to
`supplementary/phase3_galaxy_a10_benchmark_runbook.md` (the primary-target
runbook, Samsung Galaxy A10) — read that file's sections 0 and 1 first; they
describe the measurement boundary and three-result-set discipline that apply
identically here and are not repeated in full below. See
sections/00_worklog.txt and BRAIN.md for how this fits into Phase 3, and
claim C005 in the claim-evidence matrix for what remains BLOCKED until real
physical-device runs come back.

## 0. Why a separate document

The iPhone 15 Pro Max is high-end silicon; the Galaxy A10 is low/mid-range.
Expect the two numbers to look nothing alike, and expect that gap itself to
be a legitimate, worth-reporting finding — a two-tier device story is more
honest than one number pretending to represent "mobile" performance in
general. The iOS instrumentation added 2026-09-14 mirrors the Android
extended benchmark field-for-field (`common/model/BenchmarkResult.kt` is
shared Kotlin Multiplatform code — both platforms serialize the exact same
`BenchmarkExport` schema), but several individual metrics are collected
through different native APIs on each platform and are NOT directly
comparable without reading the caveats below. Do not average or otherwise
combine Android and iOS figures without accounting for this.

## 1. A build-system defect found and fixed while adding this instrumentation

Before this revision, `ExecuTorchBridge.h` (the Objective-C header Kotlin/
Native's cinterop binds against) did not declare
`runLatencyBenchmarkForClassifierWithWarmupRuns:measuredRuns:` or
`runLatencyBenchmarkForDetectorWithInputSize:...` — both of which
`app/src/iosMain/.../ObjectDetector.kt` has called since the quick benchmark
was added (2026-09-14, Entry 007). Cinterop only sees what a header declares;
an `@objc` method implemented in Swift but missing from the header is
invisible to Kotlin, which means the iOS build most likely could not compile
Entry 007's quick-benchmark call site at all. This revision adds the missing
declarations (and everything new this revision needs) to the header. **This
means the iOS quick "Run Latency Benchmark" path may never have actually
been built successfully before now** — do not assume it "worked on iOS" just
because it was written; the very first iOS build attempt with this
instrumentation is also effectively the first real compile check either
benchmark path on iOS has ever had.

**UPDATE 2026-09-14 (worklog ENTRY 011): that first real compile check has now
happened, and the suspicion above was correct — the iOS build did not succeed as
written.** Beyond the header gap, three further defects had to be fixed before
Xcode would produce an app: `"%.1f".format(...)` in `commonMain`'s
`SettingsScreen.kt` and in `iosMain`'s `ObjectDetector.kt` is a JVM-only API that
does not compile for Kotlin/Native (replaced with a shared
`common/utils/NumberFormat.kt`); `iosMain/ObjectDetector.kt` was missing several
Foundation extension imports (`timeIntervalSince1970`, `writeToFile`,
`thermalState`); and `ExecuTorchBridge.swift`'s stage-timed methods put raw
`Double`s into `[NSNumber]` arrays at 18 sites.

The iOS app now builds and links:

```
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'platform=iOS Simulator,id=<simulator udid>' \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO build
# ** BUILD SUCCEEDED **
```

All 23 selectors declared in `ExecuTorchBridge.h` were confirmed present in the
linked binary's Objective-C metadata (`otool -v -s __TEXT __objc_methname` on
`iosApp.app/iosApp.debug.dylib` — note that with Xcode's debug-dylib layout the
`iosApp` stub binary contains none of the code). Re-run that check after any
change to the bridge: **a Kotlin compile passing is not evidence that iOS
works**, which is exactly how the original header gap survived.

**Known limitation:** Kotlin/Native *test* binaries cannot link in this project.
A test executable links on its own and inherits `-framework FirebaseCore` from
the GitLive Firebase dependency, but Firebase arrives here via Swift Package
Manager as static libraries rather than `.framework` bundles, so no search path
satisfies those flags. (The shipped framework is static, so its Kotlin linker
options never reach Xcode's link line — which is why the app itself builds
fine.) The `linkDebugTest<iosTarget>` tasks are therefore disabled explicitly in
`app/build.gradle.kts`, with the reason in a comment; the shared tests run on the
JVM target over the same `commonMain` code. Fixing this needs an iOS test host —
an Xcode test target, or Firebase supplied as XCFrameworks — not a build flag.

## 1a. CORRECTION (2026-09-14, ENTRY 012): the iOS app does NOT use the shared Compose UI

Entries 007 and 010 added the benchmark controls to the shared Compose settings
screen (`ui/screens/settings/SettingsScreen.kt`) and recorded that as covering
both platforms. **That was wrong for iOS, and any earlier instruction in this
runbook to "tap the button in Settings" did not apply to iOS builds before
2026-09-14.**

`iOSApp.swift` renders `ContentView()` — a native SwiftUI `TabView`
(Diagnostics / Encyclopedia / History / Settings / Account). `MainViewController()`,
the Compose entry point, is **never hosted anywhere** in the iOS target; the only
reference to that file from Swift is `MainViewControllerKt.doInitKoin()` in the
AppDelegate. Every iOS screen is a separate SwiftUI implementation under
`iosApp/iosApp/Views/`. The shared Compose UI is dead code on iOS. (Android is
unaffected — it does render the Compose UI, which is why the same buttons
worked there.)

A native **Developer — Benchmark** section now exists in
`iosApp/iosApp/Views/Settings/SettingsView.swift`. It calls the same shared
`ObjectDetector` functions, shows progress/errors/completion, badges whether the
run was a simulator or a physical device, and offers the JSON and CSV through a
share sheet.

## 1b. Verifying ExecuTorchBridge.h — the sound procedure (supersedes ENTRY 011)

**Do not** verify the bridge by looking for selector names in the linked binary
(`otool -v -s __TEXT __objc_methname`). Entry 011 did that and reported all
selectors present; the check is unsound, because `__objc_methname` contains every
selector *referenced* anywhere — including by the cinterop call site that calls
it — so a method with no implementation at all looks identical to an implemented
one. That is exactly how `syntheticJpegDataWithWidth:height:` passed the check
while being declared in the header and never implemented in Swift, crashing the
first device run with `unrecognized selector sent to instance`.

Compare the hand-written header against Xcode's **generated** header instead,
which lists precisely what Swift exports to Objective-C:

```bash
find <derivedDataPath> -name "iosApp-Swift.h"
# then diff the selectors in @interface ExecuTorchBridge ... @end
# against those declared in iosApp/iosApp/ExecuTorchBridge.h
```

Expect zero *declared-but-not-implemented* entries. As of 2026-09-14: 23
declared, 23 implemented. Re-run this after ANY change to the bridge — a Kotlin
compile passing proves nothing here, because cinterop trusts the header.

## 1b-2. Schema v4: raw output is a contiguous buffer — pre-v4 iOS latency is NOT comparable

As of 2026-09-14 (worklog ENTRY 014) the bridge returns the detector's raw output
as a contiguous `Float32` `NSData` buffer instead of a boxed `[NSNumber]` array.
Boxing cost ~226,800 objects per call and ~11.7 MB retained per call, which is
what drove the app past 1.5 GB and got it killed by jetsam on this device.

Two consequences when reading exports:

1. **Do not compare iOS `detectorInference` across the v3/v4 boundary.** The
   boxing sat *inside* the measured span. Same simulator, same images:
   32.06 → 19.25 ms, 31.06 → 19.34 ms, 32.26 → 19.01 ms — roughly **40% of the
   pre-fix figure was boxing, not inference**. The model did not get faster.
   Android was never affected (it has always used `getDataAsFloatArray()`), so
   Android-vs-iOS detector comparison is only valid from v4 onward.
2. **`outputTransfer` is a sub-component of `detectorInference`, not an addition.**
   Both platforms define `detectorInference` as model forward **plus**
   materializing a usable contiguous primitive buffer. Never add the two.

Equivalence and memory evidence:
`supplementary/phase3_ios_simulator_schema_v4_2026-09-14/`.

Two extra DEBUG-only launch arguments exist for re-verification after any change
to the bridge or the decode path:

```bash
xcrun simctl launch --console-pty <udid> com.nkwabyte.cropdiseasedetection --run-equivalence-check
xcrun simctl launch --console-pty <udid> com.nkwabyte.cropdiseasedetection --run-memory-soak
```

The equivalence check prefers a real photograph at
`Documents/equivalence_input.jpg` (the synthetic pattern yields no detections and
so cannot exercise the detection-level half of the comparison).

## 1c. The screen must stay on

A full protocol run takes minutes with no touch input. If the display
auto-locks, iOS backgrounds the app and kills the suspended CPU-bound process.
It presents as a crash but leaves **no crash report and no JetsamEvent**, so do
not go looking for one. The app now holds `isIdleTimerDisabled` for the duration
of a run (both the button and the headless path), but still: start the run with
the phone unlocked and leave it alone until the completion summary appears.

Related: an app launched programmatically with
`xcrun devicectl device process launch` is **not** brought to the foreground, so
it gets roughly 25 seconds of background execution and is then killed — measured
repeatedly, with the app dying identically whether devicectl stays attached
(`--console`) or exits. **A physical-device run cannot be driven purely by remote
launch.** Tap the button on the device, or have the app already foregrounded.

## 1d. Scripted runs (DEBUG builds only)

Apple's first-party tooling can synthesize no touch input on a simulator or a
device (`simctl` and `devicectl` both have no tap/input subcommand, there is no
XCUITest target here, and `osascript` clicking needs assistive access). Two
DEBUG-only launch arguments exist so iOS can be exercised from a script at all.
Both are compiled out of Release builds and inert without the argument:

```bash
# Simulator — runs the full protocol headlessly and prints where it wrote:
xcrun simctl launch --console-pty <sim-udid> com.nkwabyte.cropdiseasedetection \
  --run-extended-benchmark

# Open straight to the Settings tab (for screenshots):
xcrun simctl launch <sim-udid> com.nkwabyte.cropdiseasedetection --open-settings
```

`--run-extended-benchmark` calls the same shared implementation the button calls.
On a physical device it is subject to the foreground limitation in 1c above.

## 1d-2. Never trust BUILD SUCCEEDED alone

Verify the packaged Kotlin framework is actually the one you just built:

```bash
shasum -a 256 app/build/bin/iosArm64/debugFramework/ComposeApp.framework/ComposeApp
shasum -a 256 <derivedDataPath>/Build/Products/Debug-iphoneos/ComposeApp.framework/ComposeApp
```

The hashes must be identical. Xcode skipped the Kotlin phase entirely for a
while (fixed with `alwaysOutOfDate = 1`), producing successful builds that ran
stale Kotlin. `strings` cannot see Kotlin/Native literals, so marker searches
prove nothing — compare hashes.

## 1e. Retrieving the export from a physical device

```bash
xcrun devicectl device copy from --device <udid> \
  --domain-type appDataContainer \
  --domain-identifier com.nkwabyte.cropdiseasedetection \
  --source Documents --destination <local-dir>
```

Then confirm `deviceEnvironment.isEmulator == false` before citing anything.
The in-app completion summary badges this too.

## 2. Prerequisite: build a fresh IPA/run with today's instrumentation

Neither the quick nor the extended iOS benchmark has been built or run yet.
Building requires:

- A Mac with Xcode installed, the project opened via
  `iosApp/iosApp.xcodeproj` (or the KMP-generated workspace/scheme that
  wraps it).
- The iPhone 15 Pro Max connected via cable or on the same network (for
  wireless debugging), unlocked, and trusted for this Mac.
- Code signing configured (an Apple Developer account, free or paid, is
  enough for on-device debug builds — no App Store distribution needed).
- Build and run directly onto the device from Xcode (Product > Run, with the
  iPhone selected as the destination). There is no "just hand me an .ipa"
  shortcut without going through an actual archive+export step, which is not
  necessary for benchmarking.
- Build from a clean git working tree (or at least commit first) so
  `BuildKonfig.BUILD_GIT_SHA` — used for the export's `deviceEnvironment
  .buildIdentifier` field — reflects the exact code that produced the
  results. This session's Linux VM bridge to the Mac cannot build iOS
  targets at all (no Xcode, no macOS toolchain reachable from a Linux
  container), so this step is entirely the Mac owner's to run, same as the
  Android build.

## 3. What the extended export captures on iOS, and where it differs from Android

See the Galaxy A10 runbook's section 2a for the full protocol-requirement
table; only the iOS-specific column is repeated/expanded here.

| Metric | iOS source | How it differs from Android |
|---|---|---|
| Image decode / orientation / preprocess / inference timing | `ExecuTorchBridge.runClassificationStageTimed` / `runDetectionStageTimed` / `runDetectionStretchedStageTimed`, timed with `DispatchTime.now().uptimeNanoseconds` (monotonic) | **iOS's preprocessing pipeline DOES correct EXIF orientation** (`UIImage.fixOrientation()`, inside `preprocessCHW`) before resizing. Android's does not. So `orientationCorrectionMs` is a REAL, non-zero measurement on iOS and is ALWAYS 0.0 on Android — this is a genuine pre-existing cross-platform asymmetry the benchmark surfaced, not a benchmark artifact, and is worth fixing on Android independently of this work. |
| Postprocessing/NMS | Timed in Kotlin (`decodeAndFilterDetections` + `nonMaxSuppression`), same functions `detect()` calls | Same approach as Android — box decode + NMS timed together as one field. |
| CPU utilization | `ExecuTorchBridge.processCpuTimeMs()` via `getrusage(RUSAGE_SELF)` | **Process-level**, not per-thread. Android's `Debug.threadCpuTimeNanos()` proxy is scoped to the calling thread only. The two `cpuUtilization.utilizationRatio` figures are NOT directly comparable — iOS's reflects all app threads, Android's reflects one. |
| Memory | `ExecuTorchBridge.residentMemoryBytes()` via `mach_task_basic_info.resident_size` | RSS-like, not PSS. iOS exposes no PSS-equivalent to third-party app code. Stored in the shared schema's `totalPssKb` field for lack of a dedicated slot, with an explicit per-sample `note` saying so — read it before comparing to Android's real PSS figure. |
| Thermal state | `ProcessInfo.processInfo.thermalState` (nominal/fair/serious/critical) | Different vocabulary from Android's PowerManager scale (none/light/moderate/severe/critical/emergency/shutdown) — do not assume the two scales line up 1:1 at matching severity words. Meaningless on the Simulator, same caveat as Android's emulator. |
| Installed app size | `ExecuTorchBridge.installedAppSizeBytes()` — sum of file sizes under the app's own `.app` bundle | Best-effort, like Android's APK-file-size figure: excludes on-device install-time optimizations and App Store thinning already applied, and is not what Settings > General > iPhone Storage reports. |
| Model hashes | `ExecuTorchBridge.sha256Hex(ofFileAtPath:)` (CryptoKit `SHA256`) | Same SHA-256 algorithm as Android; the actual `.pte` files are the same model artifacts shipped to both platforms, so hashes SHOULD match across platforms for the same model — a mismatch would itself be a finding worth investigating (a build packaging a different model file per platform). |
| Device details | `UIDevice.currentDevice` + `ExecuTorchBridge.deviceModelIdentifier()` (raw hardware id, e.g. `iPhone16,2`) + `cpuArchitecture()` + `isRunningOnSimulator()` | `deviceModelIdentifier()` is the specific hardware string, not the generic `UIDevice.model` ("iPhone") — use it to confirm you're actually on an iPhone 15 Pro Max and not, say, a different test device. |
| Build identifier | `BuildKonfig.BUILD_GIT_SHA` | Same shared Kotlin Multiplatform field as Android — one source of truth for both platforms' git SHA, embedded at build time. |

## 3a. Behaviour changes of 2026-09-14 that affect how iOS exports are read

The routing and orientation changes described in the Galaxy A10 runbook's
section 2b apply to iOS too — the pipeline that decides them
(`DetectionPipeline` / `CropRoutingPolicy`) lives in `commonMain` and is shared,
so both platforms make the same decision from the same classifier verdict. iOS
exports therefore also carry `endToEndByPath`, per-path
`detectorExecutedCount`/`detectorSkippedCount`, and a per-path `stageBreakdown`
with a genuinely measured `routing` stage (schema v3).

iOS-specific points:

- **iOS classifier timings captured before 2026-09-14 are inflated.**
  `postprocessClassification()` printed a line on every call and runs inside the
  span `classifyStageTimed()` measures, so every classifier measurement included
  a `println` — very expensive when stdout is piped over a device console during
  a 100-run protocol. Android had no equivalent per-call log, so the two
  platforms' `classifierInference` figures were not measuring the same work.
  Removed in ENTRY 012. Do not compare pre-2026-09-14 iOS classifier numbers
  against Android's, or against later iOS runs.
- **Check `cpuUtilization.measuredOnThread` before pooling runs.** Both
  benchmarks now run on `Dispatchers.Default`, the dispatcher production
  inference uses. Earlier runs used whatever dispatcher the caller was on.

- **Orientation was already correct on iOS** and was not changed. `preprocessCHW`
  has always called `UIImage.fixOrientation()`, so `orientationCorrection` was
  already a real measurement here. What changed is that Android now matches it,
  so the two platforms' `orientationCorrection` figures are finally measuring
  the same work — and the Android-vs-iOS asymmetry that C025 recorded is gone.
- **One new bridge method**, `orientedImageSizeWithImageData:`, reports the
  image's post-orientation dimensions using the same width/height swap rule
  Android applies. It runs no model and adds no inference cost.
- **iOS pixel behaviour was otherwise untouched.** Any difference between an iOS
  export from before and after 2026-09-14 is a change in what is *reported*, not
  in what is *computed*.

## 4. Fixed test flow

Follow the Galaxy A10 runbook's section 3 flow with these iOS-specific
substitutions:

1. Confirm exactly one iPhone 15 Pro Max is selected as the Xcode run
   destination.
2. Record before starting: `deviceModelIdentifier()` should read something
   like `iPhone16,2` for the iPhone 15 Pro Max — cross-check this against
   Apple's published device-identifier list, since `UIDevice.model` alone
   only ever says the generic "iPhone" and cannot distinguish models. Also
   record iOS version, free storage, battery level, and wall-power vs.
   battery state.
3. Run the build directly from Xcode onto the device (Product > Run).
4. Force-quit the app if it was already running (swipe up in the app
   switcher), then relaunch fresh from the home screen.
5. Navigate: Settings > scroll to "Developer — Benchmark" >
   **"Run Extended Benchmark (Publication Protocol)"** (once that row exists
   in the shared Compose UI on iOS — this project uses Compose Multiplatform
   for the UI layer, so the same `SettingsScreen.kt` renders on both
   platforms; confirm the row actually appears before assuming otherwise).
6. Wait for the snackbar / on-screen summary confirming completion.
7. Pull the exported files from the app's Documents directory via Xcode's
   Devices & Simulators window ("Download Container…"): look for
   `extended_benchmark_<timestamp>.json` and
   `extended_benchmark_<timestamp>.csv`. The quick button's
   `benchmark_<timestamp>.csv` uses a different filename pattern and is left
   untouched alongside it if both have been run.
8. **Read the `notes` array in the JSON before doing anything else with the
   numbers** — same discipline as Android, and doubly important here given
   how many fields above differ in definition from their Android
   counterparts.
9. Confirm `deviceEnvironment.isEmulator` is `false` and
   `deviceEnvironment.model` reads a real iPhone hardware identifier, not a
   Simulator string (Simulator identifiers look like `x86_64`/`arm64` host
   strings rather than `iPhoneNN,N`).
10. Repeat the whole run 2-3 times (fresh app launch each time) to observe
    thermal drift across the repeated runs — iPhones throttle aggressively
    under sustained load, and `deviceEnvironment.thermalStatus` across the
    repeated runs is exactly the signal to look at.
11. Capture the Xcode console output for each run (it mirrors every `notes`
    entry as well as the completion summary line) alongside the exported
    files, in case a run partially failed.

## 5. Driving it via automation instead of Xcode's UI

There is currently no MCP tool connected to this session that can drive a
physical iPhone (the `android-emulator` MCP is Android-only; no equivalent
iOS automation server is connected). If one becomes available, the same
measurement-boundary caveat from the Galaxy A10 runbook's section 1 applies
without exception: the automation layer's own round-trip time is never a
substitute for the in-app monotonic timing this benchmark already provides.
Absent such a tool, running this by hand from Xcode (as in section 4) is the
only currently available path, and is a fully adequate one — no MCP-driven
automation is actually required to produce citable evidence here.

## 6. After this device is measured

Bring the iPhone 15 Pro Max exports back for analysis together with the
Galaxy A10 exports, the Android emulator harness-validation exports, and the
Gradio-side desktop/server benchmark CSVs
(`outputs/benchmarks/gradio_latency/` in the ML repo). Report all of them as
separate, honestly-labeled rows, cross-referencing section 3's table above
whenever a metric's definition differs between platforms — never present a
cross-platform metric pair (e.g. Android PSS vs iOS resident-memory) as if
they were the same measurement without that caveat attached. Update claim
C005 (and add new claims for the actual measured latency figures on both
platforms) only once at least one physical-device export exists per
platform; instrumentation and harness-testing on their own, however complete
the metric coverage, do not unblock C005.
