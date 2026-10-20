package li.gkd.app.subscription

import li.gkd.app.R

import li.gkd.app.error.ErrorMessage
import li.gkd.app.error.resolveMessage
import li.gkd.app.resources.*
import androidx.annotation.StringRes

sealed interface SubscriptionResult {
    suspend fun message(): String?

    data object Busy : SubscriptionResult {
        override suspend fun message(): String = getString(R.string.subscription_busy_retry)
    }

    data class Success(
        val kind: SuccessKind = SuccessKind.None,
        val count: Int = 0,
    ) : SubscriptionResult {
        override suspend fun message(): String? {
            val resource = kind.messageResource ?: return null
            return if (kind == SuccessKind.Refreshed) {
                if (count > 0) getString(resource, count.toString()) else getString(R.string.updates_none)
            } else getString(resource)
        }
    }

    data class Failure(
        val reason: FailureReason,
        val detail: String? = null,
        val cause: Throwable? = null,
    ) : SubscriptionResult {
        override suspend fun message(): String = when (reason) {
            FailureReason.InvalidId -> getString(reason.messageResource, detail.orEmpty())
            FailureReason.DeleteData, FailureReason.DeleteFile, FailureReason.Download,
            FailureReason.Parse, FailureReason.Save -> ErrorMessage.WithDetail(
                ErrorMessage.Resource(reason.messageResource),
                ErrorMessage.Text(cause?.resolveMessage() ?: detail.orEmpty()),
            ).resolve()
            else -> getString(reason.messageResource)
        }
    }

    enum class SuccessKind(val messageResource: Int?) {
        None(null),
        Deleted(R.string.delete_success),
        Added(R.string.subscription_add_success),
        Modified(R.string.subscription_edit_success),
        Refreshed(R.string.subscriptions_updated_count),
    }

    enum class FailureReason(val messageResource: Int) {
        DeleteData(R.string.subscription_data_delete_failed),
        DeleteFile(R.string.subscription_file_delete_cancelled),
        DuplicateUrl(R.string.subscription_duplicate_link),
        Download(R.string.subscription_file_download_failed),
        Parse(R.string.subscription_file_parsing_failed),
        AlreadyExists(R.string.subscription_exists),
        InvalidId(R.string.subscription_id_reserved),
        Save(R.string.subscription_file_save_failed),
        NetworkUnavailable(R.string.network_unavailable),
    }
}
