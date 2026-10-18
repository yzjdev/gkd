package li.gkd.app.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import li.gkd.app.resources.*
import li.gkd.app.ui.MainViewModel
import li.gkd.app.ui.component.GkSubscriptionPageContent
import li.gkd.app.ui.component.autoFocus
import li.gkd.app.ui.navigation.GkEditor
import li.gkd.app.ui.navigation.UpsertRuleGroupRoute
import li.gkd.app.ui.page.EditorSession
import li.gkd.app.ui.platform.UiHost
import li.gkd.app.ui.platform.hideIme
import li.gkd.app.ui.platform.inputInsets
import li.gkd.app.ui.share.LocalDarkTheme
import li.gkd.app.ui.style.JSON5_LARGE_TEXT_THRESHOLD
import li.gkd.app.ui.style.getJson5Transformation
import li.gkd.app.ui.style.scaffoldPadding
import org.jetbrains.compose.resources.stringResource

@Composable
fun UpsertRuleGroupPage(
    host: UiHost,
    route: UpsertRuleGroupRoute,
) {
    val mainVm = MainViewModel.requireCurrent()
    val vm = viewModel {
        UpsertRuleGroupViewModel(route)
    }
    val draft by vm.draft.collectAsStateWithLifecycle()
    GkSubscriptionPageContent(vm.uiState, mainVm.navigator::pop) { data ->
        val text = draft ?: data.initialText
        val inputInsets: Modifier = inputInsets()

        GkEditor(
            EditorSession(
                title =
                    stringResource(if (vm.isEdit) Res.string.rule_edit else Res.string.rule_add),
                hasChanges = { vm.hasTextChanged(text) },
                saveEnabled = text.isNotBlank(),
                onSave = { vm.saveRule(text) },
            ),
            onSaved = vm::onSaved,
            navigator = mainVm.navigator,
            hideIme = host::hideIme,
            actions = vm.editorActions,
        ) { paddingValues ->
            val textColors =
                TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                )
            BoxWithConstraints(
                modifier = Modifier.scaffoldPadding(paddingValues).fillMaxSize(),
            ) {
                val availableWidth = maxWidth
                val horizontalScroll = rememberScrollState()
                val textMeasurer = rememberTextMeasurer()
                val density = LocalDensity.current
                val editorTextStyle = MaterialTheme.typography.bodyLarge
                // 编辑框不自动折行：按最长行撑开宽度，超出屏幕时整体横向滚动。
                val longestLineWidth = remember(text, editorTextStyle, density) {
                    if (text.length > JSON5_LARGE_TEXT_THRESHOLD) {
                        null
                    } else {
                        text.lineSequence().maxOfOrNull { line ->
                            textMeasurer.measure(line, editorTextStyle, softWrap = false).size.width
                        }?.let { width -> with(density) { width.toDp() } }
                    }
                }
                Box(modifier = Modifier.fillMaxSize().horizontalScroll(horizontalScroll)) {
                    CompositionLocalProvider(
                        LocalTextStyle provides editorTextStyle
                    ) {
                        // 预留光标与内边距的空间，避免最长行仍被挤到换行。
                        val editorWidth = longestLineWidth
                            ?.let { maxOf(availableWidth, it + 48.dp) }
                            ?: availableWidth
                        TextField(
                            value = text,
                            onValueChange = vm::setDraft,
                            modifier = Modifier.autoFocus(immediateFocus = true)
                                .width(editorWidth)
                                .fillMaxHeight()
                                .then(inputInsets),
                            shape = RectangleShape,
                            colors = textColors,
                            visualTransformation = getJson5Transformation(LocalDarkTheme.current),
                            placeholder = {
                                Text(
                                    text =
                                        if (vm.isApp) stringResource(Res.string.app_rule_input_hint)
                                        else stringResource(Res.string.global_rule_input_hint)
                                )
                            },
                        )
                    }
                }
                if (text.isNotEmpty()) {
                    Text(
                        text = text.length.toString(),
                        modifier =
                            Modifier.padding(8.dp)
                                .align(Alignment.TopEnd)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(horizontal = 2.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}
