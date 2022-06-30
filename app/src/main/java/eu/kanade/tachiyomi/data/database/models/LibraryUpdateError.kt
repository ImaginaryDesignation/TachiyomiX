package eu.kanade.tachiyomi.data.database.models

import java.io.Serializable

interface LibraryUpdateError : Serializable {

    var id: Long?

    var mangaID: Long?

    var message: String

    companion object {

        fun create(mangaID: Long, message: String): LibraryUpdateError = LibraryUpdateErrorImpl().apply {
            this.mangaID = mangaID
            this.message = message
        }
    }
}
