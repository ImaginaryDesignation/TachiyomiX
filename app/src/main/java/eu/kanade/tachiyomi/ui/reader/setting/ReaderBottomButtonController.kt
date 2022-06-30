package eu.kanade.tachiyomi.ui.reader.setting

import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import dev.chrisbanes.insetter.applyInsetter
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.ReaderBottomButtonControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.ui.base.controller.FabController
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.shrinkOnScroll
import uy.kohesive.injekt.injectLazy

class ReaderBottomButtonController :
    BaseController<ReaderBottomButtonControllerBinding>(null),
    FlexibleAdapter.OnItemClickListener,
    FabController {

    private val prefs: PreferencesHelper by injectLazy()

    private val buttonList = prefs.readerBottomButtons().get()

    private var disIndex = -1

    private var adapter: ReaderBottomButtonAdapter? = null

    private var actionFab: ExtendedFloatingActionButton? = null
    private var actionFabScrollListener: RecyclerView.OnScrollListener? = null

    override fun getTitle() = "Choose Buttons"

    override fun createBinding(inflater: LayoutInflater) = ReaderBottomButtonControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        binding.recycler.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }

        val adapterItems = mutableListOf<ReaderBottomButtonItem>()
        val both = buttonList.split("|")
        val selected = both[0].split(",")
        val rest = both[1].split(",")
        for (item in selected) {
            adapterItems.add(ReaderBottomButtonItem(ReaderBottomButton.valueOf(item), true))
        }
        for (item in rest) {
            adapterItems.add(ReaderBottomButtonItem(ReaderBottomButton.valueOf(item), false))
        }

        val ourAdapter = adapter ?: ReaderBottomButtonAdapter(
            adapterItems,
            this@ReaderBottomButtonController,
        )

        disIndex = selected.size

        adapter = ourAdapter
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.setHasFixedSize(true)
        binding.recycler.adapter = ourAdapter
        ourAdapter.itemTouchHelperCallback = null // Reset adapter touch adapter to fix drag after rotation
        ourAdapter.isHandleDragEnabled = true

        actionFabScrollListener = actionFab?.shrinkOnScroll(binding.recycler)
    }

    override fun configureFab(fab: ExtendedFloatingActionButton) {
        actionFab = fab
        fab.setText(R.string.action_save)
        fab.setIconResource(R.drawable.ic_save_24dp)
        fab.setOnClickListener {
            val selected = mutableSetOf<String>()
            val rest = mutableListOf<String>()
            for (item in adapter?.items!!) {
                if (item.isSelected) {
                    selected.add(item.button.name)
                } else {
                    rest.add(item.button.name)
                }
            }
            val newList = selected.joinToString(",", "", "|") + rest.joinToString(",", "", "")
            prefs.readerBottomButtons().set(newList)
            activity?.toast("Saved")
        }
    }

    override fun cleanupFab(fab: ExtendedFloatingActionButton) {
        fab.setOnClickListener(null)
        actionFabScrollListener?.let { binding.recycler.removeOnScrollListener(it) }
        actionFab = null
    }

    override fun onItemClick(view: View, position: Int): Boolean {
        adapter?.getItem(position)?.let {
            it.isSelected = !it.isSelected
            if (it.isSelected) {
                adapter?.moveItem(position, disIndex)
                disIndex += 1
            } else {
                disIndex -= 1
                adapter?.moveItem(position, disIndex)
            }
        }
        adapter?.notifyDataSetChanged()
        return false
    }
}
