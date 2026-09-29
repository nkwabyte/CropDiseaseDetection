EMULATOR BENCHMARK PROVENANCE

Status: exploratory harness-validation evidence only; not physical-device performance.
Capture date: 2026-09-14
AVD: Medium_Phone
Reported device: Google sdk_gphone16k_arm64
Android: 17 (SDK 37)
ABI: arm64 emulator
Display: 1080 x 2400, density 420
Application: com.nkwabyte.cropdiseasedetection
Activity: com.nkwabyte.cropdiseasedetection.MainActivity

Files:

benchmark_1789378426995.csv
SHA-256: b7ed26cc7cec57358f9de3ab10ea6128020f4df0b1410cfdbe887b041df97713
Rows including headers/blank separator: 207

benchmark_1789378457078.csv
SHA-256: 2a75c2d0250ffefdeb413208aa4620071d271612348b6bf7b8ef88a6ba80d9e2
Rows including headers/blank separator: 207

Each export contains four aggregate records followed by 200 raw measurements: 50 classifier runs and 50 detector runs for each of three source-image dimensions. Each condition records 10 warm-up runs and 50 measured runs.

Important limitations:

- The emulator uses host compute and is not representative of Samsung Galaxy A10 or iPhone 15 Pro Max hardware.
- Emulator startup reported software rendering because of host memory pressure.
- These measurements establish that the instrumentation executes and exports raw data; they do not unblock the physical mobile-latency claim.
- MCP call duration was not used as application latency.
- The CSV records inference-stage timing but does not yet demonstrate every requested publication metric, such as energy, installed application size, sustained thermal behavior, CPU utilization, or complete end-to-end image pipeline timing.
- The two runs should not be collapsed without analyzing run order and outliers. In particular, the first 640 x 480 detector run had p95 49.868 ms, while the second had p95 43.344 ms despite similar central tendency.

Next evidence required:

1. Repeat a controlled emulator session after reducing host memory pressure and record host hardware/configuration.
2. Run the same release/profile build and fixed image manifest on a physical Samsung Galaxy A10.
3. Implement/run the equivalent benchmark on iPhone 15 Pro Max.
4. Export raw structured results from both phones.
5. Verify mobile output agreement and accuracy against the held-out desktop evaluation.
