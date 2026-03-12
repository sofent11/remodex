package com.remodex.android.ui.turn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.remodex.android.data.model.ChatMessage
import com.remodex.android.data.model.MessageRole
import kotlinx.coroutines.launch

private enum class TurnAutoScrollMode {
    FOLLOW_BOTTOM,
    ANCHOR_ASSISTANT_RESPONSE,
    MANUAL,
}

@Composable
internal fun TurnTimeline(
    modifier: Modifier = Modifier,
    messages: List<ChatMessage>,
    renderItems: List<TimelineRenderItem>,
    isRunning: Boolean,
    activeTurnId: String?,
    turnViewModel: TurnViewModel,
    onSubmitStructuredInput: (kotlinx.serialization.json.JsonElement, Map<String, String>) -> Unit,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var autoScrollMode by rememberSaveable(messages.lastOrNull()?.id) {
        mutableStateOf(TurnAutoScrollMode.FOLLOW_BOTTOM)
    }
    val latestRenderAnchor = remember(renderItems) { renderItems.lastOrNull()?.key }
    val copyBlockTextByMessageId = remember(messages, activeTurnId, isRunning) {
        buildCopyBlockTextByMessageId(
            messages = messages,
            activeTurnId = activeTurnId,
            isThreadRunning = isRunning,
        )
    }
    val isNearBottom by remember(listState, renderItems.size) {
        derivedStateOf {
            if (renderItems.isEmpty()) {
                true
            } else {
                val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                lastVisibleIndex >= renderItems.lastIndex - 1
            }
        }
    }

    LaunchedEffect(isNearBottom) {
        turnViewModel.isScrolledToBottom = isNearBottom
        if (isNearBottom && autoScrollMode != TurnAutoScrollMode.ANCHOR_ASSISTANT_RESPONSE) {
            autoScrollMode = TurnAutoScrollMode.FOLLOW_BOTTOM
        }
    }

    LaunchedEffect(listState.isScrollInProgress, isNearBottom) {
        if (listState.isScrollInProgress && !isNearBottom) {
            autoScrollMode = TurnAutoScrollMode.MANUAL
        }
    }

    LaunchedEffect(turnViewModel.shouldAnchorToAssistantResponse, latestRenderAnchor) {
        if (!turnViewModel.shouldAnchorToAssistantResponse || renderItems.isEmpty()) {
            return@LaunchedEffect
        }
        val anchorIndex = renderItems.indexOfLast { item ->
            when (item) {
                is TimelineRenderItem.Message -> item.message.role != MessageRole.USER
                is TimelineRenderItem.TurnSection -> item.messages.any { message ->
                    message.role != MessageRole.USER
                }
            }
        }
        if (anchorIndex >= 0) {
            autoScrollMode = TurnAutoScrollMode.ANCHOR_ASSISTANT_RESPONSE
            listState.animateScrollToItem(anchorIndex)
            autoScrollMode = TurnAutoScrollMode.MANUAL
        }
        turnViewModel.shouldAnchorToAssistantResponse = false
    }

    LaunchedEffect(latestRenderAnchor, isRunning, autoScrollMode, turnViewModel.isScrolledToBottom) {
        if (renderItems.isEmpty() || autoScrollMode != TurnAutoScrollMode.FOLLOW_BOTTOM || !turnViewModel.isScrolledToBottom) {
            return@LaunchedEffect
        }
        listState.animateScrollToItem(renderItems.lastIndex)
    }

    Box(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        ) {
            if (messages.isEmpty()) {
                item {
                    TurnTimelineEmptyState(isRunning = isRunning)
                }
            }
            items(renderItems, key = { it.key }) { item ->
                when (item) {
                    is TimelineRenderItem.Message -> TurnMessageBubble(
                        message = item.message,
                        onSubmitStructuredInput = onSubmitStructuredInput,
                        copyBlockText = copyBlockTextByMessageId[item.message.id],
                    )

                    is TimelineRenderItem.TurnSection -> TurnSectionCard(
                        item = item,
                        copyBlockTextByMessageId = copyBlockTextByMessageId,
                        onSubmitStructuredInput = onSubmitStructuredInput,
                    )
                }
            }
        }

        if (messages.isNotEmpty() && !isNearBottom) {
            FilledTonalButton(
                onClick = {
                    turnViewModel.shouldAnchorToAssistantResponse = false
                    autoScrollMode = TurnAutoScrollMode.FOLLOW_BOTTOM
                    coroutineScope.launch {
                        listState.animateScrollToItem(renderItems.lastIndex)
                    }
                },
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
            ) {
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null)
                Text("Scroll to latest")
            }
        }
    }
}

internal fun buildCopyBlockTextByMessageId(
    messages: List<ChatMessage>,
    activeTurnId: String?,
    isThreadRunning: Boolean,
): Map<String, String> {
    if (messages.isEmpty()) {
        return emptyMap()
    }
    val result = mutableMapOf<String, String>()
    val latestBlockEnd = messages.indexOfLast { it.role != MessageRole.USER }
    var index = messages.lastIndex
    while (index >= 0) {
        if (messages[index].role == MessageRole.USER) {
            index -= 1
            continue
        }
        val blockEnd = index
        var blockStart = index
        while (blockStart > 0 && messages[blockStart - 1].role != MessageRole.USER) {
            blockStart -= 1
        }
        val blockMessages = messages.subList(blockStart, blockEnd + 1)
        val blockText = blockMessages
            .map { it.text.trim() }
            .filter(String::isNotEmpty)
            .joinToString(separator = "\n\n")
        val blockTurnId = blockMessages
            .asReversed()
            .firstNotNullOfOrNull { message -> message.turnId?.trim()?.takeIf(String::isNotEmpty) }
        val shouldShowCopyButton = when {
            blockText.isBlank() -> false
            !isThreadRunning -> true
            blockTurnId != null && activeTurnId != null -> blockTurnId != activeTurnId
            else -> blockEnd != latestBlockEnd
        }
        if (shouldShowCopyButton) {
            result[messages[blockEnd].id] = blockText
        }
        index = blockStart - 1
    }
    return result
}
