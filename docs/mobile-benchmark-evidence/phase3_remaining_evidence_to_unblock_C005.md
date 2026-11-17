# Phase 3 — Remaining Evidence Required Before Claim C005 Can Be Unblocked

Written 2026-09-14. This is a checklist, not a claim in itself — it exists so
the next person picking up Phase 3 (human or Claude) can see exactly what is
and is not done without re-deriving it from the worklog. Cross-reference
`sections/00_claim_evidence_matrix.txt` C005 for the claim's exact current
wording and status (BLOCKED, unchanged by this revision).

## What now exists (as of this revision, 2026-09-14)

- [x] Quick in-app latency instrumentation on Android and iOS
  (`ObjectDetector.runLatencyBenchmark()`, 10 warmup + 50 measured runs,
  classifier + detector, monotonic clocks). Written Entry 007; iOS side's
  build correctness was unverified until this revision (see the header-sync
  defect below).
- [x] Extended, publication-protocol instrumentation on Android
  (`ObjectDetector.runExtendedBenchmark()`): cold-load, per-stage
  (decode/orientation/preprocess/inference/postprocess-NMS), end-to-end in
  production call order, memory, CPU proxy, thermal (API 29+), model hashes,
  image manifest, device details, build identifier, offline success/failure
  counts. Every metric Android cannot measure defensibly is flagged in the
  export's `notes`, not silently omitted or invented.
- [x] The equivalent extended instrumentation on iOS
  (`ExecuTorchBridge.swift` stage-timed methods + utility methods;
  `iosMain/ObjectDetector.kt` orchestration), sharing the exact same
  `BenchmarkExport` schema and JSON/CSV formatters as Android (both are one
  Kotlin Multiplatform `common/model/BenchmarkResult.kt`).
- [x] A genuine build-system defect found and fixed: `ExecuTorchBridge.h`
  had never been updated to declare the `runLatencyBenchmarkFor*` methods
  Entry 007's Kotlin code already called — meaning the iOS quick benchmark
  most likely never actually compiled before this revision. Fixed as part
  of this revision; still unverified by an actual Xcode build (nobody has
  run one yet).
- [x] Emulator harness-validation evidence (`Medium_Phone` AVD, quick
  benchmark only, 10 warmup + 50 measured): two CSVs plus a provenance
  README in `supplementary/mobile_benchmarks/emulator/Medium_Phone_API37/`,
  captured 2026-09-14, PRESERVED UNCHANGED by this revision.
- [x] Updated Galaxy A10 runbook (extended metric set, 10+/100+ protocol) and
  a new iPhone 15 Pro Max runbook, plus a cross-platform output-agreement
  procedure using real checksum-locked images (separate from latency).
- [x] Two NEW architecture findings logged for the audit trail (not
  fabricated, not previously known): (a) production calls `detect()` before
  `classify()` and never gates detector classes by the classifier's
  prediction, contrary to the documented two-stage order; (b) iOS corrects
  EXIF orientation before inference, Android does not — a real,
  previously-undocumented cross-platform behavioral asymmetry.
- [x] **Both of those findings FIXED in the app (2026-09-14, ENTRY 011; claims
  C026/C027).** The app now classifies first and routes by exact detector class
  id, failing closed on rejection, crop mismatch and classifier failure; Android
  applies EXIF orientation at the ingest point with semantics matching iOS. 54
  shared/unit tests and 21 Android instrumentation tests cover it, and a six-case
  emulator acceptance pass (Corn/Pepper/Tomato/OOD/mismatch/EXIF-rotated) with
  real photographs proves the detector is not merely filtered but never invoked
  on the fail-closed paths.
- [x] **Benchmark corrected to schema v3** to measure that path: real stage
  order, `routing` as a genuinely measured stage, per-run `detectorExecuted`, and
  separate `endToEndByPath` entries so classifier-only rejection latency is never
  blended with classifier-plus-detector latency.

## What is still missing — every one of these blocks C005

1. ~~**A rebuilt Android APK/AAB containing this revision's instrumentation.**
   This session cannot build it (no Android SDK, no network path to
   `services.gradle.org`, from the Linux VM bridge). Someone with Android
   Studio or a terminal with the Android SDK on `PATH` must run
   `./gradlew assembleDebug` (or build from Android Studio) before ANY of
   the new code can even be exercised.~~
   **DONE 2026-09-14 (ENTRY 011):** `:app:assembleDebug` and `:app:assembleRelease`
   both succeed on the owner's Mac (JDK 25, Gradle 9.6, AGP 9.4.0). The release
   APK is signed with the owner's existing local keystore, which was neither read
   nor modified. Getting here required fixing pre-existing build breakages — see
   item 2.
2. ~~**A rebuilt iOS build containing this revision's instrumentation**,
   including the corrected `ExecuTorchBridge.h`. Requires Xcode on a Mac
   with the project open and a connected/simulated Apple device target.
   Given the header-sync defect found this revision, this build should be
   watched closely for compile errors on the FIRST attempt — there is a
   real chance more such gaps exist that neither this session nor a
   previous one could have caught without an actual compiler.~~
   **DONE 2026-09-14 (ENTRY 011), and the suspicion was justified — there were
   more gaps.** Four further defects had to be fixed before either platform
   compiled: `Project.exec {}` (removed in Gradle 9), JVM-only `"%.1f".format()`
   used in `commonMain` and `iosMain`, missing Foundation extension imports on
   iOS, a `val` assigned in both a `try` and its `catch` on Android, and raw
   `Double`s in `[NSNumber]` arrays at 18 sites in `ExecuTorchBridge.swift`. The
   unsigned iOS simulator build now reports `** BUILD SUCCEEDED **`, and all 23
   `ExecuTorchBridge.h` selectors were confirmed present in the linked binary's
   Objective-C metadata. Note the standing caveat: a Kotlin compile passing is
   **not** evidence that iOS works — check the linked binary.
3. **PARTIALLY DONE 2026-09-14 (ENTRY 011).** A 10-warmup/100-measured
   extended-protocol run was executed on `Medium_Phone` from the rebuilt APK and
   the export is preserved at
   `supplementary/phase3_emulator_acceptance_2026-09-14/`
   (`extended_benchmark_emulator.json` / `.csv`, schema v3), alongside a six-case
   behavioural acceptance pass. **Still outstanding for this item:** host Mac
   specs/load, AVD config and graphics backend were not recorded, so this is a
   harness-validation pass rather than the controlled emulator experiment of
   runbook section 6. Either way it is emulator evidence and does NOT advance
   C005.
4. ~~**At least one extended-protocol run on a physical Samsung Galaxy A10**~~
   **SUPERSEDED 2026-09-14 (ENTRY 016): the Galaxy A10 is REMOVED from the
   evaluation and was NEVER measured.** The Android target is now the
   **Xiaomi Redmi Note 14 Pro+ 5G** (`24115RA8EG`, codename `amethyst`,
   Android 16/SDK 36, SoC QTI SM7635, arm64-v8a only, ~11.09 GiB RAM), per
   `phase3_redmi_note_14_pro_plus_benchmark_runbook.md`. The Xiaomi is **not**
   equivalent to the A10 and inherits none of its assumptions. Required: at
   least **three independent, cooled sessions** with `deviceEnvironment`
   confirming real hardware, and **accepted-path** results — which the current
   synthetic-image manifest cannot produce (item 9 below). The device is now
   connected; what is missing is the evidence, not the hardware.
   **UPDATE 2026-09-14 (ENTRY 017):** item 9 is resolved and the benchmark has
   now run to completion on this handset — but **zero valid sessions exist**.
   The one completed run was made with the device **plugged in**
   (`isCharging=true`), so it is quarantined as a pilot under
   `phase3_xiaomi_redmi_note_14_pro_plus_committed_device_2026-09-14/pilot_run_not_a_measured_session/`
   and must not be cited. Three cooled, **unplugged** sessions are still owed.
5. **PARTIALLY DONE 2026-09-14 (ENTRY 015), but does NOT yet satisfy C005.**
   Three extended-protocol runs completed on a physical iPhone 15 Pro Max
   (`iPhone16,2`), schema v4, `isEmulator=false`, zero failures — preserved at
   `supplementary/phase3_iphone15promax_device_2026-09-14/`. **Two reasons they
   do not count toward C005:** they contain **no accepted-path result** (the
   synthetic image is classifier-rejected, so only the OOD path ran), and they
   were built from a **dirty working tree**, so the recorded SHA is the closest
   committed ancestor rather than the exact source. A re-run from a clean commit
   with the real-image manifest is required.
6. **The cross-platform output-agreement procedure actually executed**
   (`phase3_cross_platform_output_agreement_procedure.md`) — a checksum-
   locked real-image sample run through desktop/Android/iOS, with results
   at all three agreement levels (label/confidence/box) recorded. This is
   not strictly a latency requirement for C005, but any manuscript claim
   that mobile inference reproduces the desktop-evaluated accuracy numbers
   (C019/C020/C021/C022) needs this, or that claim is itself unsupported for
   the mobile deployment specifically.
7. **Independent confirmation that the physical-device APK/build under test
   is the SAME code as what was evaluated for accuracy** — i.e. the
   `deviceEnvironment.buildIdentifier` (git SHA) recorded for the
   latency/output-agreement runs should match (or be traceably close to,
   with a stated diff) the commit the ML-repo accuracy evaluation (C019-
   C022) was run against, so a manuscript claim can honestly say "the
   version measured is the version evaluated."
8. ~~**A decision (owner's) on the two architecture findings this revision
   surfaced**, since both affect what the benchmark numbers actually mean:~~
   **RESOLVED 2026-09-14 (ENTRY 011): the owner chose to FIX both**, and both
   fixes are in the working tree, verified by build, automated test and an
   emulator acceptance pass (claims **C026** and **C027**, superseding C024 and
   C025). The shipped app now implements the classifier-gated, class-id-routed
   architecture that C022 evaluated as a design, so the manuscript can describe
   the shipped app as implementing it — for builds from 2026-09-14 onward only.
   Android now corrects EXIF orientation with semantics matching iOS. **One
   consequence for item 6 below:** the rotated-image subset of the
   output-agreement procedure is now a check that the two platforms *agree*,
   rather than a characterisation of a known Android-only gap. The original
   open questions, kept for the audit trail:
   - Does the detect-before-classify / no-class-gating routing behavior get
     fixed to match the documented and evaluated (C022) architecture before
     physical-device measurement, or does the manuscript instead describe
     the SHIPPED behavior as evaluated (meaning C022's composed-pipeline
     evaluation would need re-running against the actual shipped call order
     to be representative of what ships)?
   - Does Android's missing EXIF orientation correction get fixed before
     physical-device measurement/output-agreement testing, given real phone
     photos are very often non-default-orientation? Testing against the
     current (unfixed) build without at least characterizing this via the
     output-agreement procedure's rotated-image subset (section 5 of that
     procedure) risks the manuscript understating a real mobile-only
     accuracy gap.

9. ~~**A checksum-locked REAL-image benchmark manifest in the app — this now
   gates BOTH devices.**~~
   **DONE 2026-09-14 (ENTRY 017).** `BenchmarkImageSet` is committed and its four
   photographs ship as app assets on both platforms, each SHA-256-pinned and
   re-verified at runtime before it is measured (a swapped or truncated asset is
   dropped loudly, never zero-filled). All **five** paths are now measurable:
   `accepted_corn`, `accepted_pepper`, `accepted_tomato`,
   `rejected_out_of_distribution`, `selected_crop_mismatch`. Confirmed by a
   completed Xiaomi run in which every path recorded 10 warmup + 100 measured,
   100 ok / 0 failed. This no longer gates C005.

10. **Both devices re-run from the SAME clean commit**, sharing one schema
   version, one model artifact set (identical SHA-256s) and one image manifest.
   Two devices measured from different source states are not comparable and must
   not appear in the same table.
   **ENTRY 017 adds a mechanical check for this:** `buildIdentifier` now carries
   a **`-dirty`** suffix whenever the tracked tree was modified at build time, so
   a build from an edited tree can no longer masquerade as the commit it sits on.
   **Any export whose `buildIdentifier` ends in `-dirty` is INVALID for C005 and
   is discarded, not annotated.**

11. **The two measurement fixes from ENTRY 017 committed, and every device
   session run from that commit.** Both were found by the Xiaomi pilot run:
   - **Memory samples never bracketed the measured work.** `before_measured_loop`
     and `after_measured_loop` were appended back-to-back at the end of the run;
     the pilot export shows identical values *and an identical timestamp*, so the
     delta was structurally zero while the export's note claimed the pair
     bracketed the measured loop. Fixed on both platforms. **Any memory figure
     from a build predating this fix is void.**
   - **`coldLoad` does not measure time-to-ready.** It reports ~0.34-1.4 ms for a
     30 MB classifier and a 9.8 MB detector because ExecuTorch's `Module.load()`
     is mmap-backed and lazy — method initialisation and weight page-in land in
     the first `forward()`. A caveat now ships in `coldLoad.note` on both
     platforms; production load behaviour was deliberately **not** changed.
   Until these are committed, no session can be run: building from the current
   tree stamps `-dirty` and is invalid under item 10.

## Explicitly not required to unblock C005 (do not wait on these)

- Faster R-CNN/ViT/Swin training completion (C006) — unrelated track.
- The 215-photo crop-label resolution (C017) — already resolved, unrelated
  to mobile latency.
- Any further desktop/Gradio accuracy re-evaluation beyond what C019-C022
  already establish, UNLESS the output-agreement procedure (item 6 above)
  finds a real mobile-vs-desktop prediction disagreement worth
  investigating further.

## SUPERSEDED 2026-09-14 (worklog ENTRY 018) — C005 refactored to PARTIAL; cooled/exact-commit evidence is now OPTIONAL, not a blocker

The owner issued an authoritative protocol decision: non-nominal thermal state, mid-session charging and back-to-back sessions are measured real-use conditions, not automatic disqualifiers. Consequences for this checklist:

- The Xiaomi pilot run (quarantined above under item 4) is **now admissible**. Charging is disclosed per-figure as the measured condition; it is not estimated for effect and the run is not described as unplugged operation.
- The three iPhone runs (item 5) are **now usable as provisional evidence**, their non-exact-commit provenance (13 uncommitted files over `31fb8f5576a6`) disclosed rather than treated as disqualifying. Each of the three is reported separately by its own `thermalStatus`; the observed +22% end-to-end drift is reported as a within-session observation, not a generalizable thermal model (n=3).
- Item 10 (both devices from one shared exact, non-`-dirty` commit) and the cooled/unplugged session requirements in items 4-5 are **reframed as an OPTIONAL supplementary baseline**, not a precondition for drafting. They remain open and are carried in the manuscript as explicit Limitations.
- Item 11's two measurement-validity fixes (memory-sample bracketing, cold-load caveat) are **NOT superseded** — they still gate what a given export's memory/cold-load numbers may be used for, independent of the charging/thermal question.
- Items 6 (output-agreement) and 7/8 (build-provenance-to-accuracy-commit traceability, architecture-fix disclosure) are unaffected and still stand as listed.

See `sections/00_worklog.txt` Entry 018 and the C005 row of `sections/00_claim_evidence_matrix.txt` (subclaims C005a-h) for the exact, current evidence-by-evidence status. Proposed manuscript tables and wording built from the evidence now admissible are drafted at `phase3_manuscript_mobile_section_draft_2026-09-14.md` for owner review.

## How to know C005 is actually unblockable (ORIGINAL, PRE-ENTRY-018 CRITERION — see supersession note above)

C005 moves from BLOCKED to a reportable, cited latency figure only once
items 1-5, 9 and 10 above are complete for BOTH physical devices — the
**Xiaomi Redmi Note 14 Pro+ 5G** (Android target, replacing the never-measured
Galaxy A10) and the **iPhone 15 Pro Max** — with valid **accepted-path** results
from **exact committed** builds sharing one manifest.
Item 6 gates any accompanying accuracy-reproduction claim, and item 8 gates
whether the reported architecture matches what was actually evaluated for
accuracy — both should be resolved or explicitly disclosed as limitations
before drafting the manuscript's mobile-performance paragraph, per the
standing instruction not to draft publication performance claims yet.
