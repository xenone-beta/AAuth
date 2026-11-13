package dev.aauth.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AccountEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "aauth.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** 1.1.2: accounts can be HOTP or Steam Guard, not only TOTP. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN type TEXT NOT NULL DEFAULT 'TOTP'")
                db.execSQL("ALTER TABLE accounts ADD COLUMN counter INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
