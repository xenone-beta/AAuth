package dev.aauth.app

import android.app.Application
import dev.aauth.app.core.SecretCipher
import dev.aauth.app.data.AccountRepository
import dev.aauth.app.data.AppDatabase
import dev.aauth.app.data.SettingsStore

/** Hand-rolled dependency container - small enough that a DI framework would be overkill. */
class AAuthApplication : Application() {
    private val database by lazy { AppDatabase.create(this) }

    val settings by lazy { SettingsStore(this) }
    val repository by lazy { AccountRepository(database.accountDao(), SecretCipher()) }
}
