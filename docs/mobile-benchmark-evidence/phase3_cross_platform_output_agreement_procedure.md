# Phase 3 Cross-Platform Output-Agreement Procedure

Status: written 2026-09-14. NOT YET EXECUTED. This procedure measures
prediction OUTPUT AGREEMENT (does the same image get the same answer on
different platforms) — it is a completely separate concern from the latency
benchmarks in `phase3_galaxy_a10_benchmark_runbook.md` /
`phase3_iphone15promax_benchmark_runbook.md`, and its results must never be
mixed into a latency table or vice versa. Nothing in this file is a
measurement yet.

## 0-pre. The four platform rows — never merged (updated 2026-09-14, ENTRY 016)

Every comparison in this project reports these as **four separate rows**, each
carrying its own label. Two are hardware; two are not and can never stand in for
hardware:

| Row | Identity | Label |
|---|---|---|
| iPhone 15 Pro Max | `iPhone16,2`, iOS 26.6.2 | **physical device (iOS)** |
| Xiaomi Redmi Note 14 Pro+ 5G | `24115RA8EG`, codename `amethyst`, Android 16/SDK 36, SoC QTI SM7635, arm64-v8a only | **physical device (Android)** |
| Android emulator | `Medium_Phone` AVD, `sdk_gphone16k_arm64` | **EMULATOR — not device performance** |
| iOS simulator | iPhone 17 Pro Max simulator, iOS 26.5 | **SIMULATOR — not device performance** |

The **Samsung Galaxy A10 is not a row** — it was removed from the evaluation on
2026-09-14 and **was never measured**. Do not add it, and do not treat the
Xiaomi as its equivalent: they are different devices of different classes.

The two physical rows are comparable to each other **only** when they share the
same committed source, schema version, model artifacts and checksum-locked image
manifest. If they do not, they belong in separate tables.

## 0. Why this exists, and why it uses different images than the latency benchmarks

The latency benchmarks deliberately use synthetic, deterministic striped
images (`syntheticJpegBytes` / `ExecuTorchBridge.syntheticJpegData`) because
latency is driven by tensor shape, not pixel content — using a fake image is
completely legitimate there and avoids needing to ship real photos as test
fixtures.

Output agreement is the opposite case: pixel content is the entire point.
Two platforms can have IDENTICAL latency behavior while producing DIFFERENT
predictions on the same real photo, because of decode/resize implementation
differences, floating-point nondeterminism across backends (XNNPACK on both
mobile platforms vs. the desktop PyTorch reference), or — as this benchmark
work already surfaced — genuine pipeline differences like iOS correcting
EXIF image orientation before inference while Android does not (see the
worklog entry logging this asymmetry). This procedure exists to catch and
quantify exactly that class of problem, using real, checksum-locked images
so every party can verify they measured the same input.

## 1. Selecting and locking the image set

1. Draw a small, fixed sample (recommend 20-30 images: enough to see a
   pattern, small enough to run by hand on two physical devices) from the
   ALREADY-FROZEN held-out test manifest used for the Phase 2 desktop/Gradio
   evaluation (`rebuilt_split_manifest*.csv` /
   `held_out_test_manifest_detector.csv` in `supplementary/` — do not draw
   from training data, and do not hand-pick "easy" images). Stratify across:
   - Each crop class (Corn/Pepper/Tomato) and a few genuine "Other"/non-crop
     images, to exercise the classifier's rejection path too.
   - At least a few images with non-default EXIF orientation (rotated
     phone photos) specifically BECAUSE of the orientation-correction
     asymmetry found this revision — this is the single most likely source
     of a real, reproducible Android/iOS disagreement, so the sample must be
     able to detect it rather than accidentally avoiding it.
   - A mix of source resolutions, since decode/resize behavior can differ by
     scale factor.
2. Copy the selected files, byte-for-byte, into
   `supplementary/mobile_benchmarks/output_agreement/images/`, named by a
   short stable id (`oa_001.jpg`, `oa_002.jpg`, …) — never by their original
   dataset filename, to avoid any accidental linkage back to a person's
   identifying information if the source dataset ever contained any.
3. Compute SHA-256 for every file in that directory and record it, the
   original manifest row it came from, its label, and its EXIF orientation
   tag, in `supplementary/mobile_benchmarks/output_agreement/image_manifest.csv`
   with columns: `id,sha256,sizeBytes,widthPx,heightPx,exifOrientation,groundTruthLabel,sourceManifestRow`.
4. This manifest — not the images' original filenames or paths — is the
   single source of truth for "which image is which" from this point
   forward. Anyone re-running this procedure re-derives the SHA-256 of the
   file they actually used and confirms it matches this manifest before
   trusting their own results.

## 2. What each platform runs

For every image in the locked set, on every platform, run the FULL
production pipeline as an end user would trigger it — not a bespoke
evaluation script — so this measures what actually ships, not what an
idealized offline pipeline would do:

- **Desktop/Gradio reference**: the existing evaluation pipeline used for
  the Phase 2 held-out evaluation (same repo, same code path already treated
  as ground truth for accuracy claims elsewhere in this project). This is
  the baseline every mobile result is compared against — not because it is
  assumed perfect, but because it is the version already exercised by the
  project's accuracy claims, so a mobile disagreement against it is exactly
  the disagreement that would undermine those claims if mobile is what ships
  to users.
- **Android**: load each locked image into the app (share-sheet import or a
  debug-only "load from file" path — do not photograph a screen or
  re-encode the image through any lossy step; the bytes reaching
  `classify()`/`detect()` must be byte-identical to the locked file) and
  record the full `ClassificationResult` and `List<DetectionResult>` exactly
  as the production `DetectionViewModel.detect()` call order produces them
  (detector first, then classifier — see the architecture-mismatch note
  elsewhere in this project; this procedure measures what ships, asymmetric
  call order included, not an idealized order).
- **iOS**: same as Android, using the equivalent import path in the KMP
  shared UI.

## 3. What to record per (image, platform) pair

Export one row per (image id, platform) combination with:

- `imageId`, `platform` (`desktop`/`android`/`ios`), `imageSha256` (recompute
  it from the exact bytes handed to the pipeline on that platform — if this
  does not match `image_manifest.csv`, stop and find out why before
  recording anything else for that row; a mismatch here invalidates the row
  entirely, since something transcoded or resized the file before it reached
  the model).
- Classifier: `classifierLabel`, `classifierConfidence`, `classifierAccepted`
  (the `isAccepted` flag from `ClassificationResult`).
- Detector: `detectionCount`, and for each detection,
  `classId,score,box(x1,y1,x2,y2)` — record ALL detections, not just the
  highest-scoring one, since a disagreement in detection COUNT is as
  important a finding as a disagreement in the top box.
- `buildIdentifier`/`appVersionName` (mobile) or the desktop pipeline's own
  git SHA (reuse `git rev-parse --short=12 HEAD` in the ML repo at capture
  time), so every row is traceable to the exact code that produced it.

Store this as
`supplementary/mobile_benchmarks/output_agreement/results_<timestamp>.csv`,
one file per full pass across all three platforms — never overwrite a prior
pass's file.

## 4. Defining "agreement" — with explicit, stated tolerances

Do not require bit-exact agreement; floating-point results legitimately
differ by backend (XNNPACK on-device vs. desktop PyTorch) even on identical
inputs and an identical model file (confirm the model files ARE identical
first — matching `sha256` from the benchmark's `modelArtifacts` across
platforms — before investigating anything else, since a hash mismatch there
would explain everything downstream without needing a subtler theory).

Report agreement at three levels, in increasing strictness, and DO NOT
collapse them into one pass/fail number:

1. **Label agreement** (loosest, and the one that actually matters for the
   user-facing claim): does the classifier's accepted/rejected label match,
   and does the detector's SET of predicted crop classes match, across
   platforms? This is what a farmer using the app actually experiences.
2. **Confidence agreement** (diagnostic): is `classifierConfidence` within a
   stated absolute tolerance (propose ±0.02 on the 0-1 softmax scale as a
   starting point — a number to justify or revise once real data comes in,
   not a number to treat as already validated) of the desktop reference?
   Report the actual observed spread, not just pass/fail against the
   tolerance.
3. **Box agreement** (diagnostic, detector only): for detections that both
   platforms/the reference agree exist for the same object, is IoU between
   the two boxes above a stated threshold (propose IoU ≥ 0.5, matching the
   NMS threshold already used elsewhere in this project)? Report per-image,
   not as one aggregate.

A disagreement at level 1 (label) is the only one that should ever feed a
manuscript claim about correctness; disagreements only at levels 2-3 are
implementation-quality findings, reportable as such, not correctness
failures.

## 5. Specifically investigating the orientation-correction asymmetry

> **UPDATE 2026-09-14 (worklog ENTRY 011, claim C027): the asymmetry this section
> was written to investigate has been FIXED, so the prediction below no longer
> describes the current build — but this section is still worth running, with its
> expectation inverted.**
>
> Android now applies EXIF orientation for all eight defined values, at the
> camera/gallery ingest point, with semantics matching iOS's
> `UIImage.fixOrientation()`; the shared definition both platforms are held to is
> `app/src/commonMain/.../common/model/ExifOrientationContract.kt`, and Android's
> implementation is asserted against it by 13 instrumentation tests covering
> upright dimensions *and* corner placement (so a rotation is distinguishable
> from a mirror). Note that C025 had located the gap one layer too late: the
> orientation tag was actually being destroyed at ingest, where the picked image
> was re-compressed to JPEG after an EXIF-blind decode.
>
> **What to expect now, as an equally falsifiable prediction:** the rotated
> images in the locked set should show Android, iOS and the desktop reference all
> *agreeing*. If Android still disagrees on a rotated image, that is a real
> regression or an incomplete fix and must be filed as such — do not attribute it
> to the known-and-fixed C025.
>
> **What is still genuinely unverified:** no pixel-level Android-vs-iOS
> comparison on real photographs has been run. Cross-platform agreement is
> currently established by shared specification and by test, **not** by
> measurement. That is precisely the gap this procedure exists to close, so
> running it is still required before any manuscript claim that mobile
> predictions reproduce the desktop-evaluated numbers. Record which build was
> used: exports and runs from before 2026-09-14 describe the *unfixed* Android
> behaviour and must not be pooled with later ones.


Because iOS corrects EXIF orientation before inference and Android does not
(see the architecture/documentation-mismatch note in the worklog and the
extended-benchmark notes on both platforms), the rotated-orientation images
in the locked set (section 1, item 1) are expected — as a specific,
falsifiable prediction, not a vague expectation — to show Android
disagreeing with the desktop reference (which almost certainly does correct
orientation, being built on a mainstream image-loading library) on those
specific images, while iOS agrees. If that prediction holds, it is a genuine
mobile-Android-only correctness bug — not a benchmark artifact, not
something to average away — and should be filed as its own claim in the
claim-evidence matrix, separate from and in addition to the routing/call-
order architecture mismatch already logged. If the prediction does NOT hold
(Android agrees anyway, e.g. because the rotation was small enough not to
flip the predicted class), that is also worth recording precisely, since it
bounds how much the gap matters in practice rather than leaving it
theoretical.

## 6. What this procedure does NOT do

- It does not measure latency — do not report timing figures alongside these
  results even informally; that is what the benchmark runbooks are for.
- It does not establish accuracy against ground truth beyond what the
  existing Phase 2 held-out evaluation already established for the desktop
  reference — this procedure treats the desktop reference's OWN correctness
  as already settled, and only asks whether mobile matches it.
- It is not a substitute for the physical-device latency runs required to
  unblock claim C005 — output agreement and latency are separate blocking
  requirements, and completing this procedure does not advance C005 on its
  own (it would, however, be the evidence behind any manuscript claim of the
  form "mobile predictions match the reference implementation").
