package eu.kanade.tachiyomi.ui.reader.setting

import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R

enum class ReaderBottomButton(val value: String, @StringRes val stringRes: Int) {
    // ViewChapters("vc", R.string.action_view_chapters),
    AddBookmark("abm", R.string.action_bookmark),
    WebView("wb", R.string.action_open_in_web_view),
    ReadingMode("rm", R.string.viewer),
    Rotation("rot", R.string.rotation_type),
    CropBordersPager("cbp", R.string.pref_crop_borders_pager),
    CropBordersContinuousVertical("cbc", R.string.pref_crop_borders_continuous_vertical),
    CropBordersWebtoon("cbw", R.string.pref_crop_borders_webtoon),
    PageLayout("pl", R.string.page_layout),
    BoostPage("bp", R.string.eh_boost_page),
    RetryAll("ra", R.string.eh_retry_all),
    ;

    // fun isIn(buttons: Collection<String>) = value in buttons
    fun isIn(buttons: Collection<String>) = name in buttons

    companion object {
        var BUTTONS_DEFAULTS = setOf(
            // ViewChapters,
            AddBookmark,
            WebView,
            CropBordersPager,
            Rotation,
            ReadingMode,
            PageLayout,
            BoostPage,
            RetryAll,
        ).joinToString(",", "", "|") + setOf(
            CropBordersContinuousVertical,
            CropBordersWebtoon,
        ).joinToString(",", "", "")

        /*val BUTTONS_DEFAULTS = setOf(
            // ViewChapters,
            AddBookmark,
            WebView,
            CropBordersPager,
            Rotation,
            ReadingMode,
            PageLayout,
            BoostPage,
            RetryAll,
        ).map { it.value }.toSet()*/

        val orderMap = mapOf(
            0 to "AddBookmark",
            1 to "WebView",
            2 to "ReadingMode",
            3 to "CropBorders",
            4 to "RetryAll",
            5 to "BoostPage",
            6 to "Rotation",
            7 to "PageLayout",
        )

        val stringMap = mapOf(
            R.string.action_bookmark to "Bookmark chapter",
            R.string.action_open_in_web_view to "Open in WebView",
            R.string.viewer to "Reading mode",
            R.string.rotation_type to "Rotation type",
            R.string.pref_crop_borders_pager to "Crop borders (Paged)",
            R.string.pref_crop_borders_continuous_vertical to "Crop borders (Cont. Vertical)",
            R.string.pref_crop_borders_webtoon to "Crop borders (Webtoon)",
            R.string.page_layout to "Page layout",
            R.string.eh_boost_page to "Boost page",
            R.string.eh_retry_all to "Retry all",
        )

        val iconMap = mapOf(
            R.string.action_bookmark to R.drawable.ic_bookmark_24dp,
            R.string.action_open_in_web_view to R.drawable.ic_public_24dp,
            R.string.viewer to R.drawable.ic_reader_default_24dp,
            R.string.rotation_type to R.drawable.ic_screen_rotation_24dp,
            R.string.pref_crop_borders_pager to R.drawable.ic_crop_24dp,
            R.string.pref_crop_borders_continuous_vertical to R.drawable.ic_crop_24dp,
            R.string.pref_crop_borders_webtoon to R.drawable.ic_crop_24dp,
            R.string.page_layout to R.drawable.ic_book_open_variant_24dp,
            R.string.eh_boost_page to R.drawable.ic_boost_page_24dp,
            R.string.eh_retry_all to R.drawable.ic_refresh_24dp,
        )
    }
}
