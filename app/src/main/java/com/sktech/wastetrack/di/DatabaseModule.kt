package com.sktech.wastetrack.di

import android.content.Context
import androidx.room.Room
import com.sktech.wastetrack.data.local.db.WasteTrackDatabase
import com.sktech.wastetrack.data.local.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): WasteTrackDatabase = Room.databaseBuilder(
        context,
        WasteTrackDatabase::class.java,
        WasteTrackDatabase.DATABASE_NAME
    )
    .addMigrations(
        WasteTrackDatabase.MIGRATION_1_2,
        WasteTrackDatabase.MIGRATION_2_3,
        WasteTrackDatabase.MIGRATION_3_4
    )
    .fallbackToDestructiveMigration(dropAllTables = false)
    .build()

    @Provides fun provideScrapEntryDao(db: WasteTrackDatabase): ScrapEntryDao = db.scrapEntryDao()
    @Provides fun provideTransferDao(db: WasteTrackDatabase): TransferDao = db.transferDao()
    @Provides fun provideQRHandshakeDao(db: WasteTrackDatabase): QRHandshakeDao = db.qrHandshakeDao()
    @Provides fun provideBinDao(db: WasteTrackDatabase): BinDao = db.binDao()
    @Provides fun provideBidDao(db: WasteTrackDatabase): BidDao = db.bidDao()
    @Provides fun provideCertificateDao(db: WasteTrackDatabase): CertificateDao = db.certificateDao()
    @Provides fun provideUserDao(db: WasteTrackDatabase): UserDao = db.userDao()
    @Provides fun provideSyncQueueDao(db: WasteTrackDatabase): SyncQueueDao = db.syncQueueDao()
}
