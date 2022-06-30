package eu.kanade.tachiyomi.data.backup.full.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupFull {
    fun getDefaultFilename(fileName: String): String {
        val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
        // TX-->
        return "${fileName}_$date.proto.gz"
        // TX<--
    }
}
