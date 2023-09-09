package tachiyomi.domain.backupManga.interactor

import tachiyomi.domain.backupManga.repository.BackupMangaRepository

class MigrateBackups(
    private val backupMangaRepository: BackupMangaRepository,
) {

    suspend fun await(mangaId: Long, prevMangaId: Long) {
        return backupMangaRepository.migrateBackups(mangaId, prevMangaId)
    }
}
