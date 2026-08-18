package com.echoos.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.echoos.domain.ActionCatalog
import com.echoos.viewmodel.EchoViewModel

/** Dashboard + NL automation creation + preview (Salman, SRS §17, §9.1). */

@Composable
fun DashboardScreen(vm: EchoViewModel, onOpenHistory: () -> Unit) {
    val automations by vm.automations.collectAsState()
    val commitments by vm.commitments.collectAsState()
    val executions by vm.executions.collectAsState()
    val online by vm.backendOnline.collectAsState()

    LazyColumn(Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("EchoOS", style = MaterialTheme.typography.headlineMedium)
            Text("Understand. Decide. Automate. Learn.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary)
            when (online) {
                null -> AssistChip(onClick = {}, label = { Text("Checking AI backend…") })
                true -> AssistChip(onClick = {}, label = { Text("AI backend online") })
                false -> AssistChip(onClick = { vm.checkBackend() },
                    label = { Text("Offline — local mode (tap to retry)") })
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Demo controls (SIMULATED)",
                        style = MaterialTheme.typography.titleSmall)
                    Text("Test events for the hackathon demo — clearly labeled, " +
                        "never presented as real device control.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.demoEnterCollege() }) {
                            Text("Enter College") }
                        OutlinedButton(onClick = { vm.demoCarConnected() }) {
                            Text("Car Connects") }
                    }
                    OutlinedButton(onClick = { vm.demoSeedDrivingPattern() }) {
                        Text("Seed driving pattern ×4") }
                }
            }
        }

        item { Text("Active automations (${automations.count { it.status == "active" }})",
            style = MaterialTheme.typography.titleMedium) }
        if (automations.isEmpty()) {
            item { Text("No automations yet — describe one in Create.",
                style = MaterialTheme.typography.bodyMedium) }
        }
        items(automations) { a ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(a.name, style = MaterialTheme.typography.titleSmall)
                    Text(a.summary, style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(onClick = {}, label = { Text(a.status) })
                        Spacer(Modifier.width(8.dp))
                        AssistChip(onClick = {}, label = { Text("autonomy: ${a.autonomy}") })
                        Spacer(Modifier.width(8.dp))
                        if (a.status == "pending_confirmation") {
                            Button(onClick = { vm.confirmPending(a.id) }) { Text("Approve") }
                        } else {
                            TextButton(onClick = {
                                vm.setAutomationStatus(a.id,
                                    if (a.status == "disabled") "active" else "disabled")
                            }) { Text(if (a.status == "disabled") "Enable" else "Disable") }
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Recent activity", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onOpenHistory) { Text("Full history →") }
            }
        }
        items(executions.take(5)) { e ->
            Text("• [${e.outcome}]${if (e.simulated) " [SIMULATED]" else ""} ${e.explanation}",
                style = MaterialTheme.typography.bodySmall)
        }

        item { Text("Commitments (${commitments.count { it.status == "accepted" }} accepted)",
            style = MaterialTheme.typography.titleMedium) }
        items(commitments.filter { it.status == "accepted" }.take(3)) { c ->
            Text("• ${c.task}${c.deadline?.let { " — due $it" } ?: ""}",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun CreateAutomationScreen(vm: EchoViewModel) {
    var text by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var autonomy by remember { mutableStateOf("confirm") }
    val state by vm.parseState.collectAsState()

    LazyColumn(Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Create automation", style = MaterialTheme.typography.headlineSmall)
            Text("Describe it in your own words — EchoOS turns it into a safe, " +
                "structured plan you approve before it exists.",
                style = MaterialTheme.typography.bodyMedium)
        }
        item {
            OutlinedTextField(value = text, onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
                label = { Text("e.g. When I reach college, start my focus routine") })
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.parseText(text) }, enabled = text.isNotBlank()) {
                Text("Understand with AI")
            }
        }

        when (val s = state) {
            is EchoViewModel.ParseState.Loading -> item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.width(24.dp).height(24.dp))
                    Spacer(Modifier.width(12.dp)); Text("Parsing your intent…")
                }
            }
            is EchoViewModel.ParseState.Error -> item {
                Card { Text("Couldn't create that: ${s.message}",
                    Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.error) }
            }
            is EchoViewModel.ParseState.Preview -> {
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Preview", style = MaterialTheme.typography.titleMedium)
                            Text(s.intent.summary)
                            Text("Trigger: ${s.intent.trigger.type}" +
                                (s.intent.trigger.place?.let { " ($it)" } ?: "") +
                                (s.intent.trigger.time?.let { " at $it" } ?: ""))
                            Text("Actions: " + s.intent.actions.joinToString {
                                it.type + if (ActionCatalog.isSimulated(it.type)) " [simulated]" else ""
                            })
                            Text("Permissions: " +
                                s.intent.requiredPermissions.joinToString().ifEmpty { "none" })
                            Text("Confidence: ${(s.intent.confidence * 100).toInt()}% · " +
                                "engine: ${s.engine}")
                            if (s.rejected.isNotEmpty()) {
                                Text("Rejected unsupported actions: ${s.rejected.joinToString()}",
                                    color = MaterialTheme.colorScheme.error)
                            }
                            Text("Autonomy level:",
                                style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("suggest", "confirm", "automatic").forEach { level ->
                                    FilterChip(selected = autonomy == level,
                                        onClick = { autonomy = level },
                                        label = { Text(level) })
                                }
                            }
                            OutlinedTextField(value = name, onValueChange = { name = it },
                                label = { Text("Name this automation") },
                                modifier = Modifier.fillMaxWidth())
                            Button(onClick = {
                                vm.saveAutomation(
                                    name.ifBlank { s.intent.summary.take(40) },
                                    s.intent, autonomy)
                                text = ""; name = ""
                            }) { Text("Save automation") }
                        }
                    }
                }
            }
            else -> {}
        }
    }
}
