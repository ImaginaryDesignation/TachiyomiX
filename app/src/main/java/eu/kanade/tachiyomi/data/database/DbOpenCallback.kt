package eu.kanade.tachiyomi.data.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import eu.kanade.tachiyomi.data.database.tables.*

class DbOpenCallback : SupportSQLiteOpenHelper.Callback(DATABASE_VERSION) {

    companion object {
        /**
         * Name of the database file.
         */
        const val DATABASE_NAME = "tachiyomi.db"

        /**
         * Version of the database.
         */
        // TX-->
        const val DATABASE_VERSION = 15
        // TX<--
    }

    override fun onCreate(db: SupportSQLiteDatabase) = with(db) {
        execSQL(MangaTable.createTableQuery)
        execSQL(ChapterTable.createTableQuery)
        execSQL(TrackTable.createTableQuery)
        execSQL(CategoryTable.createTableQuery)
        // TX-->
        execSQL(CategoryTable.createAllCategory)
        execSQL(LibraryUpdateErrorTable.createTableQuery)
        // TX<--
        execSQL(MangaCategoryTable.createTableQuery)
        execSQL(HistoryTable.createTableQuery)

        // DB indexes
        execSQL(MangaTable.createUrlIndexQuery)
        execSQL(MangaTable.createLibraryIndexQuery)
        execSQL(ChapterTable.createMangaIdIndexQuery)
        execSQL(ChapterTable.createUnreadChaptersIndexQuery)
        execSQL(HistoryTable.createChapterIdIndexQuery)
        // TX-->
        execSQL(LibraryUpdateErrorTable.createErrorIdIndexQuery)
        // TX<--
    }

    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(ChapterTable.sourceOrderUpdateQuery)

            // Fix kissmanga covers after supporting cloudflare
            db.execSQL(
                """UPDATE mangas SET thumbnail_url =
                    REPLACE(thumbnail_url, '93.174.95.110', 'kissmanga.com') WHERE source = 4""",
            )
        }
        if (oldVersion < 3) {
            // Initialize history tables
            db.execSQL(HistoryTable.createTableQuery)
            db.execSQL(HistoryTable.createChapterIdIndexQuery)
        }
        if (oldVersion < 4) {
            db.execSQL(ChapterTable.bookmarkUpdateQuery)
        }
        if (oldVersion < 5) {
            db.execSQL(ChapterTable.addScanlator)
        }
        if (oldVersion < 6) {
            db.execSQL(TrackTable.addTrackingUrl)
        }
        if (oldVersion < 7) {
            db.execSQL(TrackTable.addLibraryId)
        }
        if (oldVersion < 8) {
            db.execSQL("DROP INDEX IF EXISTS mangas_favorite_index")
            db.execSQL(MangaTable.createLibraryIndexQuery)
            db.execSQL(ChapterTable.createUnreadChaptersIndexQuery)
        }
        if (oldVersion < 9) {
            db.execSQL(TrackTable.addStartDate)
            db.execSQL(TrackTable.addFinishDate)
        }
        if (oldVersion < 10) {
            db.execSQL(MangaTable.addCoverLastModified)
        }
        if (oldVersion < 11) {
            db.execSQL(MangaTable.addDateAdded)
            db.execSQL(MangaTable.backfillDateAdded)
        }
        if (oldVersion < 12) {
            db.execSQL(MangaTable.addNextUpdateCol)
        }
        if (oldVersion < 13) {
            db.execSQL(TrackTable.renameTableToTemp)
            db.execSQL(TrackTable.createTableQuery)
            db.execSQL(TrackTable.insertFromTempTable)
            db.execSQL(TrackTable.dropTempTable)
        }
        if (oldVersion < 14) {
            db.execSQL(ChapterTable.fixDateUploadIfNeeded)
        }
        // TX-->
        if (oldVersion < 15) {
            db.execSQL(LibraryUpdateErrorTable.createTableQuery)
            db.execSQL(LibraryUpdateErrorTable.createErrorIdIndexQuery)
        }
        // TX<--
    }

    override fun onConfigure(db: SupportSQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }
}
