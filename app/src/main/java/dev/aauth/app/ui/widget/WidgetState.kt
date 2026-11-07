package dev.aauth.app.ui.widget

import android.content.Context

/**
 * Which accounts currently have their code revealed on the home screen widget, and until when.
 * Codes are hidden by default so they are not readable by anyone glancing at the home screen.
 */
object WidgetState {
    private const val PREFS = "aauth_widget"
    private const val PREFIX = "reveal_"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun reveal(context: Context, id: Long, untilMillis: Long) {
        prefs(context).edit().putLong(PREFIX + id, untilMillis).apply()
    }

    fun hide(context: Context, id: Long) {
        prefs(context).edit().remove(PREFIX + id).apply()
    }

    /** Account id to expiry time, for reveals that have not expired yet. */
    fun active(context: Context, nowMillis: Long): Map<Long, Long> =
        prefs(context).all.mapNotNull { (key, value) ->
            val id = key.removePrefix(PREFIX).toLongOrNull()
            val until = value as? Long
            if (key.startsWith(PREFIX) && id != null && until != null && until > nowMillis) id to until else null
        }.toMap()

    fun clearExpired(context: Context, nowMillis: Long) {
        val editor = prefs(context).edit()
        for ((key, value) in prefs(context).all) {
            if (key.startsWith(PREFIX) && (value as? Long ?: 0L) <= nowMillis) editor.remove(key)
        }
        editor.apply()
    }

    fun nextExpiry(context: Context, nowMillis: Long): Long? = active(context, nowMillis).values.minOrNull()
}
