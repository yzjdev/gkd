package li.gkd.app.ui.subscription

import li.gkd.app.R

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import li.gkd.app.resources.*
import li.gkd.app.ui.MainViewModel
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkSubscriptionPageContent
import li.gkd.app.ui.component.autoFocus
import li.gkd.app.ui.navigation.GkEditor
import li.gkd.app.ui.navigation.RuleHelpRoute
import li.gkd.app.ui.navigation.UpsertRuleGroupRoute
import li.gkd.app.ui.page.EditorSession
import li.gkd.app.ui.platform.UiHost
import li.gkd.app.ui.platform.hideIme
import li.gkd.app.ui.platform.inputInsets
import li.gkd.app.ui.share.LocalDarkTheme
import li.gkd.app.ui.style.JSON5_LARGE_TEXT_THRESHOLD
import li.gkd.app.ui.style.getJson5Transformation
import li.gkd.app.ui.style.scaffoldPadding
import androidx.compose.ui.res.stringResource

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
    val visualDraft by vm.visualDraft.collectAsStateWithLifecycle()
    // 默认进入可视化编辑，用户可切回 JSON 文本编辑。
    var blankPage by rememberSaveable { mutableStateOf(true) }
    val visualTarget = when {
        vm.isAddAnyApp -> VisualTarget.NewApp
        vm.appId != null -> VisualTarget.AppGroup
        else -> VisualTarget.GlobalGroup
    }
    GkSubscriptionPageContent(vm.uiState, mainVm.navigator::pop) { data ->
        // 默认可视化：数据就绪后按已有规则初始化草稿，避免编辑时从空表单开始。
        if (blankPage) {
            LaunchedEffect(Unit) { vm.startVisualDraft() }
        }
        val text = draft ?: data.initialText
        val visualText = visualDraft?.let { RuleVisualDraft.toJson5(it, visualTarget) }
        val saveText = if (blankPage) visualText else text
        val inputInsets: Modifier = inputInsets()

        GkEditor(
            EditorSession(
                title =
                    stringResource(if (vm.isEdit) R.string.rule_edit else R.string.rule_add),
                hasChanges = { saveText != null && vm.hasTextChanged(saveText) },
                saveEnabled = !saveText.isNullOrBlank() &&
                        (vm.isEdit || saveText != data.initialText),
                onSave = { saveText?.let { vm.saveRule(it) } },
                menuActions = {
                    GkIconButton(
                        // 图标表示点击后会切换到的编辑方式：代码 = JSON 文本，列表 = 可视化。
                        imageVector = if (blankPage) GkIcons.Code else GkIcons.FormatListBulleted,
                        contentDescription = stringResource(
                            if (blankPage) R.string.rule_editor_switch_to_input
                            else R.string.rule_editor_switch_to_blank,
                        ),
                        onClick = {
                            host.hideIme()
                            if (blankPage) {
                                // 切回输入框时带上可视化生成的内容，便于继续微调。
                                visualText?.let(vm::setDraft)
                            } else {
                                vm.startVisualDraft()
                            }
                            blankPage = !blankPage
                        },
                    )
                    GkIconButton(
                        imageVector = GkIcons.HelpOutline,
                        contentDescription = stringResource(R.string.rule_help),
                        onClick = {
                            host.hideIme()
                            mainVm.navigator.navigate(RuleHelpRoute)
                        },
                    )
                },
            ),
            onSaved = vm::onSaved,
            navigator = mainVm.navigator,
            hideIme = host::hideIme,
            actions = vm.editorActions,
        ) { paddingValues ->
            if (blankPage) {
                RuleVisualEditor(
                    draft = visualDraft ?: VisualRuleGroupDraft(),
                    target = visualTarget,
                    onDraftChange = vm::setVisualDraft,
                    modifier = Modifier.scaffoldPadding(paddingValues).fillMaxSize(),
                    // 只让正文避让输入法，顶部 Tab 保持不动。
                    contentInsets = inputInsets,
                )
                return@GkEditor
            }
            // 代码编辑面用轻 tonal 底，四周留出页边距，不再贴边。
            val editorSurface = MaterialTheme.colorScheme.surfaceContainerLow
            val textColors =
                TextFieldDefaults.colors(
                    focusedContainerColor = editorSurface,
                    unfocusedContainerColor = editorSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                )
            BoxWithConstraints(
                // 容器整体避让输入法，编辑框随视口收缩，不要在框内再留一份空白。
                modifier = Modifier.scaffoldPadding(paddingValues).fillMaxSize().padding(8.dp)
                    .then(inputInsets),
            ) {
                val availableWidth = maxWidth
                val horizontalScroll = rememberScrollState()
                val textMeasurer = rememberTextMeasurer()
                val density = LocalDensity.current
                // 代码区用等宽字体，与 JSON5 语法着色配合。
                val bodyLarge = MaterialTheme.typography.bodyLarge
                val editorTextStyle = remember(bodyLarge) {
                    bodyLarge.copy(fontFamily = FontFamily.Monospace)
                }
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
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textColors,
                            visualTransformation = getJson5Transformation(LocalDarkTheme.current),
                            placeholder = {
                                Text(
                                    text =
                                        if (vm.isApp) stringResource(R.string.app_rule_input_hint)
                                        else stringResource(R.string.global_rule_input_hint)
                                )
                            },
                        )
                    }
                }
                if (text.isNotEmpty()) {
                    Text(
                        text = text.length.toString(),
                        modifier =
                            Modifier.padding(12.dp)
                                .align(Alignment.TopEnd)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
