# Android Status

This repository is the current Android client prototype for the Adaptive Secure Overlay IPsec branch.

## Milestones

| Area | State | Notes |
| --- | --- | --- |
| APK build | done | Debug APK build path is working through `build-android-apk.ps1`. |
| Android UI | done | Jetpack Compose shell, route selection and session log are present. |
| Overlay session model | partial | Session flow is modeled in the client, but still mocked. |
| Control-plane transport | not yet | No live network transport is wired on Android yet. |
| Crypto/core | partial | Rust `native-core` now contains X25519, HKDF-SHA256 and first JNI exports, but Android NDK packaging is still pending. |
| Data-plane integration | not yet | `VpnService`, IKE/IPsec or equivalent Android data-plane integration is still pending. |
| Release packaging | partial | APK exists for prototype use; no production delivery path yet. |

## What is already useful

- public Android branch exists and builds;
- mobile UI direction is visible and testable;
- route semantics match the main lab model;
- repository can now serve as the Android implementation track instead of an empty placeholder.

## What comes next

1. move session logic into a background service;
2. package the Rust library through Android NDK tooling;
3. add transport between peers/services;
4. connect the branch to Android VPN integration.
