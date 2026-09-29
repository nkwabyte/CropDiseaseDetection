# Android and iOS benchmark protocol

Primary physical devices are Samsung Galaxy A10 and iPhone 15 Pro Max. Record exact variant, SoC, RAM, OS, free storage, battery condition, thermal state, power mode, build type, model, and thread configuration.

Add minimally invasive, monotonic timing and memory instrumentation to native bridges and common orchestration. Export structured records with platform/device alias, build/model hashes, cold/warm flag, image dimensions, route, model load, decode, preprocessing, classifier, routing, detector, postprocessing/NMS, end-to-end time, memory, CPU, energy/battery estimate where supported, and status. Keep network/upload time separate.

Use the same checksum-locked image set on both platforms, spanning supported crops, healthy/diseased, OOD, multiple objects, and varied resolutions. Separate cold start. Warm up, then default to at least 100 measured runs per condition when feasible. Randomize/counterbalance order, monitor throttling, and use release/profile builds.

Report median, mean, standard deviation, p90, p95, confidence intervals, model size, installed app size, offline success, Android/iOS agreement, and mobile-export accuracy drift. Record failures and exclusions.

Simulator/emulator tests may validate instrumentation and provide exploratory timing, but must be stored separately with host details and never pooled with phone results.

If Claude cannot operate the phones, it must implement export instrumentation, create Android/iOS runbooks, provide a fixed image manifest, explain release builds/cold-state/thermal/repetition/export procedures, and wait for imported JSON/CSV before finalizing physical-device Results. Never use README latency estimates as measurements.

## Android emulator MCP workflow

Claude Desktop has an MCP server named `android-emulator`, pinned to version 2.0.0. The verified local AVD is `Medium_Phone`, currently reporting Android 17/API 37, 1080 × 2400 pixels, and density 420. This AVD is exploratory infrastructure and must not be represented as a Samsung Galaxy A10.

Use MCP tools to inspect the device, install/launch/stop/clear the app, navigate repeatable flows, capture screenshots, assert UI state, and retrieve filtered logcat output. Use the Android SDK outside the MCP when starting/stopping the AVD, building the APK, collecting Android Studio profiler traces, or running benchmark/Gradle tasks.

Do not calculate inference latency from MCP call duration, screenshot duration, gesture duration, or the time between MCP requests. Those values include Claude, MCP, process, ADB, rendering, and transport overhead. Publication-grade model and pipeline timings must be emitted by monotonic in-app instrumentation, AndroidX Macrobenchmark/Microbenchmark where applicable, Perfetto/Android Studio profiling, or structured app benchmark exports.

For emulator experiments, record the host Mac hardware/load, AVD name and configuration, Android/API version, ABI, virtual CPU/RAM, graphics backend, app build, and model hash. The current emulator warned that software rendering was selected because of host memory pressure; do not publish its current timing as representative performance without repeating under controlled host conditions.
