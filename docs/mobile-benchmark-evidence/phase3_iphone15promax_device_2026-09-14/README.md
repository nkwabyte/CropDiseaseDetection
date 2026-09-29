# iPhone 15 Pro Max — physical-device extended benchmark (schema v4)

**Captured:** 2026-09-14, three runs from fresh app launches
**Device:** iPhone 15 Pro Max — `iPhone16,2`, iOS 26.6.2, 8 GB RAM, arm64
**`isEmulator = false` in all three exports.**

## THE FIRST PHYSICAL-DEVICE EXTENDED EXPORTS IN THIS PROJECT

Every earlier extended-protocol figure in this repository came from an emulator
or a simulator. These three do not.

**C005 REMAINS BLOCKED.** It stays blocked until these are paired with Samsung
Galaxy A10 results — the Galaxy A10 is the primary target for the "does this run
on the hardware Ghanaian farmers have" claim, and an iPhone 15 Pro Max says
nothing about it. Do not cite these as evidence for C005.

## Build provenance — read this before citing

`buildIdentifier` = `31fb8f5576a6`, which **is** the real `git rev-parse HEAD`
at build time (commit *"feat: Add utility for fixed-decimal formatting and crop
display names"*). **However, the working tree was still dirty**: 13 files
carrying the schema-v4 raw-output transport correction were modified but not
committed. So the SHA identifies the closest committed ancestor, **not** the
exact source that produced these numbers. To make the SHA literally exact,
commit the tree and re-run.

Uncommitted at build time: `ExecuTorchBridge.{h,swift}`, `iOSApp.swift`,
`ContentView.swift`, `KoinHelper.swift`, `Views/Settings/SettingsView.swift`,
`iosApp.xcodeproj/project.pbxproj`, iosMain/androidMain/commonMain
`ObjectDetector.kt`, `BenchmarkResult.kt`, `MainViewController.kt`,
`BenchmarkExportTest.kt`.

## Per-run results

| | run 1 | run 2 | run 3 |
|---|---|---|---|
| file | `…662421` | `…741380` | `…818047` |
| battery / charging | 100% / yes | 100% / yes | 100% / yes |
| **thermalStatus** | **nominal** | **fair** | **fair** |
| end-to-end mean (ms) | 43.14 | 44.26 | **52.63** |
| end-to-end p95 (ms) | 45.22 | 45.03 | 56.06 |
| classifier inference mean (ms) | 21.50 | 20.87 | 21.97 |
| detector 1920×1080 mean (ms) | 48.96 | **62.98** | 54.07 |
| detector 1280×960 mean (ms) | 51.77 | 53.36 | 56.35 |
| detector 640×480 mean (ms) | 53.16 | 54.63 | 56.97 |
| outputTransfer mean (ms) | 1.98–2.09 | 2.08–2.34 | 2.11–2.19 |
| failures | 0 | 0 | 0 |

All three: schema **v4**, 10 warm-up + 100 measured, path
`rejected_out_of_distribution` with **detectorExecutedCount = 0 /
detectorSkippedCount = 100**, no `NaN`/`Infinity`, 9 notes,
`outputTransfer ≤ detectorInference` in every stage.

### Thermal drift is visible and must not be averaged away

`thermalStatus` moved **nominal → fair → fair** across three back-to-back runs,
and end-to-end mean rose 43.14 → 44.26 → **52.63 ms** (+22%). Run 2's
1920×1080 p95 of 93.74 ms against a 62.98 ms mean is a single outlier, not a
steady state. **Report these as three runs with their thermal state, not as one
averaged figure.** More runs with cool-down intervals are needed before quoting
a steady-state number.

### Only one routing path was measurable

The deterministic synthetic benchmark image is rejected by the classifier, so
only the out-of-distribution path ran — and it correctly never invoked the
detector (`detectorExecutedCount = 0`). The accepted and crop-mismatch paths are
**absent, not zero**; measuring them needs a real crop photograph. Accepted,
rejected and mismatch paths are never averaged together.

## Memory — the fix holds on real hardware

Phase trace, run 1: start 55.6 → cold-load 56.2 → artifacts/images 113.8 →
classifier stage 158.5 → detector 1920×1080 **224.0** → 1280×960 224.0 →
640×480 223.1 → end-to-end 247.8 MB.

**Peak 247.8 MB.** The pre-fix boxed build reached **1594 MB** at the same point
and was killed by jetsam. Resident memory is flat across the three detector
resolutions rather than climbing.

## Cross-platform comparability

These are the **first** iOS detector figures comparable with Android's: the
`[NSNumber]` boxing that previously sat inside the measured span is gone, and
`detectorInference` now covers the same work on both platforms (forward +
materialization of a usable primitive buffer). **Pre-v4 iOS latency is not
comparable with these, and must not be pooled with them.**

## Model artifacts (identical across all three runs)

```
classifier          crop_classifier_ood.pte   30,820,096 B  sha256 39e1b63dac8d…
detector (YOLO26n)  crop_disease_yolo26.pte    9,768,796 B  sha256 baa36404dbb6…
installed app size 575,989,136 B (best-effort bundle sum)
```
