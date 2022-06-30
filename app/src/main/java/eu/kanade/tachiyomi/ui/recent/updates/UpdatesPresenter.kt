package eu.kanade.tachiyomi.ui.recent.updates

import android.os.Bundle
import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.MangaChapter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.recent.DateSectionItem
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.toDateKey
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.injectLazy
import java.text.DateFormat
import java.util.*

class UpdatesPresenter : BasePresenter<UpdatesController>() {

    val preferences: PreferencesHelper by injectLazy()
    private val db: DatabaseHelper by injectLazy()
    private val downloadManager: DownloadManager by injectLazy()
    private val sourceManager: SourceManager by injectLazy()

    private val relativeTime: Int = preferences.relativeTime().get()
    private val dateFormat: DateFormat = preferences.dateFormat()

    /**
     * List containing chapter and manga information
     */
    private var chapters: List<UpdatesItem> = emptyList()

    /**
     * Subject of list of chapters to allow updating the view without going to DB.
     */
    // TX-->
    private val chaptersRelay: PublishRelay<List<UpdatesItem>> by lazy {
        PublishRelay.create<List<UpdatesItem>>()
    }
    // TX<--

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // TX-->
        /*getUpdatesObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(UpdatesController::onNextRecentChapters)*/
        // TX<--

        chaptersRelay.observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(
                { _, chapters ->
                    view?.onNextRecentChapters(chapters)
                },
                { _, error -> logcat(LogPriority.ERROR, error) },
            )

        downloadManager.queue.getStatusObservable()
            .observeOn(Schedulers.io())
            .onBackpressureBuffer()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(
                { view, it ->
                    onDownloadStatusChange(it)
                    view.onChapterDownloadUpdate(it)
                },
                { _, error ->
                    logcat(LogPriority.ERROR, error)
                },
            )

        downloadManager.queue.getProgressObservable()
            .observeOn(Schedulers.io())
            .onBackpressureBuffer()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(UpdatesController::onChapterDownloadUpdate) { _, error ->
                logcat(LogPriority.ERROR, error)
            }

        // TX-->
        val cal = Calendar.getInstance().apply {
            time = Date()
            add(Calendar.MONTH, -3)
        }

        add(
            db.getRecentChapters(cal.time).asRxObservable()
                .map { chapters ->
                    val map = TreeMap<Date, MutableList<MangaChapter>> { d1, d2 -> d2.compareTo(d1) }
                    val byDay = chapters
                        .groupByTo(map) { it.chapter.date_fetch.toDateKey() }
                    byDay.flatMap { entry ->
                        // TX-->
                        val dateItem = DateSectionItem(entry.key, relativeTime, dateFormat, entry.value.size.toString())
                        // TX<--
                        entry.value
                            .sortedWith(compareBy({ it.chapter.date_fetch }, { it.chapter.chapter_number })).asReversed()
                            .map { UpdatesItem(it.chapter, it.manga, dateItem) }
                    }
                }
                .doOnNext { list ->
                    list.forEach { item ->
                        // Find an active download for this chapter.
                        val download = downloadManager.queue.find { it.chapter.id == item.chapter.id }

                        // If there's an active download, assign it, otherwise ask the manager if
                        // the chapter is downloaded and assign it to the status.
                        if (download != null) {
                            item.download = download
                        }
                    }
                    setDownloadedChapters(list)
                    chapters = list

                    // Set unread chapter count for bottom bar badge
                    preferences.unreadUpdatesCount().set(list.count { !it.read })
                }
                .subscribe { chaptersRelay.call(it) },
        )
        // TX<--
    }

    /**
     * Get observable containing recent chapters and date
     *
     * @return observable containing recent chapters and date
     */
    // TX<--
    /*private fun getUpdatesObservable(): Observable<List<UpdatesItem>> {
        // Set date limit for recent chapters
        val cal = Calendar.getInstance().apply {
            time = Date()
            add(Calendar.MONTH, -3)
        }

        return db.getRecentChapters(cal.time).asRxObservable()
            // Convert to a list of recent chapters.
            .map { mangaChapters ->
                val map = TreeMap<Date, MutableList<MangaChapter>> { d1, d2 -> d2.compareTo(d1) }
                val byDay = mangaChapters
                    .groupByTo(map) { it.chapter.date_fetch.toDateKey() }
                byDay.flatMap { entry ->
                    // TX-->
                    val dateItem = DateSectionItem(entry.key, relativeTime, dateFormat, entry.value.size.toString())
                    // TX<--
                    entry.value
                        .sortedWith(compareBy({ it.chapter.date_fetch }, { it.chapter.chapter_number })).asReversed()
                        .map { UpdatesItem(it.chapter, it.manga, dateItem) }
                }
            }
            .doOnNext { list ->
                list.forEach { item ->
                    // Find an active download for this chapter.
                    val download = downloadManager.queue.find { it.chapter.id == item.chapter.id }

                    // If there's an active download, assign it, otherwise ask the manager if
                    // the chapter is downloaded and assign it to the status.
                    if (download != null) {
                        item.download = download
                    }
                }
                setDownloadedChapters(list)
                chapters = list

                // Set unread chapter count for bottom bar badge
                preferences.unreadUpdatesCount().set(list.count { !it.read })
            }
    }*/
    // TX<--

    /**
     * Finds and assigns the list of downloaded chapters.
     *
     * @param items the list of chapter from the database.
     */
    private fun setDownloadedChapters(items: List<UpdatesItem>) {
        for (item in items) {
            val manga = item.manga
            val chapter = item.chapter

            if (downloadManager.isChapterDownloaded(chapter, manga)) {
                item.status = Download.State.DOWNLOADED
            }
        }
    }

    /**
     * Update status of chapters.
     *
     * @param download download object containing progress.
     */
    private fun onDownloadStatusChange(download: Download) {
        // Assign the download to the model object.
        if (download.status == Download.State.QUEUE) {
            val chapter = chapters.find { it.chapter.id == download.chapter.id }
            if (chapter != null && chapter.download == null) {
                chapter.download = download
            }
        }
    }

    fun startDownloadingNow(chapter: Chapter) {
        downloadManager.startDownloadNow(chapter)
    }

    /**
     * Mark selected chapter as read
     *
     * @param items list of selected chapters
     * @param read read status
     */
    fun markChapterRead(items: List<UpdatesItem>, read: Boolean) {
        val chapters = items.map { it.chapter }
        chapters.forEach {
            it.read = read
            if (!read) {
                it.last_page_read = 0
            }
        }

        Observable.fromCallable { db.updateChaptersProgress(chapters).executeAsBlocking() }
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    /**
     * Delete selected chapters
     *
     * @param chapters list of chapters
     */
    fun deleteChapters(chapters: List<UpdatesItem>) {
        Observable.just(chapters)
            .doOnNext { deleteChaptersInternal(it) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { view, _ ->
                    view.onChaptersDeleted()
                },
                UpdatesController::onChaptersDeletedError,
            )
    }

    /**
     * Mark selected chapters as bookmarked
     * @param items list of selected chapters
     * @param bookmarked bookmark status
     */
    fun bookmarkChapters(items: List<UpdatesItem>, bookmarked: Boolean) {
        val chapters = items.map { it.chapter }
        chapters.forEach {
            it.bookmark = bookmarked
        }

        Observable.fromCallable { db.updateChaptersProgress(chapters).executeAsBlocking() }
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    /**
     * Download selected chapters
     * @param items list of recent chapters seleted.
     */
    fun downloadChapters(items: List<UpdatesItem>) {
        items.forEach { downloadManager.downloadChapters(it.manga, listOf(it.chapter)) }
    }

    /**
     * Delete selected chapters
     *
     * @param items chapters selected
     */
    private fun deleteChaptersInternal(chapterItems: List<UpdatesItem>) {
        val itemsByManga = chapterItems.groupBy { it.manga.id }
        for ((_, items) in itemsByManga) {
            val manga = items.first().manga
            val source = sourceManager.get(manga.source) ?: continue
            val chapters = items.map { it.chapter }

            downloadManager.deleteChapters(chapters, manga, source)
            items.forEach {
                it.status = Download.State.NOT_DOWNLOADED
                it.download = null
            }
        }
    }

    // TX-->
    /**
     * Mark selected chapters as bookmarked
     * @param item selected chapter
     * @param bookmarked bookmark status
     */
    fun toggleChapterBookmark(item: UpdatesItem, bookmarked: Boolean) {
        val chapter = item.chapter
        chapter.bookmark = bookmarked

        launchIO {
            db.updateChapterProgress(chapter).executeAsBlocking()
        }
    }

    /**
     * Mark selected chapter as read
     *
     * @param item selected chapter
     * @param read read status
     */
    fun toggleChapterRead(item: UpdatesItem, read: Boolean) {
        val chapter = item.chapter
        chapter.read = read
        if (!read) {
            chapter.last_page_read = 0
        }

        launchIO {
            db.updateChapterProgress(chapter).executeAsBlocking()

            if (read && preferences.removeAfterMarkedAsRead()) {
                deleteChapter(item)
            }
        }
    }

    /**
     * Delete selected chapters
     *
     * @param item contains chapters to delete
     */
    private fun deleteChapter(item: UpdatesItem) {
        launchIO {
            try {
                val manga = item.manga
                val source = sourceManager.get(manga.source)

                if (source != null) {
                    downloadManager.deleteChapters(listOf(item.chapter), manga, source)
                    item.status = Download.State.NOT_DOWNLOADED
                    item.download = null
                    item.read = true
                }

                view?.onChapterDeleted(item)
            } catch (e: Throwable) {
                view?.onChaptersDeletedError(e)
            }
        }
    }
    // TX<--
}
