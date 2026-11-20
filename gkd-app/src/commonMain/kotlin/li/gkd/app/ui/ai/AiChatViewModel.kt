package li.gkd.app.ui.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import li.gkd.app.ai.AiApiKeyStore
import li.gkd.app.ai.AiChatSystemPrompt
import li.gkd.app.ai.AiClient
import li.gkd.app.ai.AiConnection
import li.gkd.app.ai.AiMessage
import li.gkd.app.ai.isConfigured
import li.gkd.app.ai.toChatContext
import li.gkd.app.model.ComplexSnapshot
import li.gkd.app.resources.*
import li.gkd.app.settings.SettingsRepository
import li.gkd.app.snapshot.SnapshotStore
import li.gkd.app.ui.share.launchUi
import li.gkd.app.ui.state.BaseViewModel
import li.gkd.app.util.JsonUtils

data class AiChatEntry(val fromUser: Boolean, val text: String)

/**
 * Chat over the snapshots selected on the list page. The context is read once from the snapshot
 * files and sent as a system message, so the conversation stays multi-turn without re-reading disk.
 */
class AiChatViewModel(private val snapshotIds: List<Long>) : BaseViewModel() {
    val entries: StateFlow<List<AiChatEntry>>
        field = MutableStateFlow<List<AiChatEntry>>(emptyList())

    /** Non-null while a reply is streaming; the page shows it as the pending answer. */
    val streaming: StateFlow<String?>
        field = MutableStateFlow<String?>(null)

    val input: StateFlow<String>
        field = MutableStateFlow("")

    val canSend: StateFlow<Boolean>
        field = MutableStateFlow(false)

    private var context: String? = null
    private var chatJob: Job? = null

    init {
        loadContext()
    }

    fun setInput(value: String) {
        input.value = value
        canSend.value = value.isNotBlank() && streaming.value == null
    }

    private fun loadContext() {
        scope.launchUi {
            val text =
                withContext(Dispatchers.IO) {
                    snapshotIds.mapNotNull { id ->
                        try {
                            val json = SnapshotStore.snapshotFile(id).readText()
                            JsonUtils.keepingNulls.decodeFromString<ComplexSnapshot>(json)
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
            context = text.takeIf { it.isNotEmpty() }?.toChatContext()
        }
    }

    fun send() {
        val question = input.value.trim()
        if (question.isEmpty() || streaming.value != null) return
        val connection = connection()
        if (!connection.isConfigured()) {
            entries.value = entries.value + AiChatEntry(false, Res.string.ai_config_incomplete.getSync())
            return
        }
        val history = entries.value
        entries.value = history + AiChatEntry(true, question)
        input.value = ""
        canSend.value = false
        streaming.value = ""
        chatJob =
            scope.launchUi {
                try {
                    val messages = buildList {
                        add(AiMessage("system", AiChatSystemPrompt))
                        context?.let { add(AiMessage("system", it)) }
                        history.forEach {
                            add(AiMessage(if (it.fromUser) "user" else "assistant", it.text))
                        }
                        add(AiMessage("user", question))
                    }
                    val answer =
                        AiClient.streamMessages(connection, messages) { delta ->
                            streaming.value = (streaming.value ?: "") + delta
                        }
                    entries.value = entries.value + AiChatEntry(false, answer)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    entries.value =
                        entries.value +
                            AiChatEntry(
                                false,
                                e.message ?: e.javaClass.simpleName,
                            )
                } finally {
                    streaming.value = null
                    canSend.value = input.value.isNotBlank()
                }
            }
    }

    fun stop() {
        chatJob?.cancel()
        chatJob = null
        streaming.value = null
        canSend.value = input.value.isNotBlank()
    }

    private fun connection(): AiConnection {
        val settings = SettingsRepository.settings.value
        return AiConnection(
            apiKey = AiApiKeyStore.value.value,
            baseUrl = settings.aiBaseUrl,
            model = settings.aiModel,
        )
    }
}
