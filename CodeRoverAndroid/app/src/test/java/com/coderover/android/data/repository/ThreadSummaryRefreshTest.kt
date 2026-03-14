package com.coderover.android.data.repository

import com.coderover.android.data.model.ChatMessage
import com.coderover.android.data.model.ConnectionPhase
import com.coderover.android.data.model.MessageKind
import com.coderover.android.data.model.MessageRole
import com.coderover.android.data.model.AppState
import com.coderover.android.data.model.ThreadHistoryAnchor
import com.coderover.android.data.model.ThreadHistorySegment
import com.coderover.android.data.model.ThreadHistoryState
import com.coderover.android.data.model.ThreadSummary
import com.coderover.android.data.model.ThreadSyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class ThreadSummaryRefreshTest {
    @Test
    fun refreshThreadSummaryFromMessagesUpdatesPreviewAndSortOrder() {
        val olderThread = ThreadSummary(
            id = "thread-older",
            preview = "Older preview",
            updatedAt = 100L,
            syncState = ThreadSyncState.LIVE,
        )
        val targetThread = ThreadSummary(
            id = "thread-target",
            preview = "Stale preview",
            updatedAt = 50L,
            syncState = ThreadSyncState.LIVE,
        )
        val messages = listOf(
            ChatMessage(
                threadId = "thread-target",
                role = MessageRole.SYSTEM,
                kind = MessageKind.THINKING,
                text = "Thinking...",
                createdAt = 150L,
            ),
            ChatMessage(
                threadId = "thread-target",
                role = MessageRole.ASSISTANT,
                kind = MessageKind.CHAT,
                text = "Fresh assistant reply\nwith wrap",
                createdAt = 200L,
            ),
        )

        val refreshed = listOf(targetThread, olderThread)
            .refreshThreadSummaryFromMessages("thread-target", messages)

        assertEquals("thread-target", refreshed.first().id)
        assertEquals("Fresh assistant reply with wrap", refreshed.first().preview)
        assertEquals(200L, refreshed.first().updatedAt)
    }

    @Test
    fun sidebarPreviewTextPrefersNonSystemContent() {
        val messages = listOf(
            ChatMessage(
                threadId = "thread-1",
                role = MessageRole.SYSTEM,
                kind = MessageKind.COMMAND_EXECUTION,
                text = "command output",
                createdAt = 100L,
            ),
            ChatMessage(
                threadId = "thread-1",
                role = MessageRole.USER,
                kind = MessageKind.CHAT,
                text = "user question",
                createdAt = 101L,
            ),
        )

        assertEquals("user question", messages.sidebarPreviewText())
    }

    @Test
    fun refreshThreadSummaryFromMessagesCreatesPlaceholderThreadWhenMissing() {
        val messages = listOf(
            ChatMessage(
                threadId = "thread-new",
                role = MessageRole.ASSISTANT,
                kind = MessageKind.CHAT,
                text = "Hello from another device",
                createdAt = 300L,
            ),
        )

        val refreshed = emptyList<ThreadSummary>()
            .refreshThreadSummaryFromMessages("thread-new", messages)

        assertEquals(1, refreshed.size)
        assertEquals("thread-new", refreshed.first().id)
        assertEquals("Hello from another device", refreshed.first().preview)
        assertEquals(ThreadSyncState.LIVE, refreshed.first().syncState)
        assertTrue((refreshed.first().updatedAt ?: 0L) >= 300L)
    }

    @Test
    fun selectedCodexThreadIdForTailSyncRequiresConnectedRunningSelectedCodexThread() {
        val runningCodexThread = ThreadSummary(id = "thread-running", provider = "codex")
        val connectedState = AppState(
            connectionPhase = ConnectionPhase.CONNECTED,
            threads = listOf(runningCodexThread),
            selectedThreadId = runningCodexThread.id,
            runningThreadIds = setOf(runningCodexThread.id),
        )

        assertEquals("thread-running", connectedState.selectedCodexThreadIdForTailSync())
        assertNull(connectedState.copy(connectionPhase = ConnectionPhase.OFFLINE).selectedCodexThreadIdForTailSync())
        assertNull(connectedState.copy(runningThreadIds = emptySet()).selectedCodexThreadIdForTailSync())
        assertNull(
            connectedState.copy(
                threads = listOf(runningCodexThread.copy(provider = "claude")),
            ).selectedCodexThreadIdForTailSync(),
        )
    }

    @Test
    fun shouldRequestRealtimeHistoryCatchUpSkipsNoGapAndSameTailItem() {
        assertFalse(
            shouldRequestRealtimeHistoryCatchUp(
                latestItemId = "item-10",
                incomingItemId = "item-10",
                previousItemId = null,
            ),
        )
        assertFalse(
            shouldRequestRealtimeHistoryCatchUp(
                latestItemId = "item-10",
                incomingItemId = "item-11",
                previousItemId = "item-10",
            ),
        )
        assertTrue(
            shouldRequestRealtimeHistoryCatchUp(
                latestItemId = "item-10",
                incomingItemId = "item-42",
                previousItemId = "item-12",
            ),
        )
    }

    @Test
    fun resolveOlderHistoryLoadRequestBootstrapsWhenTimelineIsEmpty() {
        val request = resolveOlderHistoryLoadRequest(
            historyState = null,
            localMessages = emptyList(),
        )

        assertTrue(request.shouldBootstrapTail)
        assertNull(request.anchor)
    }

    @Test
    fun resolveOlderHistoryLoadRequestUsesLocalFirstMessageWhenHistoryStateMissing() {
        val localMessages = listOf(
            ChatMessage(
                threadId = "thread-1",
                role = MessageRole.ASSISTANT,
                kind = MessageKind.CHAT,
                text = "first",
                createdAt = 10L,
                turnId = "turn-1",
                itemId = "item-10",
            ),
            ChatMessage(
                threadId = "thread-1",
                role = MessageRole.ASSISTANT,
                kind = MessageKind.CHAT,
                text = "second",
                createdAt = 20L,
                turnId = "turn-1",
                itemId = "item-20",
            ),
        )

        val request = resolveOlderHistoryLoadRequest(
            historyState = null,
            localMessages = localMessages,
        )

        assertFalse(request.shouldBootstrapTail)
        assertEquals("item-10", request.anchor?.itemId)
        assertEquals(10L, request.anchor?.createdAt)
    }

    @Test
    fun latestItemIdForRealtimeHistoryCatchUpPrefersLoadedHistoryAnchorOverProvisionalTail() {
        val historyState = ThreadHistoryState(
            segments = listOf(
                ThreadHistorySegment(
                    oldestAnchor = ThreadHistoryAnchor(itemId = "item-1", createdAt = 1L, turnId = "turn-1"),
                    newestAnchor = ThreadHistoryAnchor(itemId = "item-10", createdAt = 10L, turnId = "turn-1"),
                ),
            ),
            newestLoadedAnchor = ThreadHistoryAnchor(itemId = "item-10", createdAt = 10L, turnId = "turn-1"),
        )
        val localMessages = listOf(
            ChatMessage(
                threadId = "thread-1",
                role = MessageRole.ASSISTANT,
                kind = MessageKind.CHAT,
                text = "provisional",
                createdAt = 42L,
                turnId = "turn-2",
                itemId = "item-42",
            ),
        )

        assertEquals(
            "item-10",
            latestItemIdForRealtimeHistoryCatchUp(
                historyState = historyState,
                localMessages = localMessages,
            ),
        )
    }
}
