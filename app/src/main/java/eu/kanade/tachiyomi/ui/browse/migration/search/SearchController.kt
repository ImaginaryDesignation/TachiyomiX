package eu.kanade.tachiyomi.ui.browse.migration.search

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.migration.advanced.process.MigrationListController
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchController
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchPresenter
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.appcompat.QueryTextEvent
import reactivecircus.flowbinding.appcompat.queryTextEvents
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SearchController(
    private var manga: Manga? = null,
    // TX-->
    private var sources: List<CatalogueSource>? = null,
    // TX<--
) : GlobalSearchController(
    manga?.title,
    // TX-->
    bundle = bundleOf(
        OLD_MANGA to manga?.id,
        SOURCES to sources?.map { it.id }?.toLongArray(),
    ),
) {

    constructor(targetController: MigrationListController?, mangaId: Long, sources: LongArray) :
        this(
            Injekt.get<DatabaseHelper>().getManga(mangaId).executeAsBlocking(),
            sources.map { Injekt.get<SourceManager>().getOrStub(it) }.filterIsInstance<CatalogueSource>(),
        ) {
            this.targetController = targetController
        }

    @Suppress("unused")
    constructor(bundle: Bundle) : this(
        null,
        bundle.getLong(OLD_MANGA),
        bundle.getLongArray(SOURCES) ?: LongArray(0),
    )

    /**
     * Called when controller is initialized.
     */
    init {
        setHasOptionsMenu(true)
    }
    // TX<--

    // private var newManga: Manga? = null

    override fun createPresenter(): GlobalSearchPresenter {
        return SearchPresenter(
            initialQuery,
            manga!!,
            // TX-->
            sources,
            // TX<--
        )
    }

    /*override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(::manga.name, manga)
        outState.putSerializable(::newManga.name, newManga)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        manga = savedInstanceState.getSerializable(::manga.name) as? Manga
        newManga = savedInstanceState.getSerializable(::newManga.name) as? Manga
    }

    fun migrateManga(manga: Manga? = null, newManga: Manga?) {
        manga ?: return
        newManga ?: return

        (presenter as? SearchPresenter)?.migrateManga(manga, newManga, true)
    }

    fun copyManga(manga: Manga? = null, newManga: Manga?) {
        manga ?: return
        newManga ?: return

        (presenter as? SearchPresenter)?.migrateManga(manga, newManga, false)
    }*/

    // TX-->
    override fun onMangaClick(manga: Manga) {
        val migrationListController = targetController as MigrationListController
        val sourceManager = Injekt.get<SourceManager>()
        val source = sourceManager.get(manga.source) ?: return
        migrationListController.useMangaForMigration(manga, source)
        router.popCurrentController()
    }
    // TX<--

    /*override fun onMangaClick(manga: Manga) {
        newManga = manga
        val dialog =
            MigrationDialog(this.manga, newManga, this)
        dialog.targetController = this
        dialog.showDialog(router)
    }*/

    override fun onMangaLongClick(manga: Manga) {
        // Call parent's default click listener
        super.onMangaClick(manga)
    }

    /*fun renderIsReplacingManga(isReplacingManga: Boolean, newManga: Manga?) {
        binding.progress.isVisible = isReplacingManga
        if (!isReplacingManga) {
            router.popController(this)
            if (newManga != null) {
                val newMangaController = RouterTransaction.with(MangaController(newManga))
                if (router.backstack.lastOrNull()?.controller is MangaController) {
                    // Replace old MangaController
                    router.replaceTopController(newMangaController)
                } else {
                    // Push MangaController on top of MigrationController
                    router.pushController(newMangaController)
                }
            }
        }
    }

    class MigrationDialog(private val manga: Manga? = null, private val newManga: Manga? = null, private val callingController: Controller? = null) : DialogController() {

        private val preferences: PreferencesHelper by injectLazy()

        @Suppress("DEPRECATION")
        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val prefValue = preferences.migrateFlags().get()
            val enabledFlagsPositions = MigrationFlags.getEnabledFlagsPositions(prefValue)
            val items = MigrationFlags.titles
                .map { resources?.getString(it) }
                .toTypedArray()
            val selected = items
                .mapIndexed { i, _ -> enabledFlagsPositions.contains(i) }
                .toBooleanArray()

            return MaterialAlertDialogBuilder(activity!!)
                .setTitle(R.string.migration_dialog_what_to_include)
                .setMultiChoiceItems(items, selected) { _, which, checked ->
                    selected[which] = checked
                }
                .setPositiveButton(R.string.migrate) { _, _ ->
                    // Save current settings for the next time
                    val selectedIndices = mutableListOf<Int>()
                    selected.forEachIndexed { i, b -> if (b) selectedIndices.add(i) }
                    val newValue = MigrationFlags.getFlagsFromPositions(selectedIndices.toTypedArray())
                    preferences.migrateFlags().set(newValue)

                    if (callingController != null) {
                        if (callingController.javaClass == SourceSearchController::class.java) {
                            router.popController(callingController)
                        }
                    }
                    (targetController as? SearchController)?.migrateManga(manga, newManga)
                }
                .setNegativeButton(R.string.copy) { _, _ ->
                    if (callingController != null) {
                        if (callingController.javaClass == SourceSearchController::class.java) {
                            router.popController(callingController)
                        }
                    }
                    (targetController as? SearchController)?.copyManga(manga, newManga)
                }
                .setNeutralButton(android.R.string.cancel, null)
                .create()
        }
    }

    override fun onTitleClick(source: CatalogueSource) {
        presenter.preferences.lastUsedSource().set(source.id)

        router.pushController(SourceSearchController(manga, source, presenter.query).withFadeTransaction())
    }*/

    // TX-->
    /**
     * Adds items to the options menu.
     *
     * @param menu menu containing options.
     * @param inflater used to load the menu xml.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate menu.
        inflater.inflate(R.menu.global_search, menu)

        // Initialize search menu
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchItem.fixExpand({
            searchView.onActionViewExpanded() // Required to show the query in the view
            searchView.setQuery(presenter.query, false)
            true
        },)

        searchView.queryTextEvents()
            .filter { it is QueryTextEvent.QuerySubmitted }
            .onEach {
                presenter.search(it.queryText.toString())
                searchItem.collapseActionView()
                setTitle() // Update toolbar title
            }
            .launchIn(viewScope)
    }

    override fun onTitleClick(source: CatalogueSource) {
        presenter.preferences.lastUsedSource().set(source.id)

        router.pushController(SourceSearchController(targetController as? MigrationListController ?: return, manga!!, source, presenter.query).withFadeTransaction())
    }

    companion object {
        const val OLD_MANGA = "old_manga"
        const val SOURCES = "sources"
    }
    // TX<--
}
