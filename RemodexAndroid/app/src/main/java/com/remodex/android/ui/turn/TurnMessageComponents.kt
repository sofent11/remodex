package com.remodex.android.ui.turn

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.remodex.android.data.model.ChatMessage
import com.remodex.android.data.model.MessageKind
import com.remodex.android.data.model.MessageRole
import com.remodex.android.ui.shared.StatusTag
import com.remodex.android.ui.theme.Border
import com.remodex.android.ui.theme.CommandAccent
import com.remodex.android.ui.theme.PlanAccent
import com.remodex.android.ui.theme.monoFamily

@Composable
internal fun TurnMessageBubble(
    message: ChatMessage,
    onSubmitStructuredInput: (kotlinx.serialization.json.JsonElement, Map<String, String>) -> Unit,
    grouped: Boolean = false,
    replyPresentation: ReplyPresentation? = null,
) {
    when {
        message.role == MessageRole.USER -> {
            ConversationBubble(
                message = message,
                alignment = Alignment.CenterEnd,
                background = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                contentColor = MaterialTheme.colorScheme.onPrimary,
                fillFraction = if (grouped) 1f else 0.86f,
                shape = RoundedCornerShape(22.dp, 22.dp, 4.dp, 22.dp),
            )
        }

        message.role == MessageRole.ASSISTANT && message.kind == MessageKind.CHAT -> {
            ConversationBubble(
                message = message,
                alignment = Alignment.CenterStart,
                background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                fillFraction = if (grouped) 1f else 0.92f,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 0.dp,
                replyPresentation = replyPresentation,
            )
        }

        else -> SystemMessageCard(
            message = message,
            onSubmitStructuredInput = onSubmitStructuredInput,
            grouped = grouped,
        )
    }
}

@Composable
private fun ConversationBubble(
    message: ChatMessage,
    alignment: Alignment,
    background: Color,
    contentColor: Color,
    fillFraction: Float,
    shape: Shape,
    tonalElevation: androidx.compose.ui.unit.Dp = 0.dp,
    replyPresentation: ReplyPresentation? = null,
) {
    val usesRichText = message.role == MessageRole.ASSISTANT && message.kind == MessageKind.CHAT
    val bubbleBorder = when (replyPresentation) {
        ReplyPresentation.FINAL -> androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
        )

        ReplyPresentation.DRAFT -> androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
        )

        null -> null
    }
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        Surface(
            color = background,
            contentColor = contentColor,
            shape = shape,
            tonalElevation = tonalElevation,
            border = bubbleBorder,
            modifier = Modifier
                .fillMaxWidth(fillFraction)
                .animateContentSize(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                replyPresentation?.let { presentation ->
                    StatusTag(
                        text = if (presentation == ReplyPresentation.FINAL) "Final" else "Draft",
                        containerColor = if (presentation == ReplyPresentation.FINAL) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (presentation == ReplyPresentation.FINAL) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (message.attachments.isNotEmpty()) {
                    MessageAttachmentsPreview(message.attachments)
                    Spacer(Modifier.height(10.dp))
                }
                if (usesRichText) {
                    RichMessageText(
                        text = message.text,
                        textColor = contentColor,
                    )
                } else {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemMessageCard(
    message: ChatMessage,
    onSubmitStructuredInput: (kotlinx.serialization.json.JsonElement, Map<String, String>) -> Unit,
    grouped: Boolean = false,
) {
    val accent = systemAccentColor(message.kind)
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
            modifier = Modifier.fillMaxWidth(if (grouped) 1f else 0.94f),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (message.isStreaming) {
                        PulsingDot(color = accent)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(accent, CircleShape),
                        )
                    }
                    Text(
                        text = systemMessageTitle(message.kind),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
                when (message.kind) {
                    MessageKind.THINKING -> ThinkingMessageContent(message)
                    MessageKind.FILE_CHANGE -> FileChangeMessageContent(message)
                    MessageKind.COMMAND_EXECUTION -> CommandExecutionMessageContent(message)
                    MessageKind.PLAN -> PlanMessageContent(message)
                    MessageKind.USER_INPUT_PROMPT -> UserInputPromptMessageContent(message, onSubmitStructuredInput)
                    MessageKind.CHAT -> DefaultSystemMessageContent(message)
                }
            }
        }
    }
}


@Composable
private fun ThinkingMessageContent(message: ChatMessage) {
    val thinking = remember(message.id, message.text) { parseThinkingDisclosure(message.text) }
    var expandedSectionIds by remember(message.id) { mutableStateOf<Set<String>>(emptySet()) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Thinking...",
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = monoFamily),
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (thinking.sections.isNotEmpty()) {
            thinking.sections.forEach { section ->
                val isExpanded = expandedSectionIds.contains(section.id)
                val hasDetail = section.detail.isNotBlank()
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = hasDetail) {
                                    expandedSectionIds = if (isExpanded) expandedSectionIds - section.id else expandedSectionIds + section.id
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (isExpanded) "▾" else "▸",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = monoFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (hasDetail) 0.9f else 0.4f),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = monoFamily),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (isExpanded && hasDetail) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = section.detail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                            )
                        }
                    }
                }
            }
        } else if (thinking.fallbackText.isNotEmpty()) {
            Text(
                text = thinking.fallbackText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
            )
        }
    }
}

@Composable
private fun DefaultSystemMessageContent(message: ChatMessage) {
    RichMessageText(
        text = message.text.trim(),
        textColor = MaterialTheme.colorScheme.onSurfaceVariant,
        textStyle = MaterialTheme.typography.bodyMedium,
    )
}

internal fun systemMessageTitle(kind: MessageKind): String {
    return when (kind) {
        MessageKind.THINKING -> "Thinking"
        MessageKind.FILE_CHANGE -> "File change"
        MessageKind.COMMAND_EXECUTION -> "Command"
        MessageKind.PLAN -> "Plan"
        MessageKind.USER_INPUT_PROMPT -> "Input needed"
        MessageKind.CHAT -> "System"
    }
}

@Composable
internal fun systemAccentColor(kind: MessageKind): Color {
    return when (kind) {
        MessageKind.THINKING, MessageKind.PLAN -> PlanAccent
        MessageKind.COMMAND_EXECUTION -> CommandAccent
        MessageKind.FILE_CHANGE -> MaterialTheme.colorScheme.secondary
        MessageKind.USER_INPUT_PROMPT -> MaterialTheme.colorScheme.tertiary
        MessageKind.CHAT -> MaterialTheme.colorScheme.outline
    }
}
