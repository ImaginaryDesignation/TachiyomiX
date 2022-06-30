package eu.kanade.tachiyomi.ui.recent.errors

import android.view.View
import coil.dispose
import coil.load
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.databinding.LibraryUpdateErrorsItemBinding
import eu.kanade.tachiyomi.source.SourceManager
import uy.kohesive.injekt.injectLazy

/**
 * Holder that contains error item
 * UI related actions should be called from here.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @param listener a listener to react to single tap and long tap events.
 * @constructor creates a new error entry holder.
 */
class LibraryUpdateErrorsHolder(private val view: View, private val adapter: LibraryUpdateErrorsAdapter) :
    FlexibleViewHolder(view, adapter) {

    private val sourceManager: SourceManager by injectLazy()

    private val binding = LibraryUpdateErrorsItemBinding.bind(view)

    init {
        binding.mangaCover.setOnClickListener {
            adapter.coverClickListener.onCoverClick(bindingAdapterPosition)
        }
    }

    fun bind(item: LibraryUpdateErrorsItem) {
        // Set manga title
        binding.mangaTitle.text = item.manga.title

        // Set manga source
        binding.sourceName.text = sourceManager.getOrStub(item.manga.source).name

        // Set cover
        binding.mangaCover.dispose()
        binding.mangaCover.load(item.manga)
    }
}
