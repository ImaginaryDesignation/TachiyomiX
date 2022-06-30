package eu.kanade.tachiyomi.data.database.models

class LibraryUpdateErrorImpl : LibraryUpdateError {

    override var id: Long? = null

    override var mangaID: Long? = null

    override lateinit var message: String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val libraryUpdateError = other as LibraryUpdateError
        return message.equals(libraryUpdateError.message, true) && mangaID == libraryUpdateError.mangaID
    }

    override fun hashCode(): Int {
        return message.hashCode()
    }
}
