package ru.adaptive.overlay

data class OverlayNode(
    val name: String,
    val ip: String,
    val port: Int = 9000,
    val roleHint: String,
)

enum class RouteMode {
    RANDOM,
    MANUAL,
}

enum class SessionPhase(val label: String) {
    IDLE("Idle"),
    MGMT("Mgmt mesh"),
    KE("IKE-like exchange"),
    META("Privacy metadata"),
    SA("ESP material"),
    ACTIVE("ESP active"),
    FAILED("Failed"),
}

data class SessionLogEntry(
    val text: String,
    val isAccent: Boolean = false,
)

data class RouteSelection(
    val initiator: OverlayNode,
    val responder: OverlayNode,
    val x1: OverlayNode,
    val x2: OverlayNode,
)
