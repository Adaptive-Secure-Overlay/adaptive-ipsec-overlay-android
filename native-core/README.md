# Android Native Core

This folder is the first Rust `crypto/core` layer for the Android track.

It is intentionally small and focused:

- session bootstrap model for the overlay exchange;
- X25519 shared-secret derivation;
- HKDF-SHA256 directional key schedule;
- deterministic test coverage for the first cross-language core.

## Why this exists

The Android UI branch already builds and demonstrates the routing logic, but until now all crypto was still conceptual at the client layer. This Rust crate is the first step toward a real mobile core that can later be connected to:

- Kotlin/Android UI;
- Android background service;
- `VpnService` or another mobile data-plane path;
- future cross-platform mobile or desktop reuse.

## Current scope

The crate does not yet expose JNI bindings. Right now it serves as:

1. a real implementation of the first key-derivation layer;
2. a clean place to grow the Android crypto/session engine;
3. a future candidate for binding into Kotlin.

## Build

```bash
cargo test
```

