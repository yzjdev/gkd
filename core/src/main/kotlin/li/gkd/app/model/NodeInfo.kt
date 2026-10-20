package li.gkd.app.model

import li.gkd.app.R

import kotlinx.serialization.Serializable

@Serializable
data class NodeInfo(
    val id: Int,
    val pid: Int,
    val idQf: Boolean?,
    val textQf: Boolean?,
    val attr: AttrInfo,
)

