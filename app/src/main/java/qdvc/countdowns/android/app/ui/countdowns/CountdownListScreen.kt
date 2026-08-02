package qdvc.countdowns.android.app.ui.countdowns

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import qdvc.countdowns.android.app.R
import qdvc.countdowns.android.app.data.CountdownsState
import qdvc.countdowns.android.app.model.Countdown
import qdvc.countdowns.android.app.ui.components.EmptyState
import qdvc.countdowns.android.app.ui.components.Notice
import qdvc.countdowns.android.app.ui.components.icon
import qdvc.countdowns.android.app.ui.components.labelRes
import qdvc.countdowns.android.app.ui.theme.LocalTextScale
import qdvc.countdowns.android.app.ui.theme.categoryColor
import qdvc.countdowns.android.app.ui.theme.categoryOnColor
import qdvc.countdowns.android.app.util.Dates
import java.time.LocalDate
import kotlin.math.absoluteValue

@Composable
fun CountdownListScreen(
    state: CountdownsState,
    countdowns: List<Countdown>,
    today: LocalDate,
    past: Boolean,
    onOpen: (Countdown) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        is CountdownsState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        is CountdownsState.NoFile -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                icon = Icons.Filled.InsertDriveFile,
                title = stringResource(R.string.empty_no_file_title),
                body = stringResource(R.string.empty_no_file_body),
                actionLabel = stringResource(R.string.empty_no_file_action),
                onAction = onOpenSettings
            )
        }

        is CountdownsState.Failed -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                icon = Icons.Filled.WarningAmber,
                title = stringResource(R.string.error_read_title),
                body = stringResource(R.string.error_read_body, state.reason),
                actionLabel = stringResource(R.string.empty_no_file_action),
                onAction = onOpenSettings
            )
        }

        is CountdownsState.Loaded -> {
            if (countdowns.isEmpty()) {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = if (past) Icons.Filled.History else Icons.Filled.HourglassEmpty,
                        title = stringResource(
                            if (past) R.string.empty_none_past_title
                            else R.string.empty_none_upcoming_title
                        ),
                        body = stringResource(
                            if (past) R.string.empty_none_past_body
                            else R.string.empty_none_upcoming_body
                        )
                    )
                }
            } else {
                // Computed here rather than inside the LazyColumn: the content lambda
                // is a LazyListScope, not a composable scope, so a @Composable call
                // there would not compile.
                val warning = if (past) null else warnings(state)
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    if (warning != null) {
                        item { Notice(warning, MaterialTheme.colorScheme.error) }
                    }
                    items(countdowns, key = { it.key + "#" + it.rowNumber }) { countdown ->
                        CountdownRow(
                            countdown = countdown,
                            today = today,
                            onClick = { onOpen(countdown) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun warnings(state: CountdownsState.Loaded): String? {
    val parts = buildList {
        if (state.missingColumns.isNotEmpty()) {
            add(
                stringResource(
                    R.string.warning_missing_columns,
                    state.missingColumns.joinToString(", ")
                )
            )
        }
        if (state.skippedRows > 0) {
            add(stringResource(R.string.warning_skipped_rows, state.skippedRows))
        }
    }
    return parts.joinToString(" ").ifBlank { null }
}

/**
 * The list's identity: a column of day counts, one badge per row, all the same
 * width so the numbers line up and the whole list can be read as a single scale.
 * Today's badge is filled rather than tinted, so the thing happening now is the
 * one thing that pops.
 */
@Composable
private fun CountdownRow(
    countdown: Countdown,
    today: LocalDate,
    onClick: () -> Unit
) {
    val scale = LocalTextScale.current
    val days = countdown.daysFrom(today)
    val color = categoryColor(countdown.category)
    val isToday = days == 0L

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = if (isToday) color else color.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp)
                )
        ) {
            Text(
                text = days.absoluteValue.toString(),
                fontSize = (22 * scale).sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isToday) categoryOnColor(countdown.category) else color,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = countdown.name,
                fontSize = (16 * scale).sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = Dates.mediumDate(countdown.date) + "  ·  " +
                    stringResource(countdown.category.labelRes()),
                fontSize = (13 * scale).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = countdown.category.icon(),
            contentDescription = null,
            tint = color.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp)
        )
    }
}
