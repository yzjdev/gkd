package li.gkd.app.network

import li.gkd.app.R

import kotlinx.serialization.Serializable


@Serializable
data class GithubPoliciesAsset(
    val id: Int,
    val href: String,
) {
    val shortHref: String
        get() = AppLinks.FileShort + id
}
