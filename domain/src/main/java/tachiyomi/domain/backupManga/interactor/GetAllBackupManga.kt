package tachiyomi.domain.backupManga.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.backupManga.model.BackupManga
import tachiyomi.domain.backupManga.model.BackupWithMangaDetails
import tachiyomi.domain.backupManga.repository.BackupMangaRepository
import tachiyomi.domain.manga.interactor.GetManga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class GetAllBackupManga(
    private val backupMangaRepository: BackupMangaRepository,
    private val backupPreferences: BackupPreferences = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
) {
    suspend fun await(): List<BackupManga> {
        return backupMangaRepository.getAll()
    }

    fun subscribe(): Flow<List<BackupWithMangaDetails>> {
        return combine(
            backupPreferences.clearCoverBackupFilterCriteria().changes(),
            backupPreferences.clearCoverBackupSortCriteria().changes(),
            backupPreferences.clearCoverBackupSortDirection().changes(),
            backupMangaRepository.getAllAsFlow(),
        ) { filter, sortCriteria, direction, list ->
            val items = list
                .groupBy { it.mangaId }
                .map {
                    val manga = getManga.await(it.key)
                    BackupWithMangaDetails(
                        mangaId = it.key,
                        mangaTitle = manga?.title ?: "",
                        thumbnailUrl = manga?.thumbnailUrl,
                        backups = it.value,
                    )
                }
                .filter { it.backups.size >= filter }
            if (sortCriteria == "ALPHABETICAL") {
                if (direction == "ASCENDING") {
                    items.sortedBy { it.mangaTitle }
                } else {
                    items.sortedByDescending { it.mangaTitle }
                }
            } else {
                if (direction == "ASCENDING") {
                    items.sortedBy { it.backups.size }
                } else {
                    items.sortedByDescending { it.backups.size }
                }
            }
        }
    }
}
