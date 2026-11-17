> **SUPERSEDED IN PART — 2026-09-14 (worklog ENTRY 018).** The owner issued an authoritative protocol decision: mid-session charging, non-nominal thermal state and back-to-back sessions are now treated as measured real-use conditions, not automatic disqualifiers. The pilot run below is **now admissible as evidence** (charging disclosed as a condition, never estimated for effect or described as unplugged operation) — see `sections/00_worklog.txt` Entry 018 and the C005 row of the claim matrix for the exact subclaim it supports. The "Status: INCOMPLETE" line, the "not citable" framing below, and the three-cooled-sessions requirement reflect the protocol as it stood when this file was written and are kept verbatim as the historical record — do not delete them. What is **not** superseded: the memory-delta and cold-load findings below are independent measurement-validity defects, not protocol-compliance rules, and still govern (the pilot's memory delta remains unusable; its cold-load figure remains caveated).

---

# Phase 3 — Xiaomi Redmi Note 14 Pro+ 5G, committed-build device evidence

**Status: INCOMPLETE — no measured session has been recorded yet.**
This directory currently holds device metadata and one **pilot run that must not
be cited** (see `pilot_run_not_a_measured_session/NOTICE.md`).

Runbook: `../phase3_redmi_note_14_pro_plus_benchmark_runbook.md`

## Device

Xiaomi / Redmi **Redmi Note 14 Pro+ 5G**, model `24115RA8EG`, codename
`amethyst`, Android **16** (SDK 36), HyperOS **OS3.0** (`OS3.0.306.0.WOPMIXM`),
security patch `2026-08-01`, SoC **QTI SM7635** (platform `volcano`), 8 cores,
**arm64-v8a only**, ~11.09 GiB RAM, display `1220x2712` @ 480 dpi running at
60 Hz. Full dump: `device_metadata.txt`.

Inference runs on **ExecuTorch XNNPACK (CPU) only** — verified by scanning
`libexecutorch.so` from the installed APK for backend symbols. The Snapdragon
NPU and GPU are **not** exercised.

> This device is **not** equivalent to the Samsung Galaxy A10 it replaced, and
> no Galaxy A10 measurement has ever existed. Nothing may be carried across.

## Contents

| Path | What it is |
|---|---|
| `device_metadata.txt` | `getprop` / `dumpsys` dump: identity, OS, SoC, CPU frequencies, RAM, storage, display, install flags |
| `pilot_run_not_a_measured_session/` | One completed export, **not citable** — plumbing proof + the runs that exposed two measurement defects |

## Still required before C005 can move

1. **Three independent, cooled, unplugged sessions** on this handset, from an
   exact committed build whose `buildIdentifier` carries **no `-dirty` suffix`**.
2. **A matching re-run of the iPhone 15 Pro Max** from the same commit, schema,
   model artifacts and checksum-locked image manifest. The existing iPhone
   exports (entry 015) have no accepted-path result and were built dirty.
3. A four-row cross-platform comparison with every row explicitly labelled, and
   **accepted / rejected / mismatch paths never averaged together**.

## Verification checklist applied to every export before it is accepted

- `deviceEnvironment.isEmulator` = `false`
- `deviceEnvironment.manufacturer` = `Xiaomi`, `model` = `24115RA8EG`
- `deviceEnvironment.buildIdentifier` = the exact committed short SHA, **and does
  not end in `-dirty`**
- `deviceEnvironment.isCharging` = `false` (unplugged for the measurement)
- `schemaVersion` = `4`
- all four fixture SHA-256s match `BenchmarkImageSet`
- `offlineFailureCount` = 0 on every path
- `detectorExecutedCount` = 0 on `rejected_out_of_distribution` and
  `selected_crop_mismatch` (the fail-closed guarantee)
- `memorySamples` before/after differ and carry **different timestamps**
