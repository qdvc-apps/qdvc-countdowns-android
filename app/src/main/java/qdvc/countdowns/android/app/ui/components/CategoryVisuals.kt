package qdvc.countdowns.android.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.ui.graphics.vector.ImageVector
import qdvc.countdowns.android.app.R
import qdvc.countdowns.android.app.model.Category

/**
 * Three silhouettes chosen to be told apart at a glance and at badge size:
 * a burst for something to look forward to, a flag for a promise made inside the
 * team, and a gavel for one made to somebody outside it.
 */
fun Category.icon(): ImageVector = when (this) {
    Category.EVENT -> Icons.Filled.Celebration
    Category.DEADLINE_INTERNAL -> Icons.Filled.Flag
    Category.DEADLINE_EXTERNAL -> Icons.Filled.Gavel
    Category.OTHER -> Icons.Filled.HelpOutline
}

@StringRes
fun Category.labelRes(): Int = when (this) {
    Category.EVENT -> R.string.category_event
    Category.DEADLINE_INTERNAL -> R.string.category_deadline_internal
    Category.DEADLINE_EXTERNAL -> R.string.category_deadline_external
    Category.OTHER -> R.string.category_other
}
