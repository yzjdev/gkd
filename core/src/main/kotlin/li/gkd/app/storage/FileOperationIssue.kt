package li.gkd.app.storage

import li.gkd.app.R

import li.gkd.app.resources.*
import androidx.annotation.StringRes

sealed interface FileOperationIssue {
    val name: String
    val messageResource: Int
}

enum class ArchiveIssue(override val messageResource: Int) : FileOperationIssue {
    FileCountExceeded(R.string.archive_file_count_exceeded),
    FileTooLarge(R.string.archive_file_too_large),
    PathInvalid(R.string.archive_path_invalid),
    PathOutsideDestination(R.string.archive_path_outside_destination),
    TotalSizeExceeded(R.string.archive_total_size_exceeded),
}

enum class BackupIssue(override val messageResource: Int) : FileOperationIssue {
    ArchiveTooLarge(R.string.backup_archive_too_large),
    DuplicateSubscriptionId(R.string.backup_duplicate_subscription_id),
    InvalidArchive(R.string.backup_invalid_archive),
    SourceAccessDenied(R.string.backup_reselect_file),
    UnknownRuleConfigType(R.string.backup_unknown_rule_config_type),
    VersionUnsupported(R.string.backup_version_unsupported),
    SubscriptionFileIdMismatch(R.string.subscription_file_id_mismatch_detail),
    SubscriptionInvalidFilename(R.string.subscription_invalid_filename),
}

enum class SnapshotIssue(override val messageResource: Int) : FileOperationIssue {
    FilesIncomplete(R.string.snapshot_files_incomplete),
}
