package ru.adaptive.overlay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class OverlayViewModel : ViewModel() {
    val nodes = listOf(
        OverlayNode("User1", "192.168.101.10", roleHint = "Linux endpoint"),
        OverlayNode("User2", "192.168.102.10", roleHint = "Linux endpoint"),
        OverlayNode("User3", "192.168.103.10", roleHint = "Linux endpoint"),
        OverlayNode("User4", "192.168.104.10", roleHint = "Linux endpoint"),
        OverlayNode("User5", "192.168.105.10", roleHint = "Linux endpoint"),
        OverlayNode("User6", "192.168.106.10", roleHint = "Linux endpoint"),
        OverlayNode("User7", "192.168.107.10", roleHint = "Linux endpoint"),
        OverlayNode("User8", "192.168.108.10", roleHint = "Linux endpoint"),
        OverlayNode("User9", "192.168.109.10", roleHint = "Linux endpoint"),
        OverlayNode("User10", "192.168.110.10", roleHint = "Linux endpoint"),
        OverlayNode("User11", "10.255.11.2", roleHint = "OpenWRT endpoint"),
        OverlayNode("User12", "10.255.12.2", roleHint = "RouterOS relay"),
        OverlayNode("User13", "192.168.113.199", roleHint = "Windows endpoint"),
        OverlayNode("Android", "dynamic", roleHint = "Android endpoint"),
    )

    var localNode by mutableStateOf(nodes.last())
        private set

    var responder by mutableStateOf(nodes.first())
        private set

    var manualX1 by mutableStateOf(nodes[1])
        private set

    var manualX2 by mutableStateOf(nodes[2])
        private set

    var routeMode by mutableStateOf(RouteMode.RANDOM)
        private set

    var allowResearchLoops by mutableStateOf(true)
        private set

    var phase by mutableStateOf(SessionPhase.IDLE)
        private set

    var lastRoute by mutableStateOf<RouteSelection?>(null)
        private set

    var logs by mutableStateOf(
        listOf(
            SessionLogEntry("Android client UI prototype ready."),
            SessionLogEntry("Mode: custom overlay control-plane + direct ESP data-plane."),
        ),
    )
        private set

    fun selectLocalNode(node: OverlayNode) {
        localNode = node
        append("Local node set to ${node.name} (${node.roleHint}).")
    }

    fun selectResponder(node: OverlayNode) {
        responder = node
        append("Responder set to ${node.name}.")
    }

    fun selectManualX1(node: OverlayNode) {
        manualX1 = node
        append("Manual X1 set to ${node.name}.")
    }

    fun selectManualX2(node: OverlayNode) {
        manualX2 = node
        append("Manual X2 set to ${node.name}.")
    }

    fun chooseRouteMode(mode: RouteMode) {
        routeMode = mode
        append("Route mode: ${mode.name.lowercase()}.")
    }

    fun toggleResearchLoops(enabled: Boolean) {
        allowResearchLoops = enabled
        append(if (enabled) "Research loop mode enabled." else "Research loop mode disabled.")
    }

    fun connect() {
        val route = when (routeMode) {
            RouteMode.MANUAL -> RouteSelection(
                initiator = localNode,
                responder = responder,
                x1 = manualX1,
                x2 = manualX2,
            )
            RouteMode.RANDOM -> randomRoute()
        }

        lastRoute = route
        logs = emptyList()
        phase = SessionPhase.MGMT
        append("MGMT_INIT ${route.initiator.name} -> ${route.x1.name}", accent = true)
        append("MGMT_AUTH ${route.x1.name} -> ${route.x2.name}")
        phase = SessionPhase.KE
        append("PRIV_INLINE_START ${route.initiator.name} -> ${route.x1.name}")
        append("PRIV_INLINE_X2 ${route.x1.name} -> ${route.x2.name}")
        append("PRIV_DELIVER ${route.x2.name} -> ${route.responder.name}")
        phase = SessionPhase.META
        append("Sealed initiator locator delivered only to responder.")
        append("X1 sees A/X2, X2 sees B but not initiator locator.", accent = true)
        phase = SessionPhase.SA
        append("Derive ESP key material from X25519/HKDF context.")
        append("Manual SA install plan prepared.")
        phase = SessionPhase.ACTIVE
        append("ESP active: ${route.initiator.name} <-> ${route.responder.name}", accent = true)
    }

    fun disconnect() {
        phase = SessionPhase.IDLE
        append("Session cleanup requested.")
    }

    private fun randomRoute(): RouteSelection {
        val pool = if (allowResearchLoops) nodes else nodes.filter { it.name != localNode.name && it.name != responder.name }
        val fallback = pool.ifEmpty { nodes }
        val x1 = fallback.random()
        val x2 = fallback.random()
        return RouteSelection(
            initiator = localNode,
            responder = responder,
            x1 = x1,
            x2 = x2,
        )
    }

    private fun append(message: String, accent: Boolean = false) {
        logs = logs + SessionLogEntry(message, accent)
    }
}
