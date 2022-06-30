package eu.kanade.tachiyomi.ui.main

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import it.gmariotti.changelibs.library.view.ChangeLogRecyclerView

class WhatsNewDialogController(bundle: Bundle? = null) : DialogController(bundle) {

    @Suppress("DEPRECATION")
    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        // TX-->
        val view = WhatsNewRecyclerView(activity!!)
        return MaterialAlertDialogBuilder(activity!!)
            // .setTitle(activity!!.getString(R.string.updated_version, BuildConfig.VERSION_NAME))
            .setTitle("What's new")
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            /*.setNeutralButton(R.string.whats_new) { _, _ ->
                openInBrowser(RELEASE_URL)
            }*/
            // TX<--
            .create()
    }

    // TX-->
    class WhatsNewRecyclerView(context: Context) : ChangeLogRecyclerView(context) {
        override fun initAttrs(attrs: AttributeSet?, defStyle: Int) {
            mRowLayoutId = R.layout.changelog_row_layout
            mRowHeaderLayoutId = R.layout.changelog_header_layout
            mChangeLogFileResourceId = R.raw.changelog_release
        }
    }
    // TX<--
}
