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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.echoos.ai.ApiClient
import com.echoos.viewmodel.EchoViewModel

/** EchoLens + notification intelligence + pattern suggestions (Salman UI,
 *  Rajersh intelligence). SRS §9.2–§9.4. */

private val demoNotifications = listOf(
    ApiClient.SourceItem("n1", "whatsapp", "Rohit", "Can you send the report by Tuesday 6pm?"),
    ApiClient.SourceItem("n2", "whatsapp", "Maa", "Don't forget to pay the electricity bill tomorrow"),
    ApiClient.SourceItem("n3", "email", "Prof. Mehta", "Reminder: submit the OS assignment by Friday"),
    ApiClient.SourceItem("n4", "shopping_app", null, "MEGA SALE! 70% off on headphones today only"),
    ApiClient.SourceItem("n6", "slack", "Ananya", "I'll review your PR by tonight"),
)

@Composable
fun IntelligenceScreen(vm: EchoViewModel) {
    val commitments by vm.commitments.collectAsState()
    val patterns by vm.patterns.collectAsState()

    LazyColumn(Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("EchoLens", style = MaterialTheme.typography.headlineSmall)
            Text("Commitment intelligence from permitted sources only. Accept, " +
                "edit or dismiss — nothing is stored without you.",
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Button(onClick = { vm.runEchoLens(demoNotifications) }) {
                Text("Scan demo notifications (SIMULATED)")
            }
        }

        if (commitments.isEmpty()) {
            item { Text("No commitments yet — run a scan.",
                style = MaterialTheme.typography.bodySmall) }
        }
        items(commitments) { c ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(c.task, style = MaterialTheme.typography.titleSmall)
                    c.person?.let { Text("For: $it", style = MaterialTheme.typography.bodySmall) }
                    c.deadline?.let { Text("Deadline: $it", style = MaterialTheme.typography.bodySmall) }
                    Text("Confidence: ${(c.confidence * 100).toInt()}% · \"${c.rawExcerpt}\"",
                        style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {}, label = { Text(c.status) })
                        if (c.status == "candidate") {
                            Button(onClick = { vm.setCommitmentStatus(c.id, "accepted") }) {
                                Text("Accept") }
                            OutlinedButton(onClick = { vm.setCommitmentStatus(c.id, "dismissed") }) {
                                Text("Dismiss") }
                        } else if (c.status == "accepted") {
                            TextButton(onClick = { vm.setCommitmentStatus(c.id, "done") }) {
                                Text("Mark done") }
                        }
                    }
                }
            }
        }

        item {
            Text("Pattern suggestions", style = MaterialTheme.typography.titleLarge)
            Text("Repeated behavior becomes a suggestion — never a silent rule.",
                style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { vm.analyzePatterns() }) { Text("Analyze my patterns") }
        }
        items(patterns) { p ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(p.suggestionSummary)
                    Text("Seen ${p.frequency}× · confidence ${(p.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {}, label = { Text(p.status) })
                        if (p.status == "suggested") {
                            Button(onClick = { vm.setPatternStatus(p.id, "accepted") }) {
                                Text("Accept") }
                            OutlinedButton(onClick = { vm.setPatternStatus(p.id, "dismissed") }) {
                                Text("Dismiss") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlannerScreen(vm: EchoViewModel) {
    val plan by vm.planState.collectAsState()
    val loading by vm.planLoading.collectAsState()

    val demoCalendar = listOf(
        ApiClient.CalendarDto("DSA Lecture", "09:00", "10:30"),
        ApiClient.CalendarDto("Project Sync (Team EchoOS)", "15:00", "15:45"),
    )
    val demoTasks = listOf(
        ApiClient.TaskDto("Revise OS notes", 60, 2),
        ApiClient.TaskDto("Fix Compose navigation bug", 90, 1),
        ApiClient.TaskDto("Read ML paper", 45, 3),
    )

    LazyColumn(Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("AI Day Planner", style = MaterialTheme.typography.headlineSmall)
            Text("Calendar + tasks + accepted commitments → an editable plan. " +
                "Fixed events are respected; everything else you can change.",
                style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { vm.planDay(demoCalendar, demoTasks) }, enabled = !loading) {
                Text("Plan tomorrow")
            }
        }
        if (loading) item {
            Row { CircularProgressIndicator(Modifier.width(24.dp).height(24.dp))
                Spacer(Modifier.width(12.dp)); Text("Planning…") }
        }
        plan?.let { p ->
            items(p.blocks) { b ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${b.start}–${b.end}  ${b.title}",
                            style = MaterialTheme.typography.titleSmall)
                        Text("${b.kind} · ${b.source}" +
                            if (b.editable) " · editable" else " · fixed",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            items(p.notes) { n ->
                Text("ⓘ $n", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary)
            }
        }
        if (plan == null && !loading) {
            item { Text("No plan yet — tap Plan tomorrow.",
                style = MaterialTheme.typography.bodySmall) }
        }
    }
}
