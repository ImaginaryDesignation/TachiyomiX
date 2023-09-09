package tachiyomi.domain.backupManga.model

import java.io.Serializable

data class BackupManga(
    val id: Long,
    val mangaId: Long,
    val thumbnailUrl: String?,
    val backupTime: Long,
) : Serializable
