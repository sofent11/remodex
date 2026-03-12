package com.remodex.android.ui.turn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.remodex.android.data.model.AccessMode
import com.remodex.android.data.model.AppState
import com.remodex.android.data.model.ModelOption
import com.remodex.android.ui.theme.CommandAccent
import com.remodex.android.ui.theme.PlanAccent

@Composable
internal fun ComposerPrimaryToolbar(
    state: AppState,
    turnViewModel: TurnViewModel,
    selectedModel: ModelOption?,
    orderedModels: List<ModelOption>,
    selectedModelTitle: String,
    selectedReasoningTitle: String,
    reasoningOptions: List<String>,
    isRunning: Boolean,
    sendEnabled: Boolean,
    queuedCount: Int,
    isQueuePaused: Boolean,
    canResumeQueue: Boolean,
    isResumingQueue: Boolean,
    remainingAttachmentSlots: Int,
    onSelectModel: (String?) -> Unit,
    onSelectReasoning: (String?) -> Unit,
    onTapAddImage: () -> Unit,
    onTapTakePhoto: () -> Unit,
    onTapPasteImage: () -> Unit,
    onResumeQueue: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { turnViewModel.plusMenuExpanded = true },
            enabled = !isRunning,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = "Composer options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DropdownMenu(
                expanded = turnViewModel.plusMenuExpanded,
                onDismissRequest = { turnViewModel.plusMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (remainingAttachmentSlots > 0) {
                                "Add Image"
                            } else {
                                "Image Limit Reached"
                            },
                        )
                    },
                    enabled = remainingAttachmentSlots > 0,
                    onClick = {
                        onTapAddImage()
                        turnViewModel.plusMenuExpanded = false
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (remainingAttachmentSlots > 0) {
                                "Take Photo"
                            } else {
                                "Image Limit Reached"
                            },
                        )
                    },
                    enabled = remainingAttachmentSlots > 0,
                    onClick = {
                        onTapTakePhoto()
                        turnViewModel.plusMenuExpanded = false
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (remainingAttachmentSlots > 0) {
                                "Paste Image"
                            } else {
                                "Image Limit Reached"
                            },
                        )
                    },
                    enabled = remainingAttachmentSlots > 0,
                    onClick = {
                        onTapPasteImage()
                        turnViewModel.plusMenuExpanded = false
                    },
                )
                DropdownMenuItem(
                    text = { Text(if (turnViewModel.isPlanModeArmed) "Disable Plan Mode" else "Enable Plan Mode") },
                    leadingIcon = {
                        if (turnViewModel.isPlanModeArmed) {
                            Icon(Icons.Outlined.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        turnViewModel.togglePlanMode()
                        turnViewModel.plusMenuExpanded = false
                    },
                )
            }
        }

        Box {
            ComposerMetaButton(
                title = selectedModelTitle,
                enabled = orderedModels.isNotEmpty() && !isRunning,
                onClick = { turnViewModel.modelMenuExpanded = true },
            )
            DropdownMenu(
                expanded = turnViewModel.modelMenuExpanded,
                onDismissRequest = { turnViewModel.modelMenuExpanded = false },
            ) {
                orderedModels.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(composerModelTitle(model)) },
                        leadingIcon = if (selectedModel?.id == model.id) {
                            { Icon(Icons.Outlined.Check, contentDescription = null) }
                        } else {
                            null
                        },
                        onClick = {
                            onSelectModel(model.id)
                            turnViewModel.modelMenuExpanded = false
                        },
                    )
                }
            }
        }

        Box {
            ComposerMetaButton(
                title = selectedReasoningTitle,
                enabled = reasoningOptions.isNotEmpty() && !isRunning,
                onClick = { turnViewModel.reasoningMenuExpanded = true },
            )
            DropdownMenu(
                expanded = turnViewModel.reasoningMenuExpanded,
                onDismissRequest = { turnViewModel.reasoningMenuExpanded = false },
            ) {
                reasoningOptions.forEach { effort ->
                    DropdownMenuItem(
                        text = { Text(composerReasoningTitle(effort)) },
                        leadingIcon = if (state.selectedReasoningEffort == effort) {
                            { Icon(Icons.Outlined.Check, contentDescription = null) }
                        } else {
                            null
                        },
                        onClick = {
                            onSelectReasoning(effort)
                            turnViewModel.reasoningMenuExpanded = false
                        },
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))
        ContextWindowProgressRing(
            percentage = state.contextWindowUsage?.percentage ?: 0.05f,
            size = 18.dp,
        )
        Spacer(Modifier.width(4.dp))

        if (isQueuePaused && queuedCount > 0) {
            IconButton(
                onClick = onResumeQueue,
                enabled = canResumeQueue,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (canResumeQueue) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        CircleShape,
                    ),
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = if (isResumingQueue) "Resuming queued drafts" else "Resume queued drafts",
                    tint = if (canResumeQueue) {
                        MaterialTheme.colorScheme.onTertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    },
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        if (isRunning) {
            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.onSurface, CircleShape),
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Stop",
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            IconButton(
                onClick = onSend,
                enabled = sendEnabled,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (sendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape,
                    ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Send",
                        tint = if (sendEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp),
                    )
                    if (queuedCount > 0) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(start = 18.dp, bottom = 18.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error,
                        ) {
                            Text(
                                text = queuedCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ComposerSecondaryToolbar(
    state: AppState,
    turnViewModel: TurnViewModel,
    onSelectAccessMode: (AccessMode) -> Unit,
    onGitAction: (com.remodex.android.data.model.TurnGitActionKind) -> Unit,
) {
    AnimatedVisibility(visible = !turnViewModel.isFocused) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = { turnViewModel.isLocalMode = !turnViewModel.isLocalMode },
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (turnViewModel.isLocalMode) CommandAccent else PlanAccent,
                                    CircleShape,
                                ),
                        )
                        Text(
                            text = if (turnViewModel.isLocalMode) "Local" else "Cloud",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Box {
                    ComposerSecondaryChip(
                        label = "Access",
                        value = state.accessMode.displayName,
                        onClick = { turnViewModel.accessMenuExpanded = true },
                    )
                    DropdownMenu(
                        expanded = turnViewModel.accessMenuExpanded,
                        onDismissRequest = { turnViewModel.accessMenuExpanded = false },
                    ) {
                        AccessMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.displayName) },
                                leadingIcon = if (state.accessMode == mode) {
                                    { Icon(Icons.Outlined.Check, contentDescription = null) }
                                } else {
                                    null
                                },
                                onClick = {
                                    onSelectAccessMode(mode)
                                    turnViewModel.accessMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                state.gitRepoSyncResult?.branch?.let { branch ->
                    Box {
                        val isDirty = state.gitRepoSyncResult?.isDirty == true
                        val branchText = if (isDirty) "$branch*" else branch

                        Surface(
                            onClick = { turnViewModel.gitMenuExpanded = true },
                            shape = RoundedCornerShape(999.dp),
                            color = Color.Transparent,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_share),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = branchText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = turnViewModel.gitMenuExpanded,
                            onDismissRequest = { turnViewModel.gitMenuExpanded = false },
                        ) {
                            com.remodex.android.data.model.TurnGitActionKind.entries.forEach { action ->
                                DropdownMenuItem(
                                    text = { Text(action.title) },
                                    onClick = {
                                        turnViewModel.gitMenuExpanded = false
                                        onGitAction(action)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
