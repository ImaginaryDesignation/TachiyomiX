package eu.kanade.domain.manga.interactor

import eu.kanade.domain.manga.model.hasCustomCover
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.source.model.SManga
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.backupManga.interactor.DeleteBackupManga
import tachiyomi.domain.backupManga.interactor.GetAllBackupMangaByMangaId
import tachiyomi.domain.backupManga.interactor.InsertBackupManga
import tachiyomi.domain.backupManga.interactor.MigrateBackups
import tachiyomi.domain.backupManga.model.BackupManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

class UpdateManga(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(mangaUpdate: MangaUpdate): Boolean {
        return mangaRepository.update(mangaUpdate)
    }

    suspend fun awaitAll(mangaUpdates: List<MangaUpdate>): Boolean {
        return mangaRepository.updateAll(mangaUpdates)
    }

    suspend fun awaitUpdateFromSource(
        localManga: Manga,
        remoteManga: SManga,
        manualFetch: Boolean,
        coverCache: CoverCache = Injekt.get(),
    ): Boolean {
        if (Manga.backupNeeded(localManga, remoteManga)) {
            takeBackup(localManga, coverCache)
        }

        val remoteTitle = try {
            remoteManga.title
        } catch (_: UninitializedPropertyAccessException) {
            ""
        }

        // if the manga isn't a favorite, set its title from source and update in db
        val title = if (remoteTitle.isEmpty() || localManga.favorite) null else remoteTitle

        val coverLastModified =
            when {
                // Never refresh covers if the url is empty to avoid "losing" existing covers
                remoteManga.thumbnail_url.isNullOrEmpty() -> null
                !manualFetch && localManga.thumbnailUrl == remoteManga.thumbnail_url -> null
                localManga.isLocal() -> Date().time
                localManga.hasCustomCover(coverCache) -> null
                else -> Date().time
            }

        val thumbnailUrl = remoteManga.thumbnail_url?.takeIf { it.isNotEmpty() }

        return mangaRepository.update(
            MangaUpdate(
                id = localManga.id,
                title = title,
                coverLastModified = coverLastModified,
                author = remoteManga.author,
                artist = remoteManga.artist,
                description = remoteManga.description,
                genre = remoteManga.getGenres(),
                thumbnailUrl = thumbnailUrl,
                status = remoteManga.status.toLong(),
                updateStrategy = remoteManga.update_strategy,
                initialized = true,
            ),
        )
    }

    suspend fun awaitUpdateLastUpdate(mangaId: Long): Boolean {
        return mangaRepository.update(MangaUpdate(id = mangaId, lastUpdate = Date().time))
    }

    suspend fun awaitUpdateCoverLastModified(mangaId: Long): Boolean {
        return mangaRepository.update(MangaUpdate(id = mangaId, coverLastModified = Date().time))
    }

    suspend fun awaitUpdateFavorite(mangaId: Long, favorite: Boolean): Boolean {
        val dateAdded = when (favorite) {
            true -> Date().time
            false -> 0
        }
        return mangaRepository.update(
            MangaUpdate(id = mangaId, favorite = favorite, dateAdded = dateAdded),
        )
    }

    companion object {

        suspend fun migrateBackups(
            prevMangaId: Long,
            mangaId: Long,
            migrateBackups: MigrateBackups = Injekt.get(),
        ) {
            return migrateBackups.await(mangaId, prevMangaId)
        }

        suspend fun takeBackup(
            manga: Manga,
            coverCache: CoverCache,
            swap: Boolean = false,
            backupPreferences: BackupPreferences = Injekt.get(),
            getAllBackupMangaByMangaId: GetAllBackupMangaByMangaId = Injekt.get(),
            deleteBackupManga: DeleteBackupManga = Injekt.get(),
            insertBackupManga: InsertBackupManga = Injekt.get(),
        ): InsertBackupManga.Result {
            val backupManga = BackupManga(
                id = -1L,
                mangaId = manga.id,
                thumbnailUrl = manga.thumbnailUrl,
                backupTime = Date().time,
            )
            if (!swap) {
                val backupMangaList = getAllBackupMangaByMangaId.getAllByMangaId(mangaId = manga.id)
                val coverBackupSlots = backupPreferences.coverBackupLimit().get()
                if (backupMangaList.size >= coverBackupSlots) {
                    val sortedList = backupMangaList.sortedBy { it.backupTime }
                    for (i in 0..(backupMangaList.size - coverBackupSlots)) {
                        deleteBackupManga.await(sortedList[i].id)
                        coverCache.deleteFromCache(sortedList[i].thumbnailUrl)
                    }
                }
            }
            return insertBackupManga.insert(backupManga)
        }
    }
}
