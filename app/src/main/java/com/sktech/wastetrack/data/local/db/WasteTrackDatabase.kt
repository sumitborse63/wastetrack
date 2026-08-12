package com.sktech.wastetrack.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
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
    version = 1,
    exportSchema = true
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
    }
}
