package dev.aauth.app.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import dev.aauth.app.AAuthApplication
import dev.aauth.app.core.Totp
import kotlinx.coroutines.flow.first

val AccountIdKey = ActionParameters.Key<Long>("account_id")

/** Tap on a widget row: show the code until the end of its time window, or hide it again. */
class RevealAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[AccountIdKey] ?: return
        val app = context.applicationContext as AAuthApplication

        // With app lock on the widget must never show a code without authentication.
        if (app.settings.appLock.value) return

        val now = System.currentTimeMillis()
        if (WidgetState.active(context, now).containsKey(id)) {
            WidgetState.hide(context, id)
        } else {
            val account = app.repository.accounts.first().firstOrNull { it.id == id } ?: return
            val remaining = Totp.secondsRemaining(now, account.period)
            WidgetState.reveal(context, id, now + remaining * 1000L)
        }

        CodesWidget().updateAll(context)
        WidgetRefreshWorker.scheduleNext(context)
    }
}
