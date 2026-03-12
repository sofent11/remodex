package com.remodex.android.ui.turn

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.remodex.android.ui.shared.GlassCard

@Composable
internal fun TurnTimelineEmptyState(
    isRunning: Boolean,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (isRunning) "Waiting for Codex to respond…" else "Start a conversation with your paired Mac.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
