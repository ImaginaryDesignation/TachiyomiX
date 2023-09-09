package tachiyomi.domain.backupManga.interactor

import logcat.LogPriority
import tachiyomi.core.util.lang.withNonCancellableContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.backupManga.model.BackupManga
import tachiyomi.domain.backupManga.repository.BackupMangaRepository

class InsertBackupManga(
    private val backupMangaRepository: BackupMangaRepository,
) {
    suspend fun insert(backupManga: BackupManga): Result = withNonCancellableContext {
        try {
            backupMangaRepository.insert(backupManga)
            Result.Success
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            Result.InternalError(e)
        }
    }

    sealed class Result {
        object Success : Result()
        data class InternalError(val error: Throwable) : Result()
    }
}
