package qdvc.countdowns.android.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import qdvc.countdowns.android.app.model.Countdown
import qdvc.countdowns.android.app.util.CsvParser

sealed interface CountdownsState {
    data object NoFile : CountdownsState
    data object Loading : CountdownsState
    data class Loaded(
        val countdowns: List<Countdown>,
        val skippedRows: Int,
        val missingColumns: List<String>
    ) : CountdownsState
    data class Failed(val reason: String) : CountdownsState
}

/**
 * Reads the single CSV the user has granted access to.
 *
 * Access is through [android.content.ContentResolver] only, never
 * `java.io.File` — the app is given a document URI, not a path, and a path
 * fabricated from that URI carries no permission. Reads happen on
 * [Dispatchers.IO] because a content-provider read is IPC, not a local read.
 */
class CountdownRepository(private val context: Context) {

    /** Persists the grant so it survives a reboot. */
    fun takePersistablePermission(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            // Some providers hand out a one-shot grant. The file still reads for
            // this session; a later read will report a failure the user can act on.
        }
    }

    fun releasePermission(uri: Uri) {
        try {
            context.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            // Nothing to release.
        }
    }

    fun displayName(uri: Uri): String? = try {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null
        }
    } catch (e: Exception) {
        null
    }

    suspend fun load(uriString: String?): CountdownsState = withContext(Dispatchers.IO) {
        if (uriString.isNullOrBlank()) return@withContext CountdownsState.NoFile
        val uri = try {
            Uri.parse(uriString)
        } catch (e: Exception) {
            return@withContext CountdownsState.Failed("The saved file location isn't valid.")
        }

        if (DocumentFile.fromSingleUri(context, uri)?.exists() == false) {
            return@withContext CountdownsState.Failed("The file has been moved, renamed or deleted.")
        }

        try {
            val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.reader(Charsets.UTF_8).readText()
            } ?: return@withContext CountdownsState.Failed("The file couldn't be opened.")

            val parsed = CsvParser.parse(text)
            CountdownsState.Loaded(
                countdowns = parsed.countdowns,
                skippedRows = parsed.skippedRows,
                missingColumns = parsed.missingColumns
            )
        } catch (e: SecurityException) {
            CountdownsState.Failed("The app no longer has permission to read the file.")
        } catch (e: Exception) {
            CountdownsState.Failed("The file couldn't be read.")
        }
    }
}
