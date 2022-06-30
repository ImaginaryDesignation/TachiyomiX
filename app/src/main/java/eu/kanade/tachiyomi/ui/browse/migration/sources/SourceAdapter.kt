package eu.kanade.tachiyomi.ui.browse.migration.sources

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible

/**
 * Adapter that holds the catalogue cards.
 *
 * @param controller instance of [MigrationController].
 */
// TX-->
// class SourceAdapter(controller: Controller) :
class SourceAdapter(controller: /* --TX-- */ MigrationSourcesController /* --TX-- */) :
// TX<--
        FlexibleAdapter<IFlexible<*>>(null, controller, true) {

        init {
            setDisplayHeadersAtStartUp(true)
        }

        // TX-->
        /**
         * Listener for auto item clicks.
         */
        val allClickListener: OnAllClickListener? = controller

        /**
         * Listener which should be called when user clicks select.
         */
        interface OnAllClickListener {
            fun onAllClick(position: Int)
        }
        // TX<--
    }
