package tachiyomi.domain.backupManga.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.backupManga.model.BackupManga
import tachiyomi.domain.backupManga.repository.BackupMangaRepository

class GetAllBackupMangaByMangaId(
    private val backupMangaRepository: BackupMangaRepository,
) {
    suspend fun getAllByMangaId(mangaId: Long): List<BackupManga> {
        return backupMangaRepository.getAllByMangaId(mangaId)
    }

    fun getAllByMangaIdAsFlow(mangaId: Long): Flow<List<BackupManga>> {
        return backupMangaRepository.getAllByMangaIdAsFlow(mangaId)
    }
}
