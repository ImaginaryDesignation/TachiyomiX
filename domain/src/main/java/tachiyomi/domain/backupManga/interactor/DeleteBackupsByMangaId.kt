package tachiyomi.domain.backupManga.interactor

import logcat.LogPriority
import tachiyomi.core.util.lang.withNonCancellableContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.backupManga.repository.BackupMangaRepository

class DeleteBackupsByMangaId(
    private val backupMangaRepository: BackupMangaRepository,
) {

    suspend fun await(mangaIds: List<Long>) = withNonCancellableContext {
        try {
            mangaIds.forEach { mangaId ->
                backupMangaRepository.deleteByMangaId(mangaId)
            }
            Result.Success
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            return@withNonCancellableContext Result.InternalError(e)
        }
    }

    sealed class Result {
        object Success : Result()
        data class InternalError(val error: Throwable) : Result()
    }
}
