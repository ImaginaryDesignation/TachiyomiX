package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.LibraryUpdateError
import eu.kanade.tachiyomi.data.database.tables.LibraryUpdateErrorTable

interface LibraryUpdateErrorQueries : DbProvider {

    fun getLibraryErrors() = db.get()
        .listOfObjects(LibraryUpdateError::class.java)
        .withQuery(
            Query.builder()
                .table(LibraryUpdateErrorTable.TABLE)
                .build(),
        )
        .prepare()

    fun insertLibraryErrors(errors: List<LibraryUpdateError>) = db.put().objects(errors).prepare()

    fun deleteLibraryErrors() = db.delete()
        .byQuery(
            DeleteQuery.builder()
                .table(LibraryUpdateErrorTable.TABLE)
                .build(),
        )
        .prepare()
}
