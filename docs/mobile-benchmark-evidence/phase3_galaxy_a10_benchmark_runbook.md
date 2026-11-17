> # ⛔ SUPERSEDED — DEVICE NOT USED
>
> **The Samsung Galaxy A10 was removed from the physical-device evaluation by
> owner decision on 2026-09-14 and was never measured.** This document is
> retained as historical planning material only.
>
> **No Galaxy A10 measurement exists anywhere in this project.** Nothing in this
> file may be cited as a result, and its device-tier assumptions, expected
> performance and hardware specifications must not be carried over to the
> replacement device.
>
> **Active runbook:** `phase3_redmi_note_14_pro_plus_benchmark_runbook.md`
> (Xiaomi Redmi Note 14 Pro+ 5G, model `24115RA8EG`, codename `amethyst`).
> The Xiaomi is **not** equivalent to the Galaxy A10 — it is a different device
> of a different class, and the substitution changes what the mobile-performance
> claim is about.
>
> The measurement-boundary rule in section 1 below is the one part of this
> document that carries forward unchanged; it is restated in the active runbook.

# [SUPERSEDED — DEVICE NOT USED] Phase 3 Physical-Device Benchmark Runbook — Samsung Galaxy A10 (never measured)

Status: written 2026-09-14, revised 2026-09-14 (same day, after the Android
instrumentation was extended to the publication protocol's full metric list).
NOT YET EXECUTED on either physical device. Nothing in this file is a
measurement. See sections/00_worklog.txt (Entries 007-010) and BRAIN.md for
how this fits into Phase 3, and claim C005 in the claim-evidence matrix for
what remains BLOCKED until real physical-device runs come back.

## 0. Why this document exists

The mobile app's README previously stated "~18ms"/"~24ms"/"sub-100ms" latency
with zero instrumentation behind any of the three numbers (C005). Real
instrumentation now exists on both the classifier and detector paths
(`common/model/BenchmarkResult.kt`, `ObjectDetector.runLatencyBenchmark()` /
`ObjectDetector.runExtendedBenchmark()`, triggered from Settings > Developer).
This runbook is how to run the FULL publication-protocol benchmark on the two
devices the project's stated target hardware calls for, and how to get the
results back into the evaluation pipeline without contaminating them with
measurements that only look like latency numbers.

Two benchmark actions exist in the app; **use the second one for anything
that will be cited**:

- **"Run Latency Benchmark"** — the original quick developer sanity check.
  10 warm-up + 50 measured runs, classifier + detector only, writes
  `benchmark_<timestamp>.csv`. Useful for "is the button working" but does
  NOT meet the publication protocol's run-count or metric-coverage
  requirements.
- **"Run Extended Benchmark (Publication Protocol)"** — added 2026-09-14.
  10 warm-up + 100 measured runs per condition (both configurable in code,
  but the button uses the protocol defaults), and captures every metric in
  section 2a below. Writes `extended_benchmark_<timestamp>.json` and
  `extended_benchmark_<timestamp>.csv` — a different filename pattern from
  the quick button's export, so neither overwrites the other and prior
  evidence is never at risk.

## 1. The measurement boundary — read this before touching a device

Whatever tool drives the device — an MCP-based automation layer, `adb` by
hand, or Xcode's device window — its own round-trip time (the time between
"tap this button" and the tool getting a response) is **not** application
inference latency. It includes IPC overhead, screenshot capture, UI-tree
diffing, and (for a remote automation layer) network hops. None of that
belongs in a latency claim.

The only valid source of truth for this project's latency numbers is the
in-app instrumentation: `ObjectDetector.runExtendedBenchmark()` (or, for the
quick check, `runLatencyBenchmark()`) timing itself with `System.nanoTime()`
(Android) / `CFAbsoluteTimeGetCurrent()` (iOS), inside the process, around
the actual `classify()`/`detect()` calls those functions call directly — the
extended benchmark's stage-level timers wrap the same private helper
functions `classify()`/`detect()` call, so the timed code path is identical
to what a normal detection does. The automation layer's only job is to get
the app into the right state and press the right button — never to be timed
itself.

Keep three result sets separate and always labeled:

1. **Android emulator** (`Medium_Phone` AVD, arm64, Android 17/API 37) —
   useful for verifying the harness (does the button work, does the export
   get written, are the numbers the right order of magnitude, do the
   caveat-flags in `notes` fire correctly) but explicitly NOT representative
   of real mobile silicon. Never cite emulator numbers as device performance.
   `deviceEnvironment.isEmulator` is `true` for exactly this reason, and
   `notes` will always contain an explicit "this export was produced on an
   ANDROID EMULATOR" line when it is.
2. **Samsung Galaxy A10** (physical, primary target per the project's stated
   hardware) — a low/mid-range real device; this is the number that matters
   most for the "does this actually work on the hardware Ghanaian farmers
   would have" claim.
3. **iPhone 15 Pro Max** (physical, secondary) — the project's stated iOS
   target; high-end silicon, so expect it to look nothing like the Galaxy
   A10 number, which is itself a legitimate and worth-reporting finding (a
   two-tier device story is more honest than one number pretending to
   represent "mobile").

## 2. Prerequisite: build a fresh APK/IPA with today's instrumentation

The extended-benchmark code added 2026-09-14 has not been built into any
installed binary yet. Any previously-installed
`com.nkwabyte.cropdiseasedetection` package predates it and will not show the
"Run Extended Benchmark (Publication Protocol)" row — it must be rebuilt
first. The build also needs to embed a real git commit SHA into
`BuildKonfig.BUILD_GIT_SHA` (added this revision) for the export's
`buildIdentifier` field to be meaningful — build from a clean git working
tree, or at least commit before building, so the embedded SHA reflects the
code that produced the results.

This session cannot do the build itself: its Linux VM bridge to the Mac has
no Android SDK (`ANDROID_HOME` unset, no `adb`) and no network path to
`services.gradle.org` (confirmed — `./gradlew tasks` fails with
`UnknownHostException` before it can even fetch the Gradle distribution). The
build has to happen directly on the Mac:

- **Android**: open the project in Android Studio and Build > Build Bundle(s)
  / APK(s) > Build APK(s), or run `./gradlew assembleDebug` (debug is fine —
  the instrumentation itself doesn't depend on build type; a release/profile
  build additionally exercises code-shrinking/R8, which is a legitimate
  reason to ALSO run a release build once, but is not required to get valid
  latency numbers) in a terminal with the Android SDK on its PATH. Output
  lands at `app/build/outputs/apk/debug/app-debug.apk` (or
  `.../release/app-release.apk`).
- **iOS**: open `iosApp/iosApp.xcodeproj` (or the KMP-generated workspace) in
  Xcode and build+run directly onto the connected iPhone 15 Pro Max (Xcode
  handles code signing/provisioning; there is no equivalent "just hand me an
  .ipa" shortcut without an Apple Developer account export step). Requires
  the iOS mirror of the extended benchmark (see the iOS benchmark
  implementation notes, this revision) to be built first.

## 2a. What the extended export captures, and what it flags instead of faking

`BenchmarkExport` (see `common/model/BenchmarkResult.kt`) is the JSON/CSV
schema. Every field below maps directly to one item in the publication
protocol's required-metric list. Where Android (or iOS) cannot measure a
metric defensibly, the export says so explicitly in `notes` rather than
omitting the field silently or filling it with an invented value — **read
`notes` before citing any figure from an export.**

| Protocol requirement | Captured as | Defensibility |
|---|---|---|
| Cold model-loading time | `coldLoad.classifierColdLoad` / `.detectorColdLoad` | Real: `release()` forces a genuine cold path before each of 5 repetitions, monotonic-timed. |
| Image decoding | `classifierStage.imageDecode` / `detectorStageBySize.*.imageDecode` | Real: timed `BitmapFactory.decodeByteArray` call, same call `classify()`/`detect()` make. |
| Orientation correction | `*.orientationCorrection` | **Real since 2026-09-14 (C027).** Times EXIF parsing plus the bitmap transformation in `ImageOrientation.decodeUpright()`, the single decode path both models use. It was a hardcoded `0.0` before that date because Android performed no orientation correction at all (C025); a `0.0` here in any export predates the fix. |
| Preprocessing | `*.preprocess` | Real: timed resize/letterbox + float32 normalization, same calls production makes. |
| Classifier inference | `classifierStage.classifierInference` | Real: timed `Tensor.fromBlob` + `module.forward()`. |
| Routing | `endToEndByPath.*.stageBreakdown.routing` | **Real since 2026-09-14 (C026).** Routing is now an explicit step in `DetectionPipeline` — `CropRoutingPolicy.decide()` plus the class-id filter — timed per run and reported per routing path. It is genuinely small (microseconds), which is expected: the cost of the two-stage design is the classifier pass, not the decision. `classifierStage.routing` remains `0.0` by construction, because that benchmark measures the classifier alone, with no routing step in it. |
| Detector inference | `detectorStageBySize.*.detectorInference` | Real, per source-image resolution. |
| Postprocessing/NMS | `detectorStageBySize.*.postprocessNms` | Real: timed box-decode-from-raw-output + `nonMaxSuppression()`, same calls production makes. |
| Complete end-to-end latency | `endToEndByPath[<path>].stats` (and `endToEnd`, retained) | Real: times `DetectionPipeline.run()`, the same entry point `DetectionViewModel.detect()` drives — see `observedStageOrder`. **Since 2026-09-14 there is one entry per routing path and they are NOT comparable:** only `accepted_supported_crop` includes detector inference; `rejected_out_of_distribution` and `selected_crop_mismatch` stop at the classifier and record `detectorExecutedCount = 0`. Never average them or quote one without naming its path. |
| Detector actually executed | `endToEndByPath.*.detectorExecutedCount` / `.detectorSkippedCount`, and `detectorExecuted` per run | Real: recorded for every measured run. This is the machine-checkable evidence that the fail-closed paths skip the detector rather than running it and discarding the output. |
| Mean/median/stddev/p90/p95/p99/min/max | Every `LatencyStats` object | Real, computed by `computeLatencyStats()`. |
| Memory | `memorySamples` (PSS, native heap, java heap) | Real but sampled only before/after the measured loop, never inside it, so it cannot capture an in-inference peak — flagged in `notes`. |
| CPU utilization | `cpuUtilization` | Real but a single-thread proxy (`Debug.threadCpuTimeNanos()` vs wall clock) — ratio can legitimately exceed 1.0 under multi-threaded XNNPACK kernels; flagged in `notes`, not a bug. |
| Thermal state | `deviceEnvironment.thermalStatus` | Real on API 29+ physical devices. Unsupported below API 29 (flagged). On an emulator, always flagged unreliable regardless of API level — no real thermal sensor. |
| Model size | `modelArtifacts[].sizeBytes` | Real: file size of the cached `.pte` asset. |
| Installed application size | `deviceEnvironment.installedAppSizeBytes` | Best-effort: base APK file size (`ApplicationInfo.sourceDir`), NOT a `StorageStatsManager` figure — flagged in `notes` as an undercount (excludes unpacked odex/vdex, split APKs, app-private data). |
| Offline success/failure counts | `endToEnd.offlineSuccessCount` / `.offlineFailureCount` / `.failureMessages` | Real: counts exceptions thrown during each timed end-to-end call. "Offline" = no network call occurs anywhere in this path. |
| Build identifier | `deviceEnvironment.buildIdentifier` | Real once built from the updated `build.gradle.kts` (`BuildKonfig.BUILD_GIT_SHA`, embedded git commit SHA at build time). Reads a generated-but-unpopulated value only if built before this revision. |
| Model hashes | `modelArtifacts[].sha256` | Real: SHA-256 of the cached model file bytes. |
| Device details | `deviceEnvironment` (manufacturer/model/OS/ABI/RAM/battery/emulator flag) | Real, from `android.os.Build` + `ActivityManager` + `BatteryManager`. |
| Input-image manifest/checksums | `imageManifest[]` | Real: SHA-256 + size of every synthetic benchmark image used. Note: these are deterministic synthetic images for LATENCY only, not the real checksum-locked images used for the cross-platform output-agreement procedure. |

## 2b. What the app actually does as of 2026-09-14 (read this before interpreting any export)

Two behaviours changed on 2026-09-14 (worklog ENTRY 011; claims **C026** and
**C027**, superseding C024 and C025). Exports captured before that date describe
a different application, and the two sets must not be pooled.

**Routing (C026, was C024).** The app now runs the documented two-stage
pipeline, in this order:

1. ensure the classifier is loaded (idempotent);
2. decode the image and apply EXIF orientation;
3. classifier preprocess → inference → postprocess;
4. **routing decision**;
5. only if routing permits: ensure the detector is loaded (idempotent);
6. detector preprocess → inference → postprocess;
7. filter detections to the routed crop's exact class ids (Corn 0–4, Pepper
   5–14, Tomato 15–22).

Three outcomes fail closed — the detector is **neither loaded nor invoked**:
the classifier rejects the image (learned `Other`, or below the confidence
floor); the classifier accepts a crop the user did not select; or the classifier
produces no usable result at all. Previously the detector ran *first* and the
classifier's verdict was used only to set a UI flag, so an out-of-distribution
photo still paid for a full detector pass.

**Consequences for benchmarking.** A latency figure is now meaningless without
its path. Rejection latency is classifier-only and will look dramatically faster
than the accepted path; that is the architecture working, not a speed
improvement. Which paths a given run can measure depends on the classifier's
actual verdict on the benchmark image — the benchmark probes for it and reports
unreachable paths as **absent, with a note**, never as zeros. With the
deterministic synthetic benchmark images the classifier rejects the input, so
only the rejected path is measurable; measuring the accepted path needs a real
photograph, which the latency benchmark deliberately does not use (see section
2a's note on synthetic images).

**Orientation (C027, was C025).** Android now applies EXIF orientation for all
eight defined values, matching iOS's `UIImage.fixOrientation()` semantics
(`common/model/ExifOrientationContract.kt` states the shared definition). The
correction happens **once, at the camera/gallery ingest point** — that is where
the information was actually being lost, because the picked image was
re-compressed to JPEG after an EXIF-blind decode, discarding the tag before the
detector ever saw the bytes. The re-encoded image carries no orientation tag, so
nothing downstream rotates it a second time. Classifier, detector and the
bounding-box overlay all consume the same upright pixels.

**Consequences for benchmarking.** `orientationCorrection` is now a real,
non-zero measurement on Android (sub-millisecond on the synthetic images, but
genuinely measured). The previously reported constant `0.0` was the signal that
led to C025 in the first place.

## 3. Fixed test flow (same on emulator, Galaxy A10, and iPhone)

Run this identical sequence on every device so the only thing that varies is
the hardware:

1. Confirm exactly one authorized/target device is selected (`adb devices`
   shows exactly one line for Android; Xcode's device dropdown shows the
   iPhone for iOS).
2. Record before starting: device model, OS version, build number, free RAM,
   battery level, and whether the phone is on wall power or battery (a
   benchmark run in the two states is not guaranteed comparable — thermal
   throttling behaves differently). Most of this is captured automatically
   in `deviceEnvironment`, but write it down independently too as a
   cross-check.
3. Install the freshly built APK (`adb install -r app-debug.apk`) / run the
   fresh build from Xcode onto the iPhone.
4. Force-stop the app if it was already running, then launch it fresh into a
   known state (the app's own launch screen, not resumed from background).
5. Navigate: open the drawer/nav > Settings > scroll to "Developer —
   Benchmark" > tap **"Run Extended Benchmark (Publication Protocol)"**
   (not the quick "Run Latency Benchmark" row). This runs the full 10
   warm-up + 100 measured protocol described in section 2a.
6. Wait for the snackbar confirming completion (or a failure message — if it
   fails, capture the message verbatim and the device logs; do not retry
   blindly).
7. Pull BOTH exported files:
   - Android: `adb pull` the app's external files dir for
     `extended_benchmark_<timestamp>.json` and
     `extended_benchmark_<timestamp>.csv` (path:
     `context.getExternalFilesDir(null)` resolves to
     `/sdcard/Android/data/com.nkwabyte.cropdiseasedetection/files/` on most
     devices — confirm the exact path on this device with
     `adb shell run-as ... ` or by checking `get_logs` output, which prints
     the absolute path used).
   - iOS: use Xcode's Devices window ("Download Container…") or the Files
     app to retrieve the equivalent JSON/CSV pair from the app's Documents
     directory.
8. **Read the `notes` array in the JSON before doing anything else with the
   numbers.** Confirm `deviceEnvironment.isEmulator` is `false` and
   `deviceEnvironment.model`/`manufacturer` actually match the physical
   device you just ran on (see section 4).
9. Repeat the whole run 2-3 times per device (fresh app launch each time)
   before treating any single run's numbers as representative — thermal
   state drifts across repeated runs, especially on the Galaxy A10's more
   constrained thermal envelope, and that drift is itself worth reporting,
   not averaged away silently. `deviceEnvironment.thermalStatus` across the
   repeated runs is exactly the signal to look at for this.
10. Record device logs for the run (`adb logcat -d | grep ObjectDetector` on
    Android — the extended benchmark logs every entry in `notes` as well as
    the summary line; the Xcode console's captured output on iOS) alongside
    the exports, in case a run partially failed.

## 4. What "deviceEnvironment" should show to confirm you're on the right hardware

For sanity-checking that a result set actually came from the claimed device
(rather than, say, an emulator masquerading as one), the extended export's
`deviceEnvironment` object records: `manufacturer`/`model` (Galaxy A10 should
report something like `samsung`/`SM-A105...`), `osVersion` and
`apiLevelOrOsBuild`, `abis` (arm64 expected), `isEmulator` (must be `false`),
`totalRamBytes`, `batteryLevelPercent`/`isCharging`, `thermalStatus` (only
non-null on API 29+ physical hardware), `buildIdentifier` (the git SHA the
APK was built from), and `appVersionName`/`appVersionCode`. Do not proceed to
report results without checking this block per run — it is the difference
between a Galaxy A10 result and an emulator result silently mislabeled as
one. Note that the SoC itself (Galaxy A10 ships a MediaTek Helio P22) is not
reported by any Android API used here and must be recorded manually from the
device's own "About phone" or a spec lookup, since this matters for later
relating the number to other papers' "mobile latency" claims, which often
use much stronger SoCs.

## 5. If driving the physical device via an MCP automation layer

The `android-emulator` MCP server used for the `Medium_Phone` emulator
harness test may or may not also work against a physical device connected
via `adb` — that depends entirely on how that server enumerates devices
(`adb devices` against emulator IDs specifically, vs. any connected
device/serial). Check before assuming: if it lists the Galaxy A10's serial
once plugged in via USB (with USB debugging enabled and the RSA fingerprint
approved on-device), the exact same tool calls (`install_apk`, `force_stop`,
`launch_app`, `tap`/`tap_text`, `get_logs`) should work unchanged — same
measurement-boundary caveat applies even harder here, since a real device
adds real USB/adb transport latency to any timed round trip, which is yet
another reason the in-app instrumentation, not the automation layer's own
timers, is the only valid latency source. If the server is emulator-only,
drive the physical device with `adb` directly from a terminal on the Mac
instead; there is currently no tool in this session that can do that (no adb
reachable from the Linux VM bridge, and the android-emulator MCP is the only
mobile-automation tool currently connected).

## 6. The controlled Medium_Phone emulator experiment (extended protocol)

Before touching either physical device, repeat the emulator harness test
(section 3's flow, on `Medium_Phone`) using the NEW "Run Extended Benchmark"
button once a fresh debug/release build with today's instrumentation exists.
Record, in addition to `deviceEnvironment`: the host Mac's own specs (model,
chip, RAM, macOS version — `system_profiler SPHardwareDataType` on the Mac,
run from a terminal there, not from this session's Linux VM bridge, which
has no visibility into host hardware), current host load at capture time
(Activity Monitor CPU/memory pressure, or `top -l 1 | head -10`), the AVD
configuration (`Medium_Phone`, API level, RAM/heap allocated to the AVD,
whether cold or quick-boot), and the graphics backend actually in use
(`emulator -avd Medium_Phone -gpu <mode>`, or check the AVD's running-config
log for `hw.gpu.mode` — the existing capture's README already notes the
first run fell back to software rendering under host memory pressure; note
whether that recurs). Store this new emulator evidence in
`supplementary/mobile_benchmarks/emulator/Medium_Phone_API37/` alongside
(never overwriting) the two existing quick-button CSVs from the first
harness-validation run, with its own dated README following the same
provenance format. Do not describe any of this emulator evidence as Galaxy
A10 or iPhone performance.

## 7. After both physical devices are measured

Bring the exports (emulator harness-test run, the new extended-protocol
emulator run, Galaxy A10 runs, iPhone 15 Pro Max runs) back for analysis
together with the Gradio-side desktop/server benchmark CSVs
(`outputs/benchmarks/gradio_latency/` in the ML repo). Report all of them as
separate, honestly-labeled rows — do not average across devices, and do not
call the emulator numbers a stand-in for the Galaxy A10. Update claim C005
(and add new claims for the actual measured latency figures) only once at
least one physical-device export exists; instrumentation and
harness-testing on their own — however complete the metric coverage — do not
unblock C005.
