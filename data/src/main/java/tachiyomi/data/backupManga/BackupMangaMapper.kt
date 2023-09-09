package tachiyomi.data.backupManga

import tachiyomi.domain.backupManga.model.BackupManga

val backupMangaMapper: (Long, Long, String?, Long) -> BackupManga =
    { id, mangaId, thumbnailUrl, backupTime ->
        BackupManga(
            id = id,
            mangaId = mangaId,
            thumbnailUrl = thumbnailUrl,
            backupTime = backupTime,
        )
    }
