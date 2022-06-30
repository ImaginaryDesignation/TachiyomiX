package eu.kanade.tachiyomi.ui.setting

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.preference.PreferenceScreen
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.BackupConst
import eu.kanade.tachiyomi.data.backup.BackupCreatorJob
import eu.kanade.tachiyomi.data.backup.BackupRestoreService
import eu.kanade.tachiyomi.data.backup.ValidatorParseException
import eu.kanade.tachiyomi.data.backup.full.FullBackupRestoreValidator
import eu.kanade.tachiyomi.data.backup.full.models.BackupFull
import eu.kanade.tachiyomi.data.backup.legacy.LegacyBackupRestoreValidator
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.requestPermissionsSafe
import eu.kanade.tachiyomi.util.preference.*
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import kotlin.math.ceil

class SettingsBackupController : SettingsController() {

    /**
     * Flags containing information of what to backup.
     */
    private var backupFlags = 0

    // TX-->
    private var fName = ""
    // TX<--

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestPermissionsSafe(arrayOf(WRITE_EXTERNAL_STORAGE), 500)
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        // TX-->
        fName = context.resources.getString(R.string.app_name)
        // TX<--

        titleRes = R.string.label_backup

        preference {
            key = "pref_create_backup"
            titleRes = R.string.pref_create_backup
            summaryRes = R.string.pref_create_backup_summ

            onClick {
                if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
                    context.toast(R.string.restore_miui_warning, Toast.LENGTH_LONG)
                }

                if (!BackupCreatorJob.isManualJobRunning(context)) {
                    val ctrl = CreateBackupDialog()
                    ctrl.targetController = this@SettingsBackupController
                    ctrl.showDialog(router)
                } else {
                    context.toast(R.string.backup_in_progress)
                }
            }
        }
        preference {
            key = "pref_restore_backup"
            titleRes = R.string.pref_restore_backup
            summaryRes = R.string.pref_restore_backup_summ

            onClick {
                if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
                    context.toast(R.string.restore_miui_warning, Toast.LENGTH_LONG)
                }

                if (!BackupRestoreService.isRunning(context)) {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                    val title = resources?.getString(R.string.file_select_backup)
                    val chooser = Intent.createChooser(intent, title)
                    startActivityForResult(chooser, CODE_BACKUP_RESTORE)
                } else {
                    context.toast(R.string.restore_in_progress)
                }
            }
        }

        preferenceCategory {
            titleRes = R.string.pref_backup_service_category

            intListPreference {
                bindTo(preferences.backupInterval())
                titleRes = R.string.pref_backup_interval
                entriesRes = arrayOf(
                    R.string.update_never,
                    R.string.update_6hour,
                    R.string.update_12hour,
                    R.string.update_24hour,
                    R.string.update_48hour,
                    R.string.update_weekly,
                )
                entryValues = arrayOf("0", "6", "12", "24", "48", "168")
                summary = "%s"

                onChange { newValue ->
                    val interval = (newValue as String).toInt()
                    BackupCreatorJob.setupTask(context, interval)
                    true
                }
            }
            preference {
                bindTo(preferences.backupsDirectory())
                titleRes = R.string.pref_backup_directory

                onClick {
                    try {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                        startActivityForResult(intent, CODE_BACKUP_DIR)
                    } catch (e: ActivityNotFoundException) {
                        activity?.toast(R.string.file_picker_error)
                    }
                }

                visibleIf(preferences.backupInterval()) { it > 0 }

                preferences.backupsDirectory().asFlow()
                    .onEach { path ->
                        val dir = UniFile.fromUri(context, path.toUri())
                        summary = dir.filePath + "/automatic"
                    }
                    .launchIn(viewScope)
            }
            intListPreference {
                bindTo(preferences.numberOfBackups())
                titleRes = R.string.pref_backup_slots
                entries = arrayOf("1", "2", "3", "4", "5")
                entryValues = entries
                summary = "%s"

                visibleIf(preferences.backupInterval()) { it > 0 }
            }
        }

        // TX-->
        preference {
            iconRes = R.drawable.ic_info_24dp
            iconTint = context.getResourceColor(android.R.attr.textColorHint)
            isSelectable = false
            summary = context.resources.getString(R.string.backup_info_X, updateTimeString())
        }
        // infoPreference(R.string.backup_info)
        // TX<--
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.settings_backup, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_backup_help -> activity?.openInBrowser(HELP_URL)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && resultCode == Activity.RESULT_OK) {
            val activity = activity ?: return
            val uri = data.data

            if (uri == null) {
                activity.toast(R.string.backup_restore_invalid_uri)
                return
            }

            when (requestCode) {
                CODE_BACKUP_DIR -> {
                    // Get UriPermission so it's possible to write files
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                    activity.contentResolver.takePersistableUriPermission(uri, flags)
                    preferences.backupsDirectory().set(uri.toString())
                }
                CODE_BACKUP_CREATE -> {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                    activity.contentResolver.takePersistableUriPermission(uri, flags)
                    BackupCreatorJob.startNow(activity, uri, backupFlags)
                }
                CODE_BACKUP_RESTORE -> {
                    RestoreBackupDialog(uri).showDialog(router)
                }
            }
        }
    }

    // TX-->
    private fun updateTimeString(): String {
        val status = preferences.autoBackupStatus().get()
        val interval = preferences.backupInterval().get()
        val endTime = preferences.autoBackupTime().get()
        val curTime = System.currentTimeMillis()

        // Number of hours that have passed since last update
        var diff = (curTime - endTime) / 3600000.0

        val templateString = "dd/MM/yy hh:mm a"
        var toastString = "\nLast backup : Never"

        // Check if backups have been created before
        if (endTime != 0L) {
            toastString = "Last backup : " + SimpleDateFormat(templateString).format(endTime) + "\n"
            toastString += getRelativeString(diff, endTime, curTime, " ago", true) + "ago\n"
            if (status != "") {
                toastString += "Last backup status : $status\n"
            }
            // Check if auto backup is on
            if (interval != 0) {
                var times = 1
                // Check if number of hours that have passed since last update is greater than update interval
                if (diff > interval.toDouble()) {
                    // Find number of times the interval has passed
                    times = ceil((diff / interval)).toInt()
                }

                // Find next update time
                val nextTime = endTime + (interval.toLong() * 3600000 * times)
                toastString = "Next backup : " + SimpleDateFormat(templateString).format(nextTime) + "\n"

                // Find number of hours till next update time
                diff = (nextTime - curTime) / 3600000.0

                toastString += "In" + getRelativeString(diff, nextTime, curTime, "In ", false) + "\n"
            }
        }
        return toastString
    }

    private fun getRelativeString(difference: Double, startTime: Long, curTime: Long, replace: String, last: Boolean): String {
        var relativeString = ""
        var diff = difference
        var fromTime = startTime

        // Check if last/next update difference greater than 24 hours
        if (diff > 24.0) {
            relativeString += DateUtils.getRelativeTimeSpanString(fromTime, curTime, DateUtils.DAY_IN_MILLIS)
            // Find number of days
            val timesDay = (diff / 24.0).toInt()
            diff -= 24.0 * timesDay
            if (last) {
                fromTime += 86400000 * timesDay
            } else {
                fromTime -= 86400000 * timesDay
            }
        }
        // Check if last/next update difference less than 1 hour
        if (diff < 1.0) {
            relativeString += DateUtils.getRelativeTimeSpanString(fromTime, curTime, DateUtils.MINUTE_IN_MILLIS)
        } else {
            relativeString += DateUtils.getRelativeTimeSpanString(fromTime, curTime, DateUtils.HOUR_IN_MILLIS)
            if (last) {
                fromTime += diff.toInt() * 3600000
            } else {
                fromTime -= diff.toInt() * 3600000
            }
            relativeString += DateUtils.getRelativeTimeSpanString(fromTime, curTime, DateUtils.MINUTE_IN_MILLIS)
        }
        relativeString = relativeString.replace(replace, " ", true)
        return relativeString
    }
    // TX<--

    fun createBackup(flags: Int) {
        backupFlags = flags
        try {
            // Use Android's built-in file creator
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/*")
                // TX-->
                .putExtra(Intent.EXTRA_TITLE, BackupFull.getDefaultFilename(fName))
            // TX<--

            startActivityForResult(intent, CODE_BACKUP_CREATE)
        } catch (e: ActivityNotFoundException) {
            activity?.toast(R.string.file_picker_error)
        }
    }

    class CreateBackupDialog(bundle: Bundle? = null) : DialogController(bundle) {
        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val activity = activity!!
            val options = arrayOf(
                R.string.manga,
                R.string.categories,
                R.string.chapters,
                R.string.track,
                R.string.history,
            )
                .map { activity.getString(it) }
            val selected = options.map { true }.toBooleanArray()

            return MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.backup_choice)
                .setMultiChoiceItems(options.toTypedArray(), selected) { dialog, which, checked ->
                    if (which == 0) {
                        (dialog as AlertDialog).listView.setItemChecked(which, true)
                    } else {
                        selected[which] = checked
                    }
                }
                .setPositiveButton(R.string.action_create) { _, _ ->
                    var flags = 0
                    selected.forEachIndexed { i, checked ->
                        if (checked) {
                            when (i) {
                                1 -> flags = flags or BackupConst.BACKUP_CATEGORY
                                2 -> flags = flags or BackupConst.BACKUP_CHAPTER
                                3 -> flags = flags or BackupConst.BACKUP_TRACK
                                4 -> flags = flags or BackupConst.BACKUP_HISTORY
                            }
                        }
                    }

                    (targetController as? SettingsBackupController)?.createBackup(flags)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        }
    }

    class RestoreBackupDialog(bundle: Bundle? = null) : DialogController(bundle) {
        constructor(uri: Uri) : this(
            bundleOf(KEY_URI to uri),
        )

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val activity = activity!!
            val uri: Uri = args.getParcelable(KEY_URI)!!

            return try {
                var type = BackupConst.BACKUP_TYPE_FULL
                val results = try {
                    FullBackupRestoreValidator().validate(activity, uri)
                } catch (_: ValidatorParseException) {
                    type = BackupConst.BACKUP_TYPE_LEGACY
                    LegacyBackupRestoreValidator().validate(activity, uri)
                }

                var message = if (type == BackupConst.BACKUP_TYPE_FULL) {
                    activity.getString(R.string.backup_restore_content_full)
                } else {
                    activity.getString(R.string.backup_restore_content)
                }
                if (results.missingSources.isNotEmpty()) {
                    message += "\n\n${activity.getString(R.string.backup_restore_missing_sources)}\n${results.missingSources.joinToString("\n") { "- $it" }}"
                }
                if (results.missingTrackers.isNotEmpty()) {
                    message += "\n\n${activity.getString(R.string.backup_restore_missing_trackers)}\n${results.missingTrackers.joinToString("\n") { "- $it" }}"
                }

                MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.pref_restore_backup)
                    .setMessage(message)
                    .setPositiveButton(R.string.action_restore) { _, _ ->
                        BackupRestoreService.start(activity, uri, type)
                    }
                    .create()
            } catch (e: Exception) {
                MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.invalid_backup_file)
                    .setMessage(e.message)
                    .setPositiveButton(android.R.string.cancel, null)
                    .create()
            }
        }
    }
}

private const val KEY_URI = "RestoreBackupDialog.uri"

private const val CODE_BACKUP_DIR = 503
private const val CODE_BACKUP_CREATE = 504
private const val CODE_BACKUP_RESTORE = 505

private const val HELP_URL = "https://tachiyomi.org/help/guides/backups/"
