package li.gkd.app.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import li.gkd.app.ai.AiDefaultBaseUrl
import li.gkd.app.resources.*
import li.gkd.app.ui.MainViewModel
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkMenuItemRadioButton
import li.gkd.app.ui.component.GkPageBottomSpace
import li.gkd.app.ui.component.GkRotatingLoadingIcon
import li.gkd.app.ui.component.GkSettingItem
import li.gkd.app.ui.component.GkTextSwitch
import li.gkd.app.ui.component.LocalOverlayBackHandler
import li.gkd.app.ui.navigation.GkEditor
import li.gkd.app.ui.page.EditorSession
import li.gkd.app.ui.platform.LocalUiHost
import li.gkd.app.ui.platform.hideIme
import li.gkd.app.ui.style.itemHorizontalPadding
import org.jetbrains.compose.resources.stringResource

@Composable
fun AiSettingsPage() {
    val host = LocalUiHost.current
    val mainVm = MainViewModel.requireCurrent()
    val vm = viewModel { AiSettingsViewModel() }
    val enabled by vm.enabled.collectAsStateWithLifecycle()
    val baseUrl by vm.baseUrl.collectAsStateWithLifecycle()
    val apiKey by vm.apiKey.collectAsStateWithLifecycle()
    val model by vm.model.collectAsStateWithLifecycle()
    val models by vm.models.collectAsStateWithLifecycle()
    val loadingModels by vm.loadingModels.collectAsStateWithLifecycle()

    GkEditor(
        EditorSession(
            title = stringResource(Res.string.ai_title),
            hasChanges = vm::hasChanges,
            onSave = vm::save,
        ),
        navigator = mainVm.navigator,
        hideIme = host::hideIme,
        actions = vm.editorActions,
    ) { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AiCard {
                GkTextSwitch(
                    title = stringResource(Res.string.ai_enable),
                    subtitle = stringResource(Res.string.ai_enable_description),
                    checked = enabled,
                    onCheckedChange = vm::setEnabled,
                )
            }
            AiSectionTitle(stringResource(Res.string.ai_connection))
            AiCard {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = vm::setBaseUrl,
                    label = { Text(stringResource(Res.string.ai_base_url)) },
                    placeholder = { Text(AiDefaultBaseUrl) },
                    supportingText = { Text(stringResource(Res.string.ai_base_url_description)) },
                    leadingIcon = { AiFieldIcon(GkIcons.Link) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth().aiFieldPadding(),
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = vm::setApiKey,
                    label = { Text(stringResource(Res.string.ai_api_key)) },
                    supportingText = { Text(stringResource(Res.string.ai_api_key_description)) },
                    leadingIcon = { AiFieldIcon(GkIcons.Key) },
                    singleLine = true,
                    visualTransformation =
                        if (apiKey.isEmpty()) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().aiFieldPadding(),
                )
            }
            AiSectionTitle(stringResource(Res.string.ai_model))
            AiCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ModelPicker(
                        model = model,
                        models = models,
                        onSelect = vm::setModel,
                        modifier = Modifier.weight(1f),
                    )
                    GkIconButton(
                        imageVector = GkIcons.Autorenew,
                        contentDescription = stringResource(Res.string.ai_model_refresh),
                        enabled = apiKey.isNotEmpty() && !loadingModels,
                        onClick = vm::refreshModels,
                    )
                }
                if (loadingModels) {
                    AiModelHint {
                        GkRotatingLoadingIcon(loading = true)
                        Text(
                            text = stringResource(Res.string.ai_model_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (models.isEmpty()) {
                    AiModelHint {
                        Text(
                            text = stringResource(Res.string.ai_model_fetch_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            AiCard {
                GkSettingItem(
                    title = stringResource(Res.string.ai_test),
                    imageVector = null,
                    onClick = vm::testConnection,
                )
            }
            GkPageBottomSpace()
        }
    }
}

/** Items inside a card already carry their own horizontal padding. */
private fun Modifier.aiFieldPadding(): Modifier =
    this.padding(horizontal = itemHorizontalPadding)

@Composable
private fun AiFieldIcon(imageVector: androidx.compose.ui.graphics.vector.ImageVector) {
    GkIcon(
        imageVector = imageVector,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AiCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = itemHorizontalPadding),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}

@Composable
private fun AiSectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = itemHorizontalPadding, top = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun AiModelHint(content: @Composable () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(start = itemHorizontalPadding, end = itemHorizontalPadding, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = { content() },
    )
}

@Composable
private fun ModelPicker(
    model: String,
    models: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    if (expanded) LocalOverlayBackHandler.current { expanded = false }
    Box(modifier) {
        GkSettingItem(
            title = model.ifEmpty { stringResource(Res.string.ai_model_select) },
            imageVector = GkIcons.UnfoldMore,
            onClick = { if (models.isNotEmpty()) expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            models.forEach { candidate ->
                GkMenuItemRadioButton(
                    text = candidate,
                    selected = candidate == model,
                    onClick = {
                        onSelect(candidate)
                        expanded = false
                    },
                )
            }
        }
    }
}
