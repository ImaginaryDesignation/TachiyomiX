package tachiyomi.domain.backupManga.interactor

import logcat.LogPriority
import tachiyomi.core.util.lang.withNonCancellableContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.backupManga.repository.BackupMangaRepository

class DeleteBackupManga(
    private val backupMangaRepository: BackupMangaRepository,
) {
    suspend fun await() = withNonCancellableContext {
        try {
            backupMangaRepository.deleteAll()
            Result.Success
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            return@withNonCancellableContext Result.InternalError(e)
        }
    }

    suspend fun await(backupId: Long) = withNonCancellableContext {
        try {
            backupMangaRepository.delete(backupId)
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
