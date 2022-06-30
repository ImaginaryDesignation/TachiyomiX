package eu.kanade.tachiyomi.ui.reader.model

import eu.kanade.tachiyomi.source.model.Page
import java.io.InputStream

open class ReaderPage(
    index: Int,
    url: String = "",
    imageUrl: String? = null,
    // TX-->
    /** Value used to indicate that this page has been shifted to create a double page*/
    var shiftedPage: Boolean = false,
    /** Value used to indicate that this page can't be doubled up (is isolated) because the next page is too wide (is a fullPage) */
    var isolatedPage: Boolean = false,
    // TX<--
    var stream: (() -> InputStream)? = null,
) : Page(index, url, imageUrl, null) {

    open lateinit var chapter: ReaderChapter

    // TX-->
    /** Value to indicate that this page is too wide to be doubled up */
    var fullPage: Boolean = false
        set(value) {
            field = value
            // If true then set shiftedPage to false
            if (value) shiftedPage = false
        }
    // TX<--
}
