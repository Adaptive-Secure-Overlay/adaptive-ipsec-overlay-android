package ru.adaptive.overlay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.adaptive.overlay.ui.theme.AdaptiveSecureOverlayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdaptiveSecureOverlayTheme {
                OverlayApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverlayApp(vm: OverlayViewModel = viewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Adaptive Secure Overlay")
                        Text(
                            "Android basic client prototype",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RouteSummaryCard(vm)
            CryptoCoreCard(vm)
            NodeSelectionCard(vm)
            RouteControlCard(vm)
            ControlButtons(vm)
            LogCard(vm)
        }
    }
}

@Composable
private fun CryptoCoreCard(vm: OverlayViewModel) {
    OutlinedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Crypto core", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(vm.cryptoBackend.backendName) })
                AssistChip(onClick = {}, label = { Text(vm.cryptoBackend.stateLabel) })
            }
            Text(
                vm.cryptoBackend.details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RouteSummaryCard(vm: OverlayViewModel) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Session status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("Phase: ${vm.phase.label}") })
                AssistChip(onClick = {}, label = { Text("Mode: ${vm.routeMode.name}") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RouteNodeChip("A", vm.lastRoute?.initiator?.name ?: vm.localNode.name)
                RouteNodeChip("X1", vm.lastRoute?.x1?.name ?: "-")
                RouteNodeChip("X2", vm.lastRoute?.x2?.name ?: "-")
                RouteNodeChip("B", vm.lastRoute?.responder?.name ?: vm.responder.name)
            } 
            Text(
                "Control-plane goes through adaptive multi-hop overlay, then data-plane is expected to move as direct ESP.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RouteNodeChip(label: String, value: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text("$label: $value", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun NodeSelectionCard(vm: OverlayViewModel) {
    OutlinedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Nodes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            NodeDropdown("Local node", vm.localNode, vm.nodes, vm::selectLocalNode)
            NodeDropdown("Responder", vm.responder, vm.nodes.filter { it.name != vm.localNode.name }, vm::selectResponder)
        }
    }
}

@Composable
private fun RouteControlCard(vm: OverlayViewModel) {
    OutlinedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Route policy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = vm.routeMode == RouteMode.RANDOM,
                    onClick = { vm.chooseRouteMode(RouteMode.RANDOM) },
                    label = { Text("Random") },
                )
                FilterChip(
                    selected = vm.routeMode == RouteMode.MANUAL,
                    onClick = { vm.chooseRouteMode(RouteMode.MANUAL) },
                    label = { Text("Manual X1/X2") },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Research loop mode")
                    Text(
                        "Allows cases like A-B-A-B in small topology experiments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = vm.allowResearchLoops,
                    onCheckedChange = vm::toggleResearchLoops,
                )
            }
            if (vm.routeMode == RouteMode.MANUAL) {
                NodeDropdown("Manual X1", vm.manualX1, vm.nodes, vm::selectManualX1)
                NodeDropdown("Manual X2", vm.manualX2, vm.nodes, vm::selectManualX2)
            }
        }
    }
}

@Composable
private fun ControlButtons(vm: OverlayViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = vm::connect, modifier = Modifier.weight(1f)) {
            Text("Start session")
        }
        OutlinedButton(onClick = vm::disconnect, modifier = Modifier.weight(1f)) {
            Text("Cleanup")
        }
    }
}

@Composable
private fun LogCard(vm: OverlayViewModel) {
    OutlinedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Session log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(vm.logs.reversed()) { item ->
                    Text(
                        item.text,
                        color = if (item.isAccent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun NodeDropdown(
    title: String,
    selected: OverlayNode,
    options: List<OverlayNode>,
    onSelect: (OverlayNode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text("${selected.name}  ${selected.ip}")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { node ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(node.name)
                                Text(
                                    "${node.ip}:${node.port}  ${node.roleHint}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            onSelect(node)
                        },
                    )
                }
            }
        }
    }
}
