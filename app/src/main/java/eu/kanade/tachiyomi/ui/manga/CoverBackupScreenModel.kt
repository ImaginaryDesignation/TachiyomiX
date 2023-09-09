package eu.kanade.tachiyomi.ui.manga

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.backupManga.interactor.DeleteBackupManga
import tachiyomi.domain.backupManga.interactor.GetAllBackupMangaByMangaId
import tachiyomi.domain.backupManga.interactor.InsertBackupManga
import tachiyomi.domain.backupManga.model.BackupManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.UpdateMangaThumbnail
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CoverBackupScreenModel(
    private val mangaId: Long,
    private val getManga: GetManga = Injekt.get(),
    private val updateMangaThumbnail: UpdateMangaThumbnail = Injekt.get(),
    private val getAllBackupMangaByMangaId: GetAllBackupMangaByMangaId = Injekt.get(),
    private val deleteBackupManga: DeleteBackupManga = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get(),
) : StateScreenModel<CoverBackupScreenState>(CoverBackupScreenState()) {

    private var pageNumber = 0

    private val _events: Channel<BackupMangaEvent> = Channel()
    val events = _events.receiveAsFlow()

    init {
        coroutineScope.launch {
            getAllBackupMangaByMangaId.getAllByMangaIdAsFlow(mangaId)
                .collectLatest { backupManga ->
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            items = backupManga,
                        )
                    }
                }
        }
    }

    fun deleteBackup() {
        coroutineScope.launch {
            val result = deleteBackupManga.await(mutableState.value.items[pageNumber].id)
            if (result is DeleteBackupManga.Result.InternalError) {
                _events.send(BackupMangaEvent.InternalError)
            } else {
                coverCache.deleteFromCache(
                    mutableState.value.items[pageNumber].thumbnailUrl,
                )
            }
        }
    }

    suspend fun restoreBackup(backupCurrent: Boolean = false) {
        coroutineScope.launch {
            if (backupCurrent) {
                val localManga = getManga.await(mangaId)
                if (localManga != null) {
                    val result = UpdateManga.takeBackup(
                        manga = localManga,
                        coverCache = coverCache,
                        swap = true,
                    )
                    if (result is InsertBackupManga.Result.InternalError) {
                        _events.send(BackupMangaEvent.InternalError)
                        return@launch
                    }
                } else {
                    _events.send(BackupMangaEvent.InternalError)
                }
            }
            val updateMangaThumbnailResult = updateMangaThumbnail.await(
                thumbnailUrl = mutableState.value.items[pageNumber].thumbnailUrl,
                mangaId = mangaId,
            )
            if (updateMangaThumbnailResult) {
                val deleteResult =
                    deleteBackupManga.await(mutableState.value.items[pageNumber].id)
                if (deleteResult is DeleteBackupManga.Result.InternalError) {
                    _events.send(BackupMangaEvent.InternalError)
                }
            } else {
                _events.send(BackupMangaEvent.InternalError)
            }
        }
    }

    fun pageChanged(page: Int) {
        pageNumber = page
    }

    fun showDeleteConfirmation() = mutableState.update { state ->
        if (state.isLoading) return@update state
        state.copy(showDeleteConfirmation = true)
    }

    fun hideDeleteConfirmation() = mutableState.update { state ->
        if (state.isLoading) return@update state
        state.copy(showDeleteConfirmation = false)
    }

    fun showBackupCurrentDialog() = mutableState.update { state ->
        if (state.isLoading) return@update state
        state.copy(showBackupCurrentDialog = true)
    }

    fun hideBackupCurrentDialog() = mutableState.update { state ->
        if (state.isLoading) return@update state
        state.copy(showBackupCurrentDialog = false)
    }
}

sealed class BackupMangaEvent {
    sealed class LocalizedMessage(@StringRes val stringRes: Int) : BackupMangaEvent()
    object InternalError : LocalizedMessage(R.string.internal_error)
}

@Immutable
data class CoverBackupScreenState(
    val isLoading: Boolean = true,
    val items: List<BackupManga> = emptyList(),
    val showDeleteConfirmation: Boolean = false,
    val showBackupCurrentDialog: Boolean = false,
)
