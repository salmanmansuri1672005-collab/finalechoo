package com.echoos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.echoos.viewmodel.EchoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Permission & autonomy center + activity history (Salman UI, Tushar rules).
 *  SRS §9.7, §11.4, FR-11, FR-12. */

private val capabilityInfo = listOf(
    Triple("location", "Location & geofencing", "Needed for arrive/leave triggers"),
    Triple("notifications", "Notification access", "Needed for EchoLens & priorities"),
    Triple("calendar", "Calendar", "Needed for planner & event triggers"),
    Triple("connectivity", "Bluetooth / connectivity", "Needed for driving mode"),
    Triple("dnd", "Do Not Disturb control", "Needed for focus & silent modes"),
    Triple("messaging", "Messaging (sensitive)", "Always requires confirmation"),
    Triple("settings", "Device settings (simulated)", "Brightness etc. — demo only"),
    Triple("camera", "Camera intelligence", "Extract dates/tasks from images"),
)

@Composable
fun PermissionCenterScreen(vm: EchoViewModel) {
    val enabled by vm.capabilities.collectAsState()

    LazyColumn(Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Permission & Autonomy Center",
                style = MaterialTheme.typography.headlineSmall)
            Text("EchoOS uses only the sources you enable here. Disabling one " +
                "takes effect immediately — automations that need it are blocked " +
                "and the block is logged.", style = MaterialTheme.typography.bodyMedium)
        }
        items(capabilityInfo) { (cap, title, why) ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        Text(why, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = cap in enabled,
                        onCheckedChange = { vm.setCapability(cap, it) })
                }
            }
        }
        item {
            Text("Privacy controls", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.deleteAllCommitments() }) {
                    Text("Delete commitments") }
                OutlinedButton(onClick = { vm.deleteAllHistory() }) {
                    Text("Delete history") }
            }
            Text("Minimum data, progressive permissions, full deletion — SRS §15.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun HistoryScreen(vm: EchoViewModel) {
    val executions by vm.executions.collectAsState()
    val events by vm.contextEvents.collectAsState()
    val fmt = SimpleDateFormat("dd MMM HH:mm:ss", Locale.getDefault())

    LazyColumn(Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Activity History", style = MaterialTheme.typography.headlineSmall)
            Text("Every detection, suggestion, confirmation, execution and block — " +
                "with the reason why (explainability, SRS §11.4).",
                style = MaterialTheme.typography.bodyMedium)
        }
        if (executions.isEmpty()) {
            item { Text("Nothing yet. Activity appears here as EchoOS works.",
                style = MaterialTheme.typography.bodySmall) }
        }
        items(executions) { e ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text("${fmt.format(Date(e.timestamp))} · ${e.outcome.uppercase()}" +
                        if (e.simulated) " · SIMULATED" else "",
                        style = MaterialTheme.typography.titleSmall,
                        color = when {
                            e.outcome.contains("failure") || e.outcome == "blocked" ->
                                MaterialTheme.colorScheme.error
                            e.outcome == "executed_success" ->
                                MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurface
                        })
                    Text(e.explanation, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text("Context events (${events.size})",
            style = MaterialTheme.typography.titleMedium) }
        items(events.take(30)) { ev ->
            Text("• ${fmt.format(Date(ev.timestamp))} ${ev.type}" +
                (ev.value?.let { ": $it" } ?: "") + " [${ev.source}]" +
                if (ev.simulated) " [SIMULATED]" else "",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
