package eu.kanade.tachiyomi.ui.recent.errors

import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import dev.chrisbanes.insetter.applyInsetter
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.LibraryUpdateError
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.LibraryUpdateErrorsControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.migration.advanced.design.PreMigrationController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.widget.ActionModeWithToolbar
import uy.kohesive.injekt.injectLazy
import java.util.*

/**
 * Fragment that shows update errors.
 */
class LibraryUpdateErrorsController :
    BaseController<LibraryUpdateErrorsControllerBinding>(null),
    ActionModeWithToolbar.Callback,
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemLongClickListener,
    FlexibleAdapter.OnUpdateListener,
    LibraryUpdateErrorsAdapter.OnCoverClickListener {

    private val db: DatabaseHelper by injectLazy()

    private val preferences: PreferencesHelper by injectLazy()

    private var lastClickPositionStack = ArrayDeque(listOf(-1))

    private val selectedErrors = mutableSetOf<LibraryUpdateErrorsItem>()

    /**
     * Action mode for multiple selection.
     */
    private var actionMode: ActionModeWithToolbar? = null

    /**
     * Adapter containing the error entries.
     */
    var adapter: LibraryUpdateErrorsAdapter? = null
        private set

    override fun getTitle(): String {
        return resources?.getString(R.string.label_update_errors) + " (" + (adapter?.items?.size ?: 0) + ")"
    }

    override fun createBinding(inflater: LayoutInflater) = LibraryUpdateErrorsControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        binding.recycler.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }

        val err = db.getLibraryErrors().executeAsBlocking()
        val map = TreeMap<String, MutableList<LibraryUpdateError>> { d1, d2 -> d2.compareTo(d1) }
        val byError = err.groupByTo(map) { it.message }
        val items = byError.flatMap { entry ->
            val messageItem = LibraryUpdateErrorSectionItem(entry.key, entry.value.size.toString())
            entry.value
                .map {
                    val manga = db.getManga(it.mangaID!!).executeAsBlocking()
                    LibraryUpdateErrorsItem(entry.key, manga!!, messageItem)
                }
        }

        val ourAdapter = adapter ?: LibraryUpdateErrorsAdapter(
            this@LibraryUpdateErrorsController,
            items,
        )

        view.context.notificationManager.cancel(Notifications.ID_LIBRARY_ERROR)

        // Init RecyclerView and adapter
        adapter = ourAdapter
        val layoutManager = LinearLayoutManager(view.context)
        binding.recycler.layoutManager = layoutManager
        binding.recycler.setHasFixedSize(true)
        binding.recycler.adapter = adapter
        adapter!!.fastScroller = binding.fastScroller
        binding.fastScroller.isVisible = true
    }

    override fun onDestroyView(view: View) {
        destroyActionModeIfNeeded()
        adapter = null
        super.onDestroyView(view)
    }

    /**
     * Returns selected mangas
     * @return list of selected mangas
     */
    private fun getSelectedMangas(): List<LibraryUpdateErrorsItem> {
        val adapter = adapter ?: return emptyList()
        return adapter.selectedPositions.mapNotNull { adapter.getItem(it) }
    }

    private fun migrateManga(mangas: List<LibraryUpdateErrorsItem>) {
        val ids = mangas.map { it.manga.id!! }
        PreMigrationController.navigateToMigration(
            preferences.skipPreMigration().get(),
            router,
            ids,
        )
    }

    /**
     * Called when item in list is clicked
     * @param position position of clicked item
     */
    override fun onItemClick(view: View, position: Int): Boolean {
        val adapter = adapter ?: return false

        // Get item from position
        val item = adapter.getItem(position) as? LibraryUpdateErrorsItem ?: return false
        return if (actionMode != null && adapter.mode == SelectableAdapter.Mode.MULTI) {
            if (adapter.isSelected(position)) {
                lastClickPositionStack.remove(position)
            } else {
                lastClickPositionStack.push(position)
            }
            toggleSelection(position)
            true
        } else {
            migrateManga(listOf(item))
            false
        }
    }

    /**
     * Called when item in list is long clicked
     * @param position position of clicked item
     */
    override fun onItemLongClick(position: Int) {
        val adapter = adapter ?: return
        val item = adapter.getItem(position) as? LibraryUpdateErrorsItem ?: return
        val activity = activity
        if (actionMode == null && activity is MainActivity) {
            actionMode = activity.startActionModeAndToolbar(this)
        }
        val lastClickPosition = lastClickPositionStack.peek()!!
        when {
            lastClickPosition == -1 -> setSelection(position)
            lastClickPosition > position -> {
                for (i in position until lastClickPosition) setSelection(i)
                adapter.notifyItemRangeChanged(position, lastClickPosition, position)
            }
            lastClickPosition < position -> {
                for (i in lastClickPosition + 1..position) setSelection(i)
                adapter.notifyItemRangeChanged(lastClickPosition + 1, position, position)
            }
            else -> setSelection(position)
        }
        if (lastClickPosition != position) {
            lastClickPositionStack.remove(position)
            lastClickPositionStack.push(position)
        }
    }

    private fun setSelection(position: Int) {
        val adapter = adapter ?: return
        val item = adapter.getItem(position) ?: return
        if (!adapter.isSelected(position)) {
            adapter.toggleSelection(position)
            selectedErrors.add(item)
            actionMode?.invalidate()
        }
    }

    /**
     * Called to toggle selection
     * @param position position of selected item
     */
    private fun toggleSelection(position: Int) {
        val adapter = adapter ?: return
        val item = adapter.getItem(position) ?: return
        adapter.toggleSelection(position)
        if (adapter.isSelected(position)) {
            selectedErrors.add(item)
        } else {
            selectedErrors.remove(item)
        }
        actionMode?.invalidate()
    }

    override fun onUpdateEmptyView(size: Int) {
        if (size > 0) {
            binding.emptyView.hide()
        } else {
            binding.emptyView.show(R.string.information_no_error)
        }
    }

    private fun destroyActionModeIfNeeded() {
        lastClickPositionStack.clear()
        lastClickPositionStack.push(-1)
        actionMode?.finish()
    }

    override fun onCoverClick(position: Int) {
        destroyActionModeIfNeeded()

        val entryClicked = adapter?.getItem(position) ?: return
        openManga(entryClicked)
    }

    private fun openManga(entry: LibraryUpdateErrorsItem) {
        router.pushController(MangaController(entry.manga).withFadeTransaction())
    }

    /**
     * Called when ActionMode created.
     * @param mode the ActionMode object
     * @param menu menu object of ActionMode
     */
    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.generic_selection, menu)
        adapter?.mode = SelectableAdapter.Mode.MULTI
        return true
    }

    override fun onCreateActionToolbar(menuInflater: MenuInflater, menu: Menu) {
        menuInflater.inflate(R.menu.library_update_error_entry_selection, menu)
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = adapter?.selectedItemCount ?: 0
        if (count == 0) {
            // Destroy action mode if there are no items selected.
            destroyActionModeIfNeeded()
        } else {
            mode.title = count.toString()
        }
        return true
    }

    override fun onPrepareActionToolbar(toolbar: ActionModeWithToolbar, menu: Menu) {
        val chapters = getSelectedMangas()
        if (chapters.isEmpty()) return
    }

    /**
     * Called when ActionMode item clicked
     * @param mode the ActionMode object
     * @param item item from ActionMode.
     */
    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return onActionItemClicked(item)
    }

    private fun onActionItemClicked(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_select_all -> selectAll()
            R.id.action_select_inverse -> selectInverse()
            R.id.action_migrate -> migrateManga(getSelectedMangas())
            else -> return false
        }
        return true
    }

    /**
     * Called when ActionMode destroyed
     * @param mode the ActionMode object
     */
    override fun onDestroyActionMode(mode: ActionMode) {
        adapter?.mode = SelectableAdapter.Mode.IDLE
        adapter?.clearSelection()
        selectedErrors.clear()
        actionMode = null
    }

    private fun selectAll() {
        val adapter = adapter ?: return
        adapter.selectAll()
        selectedErrors.addAll(adapter.items)
        actionMode?.invalidate()
    }

    private fun selectInverse() {
        val adapter = adapter ?: return

        selectedErrors.clear()
        for (i in 0..adapter.itemCount) {
            adapter.toggleSelection(i)
            adapter.notifyItemChanged(i, i)
        }
        selectedErrors.addAll(adapter.selectedPositions.mapNotNull { adapter.getItem(it) })

        actionMode?.invalidate()
        // adapter.notifyDataSetChanged()
    }
}
