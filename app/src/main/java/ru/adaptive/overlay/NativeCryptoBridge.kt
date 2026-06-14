package ru.adaptive.overlay

object NativeCryptoBridge {
    private const val LIB_NAME = "adaptive_overlay_core"

    private val loadResult: NativeLoadResult by lazy {
        try {
            System.loadLibrary(LIB_NAME)
            val version = nativeVersion()
            val selfTest = nativeSelfTest()
            NativeLoadResult(
                loaded = true,
                version = version,
                selfTestPassed = selfTest,
                message = if (selfTest) {
                    "JNI bridge is live and Rust self-test passed."
                } else {
                    "JNI bridge loaded, but Rust self-test failed."
                },
            )
        } catch (_: UnsatisfiedLinkError) {
            NativeLoadResult(
                loaded = false,
                version = "native library not packaged yet",
                selfTestPassed = false,
                message = "JNI bridge is scaffolded in Kotlin and Rust, but Android NDK packaging is still the next step.",
            )
        } catch (t: Throwable) {
            NativeLoadResult(
                loaded = false,
                version = "load error",
                selfTestPassed = false,
                message = "JNI bridge exists, but runtime loading failed: ${t.javaClass.simpleName}.",
            )
        }
    }

    fun backendStatus(): CryptoBackendStatus {
        val state = when {
            loadResult.loaded && loadResult.selfTestPassed -> "JNI live"
            loadResult.loaded -> "JNI loaded"
            else -> "JNI scaffolded"
        }
        return CryptoBackendStatus(
            backendName = "Rust native-core",
            stateLabel = state,
            details = "${loadResult.message} Version: ${loadResult.version}.",
        )
    }

    private external fun nativeVersion(): String
    private external fun nativeSelfTest(): Boolean
}

data class NativeLoadResult(
    val loaded: Boolean,
    val version: String,
    val selfTestPassed: Boolean,
    val message: String,
)
