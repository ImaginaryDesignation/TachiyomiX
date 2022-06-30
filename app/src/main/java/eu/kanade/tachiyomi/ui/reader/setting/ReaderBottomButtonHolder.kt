package eu.kanade.tachiyomi.ui.reader.setting

import android.graphics.Paint
import android.view.View
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.databinding.ReaderBottomButtonItemBinding

class ReaderBottomButtonHolder(view: View, val adapter: ReaderBottomButtonAdapter) : FlexibleViewHolder(view, adapter) {

    private val binding = ReaderBottomButtonItemBinding.bind(view)

    init {
        setDragHandleView(binding.reorder)
    }

    /**
     * Binds this holder with the given button.
     *
     * @param button The button to bind.
     */
    fun bind(button: ReaderBottomButton, isSelected: Boolean) {
        binding.title.text = ReaderBottomButton.stringMap[button.stringRes]

        // Update circle letter image.
        itemView.post {
            val icon = ReaderBottomButton.iconMap[button.stringRes]
            if (icon != null) {
                binding.image.setImageResource(icon)
            }
        }

        if (isSelected) {
            binding.title.alpha = 1.0f
            binding.image.alpha = 1.0f
            binding.title.paintFlags = binding.title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        } else {
            binding.title.alpha = DISABLED_ALPHA
            binding.image.alpha = DISABLED_ALPHA
            binding.title.paintFlags = binding.title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }
    }

    /**
     * Called when an item is released.
     *
     * @param position The position of the released item.
     */
    override fun onItemReleased(position: Int) {
        super.onItemReleased(position)
        adapter.updateItems()
    }

    companion object {
        private const val DISABLED_ALPHA = 0.3f
    }
}
