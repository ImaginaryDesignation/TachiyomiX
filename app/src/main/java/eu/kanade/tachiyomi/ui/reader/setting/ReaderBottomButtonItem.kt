package eu.kanade.tachiyomi.ui.reader.setting

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R

/**
 * Reader Bottom Button item for a recycler view.
 */
class ReaderBottomButtonItem(val button: ReaderBottomButton, selected: Boolean) : AbstractFlexibleItem<ReaderBottomButtonHolder>() {

    /**
     * Whether this item is currently selected.
     */
    var isSelected = selected

    /**
     * Returns the layout resource for this item.
     */
    override fun getLayoutRes(): Int {
        return R.layout.reader_bottom_button_item
    }

    /**
     * Returns a new view holder for this item.
     *
     * @param view The view of this item.
     * @param adapter The adapter of this item.
     */
    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): ReaderBottomButtonHolder {
        return ReaderBottomButtonHolder(view, adapter as ReaderBottomButtonAdapter)
    }

    /**
     * Binds the given view holder with this item.
     *
     * @param adapter The adapter of this item.
     * @param holder The holder to bind.
     * @param position The position of this item in the adapter.
     * @param payloads List of partial changes.
     */
    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: ReaderBottomButtonHolder,
        position: Int,
        payloads: List<Any?>?,
    ) {
        holder.bind(button, isSelected)
    }

    /**
     * Returns true if this item is draggable.
     */
    override fun isDraggable(): Boolean {
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is ReaderBottomButtonItem) {
            return button.value.equals(other.button.value, true)
        }
        return false
    }

    override fun hashCode(): Int {
        return button.ordinal
    }
}
