package eu.kanade.tachiyomi.data.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.pushtorefresh.storio.sqlite.impl.DefaultStorIOSQLite
import eu.kanade.tachiyomi.data.database.mappers.*
import eu.kanade.tachiyomi.data.database.models.*
import eu.kanade.tachiyomi.data.database.queries.*
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory

/**
 * This class provides operations to manage the database through its interfaces.
 */
open class DatabaseHelper(context: Context) :
    MangaQueries, ChapterQueries, TrackQueries, CategoryQueries, MangaCategoryQueries, HistoryQueries/* TX-->*/, LibraryUpdateErrorQueries/* TX<--*/ {

    private val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(DbOpenCallback.DATABASE_NAME)
        .callback(DbOpenCallback())
        .build()

    override val db = DefaultStorIOSQLite.builder()
        .sqliteOpenHelper(RequerySQLiteOpenHelperFactory().create(configuration))
        .addTypeMapping(Manga::class.java, MangaTypeMapping())
        .addTypeMapping(Chapter::class.java, ChapterTypeMapping())
        .addTypeMapping(Track::class.java, TrackTypeMapping())
        .addTypeMapping(Category::class.java, CategoryTypeMapping())
        .addTypeMapping(MangaCategory::class.java, MangaCategoryTypeMapping())
        .addTypeMapping(History::class.java, HistoryTypeMapping())
        // TX-->
        .addTypeMapping(LibraryUpdateError::class.java, LibraryUpdateErrorTypeMapping())
        // TX<--
        .build()

    inline fun inTransaction(block: () -> Unit) = db.inTransaction(block)
}
