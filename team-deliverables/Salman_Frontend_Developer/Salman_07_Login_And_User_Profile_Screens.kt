package com.echoos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
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
import com.echoos.viewmodel.EchoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Login + user info screens (Salman UI, Tushar data rules).
 *
 * There is no server and no password: the profile is created and kept locally,
 * exactly like automations and commitments. Signing out clears the profile but
 * deliberately leaves the user's automations and history on the device.
 */

private fun isValidEmail(email: String): Boolean =
    Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$").matches(email.trim())

fun initialsOf(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    if (parts.isEmpty()) return "?"
    return parts.take(2).map { it.first().uppercaseChar() }.joinToString("")
}

@Composable
fun LoginScreen(vm: EchoViewModel) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("EchoOS", style = MaterialTheme.typography.headlineMedium)
            Text("Understand · Decide · Automate · Learn",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(14.dp))
            Text("Welcome — tell me who you are and I'll pick up where you left off.",
                style = MaterialTheme.typography.bodyMedium)
        }
        item {
            OutlinedTextField(
                value = name, onValueChange = { name = it; error = "" },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Your name") })
        }
        item {
            OutlinedTextField(
                value = email, onValueChange = { email = it; error = "" },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") })
        }
        if (error.isNotEmpty()) {
            item { Text("⚠ $error", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error) }
        }
        item {
            Button(onClick = {
                when {
                    name.trim().length < 2 ->
                        error = "Please enter your name (at least 2 characters)."
                    !isValidEmail(email) ->
                        error = "That doesn't look like a valid email address."
                    else -> vm.signIn(name.trim(), email.trim())
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Sign in") }
        }
        item {
            TextButton(onClick = { vm.signIn("Guest", "", guest = true) },
                modifier = Modifier.fillMaxWidth()) { Text("Continue as guest") }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("🔒 No account, no server, no password",
                        style = MaterialTheme.typography.titleSmall)
                    Text("EchoOS keeps your profile on this device only — the same rule it " +
                        "applies to your automations, commitments and history.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(vm: EchoViewModel) {
    val user by vm.user.collectAsState()
    val automations by vm.automations.collectAsState()
    val commitments by vm.commitments.collectAsState()
    val u = user ?: return

    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(u.name) }
    var email by remember { mutableStateOf(u.email) }
    val since = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(u.createdAt))

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Your profile", style = MaterialTheme.typography.headlineSmall)
            Text("Everything EchoOS knows about you — separate from what it senses, " +
                "and all of it stored on this device.",
                style = MaterialTheme.typography.bodyMedium)
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(initialsOf(u.name), style = MaterialTheme.typography.headlineMedium)
                    Text(u.name.ifBlank { "Guest" },
                        style = MaterialTheme.typography.titleMedium)
                    Text(u.email.ifBlank { "—" }, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {},
                            label = { Text(if (u.isGuest) "guest session" else "signed in") })
                        AssistChip(onClick = {}, label = { Text("since $since") })
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Account", style = MaterialTheme.typography.titleSmall)
                    Text("Account type: ${if (u.isGuest) "Guest (local)" else "Local profile"}",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Stored where: this device only",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Default autonomy: ${u.defaultAutonomy}",
                        style = MaterialTheme.typography.bodySmall)
                    if (editing) {
                        OutlinedTextField(value = name, onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Name") })
                        OutlinedTextField(value = email, onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Email") })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                if (name.trim().length >= 2) {
                                    vm.updateProfile(name.trim(), email.trim()); editing = false
                                }
                            }) { Text("Save") }
                            OutlinedButton(onClick = { editing = false }) { Text("Cancel") }
                        }
                    } else {
                        OutlinedButton(onClick = { editing = true }) { Text("Edit details") }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Your EchoOS at a glance", style = MaterialTheme.typography.titleSmall)
                    Text("${automations.size} automation(s) · " +
                        "${commitments.count { it.status == "accepted" }} accepted commitment(s)",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Data & privacy", style = MaterialTheme.typography.titleSmall)
                    Text("You own everything here. Delete any part of it, instantly.",
                        style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.deleteAllCommitments() }) {
                            Text("Delete commitments") }
                        OutlinedButton(onClick = { vm.deleteAllHistory() }) {
                            Text("Delete history") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.wipeAll() }) { Text("Delete all my data") }
                        Button(onClick = { vm.signOut() }) { Text("Sign out") }
                    }
                }
            }
        }
    }
}
