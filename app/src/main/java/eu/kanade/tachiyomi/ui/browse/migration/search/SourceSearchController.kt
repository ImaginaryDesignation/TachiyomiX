package eu.kanade.tachiyomi.ui.browse.migration.search

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.browse.migration.advanced.process.MigrationListController
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.ui.browse.source.browse.SourceItem
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SourceSearchController(
    bundle: Bundle,
) : BrowseSourceController(bundle) {

    // TX-->
    constructor(targetController: MigrationListController, manga: Manga, source: CatalogueSource, searchQuery: String? = null) : this(
        bundleOf(
            SOURCE_ID_KEY to source.id,
            MANGA_KEY to manga,
            SEARCH_QUERY_KEY to searchQuery,
        ),
    ) {
        this.targetController = targetController
    }

    override fun onItemClick(view: View, position: Int): Boolean {
        val manga = (adapter?.getItem(position) as? SourceItem)?.manga ?: return false
        val migrationListController = targetController as? MigrationListController ?: return false
        val sourceManager = Injekt.get<SourceManager>()
        val source = sourceManager.get(manga.source) ?: return false
        migrationListController.useMangaForMigration(manga, source)
        router.popCurrentController()
        router.popCurrentController()
        return true
    }
    // TX<--

    /*constructor(manga: Manga? = null, source: CatalogueSource, searchQuery: String? = null) : this(
        Bundle().apply {
            putLong(SOURCE_ID_KEY, source.id)
            putSerializable(MANGA_KEY, manga)
            if (searchQuery != null) {
                putString(SEARCH_QUERY_KEY, searchQuery)
            }
        },
    )
    private var oldManga: Manga? = args.getSerializable(MANGA_KEY) as Manga?
    private var newManga: Manga? = null

    override fun onItemClick(view: View, position: Int): Boolean {
        val item = adapter?.getItem(position) as? SourceItem ?: return false
        newManga = item.manga
        val searchController = router.backstack.findLast { it.controller.javaClass == SearchController::class.java }?.controller as SearchController?
        val dialog =
            SearchController.MigrationDialog(oldManga, newManga, this)
        dialog.targetController = searchController
        dialog.showDialog(router)
        return true
    }*/

    override fun onItemLongClick(position: Int) {
        view?.let { super.onItemClick(it, position) }
    }
}

private const val MANGA_KEY = "oldManga"
