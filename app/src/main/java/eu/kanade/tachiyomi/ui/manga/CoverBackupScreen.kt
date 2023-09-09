package eu.kanade.tachiyomi.ui.manga

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.manga.CoverBackupScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import tachiyomi.core.util.lang.launchUI

class CoverBackupScreen(
    private val mangaId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val screenModel = rememberScreenModel { CoverBackupScreenModel(mangaId) }
        val state by screenModel.state.collectAsState()
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            screenModel.events.collectLatest { event ->
                if (event is BackupMangaEvent.LocalizedMessage) {
                    context.toast(event.stringRes)
                }
            }
        }

        CoverBackupScreen(
            state = state,
            navigateUp = navigator::pop,
            onClickRestore = { backupCurrent -> scope.launchUI { screenModel.restoreBackup(backupCurrent) } },
            onClickDelete = screenModel::deleteBackup,
            onPageChange = { page -> screenModel.pageChanged(page) },
            showBackupCurrentDialog = screenModel::showBackupCurrentDialog,
            hideBackupCurrentDialog = screenModel::hideBackupCurrentDialog,
            showDeleteConfirmation = screenModel::showDeleteConfirmation,
            hideDeleteConfirmation = screenModel::hideDeleteConfirmation,
        )
    }
}
