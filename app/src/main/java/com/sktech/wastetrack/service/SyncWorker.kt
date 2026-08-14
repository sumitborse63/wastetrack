package com.sktech.wastetrack.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sktech.wastetrack.data.local.db.dao.CertificateDao
import com.sktech.wastetrack.data.local.db.dao.ScrapEntryDao
import com.sktech.wastetrack.data.local.db.dao.SyncQueueDao
import com.sktech.wastetrack.data.local.db.dao.TransferDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val syncQueueDao: SyncQueueDao,
    private val scrapEntryDao: ScrapEntryDao,
    private val transferDao: TransferDao,
    private val certificateDao: CertificateDao
) : CoroutineWorker(context, params) {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val gson = Gson()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val pendingItems = syncQueueDao.dequeueOldest(limit = 50)
            if (pendingItems.isEmpty()) {
                return@withContext Result.success()
            }

            Log.d(TAG, "SyncWorker started: processing ${pendingItems.size} pending items")

            for (item in pendingItems) {
                try {
                    val mapType = object : TypeToken<Map<String, Any>>() {}.type
                    val data: Map<String, Any> = gson.fromJson(item.payload, mapType)

                    when (item.entityType) {
                        "SCRAP_ENTRY" -> {
                            firestore.collection("scrap_entries")
                                .document(item.entityId)
                                .set(data, SetOptions.merge())
                                .await()

                            scrapEntryDao.markSynced(item.entityId)
                            syncQueueDao.deleteById(item.id)
                            Log.d(TAG, "Synced scrap entry: ${item.entityId}")
                        }
                        "TRANSFER" -> {
                            firestore.collection("transfers")
                                .document(item.entityId)
                                .set(data, SetOptions.merge())
                                .await()

                            transferDao.markSynced(item.entityId)
                            syncQueueDao.deleteById(item.id)
                            Log.d(TAG, "Synced transfer: ${item.entityId}")
                        }
                        "CERTIFICATE" -> {
                            firestore.collection("certificates")
                                .document(item.entityId)
                                .set(data, SetOptions.merge())
                                .await()

                            syncQueueDao.deleteById(item.id)
                            Log.d(TAG, "Synced certificate: ${item.entityId}")
                        }
                        "BID_REQUEST" -> {
                            firestore.collection("bid_requests")
                                .document(item.entityId)
                                .set(data, SetOptions.merge())
                                .await()

                            syncQueueDao.deleteById(item.id)
                            Log.d(TAG, "Synced bid request: ${item.entityId}")
                        }
                        "BID" -> {
                            firestore.collection("bids")
                                .document(item.entityId)
                                .set(data, SetOptions.merge())
                                .await()

                            syncQueueDao.deleteById(item.id)
                            Log.d(TAG, "Synced bid: ${item.entityId}")
                        }
                        "SMART_BIN" -> {
                            firestore.collection("smart_bins")
                                .document(item.entityId)
                                .set(data, SetOptions.merge())
                                .await()

                            syncQueueDao.deleteById(item.id)
                            Log.d(TAG, "Synced smart bin: ${item.entityId}")
                        }
                        "USER" -> {
                            firestore.collection("users")
                                .document(item.entityId)
                                .set(data, SetOptions.merge())
                                .await()

                            syncQueueDao.deleteById(item.id)
                            Log.d(TAG, "Synced user profile: ${item.entityId}")
                        }
                        else -> {
                            syncQueueDao.deleteById(item.id)
                        }
                    }
                } catch (itemException: Exception) {
                    Log.w(TAG, "Failed to sync item ${item.id} (${item.entityType}): ${itemException.message}")
                    syncQueueDao.incrementRetry(item.id)
                }
            }

            // Cleanup permanently failed items exceeding retry limit
            syncQueueDao.clearFailed(maxRetries = 5)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker encountered an error: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "WasteTrackSyncWorker"
        const val WORK_NAME = "wastetrack_cloud_sync"
    }
}
