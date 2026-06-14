# JNI Bridge Notes

This crate now includes the first JNI-facing exports for the Android track.

Current exported functions:

- `nativeVersion()` -> returns the Rust core version label
- `nativeSelfTest()` -> runs a deterministic X25519/HKDF self-test

## Current state

The Kotlin side already calls the bridge through `NativeCryptoBridge`, but the native library is not yet packaged into the APK. Until Android NDK integration is added, the app will report `JNI scaffolded` instead of loading the Rust library at runtime.

## Next integration step

1. add Android NDK packaging/build flow for the Rust cdylib;
2. place the resulting `libadaptive_overlay_core.so` into the app native packaging path;
3. replace the scaffolded runtime status with live JNI status.
