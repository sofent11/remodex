package com.remodex.android.ui.turn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.remodex.android.data.model.AppState
import com.remodex.android.data.model.ConnectionPhase
import com.remodex.android.ui.shared.GlassCard

@Composable
internal fun ComposerDisconnectedBanner(
    state: AppState,
    onReconnect: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Disconnected", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = composerConnectionMessage(state),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.activePairing != null) {
                TextButton(
                    onClick = onReconnect,
                    enabled = state.connectionPhase != ConnectionPhase.CONNECTING,
                ) {
                    Text(
                        text = if (state.connectionPhase == ConnectionPhase.CONNECTING) "Reconnecting..." else "Reconnect",
                    )
                }
            }
        }
    }
}
