package tachiyomi.data.backupManga

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.backupManga.model.BackupManga
import tachiyomi.domain.backupManga.repository.BackupMangaRepository

class BackupMangaRepositoryImpl(
    private val handler: DatabaseHandler,
) : BackupMangaRepository {

    override suspend fun getAll(): List<BackupManga> {
        return handler.awaitList {
            manga_backupQueries.getAllBackups(
                backupMangaMapper,
            )
        }
    }

    override fun getAllAsFlow(): Flow<List<BackupManga>> {
        return handler.subscribeToList {
            manga_backupQueries.getAllBackups(
                backupMangaMapper,
            )
        }
    }

    override suspend fun getAllByMangaId(mangaId: Long): List<BackupManga> {
        return handler.awaitList {
            manga_backupQueries.getBackupsByMangaId(
                mangaId,
                backupMangaMapper,
            )
        }
    }

    override fun getAllByMangaIdAsFlow(mangaId: Long): Flow<List<BackupManga>> {
        return handler.subscribeToList {
            manga_backupQueries.getBackupsByMangaId(
                mangaId,
                backupMangaMapper,
            )
        }
    }

    override suspend fun migrateBackups(mangaId: Long, prevMangaId: Long) {
        val backups = handler.awaitList {
            manga_backupQueries.getBackupsByMangaId(
                prevMangaId,
                backupMangaMapper,
            )
        }
        handler.await(inTransaction = true) {
            backups.forEach {
                manga_backupQueries.update(mangaId, it.id)
            }
        }
    }

    override suspend fun deleteAll() {
        return handler.await { manga_backupQueries.deleteAll() }
    }

    override suspend fun delete(backupId: Long) {
        return handler.await {
            manga_backupQueries.delete(
                id = backupId,
            )
        }
    }

    override suspend fun deleteByMangaId(mangaId: Long) {
        return handler.await {
            manga_backupQueries.deleteByMangaId(
                mangaId = mangaId,
            )
        }
    }

    override suspend fun insert(backupManga: BackupManga) {
        return handler.await(inTransaction = true) {
            manga_backupQueries.insert(
                mangaId = backupManga.mangaId,
                thumbnailUrl = backupManga.thumbnailUrl,
                backupTime = backupManga.backupTime,
            )
        }
    }
}
