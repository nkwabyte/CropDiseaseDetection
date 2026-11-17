# iOS raw-output transport correction (schema v4) — simulator verification

**Captured:** 2026-09-14
**Device:** iPhone 17 Pro Max **Simulator** (iOS 26.5) — `isEmulator = true`
**Build:** Kotlin app repo, branch `dev`, HEAD `46380a24d046` + uncommitted working tree

## READ THIS FIRST

Simulator figures. **Not** iPhone 15 Pro Max performance, **not** Galaxy A10
performance, and **not** usable for claim C005, which remains BLOCKED. This
directory exists to demonstrate that a production inference-path change is
lossless and that memory now plateaus — not to provide latency evidence.

## What changed

The Objective-C bridge used to return the detector's entire raw output tensor as
a boxed `[NSNumber]` array — **226,800 objects per call** for YOLO26's
`[1, 27, 8400]`, plus a second set written by the un-letterbox pass. It now
returns a contiguous `Float32` `NSData` buffer (one ~907 KB copy, native byte
order), decoded in Kotlin into a `FloatArray` with explicit validation.

The copy is deliberate: ExecuTorch owns the tensor memory and does not guarantee
it outlives the bridge call, so a no-copy wrapper could dangle.

## Equivalence — the transport is lossless

`--run-equivalence-check` runs ONE forward pass and returns the same
post-un-letterbox data *both* ways, so a difference can only be the transport,
never model nondeterminism. Fixture: `corn.jpg`, SHA-256
`6b829037a1c1026dabee932f81253ea3277cceb4aa1d52eedad202ab67f6318d` (ML repo
`data/yolo/test/images`, ground-truth class 0).

```
EQUIVALENCE: PASS — elements=226800 bitExactFloats=226800/226800
maxAbsDiff=0.0 firstMismatchIndex=-1
detections boxed=21 buffer=21 identical=true
classIds=[0, 0, 0, ... 0]   (21 x class 0)
```

Bit-for-bit identical (compared via `toRawBits`, so NaN and -0.0 compare
exactly), identical detection count, class ids, scores and boxes after NMS. The
21 x class-0 result also matches the independent Android on-device acceptance
run for the same image.

## Memory — plateaus instead of growing

`--run-memory-soak`: 20 warm-up + 500 measured detector calls.

| point | resident |
|---|---|
| start | 385.7 MB |
| post-warm-up (20) | 441.8 MB |
| call 50 | 441.4 MB |
| call 100 | 395.4 MB |
| call 200 | 395.7 MB |
| call 300 | 395.7 MB |
| call 400 | 368.2 MB |
| call 500 | 367.9 MB |
| **peak** | **443.5 MB** |
| final | 367.9 MB |

Growth after warm-up: **−73.9 MB** over 500 calls. Flat, then settling — no
linear growth. Before the fix the same work retained ~11.7 MB per call
(measured on the physical iPhone 15 Pro Max, worklog ENTRY 013); 500 calls would
have been several GB.

Across the full extended protocol, resident memory now peaks at **427 MB**
(309 → 427 → 364 → 362 through the three detector resolutions) where the boxed
build climbed to 1594 MB on device before jetsam killed it.

## Latency: pre-fix iOS numbers are NOT comparable

Same simulator, same synthetic images, detector stage:

| input | v3 (boxed, pre-fix) | v4 (buffer) | difference |
|---|---|---|---|
| 1920x1080 | 32.06 ms | 19.25 ms | −12.8 ms |
| 1280x960 | 31.06 ms | 19.34 ms | −11.7 ms |
| 640x480 | 32.26 ms | 19.01 ms | −13.3 ms |

**About 40% of what pre-fix iOS reported as "detector inference" was NSNumber
boxing, not inference.** Post-fix figures are lower because that overhead is
gone, not because the model got faster. Any iOS detector latency from before
schema v4 must be treated as non-comparable — with later iOS runs and with
Android, which never boxed (it has always used `Tensor.getDataAsFloatArray()`).

## Timing boundary, stated explicitly

`detectorInference` on **both** platforms = model forward **plus** materializing
a usable contiguous primitive buffer. On iOS that is the tensor -> `[Float]` ->
`NSData` copies plus the `NSData` -> `FloatArray` copy; on Android it is
`getDataAsFloatArray()`. The mandatory conversion was **not** moved outside the
measured stages. `outputTransfer` reports it separately and is a **sub-component**
of `detectorInference`, never an addition — asserted in
`BenchmarkExportSchemaTest.outputTransferIsReportedAndStaysInsideDetectorInference`.

Measured here: outputTransfer ≈ 1.386 ms of a 19.25 ms detector total.

## Export validity

`extended_benchmark_ios_sim_17promax.json` / `.csv` — schema **v4**, 10 warm-up +
100 measured, `rejected_out_of_distribution` path with
`detectorExecutedCount = 0 / detectorSkippedCount = 100`, routing a real measured
stage, no `NaN`/`Infinity`, 11 notes. Accepted/rejected/mismatch paths are kept
separate and are never averaged.
