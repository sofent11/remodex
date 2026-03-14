package com.coderover.android.ui.turn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coderover.android.data.model.FuzzyFileMatch
import com.coderover.android.data.model.QueuedTurnDraft
import com.coderover.android.data.model.SkillMetadata
import com.coderover.android.ui.shared.StatusTag
import com.coderover.android.ui.theme.PlanAccent

@Composable
internal fun ComposerTopPanels(
    turnViewModel: TurnViewModel,
    queuedDrafts: List<QueuedTurnDraft>,
    canSteerDrafts: Boolean,
    showsPlanMode: Boolean,
    isCodexThread: Boolean,
    onSteerDraft: (String) -> Unit,
    onFileSelected: (FuzzyFileMatch) -> Unit,
    onSkillSelected: (SkillMetadata) -> Unit,
    onSelectSlashCommand: (TurnComposerSlashCommand) -> Unit,
    onSelectCodeReviewTarget: (TurnComposerReviewTarget) -> Unit,
    onRemoveDraft: (String) -> Unit,
) {
    AnimatedVisibility(
        visible = turnViewModel.composerNoticeMessage != null ||
            turnViewModel.autocompleteFiles.isNotEmpty() ||
            turnViewModel.autocompleteSkills.isNotEmpty() ||
            turnViewModel.slashCommandPanelState !is TurnComposerSlashCommandPanelState.Hidden ||
            queuedDrafts.isNotEmpty() ||
            (turnViewModel.isPlanModeArmed && showsPlanMode),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            turnViewModel.composerNoticeMessage?.let { notice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusTag(
                        text = "Images",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = notice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (turnViewModel.isPlanModeArmed && showsPlanMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusTag(
                        text = "Plan mode",
                        containerColor = PlanAccent.copy(alpha = 0.14f),
                        contentColor = PlanAccent,
                    )
                    Text(
                        text = "Structured plan before execution.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            FileAutocompletePanel(
                files = turnViewModel.autocompleteFiles,
                onSelect = onFileSelected,
            )
            SkillAutocompletePanel(
                skills = turnViewModel.autocompleteSkills,
                onSelect = onSkillSelected,
            )
            if (isCodexThread) {
                SlashCommandAutocompletePanel(
                    state = turnViewModel.slashCommandPanelState,
                    onSelectCommand = onSelectSlashCommand,
                    onSelectReviewTarget = onSelectCodeReviewTarget,
                )
            }
            QueuedDraftsPanel(
                drafts = queuedDrafts,
                canSteerDrafts = canSteerDrafts,
                steeringDraftId = turnViewModel.steeringDraftId,
                onSteerDraft = onSteerDraft,
                onRemoveDraft = onRemoveDraft,
            )
        }
    }
}

@Composable
private fun SlashCommandAutocompletePanel(
    state: TurnComposerSlashCommandPanelState,
    onSelectCommand: (TurnComposerSlashCommand) -> Unit,
    onSelectReviewTarget: (TurnComposerReviewTarget) -> Unit,
) {
    when (state) {
        is TurnComposerSlashCommandPanelState.Hidden -> Unit
        is TurnComposerSlashCommandPanelState.Commands -> {
            val query = state.query.trim().lowercase()
            val commands = listOf(TurnComposerSlashCommand.CODE_REVIEW, TurnComposerSlashCommand.STATUS)
                .filter { command ->
                    val haystack = when (command) {
                        TurnComposerSlashCommand.CODE_REVIEW -> "review /review code review"
                        TurnComposerSlashCommand.STATUS -> "status /status"
                    }
                    query.isEmpty() || haystack.contains(query)
                }
            if (commands.isEmpty()) return
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
                    .padding(4.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 4.dp),
            ) {
                items(commands) { command ->
                    val title = when (command) {
                        TurnComposerSlashCommand.CODE_REVIEW -> "/review"
                        TurnComposerSlashCommand.STATUS -> "/status"
                    }
                    val subtitle = when (command) {
                        TurnComposerSlashCommand.CODE_REVIEW -> "Run the reviewer on local changes"
                        TurnComposerSlashCommand.STATUS -> "Show context usage and rate limits"
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCommand(command) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(text = title, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        is TurnComposerSlashCommandPanelState.CodeReviewTargets -> {
            val targets = listOf(
                TurnComposerReviewTarget.UNCOMMITTED_CHANGES,
                TurnComposerReviewTarget.BASE_BRANCH,
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .padding(4.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 4.dp),
            ) {
                items(targets) { target ->
                    val subtitle = when (target) {
                        TurnComposerReviewTarget.UNCOMMITTED_CHANGES -> "Review working tree changes"
                        TurnComposerReviewTarget.BASE_BRANCH -> "Review diff against selected base branch"
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectReviewTarget(target) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(text = target.title, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
