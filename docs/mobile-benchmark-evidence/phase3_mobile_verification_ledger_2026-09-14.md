# Mobile-Performance Verification Ledger

Written 2026-09-14 during manuscript integration (worklog Entry 019). Every
number used in `sections/13_results.txt`'s mobile-performance subsection,
`sections/14_discussion.txt`'s mobile subsection, and
`sections/15_limitations_and_future_work.txt`'s mobile items was re-derived
directly from its named export file by an independent Python check run this
session (not copied from the prior draft pass), before any manuscript prose
was written. Result: **61/61 checks MATCH exactly. Zero corrections were
required.**

| # | Claim | Source | Field path | Expected (drafted) | Actual (re-derived) | Status |
|---|---|---|---|---|---|---|
| 1 | iPhone run1 end-to-end mean | run1 | `endToEnd.stats.meanMs` | 43.14 | 43.14 | MATCH |
| 2 | iPhone run1 end-to-end p95 | run1 | `endToEnd.stats.p95Ms` | 45.22 | 45.22 | MATCH |
| 3 | iPhone run1 thermalStatus | run1 | `deviceEnvironment.thermalStatus` | nominal | nominal | MATCH |
| 4 | iPhone run1 isEmulator | run1 | `deviceEnvironment.isEmulator` | False | False | MATCH |
| 5 | iPhone run1 buildIdentifier | run1 | `deviceEnvironment.buildIdentifier` | 31fb8f5576a6 | 31fb8f5576a6 | MATCH |
| 6 | iPhone run1 detectorExecutedCount | run1 | `endToEnd.detectorExecutedCount` | 0 | 0 | MATCH |
| 7 | iPhone run2 end-to-end mean | run2 | `endToEnd.stats.meanMs` | 44.26 | 44.26 | MATCH |
| 8 | iPhone run2 end-to-end p95 | run2 | `endToEnd.stats.p95Ms` | 45.03 | 45.03 | MATCH |
| 9 | iPhone run2 thermalStatus | run2 | `deviceEnvironment.thermalStatus` | fair | fair | MATCH |
| 10 | iPhone run2 isEmulator | run2 | `deviceEnvironment.isEmulator` | False | False | MATCH |
| 11 | iPhone run2 buildIdentifier | run2 | `deviceEnvironment.buildIdentifier` | 31fb8f5576a6 | 31fb8f5576a6 | MATCH |
| 12 | iPhone run2 detectorExecutedCount | run2 | `endToEnd.detectorExecutedCount` | 0 | 0 | MATCH |
| 13 | iPhone run3 end-to-end mean | run3 | `endToEnd.stats.meanMs` | 52.63 | 52.63 | MATCH |
| 14 | iPhone run3 end-to-end p95 | run3 | `endToEnd.stats.p95Ms` | 56.06 | 56.06 | MATCH |
| 15 | iPhone run3 thermalStatus | run3 | `deviceEnvironment.thermalStatus` | fair | fair | MATCH |
| 16 | iPhone run3 isEmulator | run3 | `deviceEnvironment.isEmulator` | False | False | MATCH |
| 17 | iPhone run3 buildIdentifier | run3 | `deviceEnvironment.buildIdentifier` | 31fb8f5576a6 | 31fb8f5576a6 | MATCH |
| 18 | iPhone run3 detectorExecutedCount | run3 | `endToEnd.detectorExecutedCount` | 0 | 0 | MATCH |
| 19 | iPhone run1 classifierInference mean (table claim) | run1 | `classifierStage.classifierInference.meanMs` | 21.5 | 21.5 | MATCH |
| 20 | iPhone run2 classifierInference mean (table claim) | run2 | `classifierStage.classifierInference.meanMs` | 20.87 | 20.87 | MATCH |
| 21 | iPhone run3 classifierInference mean (table claim) | run3 | `classifierStage.classifierInference.meanMs` | 21.97 | 21.97 | MATCH |
| 22 | Xiaomi accepted_corn mean | accepted_corn | `stats.meanMs` | 147.73 | 147.73 | MATCH |
| 23 | Xiaomi accepted_corn median | accepted_corn | `stats.medianMs` | 148.58 | 148.58 | MATCH |
| 24 | Xiaomi accepted_corn p95 | accepted_corn | `stats.p95Ms` | 157.59 | 157.59 | MATCH |
| 25 | Xiaomi accepted_corn detectorExecutedCount | accepted_corn | `detectorExecutedCount` | 100 | 100 | MATCH |
| 26 | Xiaomi accepted_corn detectorSkippedCount | accepted_corn | `detectorSkippedCount` | 0 | 0 | MATCH |
| 27 | Xiaomi accepted_pepper mean | accepted_pepper | `stats.meanMs` | 151.81 | 151.81 | MATCH |
| 28 | Xiaomi accepted_pepper median | accepted_pepper | `stats.medianMs` | 151.29 | 151.29 | MATCH |
| 29 | Xiaomi accepted_pepper p95 | accepted_pepper | `stats.p95Ms` | 160.91 | 160.91 | MATCH |
| 30 | Xiaomi accepted_pepper detectorExecutedCount | accepted_pepper | `detectorExecutedCount` | 100 | 100 | MATCH |
| 31 | Xiaomi accepted_pepper detectorSkippedCount | accepted_pepper | `detectorSkippedCount` | 0 | 0 | MATCH |
| 32 | Xiaomi accepted_tomato mean | accepted_tomato | `stats.meanMs` | 150.25 | 150.25 | MATCH |
| 33 | Xiaomi accepted_tomato median | accepted_tomato | `stats.medianMs` | 150.7 | 150.7 | MATCH |
| 34 | Xiaomi accepted_tomato p95 | accepted_tomato | `stats.p95Ms` | 158.67 | 158.67 | MATCH |
| 35 | Xiaomi accepted_tomato detectorExecutedCount | accepted_tomato | `detectorExecutedCount` | 100 | 100 | MATCH |
| 36 | Xiaomi accepted_tomato detectorSkippedCount | accepted_tomato | `detectorSkippedCount` | 0 | 0 | MATCH |
| 37 | Xiaomi rejected_out_of_distribution mean | rejected_out_of_distribution | `stats.meanMs` | 53.64 | 53.64 | MATCH |
| 38 | Xiaomi rejected_out_of_distribution median | rejected_out_of_distribution | `stats.medianMs` | 53.12 | 53.12 | MATCH |
| 39 | Xiaomi rejected_out_of_distribution p95 | rejected_out_of_distribution | `stats.p95Ms` | 57.58 | 57.58 | MATCH |
| 40 | Xiaomi rejected_out_of_distribution detectorExecutedCount | rejected_out_of_distribution | `detectorExecutedCount` | 0 | 0 | MATCH |
| 41 | Xiaomi rejected_out_of_distribution detectorSkippedCount | rejected_out_of_distribution | `detectorSkippedCount` | 100 | 100 | MATCH |
| 42 | Xiaomi selected_crop_mismatch mean | selected_crop_mismatch | `stats.meanMs` | 48.93 | 48.93 | MATCH |
| 43 | Xiaomi selected_crop_mismatch median | selected_crop_mismatch | `stats.medianMs` | 48.55 | 48.55 | MATCH |
| 44 | Xiaomi selected_crop_mismatch p95 | selected_crop_mismatch | `stats.p95Ms` | 56.99 | 56.99 | MATCH |
| 45 | Xiaomi selected_crop_mismatch detectorExecutedCount | selected_crop_mismatch | `detectorExecutedCount` | 0 | 0 | MATCH |
| 46 | Xiaomi selected_crop_mismatch detectorSkippedCount | selected_crop_mismatch | `detectorSkippedCount` | 100 | 100 | MATCH |
| 47 | Xiaomi buildIdentifier | pilot | `deviceEnvironment.buildIdentifier` | f5193cb92d24 | f5193cb92d24 | MATCH |
| 48 | Xiaomi isCharging | pilot | `deviceEnvironment.isCharging` | True | True | MATCH |
| 49 | Xiaomi isEmulator | pilot | `deviceEnvironment.isEmulator` | False | False | MATCH |
| 50 | Xiaomi thermalStatus | pilot | `deviceEnvironment.thermalStatus` | none | none | MATCH |
| 51 | Xiaomi memory sample 1 timestamp | pilot | `memorySamples[0].timestampEpochMs` | 1789410592298 | 1789410592298 | MATCH |
| 52 | Xiaomi memory sample 2 timestamp | pilot | `memorySamples[1].timestampEpochMs` | 1789410592298 | 1789410592298 | MATCH |
| 53 | iPhone run1 memory before (KB) | run1 | `memorySamples[0].totalPssKb` | 251936 | 251936 | MATCH |
| 54 | iPhone run1 memory after (KB) | run1 | `memorySamples[1].totalPssKb` | 251952 | 251952 | MATCH |
| 55 | Emulator endToEnd mean | emulator | `endToEnd.stats.meanMs` | 18.93 | 18.93 | MATCH |
| 56 | Emulator classifierInference mean | emulator | `classifierStage.classifierInference.meanMs` | 17.04 | 17.04 | MATCH |
| 57 | Simulator endToEnd mean | sim | `endToEnd.stats.meanMs` | 27.8 | 27.8 | MATCH |
| 58 | Simulator classifierInference mean | sim | `classifierStage.classifierInference.meanMs` | 10.84 | 10.84 | MATCH |
| 59 | Xiaomi detector 1920x1080 mean | pilot | `detectorStageBySize.1920x1080.detectorInference.meanMs` | 93.75 | 93.75 | MATCH |
| 60 | Xiaomi detector 1280x960 mean | pilot | `detectorStageBySize.1280x960.detectorInference.meanMs` | 92.79 | 92.79 | MATCH |
| 61 | Xiaomi detector 640x480 mean | pilot | `detectorStageBySize.640x480.detectorInference.meanMs` | 94.13 | 94.13 | MATCH |

## Source files checked

- `supplementary/phase3_iphone15promax_device_2026-09-14/extended_benchmark_1789406662421.json` (run 1)
- `supplementary/phase3_iphone15promax_device_2026-09-14/extended_benchmark_1789406741380.json` (run 2)
- `supplementary/phase3_iphone15promax_device_2026-09-14/extended_benchmark_1789406818047.json` (run 3)
- `supplementary/phase3_xiaomi_redmi_note_14_pro_plus_committed_device_2026-09-14/pilot_run_not_a_measured_session/extended_benchmark_1789410592317.json`
- `supplementary/phase3_emulator_acceptance_2026-09-14/extended_benchmark_emulator.json` (supplementary/harness only)
- `supplementary/phase3_ios_simulator_schema_v4_2026-09-14/extended_benchmark_ios_sim_17promax.json` (supplementary/harness only)

## Method

A standalone Python script (not the manuscript-writing process) loaded each
JSON export fresh, extracted the field at the stated path, rounded to 2
decimal places where the drafted claim was numeric, and compared. Boolean and
string fields (`isEmulator`, `isCharging`, `thermalStatus`, `buildIdentifier`)
were compared for exact equality. No value in this ledger was taken from
memory, from the prior conversation turn's summary, or from any intermediate
notebook — each was read from the file on this pass.

## Outcome

No mismatch was found, so no correction to the mobile-performance numbers was
needed and none is logged in the worklog beyond noting this ledger's clean
result. Had a mismatch occurred, the rule applied would have been: the raw
export wins, the manuscript number is corrected to match it, and the
discrepancy and its resolution are logged as a new worklog entry per the
owner's instruction.

