package eu.kanade.tachiyomi.data.database.mappers

import android.database.Cursor
import androidx.core.content.contentValuesOf
import com.pushtorefresh.storio.sqlite.SQLiteTypeMapping
import com.pushtorefresh.storio.sqlite.operations.delete.DefaultDeleteResolver
import com.pushtorefresh.storio.sqlite.operations.get.DefaultGetResolver
import com.pushtorefresh.storio.sqlite.operations.put.DefaultPutResolver
import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.InsertQuery
import com.pushtorefresh.storio.sqlite.queries.UpdateQuery
import eu.kanade.tachiyomi.data.database.models.LibraryUpdateError
import eu.kanade.tachiyomi.data.database.models.LibraryUpdateErrorImpl
import eu.kanade.tachiyomi.data.database.tables.LibraryUpdateErrorTable

class LibraryUpdateErrorTypeMapping : SQLiteTypeMapping<LibraryUpdateError>(
    LibraryUpdateErrorPutResolver(),
    LibraryUpdateErrorGetResolver(),
    LibraryUpdateErrorDeleteResolver(),
)

class LibraryUpdateErrorPutResolver : DefaultPutResolver<LibraryUpdateError>() {

    override fun mapToInsertQuery(obj: LibraryUpdateError) = InsertQuery.builder()
        .table(LibraryUpdateErrorTable.TABLE)
        .build()

    override fun mapToUpdateQuery(obj: LibraryUpdateError) = UpdateQuery.builder()
        .table(LibraryUpdateErrorTable.TABLE)
        .where("${LibraryUpdateErrorTable.COL_ID} = ?")
        .whereArgs(obj.id)
        .build()

    override fun mapToContentValues(obj: LibraryUpdateError) =
        contentValuesOf(
            LibraryUpdateErrorTable.COL_ID to obj.id,
            LibraryUpdateErrorTable.COL_MANGA_ID to obj.mangaID,
            LibraryUpdateErrorTable.COL_ERROR_MESSAGE to obj.message,
        )
}

class LibraryUpdateErrorGetResolver : DefaultGetResolver<LibraryUpdateError>() {

    override fun mapFromCursor(cursor: Cursor): LibraryUpdateError = LibraryUpdateErrorImpl().apply {
        id = cursor.getLong(cursor.getColumnIndexOrThrow(LibraryUpdateErrorTable.COL_ID))
        mangaID = cursor.getLong(cursor.getColumnIndexOrThrow(LibraryUpdateErrorTable.COL_MANGA_ID))
        message = cursor.getString(cursor.getColumnIndexOrThrow(LibraryUpdateErrorTable.COL_ERROR_MESSAGE))
    }
}

class LibraryUpdateErrorDeleteResolver : DefaultDeleteResolver<LibraryUpdateError>() {

    override fun mapToDeleteQuery(obj: LibraryUpdateError) = DeleteQuery.builder()
        .table(LibraryUpdateErrorTable.TABLE)
        .where("${LibraryUpdateErrorTable.COL_ID} = ?")
        .whereArgs(obj.id)
        .build()
}
