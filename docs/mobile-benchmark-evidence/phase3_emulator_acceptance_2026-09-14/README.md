# Emulator acceptance pass — corrected two-stage pipeline (C024) and Android EXIF orientation (C025)

**Captured:** 2026-09-14
**Device:** Medium_Phone AVD — `sdk_gphone16k_arm64`, Android API 37, 1080x2400
**Build:** Kotlin app repo, branch `dev`, HEAD `46380a24d046` **plus uncommitted working-tree changes**
(the fixes described in worklog ENTRY 011; nothing was committed)

## READ THIS FIRST — what these files are and are not

Every number here is from a **software emulator running on the owner's Mac CPU**.
It is **NOT** a Samsung Galaxy A10 measurement, **NOT** an iPhone 15 Pro Max
measurement, and **NOT** usable as physical-device performance anywhere in the
manuscript. Claim **C005 (mobile latency) remains BLOCKED** — this pass changes
nothing about that.

What this pass *is* evidence for: the corrected production pipeline behaves as
designed on a real Android runtime, and the benchmark harness produces a
well-formed schema-v3 export in the corrected stage order.

## Behavioural evidence

`pipeline_acceptance_logcat.txt` — structured, one line per case, emitted by
`PipelineDeviceAcceptanceTest` running the REAL ExecuTorch models against real
photographs. `detectCalls` is a counter around the live inference engine, so it
distinguishes "the detector ran and was filtered" from "the detector never ran":

| case | classifier | detector executed | detectCalls | routed class ids |
|---|---|---|---|---|
| supported Corn | Corn @ 0.924 | yes | 1 | all in 0–4 |
| supported Pepper | Pepper @ 0.891 | yes | 1 | all in 5–14 |
| supported Tomato | Tomato @ 0.949 | yes | 1 | all in 15–22 |
| OOD cassava leaf | unknown @ 0.800 | **no** | **0** | — |
| crop mismatch (Corn image, Tomato selected) | Corn @ 0.924 | **no** | **0** | — |
| EXIF 6 rotated Corn | Corn @ 0.925 | yes | 1 | all in 0–4 |

Screenshots of the same six cases driven through the real UI:

| file | case |
|---|---|
| `01_app_launch.png` | app launches; both models load |
| `02_after_get_started.png` | select-crop screen |
| `03_home_corn_selected.png` | home screen, Corn selected |
| `06_corn_upright_exif1_result.png` | Corn, 21 detections, boxes aligned |
| `07_ood_cassava_corn_selected.png` | OOD rejection message, no detections |
| `08_mismatch_tomato_selected_corn_image.png` | mismatch message naming both crops |
| `09_pepper_result.png` | Pepper |
| `10_tomato_result.png` | Tomato |
| `11_corn_rotated_exif6_result.png` | EXIF-6 image shown upright, boxes aligned |

### The orientation case is a before/after, not just an after

The same gallery slot (`acc_corn_exif6.jpg`: `corn.jpg` pixels plus an EXIF
orientation-6 tag) was run through the UI twice:

- **Before** the ingest fix: 21 detections — byte-identical to the *unrotated*
  image, i.e. the EXIF tag was being ignored end to end.
- **After** the ingest fix: 4 detections — matching the independently-measured
  correctly-rotated result from `PipelineDeviceAcceptanceTest`.

That difference is the C025 fix taking effect on a live device.

## Benchmark harness output

`extended_benchmark_emulator.json` / `.csv` — one full
`runExtendedBenchmark()` run (10 warm-up + 100 measured, protocol defaults),
schema version **3**.

- `observedStageOrder` is the corrected order: classifier → routing → detector.
- Only the `rejected_out_of_distribution` path was measurable, because the
  classifier genuinely rejects the deterministic synthetic benchmark image.
  `detectorExecutedCount = 0`, `detectorSkippedCount = 100` — the fail-closed
  guarantee, machine-checked. The accepted and mismatch paths are **absent, not
  zero**; forcing them would need a real photograph or a changed classifier
  threshold, and the benchmark does neither. See note [3] in the export.
- `routing` is a genuinely measured stage (mean ≈ 0.003 ms, min ≠ max), and
  `orientationCorrection` on Android is now non-zero (mean ≈ 0.027 ms) where it
  used to be a hardcoded `0.0` — that constant zero was the original C025 signal.

## Test fixtures (SHA-256)

Three from the ML project's held-out detector test split
(`data/yolo/test/images/`), one from its OOD sample set
(`data/ood_external_samples/cassava/`), one derived:

```
6b829037a1c1026dabee932f81253ea3277cceb4aa1d52eedad202ab67f6318d  corn.jpg          (ground-truth label 0)
b2dc772ab5a232e2e49a609f1afe264cb21d1678c00494300b4e437f5d4ec9ba  pepper.jpg        (ground-truth label 8)
b5c7118b1dfa9dbc7ec8f11bb3f8be50f3236b7b243b248797e66624f71ac26d  tomato.jpg        (ground-truth label 16)
59b0bddc1f353936e1319f20e1281534116fbb99c28a45bdbcd9386f8af7156f  ood_cassava.jpg   (non-crop)
e1afe7506688b2264da96c55f9eaf5def8ca9f20c0e0071f014d157f8d8a4571  corn_exif6.jpg    (corn.jpg + EXIF orientation 6)
```

In all three supported-crop cases the routed detector class matched the image's
ground-truth label. That is a 3-image spot check, not an accuracy measurement.

## Tooling note

The `android-emulator` MCP server used in worklog entries 008–010 was **not
available** in this session. The emulator was driven with `adb` directly
(`am start`, `input tap`, `exec-out screencap`, `am instrument`). The
measurement boundary from
`phase3_galaxy_a10_benchmark_runbook.md` section 1 still holds and was
respected: every latency figure comes from the app's own in-process
`System.nanoTime()` instrumentation. No adb round-trip time appears in any
number here.
