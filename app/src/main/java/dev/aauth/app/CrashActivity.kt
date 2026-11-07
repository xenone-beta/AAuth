package dev.aauth.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Plain-framework screen (no Compose, no app theme) that shows what crashed, with a Copy button.
 * It runs in its own process, so it is shown even if the main process dies during startup.
 */
class CrashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = intent.getStringExtra(EXTRA_TRACE).orEmpty()
        val pad = (16 * resources.displayMetrics.density).toInt()

        val title = TextView(this).apply {
            text = getString(R.string.crash_title)
            textSize = 20f
        }
        val hint = TextView(this).apply { text = getString(R.string.crash_hint) }
        val body = TextView(this).apply {
            text = trace
            textSize = 11f
            typeface = android.graphics.Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        val scroll = ScrollView(this).apply { addView(body) }
        val copy = Button(this).apply {
            text = getString(R.string.crash_copy)
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("AAuth crash", trace))
            }
        }
        val close = Button(this).apply {
            text = getString(R.string.crash_close)
            setOnClickListener { finishAndRemoveTask() }
        }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(copy, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(close, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(pad, pad * 2, pad, pad)
                addView(title)
                addView(hint)
                addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
                addView(buttons)
            },
        )
    }

    companion object {
        const val EXTRA_TRACE = "trace"
    }
}
