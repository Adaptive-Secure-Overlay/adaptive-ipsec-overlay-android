# Android Status

This repository is the current Android client prototype for the Adaptive Secure Overlay IPsec branch.

## Milestones

| Area | State | Notes |
| --- | --- | --- |
| APK build | done | Debug APK build path is working through `build-android-apk.ps1`. |
| Android UI | done | Jetpack Compose shell, route selection and session log are present. |
| Overlay session model | partial | Session flow is modeled in the client, but still mocked. |
| Control-plane transport | not yet | No live network transport is wired on Android yet. |
| Crypto/core | partial | Rust `native-core` now contains X25519, HKDF-SHA256 and JNI exports; real `.so` libraries are built for `arm64-v8a`, `armeabi-v7a` and `x86_64`, packaged into the APK and verified live in an Android emulator. |
| Data-plane integration | not yet | `VpnService`, IKE/IPsec or equivalent Android data-plane integration is still pending. |
| Release packaging | partial | APK exists for prototype use; no production delivery path yet. |

## What is already useful

- public Android branch exists and builds;
- mobile UI direction is visible and testable;
- route semantics match the main lab model;
- Rust JNI bridge is confirmed live on Android Emulator (`x86_64`, API 35);
- repository can now serve as the Android implementation track instead of an empty placeholder.

## What comes next

1. move session logic into a background service;
2. extend the live JNI proof into real control-plane messages and session state handoff;
3. add transport between peers/services;
4. connect the branch to Android VPN integration.

## Latest live proof

Date: `2026-06-14`

Environment:

- AVD: `ASO_X86_64_API35`
- ABI: `x86_64`
- APK: debug build from `app/build/outputs/apk/debug/app-debug.apk`

Observed startup log:

```text
AdaptiveSecureOverlay: native_backend=Rust native-core; state=JNI live; details=JNI bridge is live and Rust self-test passed. Version: adaptive-native-core/0.1.0-jni.
```
