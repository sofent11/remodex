package com.coderover.android.ui.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.coderover.android.data.model.ThreadRunBadgeState

@Composable
fun SidebarThreadRunBadgeView(
    state: ThreadRunBadgeState,
    modifier: Modifier = Modifier
) {
    val color = when (state) {
        ThreadRunBadgeState.RUNNING -> Color(0xFF2196F3) // Blue
        ThreadRunBadgeState.READY -> Color(0xFF4CAF50) // Green
        ThreadRunBadgeState.FAILED -> Color(0xFFF44336) // Red
    }

    Box(
        modifier = modifier
            .size(10.dp)
            .background(color, shape = CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.background, CircleShape)
    )
}
