package eu.kanade.tachiyomi.ui.recent.errors

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.SectionHeaderItemBinding

class LibraryUpdateErrorSectionItem(private val errorString: String, private val headerCount: String) :
    AbstractHeaderItem<LibraryUpdateErrorSectionItem.LibraryUpdateErrorSectionItemHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.section_header_item
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): LibraryUpdateErrorSectionItemHolder {
        return LibraryUpdateErrorSectionItemHolder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>, holder: LibraryUpdateErrorSectionItemHolder, position: Int, payloads: List<Any?>?) {
        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is LibraryUpdateErrorSectionItem) {
            return errorString.equals(other.errorString, false)
        }
        return false
    }

    override fun hashCode(): Int {
        return errorString.hashCode()
    }

    inner class LibraryUpdateErrorSectionItemHolder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter, true) {

        private val binding = SectionHeaderItemBinding.bind(view)

        fun bind(item: LibraryUpdateErrorSectionItem) {
            val headerText = item.errorString + " (" + item.headerCount + ")"
            binding.title.text = headerText
        }
    }
}
