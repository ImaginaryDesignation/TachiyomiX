package eu.kanade.tachiyomi.data.database.tables

object LibraryUpdateErrorTable {

    /**
     * Table name
     */
    const val TABLE = "library_update_errors"

    /**
     * Id column name
     */
    const val COL_ID = "_id"

    /**
     * Manga id column name
     */
    const val COL_MANGA_ID = "manga_id"

    /**
     * Error message column name
     */
    const val COL_ERROR_MESSAGE = "message"

    /**
     * query to create library_update_error table
     */
    val createTableQuery: String
        get() =
            """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_MANGA_ID INTEGER NOT NULL,
            $COL_ERROR_MESSAGE TEXT NOT NULL
            )"""

    /**
     * query to index library_update_error id
     */
    val createErrorIdIndexQuery: String
        get() = "CREATE INDEX ${TABLE}_${COL_ID}_index ON $TABLE($COL_ID)"
}
