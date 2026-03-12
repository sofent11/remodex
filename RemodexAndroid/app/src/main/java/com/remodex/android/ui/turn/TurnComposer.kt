package com.remodex.android.ui.turn

import androidx.compose.runtime.Composable
import com.remodex.android.app.AppViewModel
import com.remodex.android.data.model.AccessMode
import com.remodex.android.data.model.AppState
import com.remodex.android.data.model.CodexImageAttachment
import com.remodex.android.data.model.CodexTurnSkillMention

@Composable
fun TurnComposer(
    state: AppState,
    input: String,
    onInputChanged: (String) -> Unit,
    isRunning: Boolean,
    onSend: (String, List<CodexImageAttachment>, List<CodexTurnSkillMention>, Boolean) -> Unit,
    onStop: () -> Unit,
    onReconnect: () -> Unit,
    onSelectModel: (String?) -> Unit,
    onSelectReasoning: (String?) -> Unit,
    onSelectAccessMode: (AccessMode) -> Unit,
    viewModel: AppViewModel,
) {
    TurnComposerHost(
        state = state,
        input = input,
        onInputChanged = onInputChanged,
        isRunning = isRunning,
        onSend = onSend,
        onStop = onStop,
        onReconnect = onReconnect,
        onSelectModel = onSelectModel,
        onSelectReasoning = onSelectReasoning,
        onSelectAccessMode = onSelectAccessMode,
        viewModel = viewModel,
    )
}
