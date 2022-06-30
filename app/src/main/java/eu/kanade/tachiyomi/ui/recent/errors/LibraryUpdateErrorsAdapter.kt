package eu.kanade.tachiyomi.ui.recent.errors

import eu.davidea.flexibleadapter.FlexibleAdapter

class LibraryUpdateErrorsAdapter(
    val controller: LibraryUpdateErrorsController,
    val items: List<LibraryUpdateErrorsItem>,
) : FlexibleAdapter<LibraryUpdateErrorsItem>(items, controller, true) {

    val coverClickListener: OnCoverClickListener = controller

    init {
        setDisplayHeadersAtStartUp(true)
        setStickyHeaders(true)
    }

    interface OnCoverClickListener {
        fun onCoverClick(position: Int)
    }
}
