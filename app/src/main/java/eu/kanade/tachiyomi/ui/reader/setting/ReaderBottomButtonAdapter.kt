package eu.kanade.tachiyomi.ui.reader.setting

import eu.davidea.flexibleadapter.FlexibleAdapter

class ReaderBottomButtonAdapter(
    var items: List<ReaderBottomButtonItem>,
    controller: ReaderBottomButtonController,
) : FlexibleAdapter<ReaderBottomButtonItem>(
    items,
    controller,
    true,
) {

    fun updateItems() {
        items = currentItems
    }
}
