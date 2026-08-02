package qdvc.countdowns.android.app.ui.countdowns

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import qdvc.countdowns.android.app.R
import qdvc.countdowns.android.app.model.Countdown
import qdvc.countdowns.android.app.ui.components.icon
import qdvc.countdowns.android.app.ui.components.labelRes
import qdvc.countdowns.android.app.ui.theme.LocalTextScale
import qdvc.countdowns.android.app.ui.theme.categoryColor
import qdvc.countdowns.android.app.ui.theme.categoryOnColor
import qdvc.countdowns.android.app.util.Dates
import java.time.LocalDate
import kotlin.math.absoluteValue

/**
 * The counter, made unmissable: one number at display size, the category stated
 * plainly above it in its own colour, and everything else kept quiet underneath.
 */
@Composable
fun CountdownDetailScreen(
    countdown: Countdown,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    val scale = LocalTextScale.current
    val days = countdown.daysFrom(today)
    val color = categoryColor(countdown.category)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .background(color, CircleShape)
        ) {
            Icon(
                imageVector = countdown.category.icon(),
                contentDescription = null,
                tint = categoryOnColor(countdown.category),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(countdown.category.labelRes()).uppercase(),
            fontSize = (12 * scale).sp,
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = days.absoluteValue.toString(),
            fontSize = (112 * scale).sp,
            lineHeight = (116 * scale).sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-4).sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = unitLabel(days),
            fontSize = (16 * scale).sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        Text(
            text = countdown.name,
            fontSize = (24 * scale).sp,
            lineHeight = (30 * scale).sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = Dates.longDate(countdown.date),
            fontSize = (15 * scale).sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (countdown.extras.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.detail_other_columns).uppercase(),
                    fontSize = (11 * scale).sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
                countdown.extras.forEachIndexed { index, (label, value) ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            fontSize = (14 * scale).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = value,
                            fontSize = (14 * scale).sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.detail_row_count, countdown.rowNumber),
            fontSize = (12 * scale).sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun unitLabel(days: Long): String = when {
    days == 0L -> stringResource(R.string.happening_today)
    days == 1L -> stringResource(R.string.day_until_label)
    days == -1L -> stringResource(R.string.day_since_label)
    days > 0 -> stringResource(R.string.days_until_label)
    else -> stringResource(R.string.days_since_label)
}
