package tachiyomi.domain.manga.interactor

import logcat.LogPriority
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.manga.repository.MangaRepository

class UpdateMangaThumbnail(
    private val mangaRepository: MangaRepository,
) {
    suspend fun await(thumbnailUrl: String?, mangaId: Long): Boolean {
        return try {
            mangaRepository.updateThumbnail(thumbnailUrl, mangaId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }
}
