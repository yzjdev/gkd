package li.gkd.app.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import li.gkd.app.resources.*
import li.gkd.app.settings.SettingsRepository
import li.gkd.app.ui.MainViewModel
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkMenuItemCheckbox
import li.gkd.app.ui.component.GkSubscriptionPageContent
import li.gkd.app.ui.component.autoFocus
import li.gkd.app.ui.navigation.GkEditor
import li.gkd.app.ui.navigation.UpsertRuleGroupRoute
import li.gkd.app.ui.page.EditorSession
import li.gkd.app.ui.platform.LocalUiHost
import li.gkd.app.ui.platform.hideIme
import li.gkd.app.ui.platform.inputInsets
import li.gkd.app.ui.share.LocalDarkTheme
import li.gkd.app.ui.style.LocalRuleTextHighlighter
import li.gkd.app.ui.style.scaffoldPadding
import li.gkd.app.util.ToastUtils
import org.jetbrains.compose.resources.stringResource

private val EditorGutterWidth = 44.dp
private val EditorTextHorizontalPadding = 32.dp
private val EditorTextVerticalPadding = 16.dp

@Composable
fun UpsertRuleGroupPage(
    route: UpsertRuleGroupRoute,
) {
    val host = LocalUiHost.current
    val mainVm = MainViewModel.requireCurrent()
    val vm = viewModel {
        UpsertRuleGroupViewModel(route)
    }
    val draft by vm.draft.collectAsStateWithLifecycle()
    val settings by SettingsRepository.settings.collectAsStateWithLifecycle()
    val wrapLines = settings.editorWrapLines
    val indentSize = settings.editorIndentSize
    var menuExpanded by remember { mutableStateOf(false) }
    GkSubscriptionPageContent(vm.uiState, mainVm.navigator::pop) { data ->
        val text = draft ?: data.initialText
        val missingGroup = vm.isEdit && data.initialGroup == null
        val inputInsets: Modifier = inputInsets()

        GkEditor(
            EditorSession(
                title =
                    stringResource(if (vm.isEdit) Res.string.rule_edit else Res.string.rule_add),
                hasChanges = { vm.hasTextChanged(text) },
                saveEnabled = text.isNotBlank() && !missingGroup,
                onSave = { vm.saveRule(text) },
            ),
            onSaved = vm::onSaved,
            navigator = mainVm.navigator,
            hideIme = host::hideIme,
            actions = vm.editorActions,
            saveBeforeMenu = true,
            menuContent = {
                Box {
                    GkIconButton(
                        imageVector = GkIcons.MoreVert,
                        contentDescription = stringResource(Res.string.more_label),
                        onClick = { menuExpanded = true },
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        GkMenuItemCheckbox(
                            text = stringResource(Res.string.rule_editor_wrap_lines),
                            checked = wrapLines,
                            onClick = { SettingsRepository.setEditorWrapLines(!wrapLines) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.rule_editor_format)) },
                            onClick = {
                                menuExpanded = false
                                vm.formatDraft(text)?.let {
                                    ToastUtils.show(Res.string.rule_editor_format_failed.getSync())
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        Res.string.rule_editor_indent_size,
                                        indentSize.toString(),
                                    )
                                )
                            },
                            onClick = {
                                SettingsRepository.setEditorIndentSize(
                                    if (indentSize >= 8) 2 else indentSize + 2
                                )
                            },
                        )
                    }
                }
            },
        ) { paddingValues ->
            val textColors =
                TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                )
            BoxWithConstraints(modifier = Modifier.scaffoldPadding(paddingValues).fillMaxSize()) {
                val density = LocalDensity.current
                val measurer = rememberTextMeasurer()
                val bodyStyle =
                    MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace)
                CompositionLocalProvider(LocalTextStyle provides bodyStyle) {
                    val modifier = Modifier.autoFocus(immediateFocus = true)
                    var fieldValue by remember { mutableStateOf(TextFieldValue(text)) }
                    LaunchedEffect(text) {
                        if (fieldValue.text != text) {
                            fieldValue = fieldValue.copy(text = text)
                        }
                    }
                    val scrollState = rememberScrollState()
                    var viewportPx by remember { mutableStateOf(0) }
                    val gutterWidthPx = with(density) { EditorGutterWidth.toPx() }
                    val horizontalPaddingPx = with(density) { EditorTextHorizontalPadding.toPx() }
                    val textWidthPx =
                        with(density) { maxWidth.toPx() } - gutterWidthPx - horizontalPaddingPx
                    val contentWidth =
                        remember(text, bodyStyle) {
                            text.lineSequence()
                                .maxOfOrNull { line ->
                                    measurer
                                        .measure(
                                            text = AnnotatedString(line),
                                            style = bodyStyle,
                                            softWrap = false,
                                            maxLines = 1,
                                        )
                                        .size
                                        .width
                                }
                                ?.toFloat() ?: 0f
                        }
                    val lineRows =
                        remember(text, wrapLines, textWidthPx, bodyStyle) {
                            val limit = textWidthPx.toInt().coerceAtLeast(1)
                            text.split('\n').map { line ->
                                if (!wrapLines) {
                                    1
                                } else {
                                    measurer
                                        .measure(
                                            text = AnnotatedString(line.ifEmpty { " " }),
                                            style = bodyStyle,
                                            constraints = Constraints(maxWidth = limit),
                                        )
                                        .lineCount
                                        .coerceAtLeast(1)
                                }
                            }
                        }
                    val editorField: @Composable (Modifier) -> Unit = { fieldModifier ->
                        RuleTextField(
                            value = fieldValue,
                            onValueChange = { value ->
                                fieldValue = value
                                vm.setDraft(value.text)
                            },
                            missingGroup = missingGroup,
                            groupKey = route.groupKey,
                            isApp = vm.isApp,
                            textColors = textColors,
                            textStyle = bodyStyle,
                            modifier = fieldModifier,
                        )
                    }
                    // 光标行滚动：键盘弹出或光标移动时，让光标行保持在键盘上方可视区域内
                    val lineHeightPx =
                        remember(bodyStyle) {
                            measurer
                                .measure(text = AnnotatedString("中"), style = bodyStyle, maxLines = 1)
                                .size
                                .height
                        }
                    val cursorLine =
                        remember(fieldValue.selection, text) {
                            text.take(fieldValue.selection.end.coerceIn(0, text.length))
                                .count { it == '\n' }
                        }
                    LaunchedEffect(cursorLine, viewportPx, lineRows, lineHeightPx) {
                        if (viewportPx <= 0) return@LaunchedEffect
                        val visiblePx = viewportPx.toFloat().coerceAtLeast(lineHeightPx.toFloat())
                        val cursorRowStart = lineRows.take(cursorLine).sum()
                        val cursorRowEnd = cursorRowStart + (lineRows.getOrNull(cursorLine) ?: 1)
                        val rowTopPx = cursorRowStart * lineHeightPx
                        val rowBottomPx = cursorRowEnd * lineHeightPx
                        val marginPx = lineHeightPx * 2
                        when {
                            rowTopPx < scrollState.value ->
                                scrollState.scrollTo(rowTopPx.toInt())
                            rowBottomPx + marginPx > scrollState.value + visiblePx ->
                                scrollState.scrollTo((rowBottomPx + marginPx - visiblePx).toInt())
                        }
                    }
                    Row(
                        // inputInsets 放在 verticalScroll 之前：键盘弹出时收缩视口而非撑大滚动内容
                        modifier =
                            Modifier.fillMaxSize()
                                .then(inputInsets)
                                .onSizeChanged { viewportPx = it.height }
                                .verticalScroll(scrollState),
                        verticalAlignment = Alignment.Top,
                    ) {
                        LineNumberGutter(rows = lineRows, textStyle = bodyStyle)
                        Box(modifier = Modifier.weight(1f)) {
                            if (wrapLines) {
                                editorField(modifier.fillMaxSize())
                            } else {
                                Box(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                ) {
                                    editorField(
                                        Modifier
                                            .fillMaxHeight()
                                            .width(
                                                with(density) {
                                                    (contentWidth + horizontalPaddingPx + 8.dp.toPx())
                                                        .toDp()
                                                },
                                            ),
                                    )
                                }
                            }
                        }
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

@Composable
private fun LineNumberGutter(rows: List<Int>, textStyle: TextStyle) {
    Column(
        modifier =
            Modifier.width(EditorGutterWidth)
                .padding(top = EditorTextVerticalPadding, end = 8.dp),
        horizontalAlignment = Alignment.End,
    ) {
        var lineNumber = 1
        rows.forEach { rowCount ->
            Text(
                text = lineNumber.toString(),
                style = textStyle,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                softWrap = false,
            )
            repeat(rowCount - 1) {
                Text(
                    text = " ",
                    style = textStyle,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            lineNumber++
        }
    }
}

@Composable
private fun RuleTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    missingGroup: Boolean,
    groupKey: Int?,
    isApp: Boolean,
    textColors: TextFieldColors,
    textStyle: TextStyle,
    modifier: Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        isError = missingGroup,
        textStyle = textStyle,
        supportingText =
            if (missingGroup) {
                {
                    Text(
                        stringResource(
                            Res.string.subscription_rule_missing_detail,
                            groupKey.toString(),
                        ),
                    )
                }
            } else null,
        modifier = modifier,
        shape = RectangleShape,
        colors = textColors,
        visualTransformation = LocalRuleTextHighlighter.current(LocalDarkTheme.current),
        placeholder = {
            Text(
                text =
                    if (isApp) stringResource(Res.string.app_rule_input_hint)
                    else stringResource(Res.string.global_rule_input_hint),
            )
        },
    )
}
