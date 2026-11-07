package dev.aauth.app

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import dev.aauth.app.core.SecretCipher
import dev.aauth.app.data.AccountRepository
import dev.aauth.app.data.AppDatabase
import dev.aauth.app.data.SettingsStore

/** Hand-rolled dependency container - small enough that a DI framework would be overkill. */
class AAuthApplication : Application() {
    private val database by lazy { AppDatabase.create(this) }

    override fun onCreate() {
        super.onCreate()
        installCrashReporter()
    }

    /** On an unhandled crash, shows the stack trace in [CrashActivity] so it can be copied and reported. */
    private fun installCrashReporter() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val trace = buildString {
                    append("AAuth ${BuildConfig.VERSION_NAME} / Android ${Build.VERSION.RELEASE} ")
                    append("(API ${Build.VERSION.SDK_INT}) / ${Build.MANUFACTURER} ${Build.MODEL}\n\n")
                    append(Log.getStackTraceString(throwable))
                }.take(20_000)
                startActivity(
                    Intent(this, CrashActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra(CrashActivity.EXTRA_TRACE, trace),
                )
            }
            if (previous != null) previous.uncaughtException(thread, throwable)
            else {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
    }

    val settings by lazy { SettingsStore(this) }
    val repository by lazy { AccountRepository(database.accountDao(), SecretCipher()) }
}
