package tachiyomi.domain.backupManga.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.backupManga.model.BackupManga

interface BackupMangaRepository {

    suspend fun getAll(): List<BackupManga>

    fun getAllAsFlow(): Flow<List<BackupManga>>

    suspend fun getAllByMangaId(mangaId: Long): List<BackupManga>

    fun getAllByMangaIdAsFlow(mangaId: Long): Flow<List<BackupManga>>

    suspend fun migrateBackups(mangaId: Long, prevMangaId: Long)

    suspend fun deleteAll()

    suspend fun delete(backupId: Long)

    suspend fun deleteByMangaId(mangaId: Long)

    suspend fun insert(backupManga: BackupManga)
}
