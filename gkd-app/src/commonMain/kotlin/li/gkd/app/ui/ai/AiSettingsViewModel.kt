package li.gkd.app.ui.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import li.gkd.app.ai.AiApiKeyStore
import li.gkd.app.ai.AiClient
import li.gkd.app.ai.AiConnection
import li.gkd.app.ai.isConfigured
import li.gkd.app.resources.*
import li.gkd.app.settings.SettingsRepository
import li.gkd.app.ui.page.EditorActionsState
import li.gkd.app.ui.page.EditorSaveResult
import li.gkd.app.ui.state.BaseViewModel
import li.gkd.app.ui.share.launchUi
import li.gkd.app.util.ToastUtils

class AiSettingsViewModel : BaseViewModel() {
    val editorActions = EditorActionsState(scope)

    private val initial = SettingsRepository.settings.value
    private val initialApiKey = AiApiKeyStore.value.value

    val enabled: StateFlow<Boolean>
        field = MutableStateFlow(initial.enableAi)

    val baseUrl: StateFlow<String>
        field = MutableStateFlow(initial.aiBaseUrl)

    val apiKey: StateFlow<String>
        field = MutableStateFlow(initialApiKey)

    val model: StateFlow<String>
        field = MutableStateFlow(initial.aiModel)

    val models: StateFlow<List<String>>
        field = MutableStateFlow(emptyList())

    val loadingModels: StateFlow<Boolean>
        field = MutableStateFlow(false)

    init {
        if (initialApiKey.isNotEmpty()) refreshModels()
    }

    fun setEnabled(value: Boolean) {
        enabled.value = value
    }

    fun setBaseUrl(value: String) {
        baseUrl.value = value.trim()
    }

    fun setApiKey(value: String) {
        apiKey.value = value.filter { it != '\n' && it != '\r' }.trim()
    }

    fun setModel(value: String) {
        model.value = value
    }

    private fun connection() = AiConnection(apiKey.value, baseUrl.value, model.value)

    /** Models come from the service, so a saved id that disappeared stays selectable. */
    fun refreshModels() {
        if (apiKey.value.isEmpty() || loadingModels.value) return
        loadingModels.value = true
        scope.launchUi {
            try {
                models.value = AiClient.models(connection())
            } finally {
                loadingModels.value = false
            }
        }
    }

    suspend fun hasChanges(): Boolean =
        enabled.value != initial.enableAi ||
            baseUrl.value != initial.aiBaseUrl ||
            apiKey.value != initialApiKey ||
            model.value != initial.aiModel

    suspend fun save(): EditorSaveResult {
        if (enabled.value && !connection().isConfigured()) {
            return EditorSaveResult.Rejected(Res.string.ai_config_incomplete.getSync())
        }
        SettingsRepository.updateSettings {
            it.copy(
                enableAi = enabled.value,
                aiBaseUrl = baseUrl.value,
                aiModel = model.value,
            )
        }
        if (apiKey.value != initialApiKey) AiApiKeyStore.save(apiKey.value)
        return EditorSaveResult.Saved
    }

    fun testConnection() {
        val connection = connection()
        if (!connection.isConfigured()) {
            ToastUtils.show(Res.string.ai_config_incomplete.getSync())
            return
        }
        scope.launchUi {
            AiClient.complete(connection, "ping")
            ToastUtils.show(Res.string.ai_test_success.getSync())
        }
    }
}
