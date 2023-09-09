package tachiyomi.domain.backupManga.model

data class BackupWithMangaDetails(
    val mangaId: Long,
    val mangaTitle: String,
    val thumbnailUrl: String?,
    val backups: List<BackupManga>,
)
