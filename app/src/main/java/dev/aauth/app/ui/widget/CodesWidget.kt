package dev.aauth.app.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.aauth.app.AAuthApplication
import dev.aauth.app.MainActivity
import dev.aauth.app.R
import dev.aauth.app.core.Totp
import kotlinx.coroutines.flow.first

private const val MAX_ROWS = 8

private class WidgetRow(
    val id: Long,
    val title: String,
    val subtitle: String,
    /** `null` while hidden. */
    val code: String?,
    val broken: Boolean,
)

/**
 * Home screen widget with one row per account.
 *
 * Codes stay hidden until the row is tapped, then show until the end of the current 30 s window.
 * When the app lock is on, rows never reveal a code: a tap just opens the app, which asks to unlock.
 */
class CodesWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as AAuthApplication
        val accounts = app.repository.accounts.first()
        val locked = app.settings.appLock.value
        val now = System.currentTimeMillis()
        val revealed = if (locked) emptyMap() else WidgetState.active(context, now)

        val rows = accounts.take(MAX_ROWS).map { account ->
            val secret = account.secret
            val code = if (secret != null && revealed.containsKey(account.id)) {
                Totp.generate(secret, now, account.algorithm, account.digits, account.period)
            } else {
                null
            }
            WidgetRow(account.id, account.title, account.subtitle, code, broken = secret == null)
        }

        provideContent {
            GlanceTheme {
                WidgetContent(rows = rows, locked = locked)
            }
        }
    }
}

@Composable
private fun WidgetContent(rows: List<WidgetRow>, locked: Boolean) {
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(12.dp),
    ) {
        Text(
            text = context.getString(R.string.widget_title),
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier.padding(bottom = 6.dp),
        )

        if (rows.isEmpty()) {
            Text(
                text = context.getString(R.string.widget_empty),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
            )
        } else {
            LazyColumn {
                items(rows, itemId = { it.id }) { row -> AccountRow(row, locked) }
            }
        }
    }
}

@Composable
private fun AccountRow(row: WidgetRow, locked: Boolean) {
    val context = LocalContext.current
    val onClick: Action = if (locked) {
        actionStartActivity<MainActivity>()
    } else {
        actionRunCallback<RevealAction>(actionParametersOf(AccountIdKey to row.id))
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = row.title,
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            if (row.subtitle.isNotEmpty()) {
                Text(
                    text = row.subtitle,
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                )
            }
        }

        val shown = when {
            row.broken -> "—"
            row.code != null -> formatCode(row.code)
            locked -> "••• •••"
            else -> context.getString(R.string.widget_tap_to_show)
        }
        val revealed = row.code != null
        Text(
            text = shown,
            style = TextStyle(
                color = if (revealed) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant,
                fontSize = if (revealed) 20.sp else 12.sp,
                fontFamily = if (revealed) FontFamily.Monospace else FontFamily.SansSerif,
                fontWeight = if (revealed) FontWeight.Medium else FontWeight.Normal,
            ),
        )
    }
}

/** "123456" -> "123 456", "12345678" -> "1234 5678". */
private fun formatCode(code: String): String {
    val mid = (code.length + 1) / 2
    return code.substring(0, mid) + " " + code.substring(mid)
}
