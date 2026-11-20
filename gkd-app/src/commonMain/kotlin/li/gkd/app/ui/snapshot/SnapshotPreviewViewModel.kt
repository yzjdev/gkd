package li.gkd.app.ui.snapshot

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import li.gkd.app.ai.AiApiKeyStore
import li.gkd.app.ai.AiConnection
import li.gkd.app.ai.AiRuleResult
import li.gkd.app.ai.addRuleGroupToLocalSubscription
import li.gkd.app.ai.generateRuleGroup
import li.gkd.app.ai.isConfigured
import li.gkd.app.model.ComplexSnapshot
import li.gkd.app.resources.*
import li.gkd.app.settings.SettingsRepository
import li.gkd.app.snapshot.SnapshotNodeTree
import li.gkd.app.snapshot.SnapshotStore
import li.gkd.app.state.Loadable
import li.gkd.app.subscription.SubscriptionEditResult
import li.gkd.app.ui.share.launchUi
import li.gkd.app.ui.state.BaseViewModel
import li.gkd.app.util.JsonUtils
import li.gkd.app.util.ToastUtils

data class SnapshotNodeSelection(
    val id: Int? = null,
)

class SnapshotPreviewViewModel : BaseViewModel() {
    val tree: StateFlow<Loadable<SnapshotNodeTree>>
        field = MutableStateFlow<Loadable<SnapshotNodeTree>>(Loadable.Loading)
    val selection: StateFlow<SnapshotNodeSelection>
        field = MutableStateFlow(SnapshotNodeSelection())
    private var loadJob: Job? = null
    private var hitJob: Job? = null

    fun load(id: Long) {
        loadJob?.cancel()
        clearSelection()
        tree.value = Loadable.Loading
        loadJob = scope.launch {
            tree.value =
                try {
                    val text =
                        withContext(Dispatchers.IO) { SnapshotStore.snapshotFile(id).readText() }
                    Loadable.Ready(
                        withContext(Dispatchers.Default) {
                            val snapshot =
                                JsonUtils.keepingNulls.decodeFromString<ComplexSnapshot>(text)
                            SnapshotNodeTree(snapshot)
                        },
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Loadable.Failure(e)
                }
        }
    }

    fun hit(x: Float, y: Float, onSelected: (Int) -> Unit) {
        val current = tree.value.value ?: return
        hitJob?.cancel()
        hitJob = scope.launch {
            val hits =
                withContext(Dispatchers.Default) {
                    current.hit(x * current.snapshot.screenWidth, y * current.snapshot.screenHeight)
                }
            val firstHit = hits.firstOrNull()
            selection.value = SnapshotNodeSelection(id = firstHit ?: selection.value.id)
            if (firstHit != null) onSelected(firstHit)
        }
    }

    fun selectFirstNodeIfNone(snapshotId: Long) {
        val current = tree.value.value ?: return
        if (selection.value.id == null && current.snapshot.id == snapshotId) {
            current.roots.firstOrNull()?.let { select(it.id) }
        }
    }

    fun select(id: Int) {
        selection.value = SnapshotNodeSelection(id)
    }

    fun clearSelection() {
        hitJob?.cancel()
        selection.value = SnapshotNodeSelection()
    }

    /** Live streamed text while the model writes; null when no generation is running. */
    val ruleStream: StateFlow<String?>
        field = MutableStateFlow<String?>(null)

    val ruleResult: StateFlow<AiRuleResult?>
        field = MutableStateFlow<AiRuleResult?>(null)

    private var ruleJob: Job? = null

    /** Reads the loaded snapshot and asks AI for one rule group; the caller shows the result. */
    fun generateRule(goal: String) {
        val snapshot = tree.value.value?.snapshot ?: return
        if (ruleJob?.isActive == true) return
        val settings = SettingsRepository.settings.value
        val connection =
            AiConnection(
                apiKey = AiApiKeyStore.value.value,
                baseUrl = settings.aiBaseUrl,
                model = settings.aiModel,
            )
        if (!connection.isConfigured()) {
            ruleResult.value = AiRuleResult.Invalid(Res.string.ai_config_incomplete.getSync())
            return
        }
        ruleStream.value = ""
        ruleJob =
            scope.launchUi {
                try {
                    val result =
                        generateRuleGroup(connection, snapshot, goal) { delta ->
                            ruleStream.value = (ruleStream.value ?: "") + delta
                        }
                    ruleResult.value = result
                } finally {
                    ruleStream.value = null
                }
            }
    }

    fun cancelRuleGeneration() {
        ruleJob?.cancel()
        ruleJob = null
        ruleStream.value = null
    }

    fun consumeRuleResult() {
        ruleResult.value = null
    }

    /** Appends the validated group to the local subscription of the snapshot's app. */
    fun addRuleResult(appName: String?) {
        val snapshot = tree.value.value?.snapshot ?: return
        val group = (ruleResult.value as? AiRuleResult.Success)?.group ?: return
        if (ruleJob?.isActive == true) return
        ruleJob =
            scope.launchUi {
                when (val saved = addRuleGroupToLocalSubscription(snapshot.appId, appName, group)) {
                    is SubscriptionEditResult.Rejected ->
                        ToastUtils.show(saved.errorMessage.resolve())
                    is SubscriptionEditResult.Success -> {
                        ToastUtils.show(
                            (if (saved.value) Res.string.ai_rule_added
                                    else Res.string.ai_rule_add_unchanged)
                                .getSync()
                        )
                        ruleResult.value = null
                    }
                }
            }
    }
}
