package com.stashed.app.ui.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stashed.app.data.local.MemoryEntity
import com.stashed.app.ui.components.MemoryCard
import com.stashed.app.ui.components.SwipeToDeleteContainer
import kotlinx.coroutines.launch

@Composable
fun MemoryListScreen(
    paddingValues: PaddingValues,
    viewModel: MemoryListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editingMemory by remember { mutableStateOf<MemoryEntity?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "All Stashed",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )

            when (val state = uiState) {
                ListUiState.Loading -> Unit

                ListUiState.Empty -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📦", style = MaterialTheme.typography.headlineLarge)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Nothing stashed yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                is ListUiState.Loaded -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.memories, key = { it.id }) { memory ->
                            SwipeToDeleteContainer(
                                onDelete = {
                                    viewModel.delete(memory.id)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Deleted",
                                            actionLabel = "Undo",
                                        )
                                    }
                                },
                            ) {
                                MemoryCard(
                                    entity = memory,
                                    onClick = { editingMemory = memory },
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    editingMemory?.let { memory ->
        EditMemoryDialog(
            memory = memory,
            viewModel = viewModel,
            onConfirm = { newItem, newLocation ->
                viewModel.update(memory.id, newItem, newLocation)
                editingMemory = null
            },
            onDismiss = { editingMemory = null },
        )
    }
}

@Composable
private fun EditMemoryDialog(
    memory: MemoryEntity,
    viewModel: MemoryListViewModel,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var item by remember { mutableStateOf(memory.item) }
    var location by remember { mutableStateOf(memory.location) }
    val history by viewModel.getLocationHistory(memory.id)
        .collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit memory",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = item,
                    onValueChange = { item = it },
                    label = { Text("Item") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )

                if (history.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Previously:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    history.forEach { entry ->
                        val daysAgo = (System.currentTimeMillis() - entry.changedAt) / 86_400_000
                        val timeLabel = when {
                            daysAgo < 1 -> "today"
                            daysAgo == 1L -> "yesterday"
                            else -> "$daysAgo days ago"
                        }
                        Text(
                            text = "${entry.previousLocation} ($timeLabel)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (item.isNotBlank()) onConfirm(item, location) },
            ) {
                Text(
                    "Save",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        shape = MaterialTheme.shapes.large,
    )
}
