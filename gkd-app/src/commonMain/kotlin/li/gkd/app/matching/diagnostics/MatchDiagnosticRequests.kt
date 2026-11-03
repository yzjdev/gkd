package li.gkd.app.matching.diagnostics

import kotlinx.serialization.Serializable
import li.gkd.app.model.ActionResult

@Serializable
data class MatchDebugRunRequest(
    val requestId: String,
    val subscription: String,
    val rule: String,
    val execute: Boolean = false,
)

@Serializable
data class MatchDebugResult(
    val matched: Boolean,
    val reason: String,
    val action: ActionResult? = null,
)

@Serializable
data class MatchDebugRunState(
    val request: MatchDebugRunRequest,
    val phase: MatchDebugRunPhase,
    val result: MatchDebugResult? = null,
    val error: String? = null,
    val engineRunId: Long? = null,
)
