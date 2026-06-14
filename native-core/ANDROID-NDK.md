# Android NDK Packaging

This document describes the next step after the current JNI scaffold.

## Current state

- Rust exports already exist in `native-core/src/ffi.rs`
- Kotlin bridge already exists in `app/src/main/java/ru/adaptive/overlay/NativeCryptoBridge.kt`
- Android packaging path already exists in `app/src/main/jniLibs/`
- APK build script already calls `build-native-core.ps1`

## What is still missing

The actual shared library build for Android ABIs:

- `arm64-v8a`
- `armeabi-v7a`
- `x86_64`

## Intended packaging flow

1. build Rust `cdylib` as `libadaptive_overlay_core.so` for each Android target;
2. place the output in:
   - `app/src/main/jniLibs/arm64-v8a/`
   - `app/src/main/jniLibs/armeabi-v7a/`
   - `app/src/main/jniLibs/x86_64/`
3. package APK normally through Gradle;
4. `NativeCryptoBridge` stops reporting `JNI scaffolded` and starts reporting a live loaded backend.

## Build helper

`build-native-core.ps1` is now the dedicated entry point for this packaging layer.

Today it does two useful jobs:

- creates the Android `jniLibs` packaging layout;
- copies prebuilt `.so` files if they already exist;
- cleanly skips native packaging when Rust is not installed on the machine.
