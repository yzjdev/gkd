package li.gkd.app.subscription

import li.gkd.app.R

import li.gkd.app.error.BusinessError
import li.gkd.app.error.ErrorMessage
import li.gkd.app.resources.*
import androidx.annotation.StringRes

/** Business failures retain their identity and diagnostic cause until the display boundary. */
class SubscriptionException(
    val reason: SubscriptionFailureReason,
    val arguments: List<String> = emptyList(),
    cause: Throwable? = null,
) : IllegalStateException(
    reason.name + if (arguments.isEmpty()) "" else arguments.joinToString(prefix = ": "),
    cause,
), BusinessError {
    private val inputParseError: SubscriptionParseException?
        get() = if (reason == SubscriptionFailureReason.RuleInvalidDetail) cause as? SubscriptionParseException else null

    override val errorCode: String
        get() = when {
            inputParseError != null || reason == SubscriptionFailureReason.FormatInvalidDetail -> "INVALID_SUBSCRIPTION"
            reason == SubscriptionFailureReason.SelectorInvalidDetail -> "INVALID_SELECTOR"
            else -> reason.name
        }

    override val errorPath: String?
        get() = inputParseError?.path ?: if (reason == SubscriptionFailureReason.FormatInvalidDetail) "$" else null

    override val errorMessage: ErrorMessage
        get() {
            // A nested parser failure keeps its field path in the business failure's detail.
            val detail = cause?.let {
                when (it) {
                    is SubscriptionParseException -> ErrorMessage.Text(it.message.orEmpty())
                    is BusinessError -> it.errorMessage
                    else -> ErrorMessage.Text(it.message ?: it.toString())
                }
            }
            val embedsCause = reason == SubscriptionFailureReason.RuleInvalidDetail ||
                reason == SubscriptionFailureReason.FormatInvalidDetail
            val parameters = arguments.map { ErrorMessage.Text(it) } +
                if (embedsCause) listOf(detail ?: ErrorMessage.Text("")) else emptyList()
            val text = ErrorMessage.Resource(reason.messageResource, parameters)
            return if (embedsCause || detail == null) text else ErrorMessage.WithDetail(text, detail)
        }
}

enum class SubscriptionFailureReason(val messageResource: Int) {
    CategoryConfigConflict(R.string.category_config_conflict),
    CategoryKeyExhausted(R.string.category_key_exhausted),
    CategoryMissing(R.string.category_missing),
    CategoryNameDuplicate(R.string.category_name_duplicate),
    CategoryNameRequired(R.string.category_name_required),
    ExclusionConfigConflict(R.string.exclusion_config_conflict),
    FileDeleteFailed(R.string.file_delete_failed),
    FormatInvalidDetail(R.string.format_invalid_detail),
    PageExclusionConflict(R.string.page_exclusion_conflict),
    RemoteCategoryDeleteUnsupported(R.string.remote_category_delete_unsupported),
    RemoteCategoryEditUnsupported(R.string.remote_category_edit_unsupported),
    RuleAppIdMismatch(R.string.rule_app_id_mismatch),
    RuleEditConflict(R.string.rule_edit_conflict),
    RuleIdRequired(R.string.rule_id_required),
    RuleInputRequired(R.string.rule_input_required),
    RuleInvalidDetail(R.string.rule_invalid_detail),
    RuleInvalidPosition(R.string.rule_invalid_position),
    RuleKeyMismatch(R.string.rule_key_mismatch),
    RuleMissing(R.string.rule_missing),
    RuleMissingKey(R.string.subscription_rule_missing_detail),
    RuleNameDuplicate(R.string.rule_name_duplicate),
    RuleObjectRequired(R.string.rule_object_required),
    RuleSwitchConflict(R.string.rule_switch_conflict),
    SelectedRulesMissing(R.string.selected_rules_missing),
    SelectorInvalidDetail(R.string.selector_invalid_detail),
    SubscriptionAppMissing(R.string.subscription_app_missing),
    SubscriptionContentConflict(R.string.subscription_content_conflict),
    SubscriptionFileIdMismatch(R.string.subscription_file_id_mismatch),
    SubscriptionFileMissing(R.string.subscription_file_missing),
    SubscriptionFileParseFailed(R.string.subscription_file_parse_failed),
    SubscriptionIdImmutable(R.string.subscription_id_immutable),
    SubscriptionItemIdMismatch(R.string.subscription_item_id_mismatch),
    SubscriptionMissing(R.string.subscription_missing),
    SubscriptionMissingId(R.string.subscription_missing_id),
    SubscriptionNotLoaded(R.string.subscription_not_loaded),
    SubscriptionNotLoadedId(R.string.subscription_not_loaded_id),
    SubscriptionPreviewConflict(R.string.subscription_preview_conflict),
    SubscriptionUnloadedOrMissing(R.string.subscription_unloaded_or_missing),
    SubscriptionUpdateUrlRequestFailed(R.string.subscription_update_url_request_failed),
    SubscriptionUpdatedIdMismatch(R.string.subscription_updated_id_mismatch),
    TextParseFailed(R.string.text_parse_failed),
}
