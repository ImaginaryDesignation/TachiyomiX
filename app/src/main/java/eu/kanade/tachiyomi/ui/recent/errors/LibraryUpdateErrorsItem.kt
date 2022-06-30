package eu.kanade.tachiyomi.ui.recent.errors

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga

class LibraryUpdateErrorsItem(val message: String, val manga: Manga, header: LibraryUpdateErrorSectionItem) :
    AbstractSectionableItem<LibraryUpdateErrorsHolder, LibraryUpdateErrorSectionItem>(header) {

    override fun getLayoutRes(): Int {
        return R.layout.library_update_errors_item
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): LibraryUpdateErrorsHolder {
        return LibraryUpdateErrorsHolder(view, adapter as LibraryUpdateErrorsAdapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: LibraryUpdateErrorsHolder,
        position: Int,
        payloads: List<Any?>?,
    ) {
        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is LibraryUpdateErrorsItem) {
            return manga.id == other.manga.id
        }
        return false
    }

    override fun hashCode(): Int {
        return manga.id.hashCode()
    }
}
