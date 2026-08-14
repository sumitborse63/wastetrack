package com.sktech.wastetrack.data.local.db

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sktech.wastetrack.data.local.db.dao.*
import com.sktech.wastetrack.data.local.db.entity.*

@Database(
    entities = [
        ScrapEntryEntity::class,
        TransferEntity::class,
        QRHandshakeEntity::class,
        BinEntity::class,
        BidRequestEntity::class,
        BidEntity::class,
        CertificateEntity::class,
        UserEntity::class,
        SyncQueueEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class WasteTrackDatabase : RoomDatabase() {
    abstract fun scrapEntryDao(): ScrapEntryDao
    abstract fun transferDao(): TransferDao
    abstract fun qrHandshakeDao(): QRHandshakeDao
    abstract fun binDao(): BinDao
    abstract fun bidDao(): BidDao
    abstract fun certificateDao(): CertificateDao
    abstract fun userDao(): UserDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        const val DATABASE_NAME = "wastetrack_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE bid_requests ADD COLUMN createdByUserId TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                migrateUsersTable(database)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                migrateUsersTable(database)
            }
        }

        private fun migrateUsersTable(database: SupportSQLiteDatabase) {
            try {
                database.execSQL("ALTER TABLE users ADD COLUMN organizationName TEXT NOT NULL DEFAULT ''")
            } catch (e: Exception) {}
            try {
                database.execSQL("ALTER TABLE users ADD COLUMN industrialArea TEXT NOT NULL DEFAULT ''")
            } catch (e: Exception) {}
            try {
                database.execSQL("ALTER TABLE users ADD COLUMN registrationNumber TEXT NOT NULL DEFAULT ''")
            } catch (e: Exception) {}
            try {
                database.execSQL("ALTER TABLE users ADD COLUMN isProfileComplete INTEGER NOT NULL DEFAULT 1")
            } catch (e: Exception) {}
        }
    }
}
