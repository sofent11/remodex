package com.remodex.android.ui.turn

import androidx.compose.runtime.Composable
import com.remodex.android.data.model.AccessMode
import com.remodex.android.data.model.AppState

@Composable
internal fun TurnToolbarContent(
    state: AppState,
    turnViewModel: TurnViewModel,
    onSelectAccessMode: (AccessMode) -> Unit,
    onGitAction: (com.remodex.android.data.model.TurnGitActionKind) -> Unit,
) {
    ComposerSecondaryToolbar(
        state = state,
        turnViewModel = turnViewModel,
        onSelectAccessMode = onSelectAccessMode,
        onGitAction = onGitAction,
    )
}
