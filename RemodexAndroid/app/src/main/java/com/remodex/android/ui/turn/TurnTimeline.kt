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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.remodex.android.data.model.ChatMessage

@Composable
internal fun TurnTimeline(
    modifier: Modifier = Modifier,
    messages: List<ChatMessage>,
    renderItems: List<TimelineRenderItem>,
    isRunning: Boolean,
    onSubmitStructuredInput: (kotlinx.serialization.json.JsonElement, Map<String, String>) -> Unit,
) {
    val listState = rememberLazyListState()
    var autoFollowLatest by rememberSaveable(messages.lastOrNull()?.id) { mutableStateOf(true) }
    val latestMessageAnchor = remember(messages) {
        messages.lastOrNull()?.let { message ->
            buildString {
                append(messages.size)
                append('|')
                append(message.id)
                append('|')
                append(message.isStreaming)
                append('|')
                append(message.text.hashCode())
            }
        }
    }
    val isNearBottom by remember(listState, messages.size) {
        derivedStateOf {
            if (messages.isEmpty()) {
                true
            } else {
                val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                lastVisibleIndex >= messages.lastIndex - 1
            }
        }
    }

    LaunchedEffect(isNearBottom) {
        if (isNearBottom) {
            autoFollowLatest = true
        }
    }

    LaunchedEffect(listState.isScrollInProgress, isNearBottom) {
        if (listState.isScrollInProgress && !isNearBottom) {
            autoFollowLatest = false
        }
    }

    LaunchedEffect(latestMessageAnchor, isRunning, autoFollowLatest) {
        if (messages.isNotEmpty() && autoFollowLatest) {
            listState.animateScrollToItem(messages.lastIndex)
        }
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
                    )

                    is TimelineRenderItem.TurnSection -> TurnSectionCard(
                        item = item,
                        onSubmitStructuredInput = onSubmitStructuredInput,
                    )
                }
            }
        }

        if (messages.isNotEmpty() && !isNearBottom) {
            FilledTonalButton(
                onClick = { autoFollowLatest = true },
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
            ) {
                Text(if (isRunning) "Jump to live" else "Jump to latest")
            }
        }
    }
}
