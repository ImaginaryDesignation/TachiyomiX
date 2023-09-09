package tachiyomi.domain.backup.service

import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.provider.FolderProvider

class BackupPreferences(
    private val folderProvider: FolderProvider,
    private val preferenceStore: PreferenceStore,
) {

    fun backupsDirectory() = preferenceStore.getString("backup_directory", folderProvider.path())

    fun numberOfBackups() = preferenceStore.getInt("backup_slots", 2)

    fun backupInterval() = preferenceStore.getInt("backup_interval", 12)

    fun showAutoBackupNotifications() = preferenceStore.getBoolean("show_auto_backup_notifications", true)

    fun showAutoBackupErrorNotificationOnly() = preferenceStore.getBoolean("show_auto_backup_error_notification_only", false)

    fun backupLastTimestamp() = preferenceStore.getLong("backup_last_timestamp", 0L)

    fun autoBackupStatus() = preferenceStore.getInt("auto_backup_status", -1)

    fun coverBackupLimit() = preferenceStore.getInt("cover_backup_slots", 3)

    fun clearCoverBackupFilterCriteria() = preferenceStore.getInt("pref_clear_backup_filter_criteria", 0)

    fun clearCoverBackupSortCriteria() = preferenceStore.getString("pref_clear_backup_sort_criteria", "ALPHABETICAL")

    fun clearCoverBackupSortDirection() = preferenceStore.getString("pref_clear_backup_sort_direction", "ASCENDING")
}
