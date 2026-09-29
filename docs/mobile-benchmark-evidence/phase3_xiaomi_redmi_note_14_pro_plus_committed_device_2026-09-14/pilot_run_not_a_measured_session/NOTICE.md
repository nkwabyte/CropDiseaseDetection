> **SUPERSEDED IN PART — 2026-09-14 (worklog ENTRY 018).** Reason (1) below ("the device was plugged in") is **superseded**: the owner ruled charging is a measured experimental condition for this application, not a disqualifier, so this run's latency figures **are now citable**, with charging disclosed per-figure and never used to estimate an unplugged result. Reasons (2) and (3) are **NOT superseded** — they are measurement-validity defects (a structurally void, identical-timestamp memory delta; a cold-load figure that is mmap setup, not first-use latency), independent of the charging question, and still apply: do not cite this run's memory delta, and do not present its cold-load figure as complete cold-start latency. See `sections/00_worklog.txt` Entry 018 and the C005 claim-matrix row for the exact subclaims this run now supports.

---

# ⚠️ PILOT RUN — NOT A MEASURED SESSION. DO NOT CITE.

This export is retained as **plumbing evidence only**: it proves the extended
benchmark runs to completion on the Xiaomi Redmi Note 14 Pro+ 5G from a release
build of the committed source, and it is what exposed two measurement defects.

**It is not one of the three required sessions and no number in it may be
quoted**, for three independent reasons:

1. **The device was plugged in.** `deviceEnvironment.isCharging = true`
   (`AC powered: true`, level 100%). The protocol requires the handset to be
   charged beforehand and **unplugged** during measurement.
2. **The memory figures are void.** `memorySamples` contains two entries with
   identical values *and an identical timestamp* (`1789410592298`), because both
   samples were taken back-to-back at the end of the run rather than bracketing
   it. The delta is structurally zero and the accompanying note ("taken
   immediately before and after the measured loop") was false.
3. **The build predates the fixes** for (2) and for the cold-load caveat below,
   so it will not match the source that produces the real sessions.

## What this run does legitimately establish

- The five routing paths are all measurable against the checksum-locked real
  photographs: **accepted corn / pepper / tomato, out-of-distribution, and
  selected-crop mismatch**, each 10 warmup + 100 measured, **100 ok / 0 failed**.
- **The fail-closed guarantee holds on this device.** Both non-accepted paths
  report `detectorExecutedCount = 0` and `detectorSkippedCount = 100` — the
  detector never ran for out-of-distribution or crop-mismatched input.
- Provenance is intact: `buildIdentifier = f5193cb92d24` (no `-dirty` suffix),
  `schemaVersion = 4`, `isEmulator = false`, `manufacturer = Xiaomi`,
  `model = 24115RA8EG`, all four fixture SHA-256s matching `BenchmarkImageSet`.
- The app was the **release** build (`flags=[ HAS_CODE ALLOW_CLEAR_USER_DATA
  ALLOW_BACKUP ]` — no `DEBUGGABLE`, no `TEST_ONLY`).

## Third defect surfaced (caveat, not a code-behaviour bug)

`coldLoad` reports **~0.34–1.4 ms** to "cold load" a 30 MB classifier and a
9.8 MB detector. `release()` genuinely destroys both modules, so the reload is
real — but sub-millisecond is far too fast to have read those files off storage.
ExecuTorch's `Module.load()` is mmap-backed and lazy: it sets up the program
header, and method initialisation plus page-in of the weights are deferred to
the first `forward()`. The figure is therefore **not** "time until the model is
ready to infer"; that cost is inside the first inference. A caveat to this
effect was added to the export's own `coldLoad.note` on both platforms rather
than changing production load behaviour.

## Conditions at the time of this run

Thermal status 0 - battery 100% - AC powered - battery temp 31.8 °C -
power saving off - adaptive brightness ON (not yet pinned) - display 60 Hz.
