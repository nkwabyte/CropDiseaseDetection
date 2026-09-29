# Phase 3 Physical-Device Benchmark Runbook — Xiaomi Redmi Note 14 Pro+ 5G (primary Android target)

**Status:** ACTIVE. This runbook supersedes
`phase3_galaxy_a10_benchmark_runbook.md`, which is marked
**SUPERSEDED — DEVICE NOT USED**.

**Owner decision, 2026-09-14:** the Samsung Galaxy A10 is no longer part of the
physical-device evaluation. The Xiaomi Redmi Note 14 Pro+ 5G replaces it as the
Android target everywhere.

> **No Galaxy A10 measurement has ever existed.** Nothing in this project has
> been measured on that device. Do not carry over Galaxy A10 device-tier
> assumptions, expected performance, or hardware specifications, and never
> describe the Xiaomi as equivalent to it — they are different devices of
> different classes, and the substitution changes what the mobile-performance
> claim is about. Any text implying an A10 result is an error.

---

## 0. Actual device facts — measured, not assumed

Collected from the connected handset with `adb getprop` / `dumpsys` on
2026-09-14. **Do not infer specifications from the marketing name.**

| Field | Value |
|---|---|
| manufacturer | `Xiaomi` |
| brand | `Redmi` |
| commercial name | `Redmi Note 14 Pro+ 5G` |
| Android model number | `24115RA8EG` |
| device codename | `amethyst` (product `amethyst_global`) |
| Android version | **16** (SDK **36**) |
| build id | `BP2A.250605.031.A3` |
| build fingerprint | `Redmi/amethyst_global/amethyst:16/BP2A.250605.031.A3/OS3.0.306.0.WOPMIXM:user/release-keys` |
| security patch | `2026-08-01` |
| HyperOS | `OS3.0` (code `3`), incremental `OS3.0.306.0.WOPMIXM` |
| MIUI UI version | `V816` |
| SoC | `QTI` `SM7635`, board platform `volcano`, hardware `qcom` |
| CPU cores | 8 |
| CPU clusters (max kHz) | 4 x 1,804,800 - 3 x 2,400,000 - 1 x 2,496,000 |
| CPU ABI | `arm64-v8a` |
| supported ABIs | `arm64-v8a` **only — no 32-bit ABI** |
| RAM | 11,630,372 kB ≈ **11.09 GiB** |
| storage (`/data`) | 227 GiB total, 135 GiB available (41% used) |
| display | `1220x2712`, density `480`, 445.87 x 445.86 dpi |
| display refresh | **running at 60 Hz** (`modeId 3`); panel also supports 90 and 120 Hz |
| region | `GH` |
| build type / tags | `user` / `release-keys` |
| inference backend | ExecuTorch **XNNPACK (CPU) only** |

**Backend verification.** `libexecutorch.so` (arm64-v8a) was extracted from the
installed APK and scanned: it contains XNNPACK symbols and **no** Vulkan, NNAPI,
QNN or Hexagon symbols. The Snapdragon NPU/GPU is therefore **not** exercised by
any figure in this evaluation; every latency reported here is CPU inference.
Note the APK also ships `armeabi-v7a`, `x86` and `x86_64` folders, but only the
`arm64-v8a` and `x86_64` ones contain `libexecutorch.so`; on this 64-bit-only
handset the `arm64-v8a` library is the one loaded.

Two points that matter for interpretation:

1. **This is a 64-bit-only device** (`abilist32` empty). The APK must supply
   `arm64-v8a` ExecuTorch native libraries; there is no 32-bit fallback.
2. **Android 16 / SDK 36 is newer than the app's `targetSdk` (35)** and its
   `compileSdk` (35). Record that in every export; behavioural differences under
   a newer platform than the app targets are a real interpretation caveat.

---

## 1. The measurement boundary

Unchanged from the superseded runbook and restated because it is the rule most
easily broken: **only the app's own in-process instrumentation counts.**
Timing comes from `System.nanoTime()` inside `ObjectDetector`/`DetectionPipeline`.
Automation-layer round-trip time — `adb`, an MCP server, a shell loop — is never
the reported latency and must not appear in any figure.

Emulator and simulator numbers are never a substitute for this device, and this
device's numbers are never a substitute for the emulator's. They are separately
labelled data points (see section 6).

---

## 2. Prerequisites — the exact committed build

`C005` requires results from an **exact committed** build. Before every session:

```bash
cd <kotlin app repo>
git status --porcelain          # MUST be empty
git rev-parse --short=12 HEAD   # record this; it is the buildIdentifier
./gradlew :app:assembleRelease  # or assembleDebug, recorded either way
```

`BuildKonfig.BUILD_GIT_SHA` embeds that SHA at build time and it appears in the
export as `deviceEnvironment.buildIdentifier`. **If the working tree is dirty,
the SHA identifies the closest committed ancestor and NOT the source that
produced the numbers** — that is exactly the caveat recorded against the first
iPhone 15 Pro Max exports (worklog ENTRY 015).

**This is now enforced rather than left to discipline.** `gitCommitSha` in
`app/build.gradle.kts` appends `-dirty` when `git status --porcelain
--untracked-files=no` is non-empty, so a build made from a modified tree stamps
e.g. `f5193cb92d24-dirty` and can never masquerade as a build of that commit.

> **Acceptance rule:** any export whose `deviceEnvironment.buildIdentifier`
> ends in `-dirty` is INVALID for C005 and must be discarded, not annotated.

Record `applicationBuildType` (debug vs release) with the results. Do not
compare a debug-build figure with a release-build figure.

Use the **same** source commit, schema version, model artifacts and
checksum-locked benchmark image manifest as the iPhone 15 Pro Max run. If any of
those differ, the two devices are not comparable and must not be placed in the
same table.

---

## 3. Per-session preparation — all eight steps, every session

1. **Close other applications** (swipe away recents).
2. **Disable power-saving mode.** Verify:
   `adb shell settings get global low_power` → `0`, and
   `adb shell dumpsys power | grep mSettingBatterySaverEnabled` → `false`.
3. **Fix screen brightness** to a consistent level and keep it identical across
   all sessions. Record the value. This handset ships with **adaptive brightness
   ON** (`settings get system screen_brightness_mode` -> `1`), which varies the
   panel load between sessions. Turn it off and pin a fixed level:
   ```bash
   adb shell settings put system screen_brightness_mode 0   # 0 = manual
   adb shell settings put system screen_brightness 128      # same value every session
   ```
   Also raise the screen timeout so the device cannot sleep mid-run:
   `adb shell settings put system screen_off_timeout 1800000`.
4. **Charge beforehand, then UNPLUG for the measurement.** Charging changes both
   thermal behaviour and CPU governor headroom. Verify after unplugging:
   `adb shell dumpsys battery | grep -E "AC powered|USB powered|level"`.
   (Note: `adb` over USB normally implies powered — use wireless `adb` or
   `adb shell dumpsys battery unplug` to simulate, and say which you did.)
5. **Wait for the lowest available thermal state.**
   `adb shell dumpsys thermalservice | grep "Thermal Status"` → `0`.
   Do not start until it reads 0.
6. **Force-stop and relaunch the app:**
   `adb shell am force-stop com.nkwabyte.cropdiseasedetection`.
7. **Confirm the committed build SHA and model hashes** match section 2 and the
   iPhone run's artifacts.
8. **Run the fixed benchmark manifest.** Do not change detection/IoU/classifier
   thresholds or the selected detector model between sessions.

### 3a. Driving the UI on HyperOS — `input` does not work

HyperOS/MIUI blocks shell input injection. `adb shell input tap` fails with:

```
java.lang.SecurityException: Injecting input events requires the caller (or the
source of the instrumentation, if any) to have the INJECT_EVENTS permission.
```

Enabling it needs "USB debugging (Security settings)" in Developer options,
which requires a signed-in Mi account and manual interaction on the handset.

**`adb shell monkey` is not blocked** and accepts a script of `DispatchPointer`
events, which gives targeted taps and swipes without that toggle. A tap:

```bash
cat > /tmp/mtap.txt <<SCRIPT
type= raw events
count= 1
speed= 1.0
start data >>
DispatchPointer(0,0,0,<X>,<Y>,1.0,1.0,0,1.0,1.0,0,0)
DispatchPointer(0,0,1,<X>,<Y>,1.0,1.0,0,1.0,1.0,0,0)
SCRIPT
adb push /tmp/mtap.txt /data/local/tmp/mtap.txt
adb shell monkey -p com.nkwabyte.cropdiseasedetection --throttle 200   -f /data/local/tmp/mtap.txt 1
```

A successful injection prints `Events injected: 2`. A swipe is the same with
`action=2` (MOVE) samples interpolated between the down and up points.

Navigation path to the benchmark on this build: launch -> **GET STARTED** ->
hamburger (top-left) -> **Settings & Preferences** -> scroll to the bottom ->
**Developer — Benchmark** -> **Run Extended Benchmark (Publication Protocol)**.
The section is **not** debug-gated, so it is present in the release build.

Note this drives the UI only; it never enters any timed region. The measurement
boundary in section 1 is unaffected.

**Cooling between sessions is now OPTIONAL, not required** (owner protocol decision, worklog Entry 018): back-to-back sessions and any resulting thermal drift are accepted real-use conditions, provided `thermalStatus`, `isCharging` and `batteryLevelPercent` are recorded for every session and reported per-session rather than averaged — the iPhone runs showed a +22% end-to-end drift across three consecutive runs, and that must stay visible, not be averaged away. A cooled/unplugged run set remains a valid supplementary baseline to add later (see phase3_remaining_evidence_to_unblock_C005.md); its absence does not block using the sessions you already have.

---

## 4. What the session must measure separately

Each is reported on its own. **Accepted, rejected and mismatch paths are never
averaged together** — only the accepted path includes detector inference.

| Requirement | Reported as |
|---|---|
| accepted Corn | `endToEndByPath` entry, accepted path, Corn fixture |
| accepted Pepper | same, Pepper fixture |
| accepted Tomato | same, Tomato fixture |
| OOD / rejected input | `rejected_out_of_distribution`, `detectorExecutedCount` must be 0 |
| deliberate selected-crop mismatch | `selected_crop_mismatch`, `detectorExecutedCount` must be 0 |
| cold model loading | `coldLoad.classifierColdLoad` / `.detectorColdLoad` |
| warm classifier inference | `classifierStage.classifierInference` |
| routing | `stageBreakdown.routing` |
| detector inference | `detectorStageBySize.*.detectorInference` |
| output materialization | `outputTransfer` (a **sub-component** of `detectorInference`, never an addition) |
| postprocessing / NMS | `postprocessNms` |
| accepted-path end-to-end | `endToEndByPath[accepted_*].stats` |
| rejected-path latency | `endToEndByPath[rejected_out_of_distribution].stats` |
| mismatch-path latency | `endToEndByPath[selected_crop_mismatch].stats` |
| memory | `memorySamples` (PSS; sampled outside the timed loop) |
| CPU utilization | `cpuUtilization` (single-thread proxy; ratio may exceed 1.0) |
| thermal state | `deviceEnvironment.thermalStatus` |
| installed app + model sizes | `deviceEnvironment.installedAppSizeBytes`, `modelArtifacts[].sizeBytes` |
| offline success/failure counts | `offlineSuccessCount` / `offlineFailureCount` / `failureMessages` |
| hardware acceleration / delegates | recorded in notes — whether XNNPACK or any other delegate is active |

---

## 5. Evidence layout

Store each dated capture under:

```
supplementary/phase3_xiaomi_redmi_note_14_pro_plus_committed_device_<date>/
```

containing raw JSON, raw CSV, SHA-256 checksums of every artifact, device
metadata, application/build metadata, model hashes, the input-image manifest,
battery and thermal conditions per session, logs, failure records, and a README
stating the protocol and its limitations.

Retrieve with:

```bash
adb pull /storage/emulated/0/Android/data/com.nkwabyte.cropdiseasedetection/files/ <dest>
```

---

## 6. Cross-platform comparison — four separate rows, never merged

| Row | Label it as |
|---|---|
| iPhone 15 Pro Max (`iPhone16,2`) | physical device, iOS |
| Xiaomi Redmi Note 14 Pro+ 5G (`24115RA8EG`) | physical device, Android |
| Android emulator (`Medium_Phone`) | **emulator — not device performance** |
| iOS simulator (iPhone 17 Pro Max) | **simulator — not device performance** |

The two physical devices are comparable to each other **only** when they share
the same committed source, schema version, model artifacts and image manifest.
Emulator and simulator rows exist to validate the harness, never to stand in for
hardware.

---

## 7. After both physical devices are measured

`C005` moves from BLOCKED only when valid **accepted-path** results exist for
**both** the exact committed iPhone 15 Pro Max build **and** the exact committed
Xiaomi Redmi Note 14 Pro+ build. Until then it stays BLOCKED, and no mobile
latency figure may be drafted into the manuscript.
