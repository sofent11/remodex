package com.remodex.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.remodex.android.data.model.AppState
import com.remodex.android.data.model.ThreadSummary
import com.remodex.android.data.model.ThreadSyncState
import com.remodex.android.ui.sidebar.SidebarFloatingSettingsButton
import com.remodex.android.ui.sidebar.SidebarHeaderView
import com.remodex.android.ui.sidebar.SidebarNewChatButton
import com.remodex.android.ui.sidebar.SidebarProjectPickerSheet
import com.remodex.android.ui.sidebar.SidebarSearchField
import com.remodex.android.ui.sidebar.SidebarThreadListView
import com.remodex.android.ui.sidebar.buildSidebarThreadGroups
import com.remodex.android.ui.theme.Danger

@Composable
fun SidebarScreen(
    state: AppState,
    onCreateThread: (String?) -> Unit,
    onSelectThread: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onDeleteThread: (String) -> Unit,
    onArchiveThread: (String) -> Unit,
    onUnarchiveThread: (String) -> Unit,
    onRenameThread: (String, String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showProjectPicker by rememberSaveable { mutableStateOf(false) }
    var threadPendingRename by remember { mutableStateOf<ThreadSummary?>(null) }
    var threadPendingDeletion by remember { mutableStateOf<ThreadSummary?>(null) }

    val groups = remember(state.threads, query) {
        buildSidebarThreadGroups(state.threads, query)
    }
    val projectPaths = remember(state.threads) {
        state.threads
            .mapNotNull { it.normalizedProjectPath }
            .distinct()
            .sorted()
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        SidebarHeaderView()
        SidebarSearchField(
            value = query,
            onValueChange = { query = it },
        )
        SidebarNewChatButton(
            enabled = state.isConnected,
            onClick = {
                if (projectPaths.isEmpty()) {
                    onCreateThread(null)
                } else {
                    showProjectPicker = true
                }
            },
        )
        SidebarThreadListView(
            groups = groups,
            selectedThreadId = state.selectedThreadId,
            runningThreadIds = state.runningThreadIds,
            onSelectThread = { onSelectThread(it.id) },
            onCreateThreadInProject = onCreateThread,
            onRequestRenameThread = { threadPendingRename = it },
            onRequestDeleteThread = { threadPendingDeletion = it },
            onArchiveToggleThread = { thread ->
                if (thread.syncState == ThreadSyncState.LIVE) {
                    onArchiveThread(thread.id)
                } else {
                    onUnarchiveThread(thread.id)
                }
            },
            isFiltering = query.isNotBlank(),
            isConnected = state.isConnected,
        )
        SidebarFloatingSettingsButton(onClick = onOpenSettings)
    }

    if (showProjectPicker) {
        SidebarProjectPickerSheet(
            projectPaths = projectPaths,
            onDismiss = { showProjectPicker = false },
            onSelectProject = { projectPath ->
                showProjectPicker = false
                onCreateThread(projectPath)
            },
        )
    }

    threadPendingRename?.let { thread ->
        var newName by rememberSaveable(thread.id) {
            mutableStateOf(thread.name ?: thread.title ?: "")
        }
        AlertDialog(
            onDismissRequest = { threadPendingRename = null },
            title = { Text("Rename Chat") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Chat Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameThread(thread.id, newName)
                        threadPendingRename = null
                    },
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { threadPendingRename = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    threadPendingDeletion?.let { thread ->
        AlertDialog(
            onDismissRequest = { threadPendingDeletion = null },
            title = { Text("Delete Chat") },
            text = {
                Text("Delete \"${thread.displayTitle}\"? This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteThread(thread.id)
                        threadPendingDeletion = null
                    },
                ) {
                    Text("Delete", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { threadPendingDeletion = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}
