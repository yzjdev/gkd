package li.gkd.app.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import li.gkd.app.resources.*
import li.gkd.app.ui.MainViewModel
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkOutlinedTextField
import li.gkd.app.ui.component.GkScaffold
import li.gkd.app.ui.component.GkTopAppBar
import li.gkd.app.ui.component.gkPageBottomSpace
import li.gkd.app.ui.navigation.AiChatRoute
import org.jetbrains.compose.resources.stringResource

@Composable
fun AiChatPage(route: AiChatRoute) {
    val mainVm = MainViewModel.requireCurrent()
    val vm = viewModel { AiChatViewModel(route.snapshotIds) }
    val entries by vm.entries.collectAsStateWithLifecycle()
    val streaming by vm.streaming.collectAsStateWithLifecycle()
    val input by vm.input.collectAsStateWithLifecycle()
    val canSend by vm.canSend.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Follow the newest message, including the streaming answer.
    LaunchedEffect(entries.size, streaming?.length) {
        val lastIndex = entries.size + if (streaming != null) 1 else 0
        if (lastIndex > 0) listState.animateScrollToItem(lastIndex - 1)
    }

    GkScaffold(
        topBar = {
            GkTopAppBar(
                navigationIcon = {
                    GkIconButton(
                        imageVector = GkIcons.ArrowBack,
                        onClick = mainVm.navigator::pop,
                    )
                },
                title = { Text(stringResource(Res.string.ai_chat)) },
                actions = {
                    if (streaming != null) {
                        GkIconButton(
                            imageVector = GkIcons.Stop,
                            contentDescription = stringResource(Res.string.action_stop),
                            onClick = vm::stop,
                        )
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .windowInsetsPadding(
                        WindowInsets.navigationBars.union(WindowInsets.ime)
                    ),
        ) {
            if (entries.isEmpty() && streaming == null) {
                Text(
                    text = stringResource(Res.string.ai_chat_empty),
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(entries) { entry -> AiChatBubble(entry) }
                    if (streaming != null) {
                        item {
                            AiChatBubble(
                                AiChatEntry(fromUser = false, text = streaming.orEmpty())
                            )
                        }
                    }
                    gkPageBottomSpace()
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GkOutlinedTextField(
                    value = input,
                    onValueChange = vm::setInput,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(Res.string.ai_chat_hint)) },
                    maxLines = 5,
                )
                GkIconButton(
                    imageVector = GkIcons.ArrowUpward,
                    contentDescription = stringResource(Res.string.ai_chat_send),
                    enabled = canSend,
                    onClick = vm::send,
                )
            }
        }
    }
}

@Composable
private fun AiChatBubble(entry: AiChatEntry) {
    val alignment = if (entry.fromUser) Alignment.End else Alignment.Start
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color =
                if (entry.fromUser) colors.primaryContainer else colors.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(0.92f),
        ) {
            Text(
                text = entry.text.ifEmpty { "…" },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (entry.fromUser) colors.onPrimaryContainer else colors.onSurface,
            )
        }
    }
}
