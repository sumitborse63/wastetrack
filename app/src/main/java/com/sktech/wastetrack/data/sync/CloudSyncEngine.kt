package com.sktech.wastetrack.data.sync

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.sktech.wastetrack.data.local.db.dao.*
import com.sktech.wastetrack.data.local.db.entity.*
import com.sktech.wastetrack.domain.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncEngine @Inject constructor(
    private val scrapEntryDao: ScrapEntryDao,
    private val bidDao: BidDao,
    private val transferDao: TransferDao,
    private val certificateDao: CertificateDao,
    private val binDao: BinDao,
    private val syncQueueDao: SyncQueueDao,
    private val userDao: UserDao
) {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val activeListeners = CopyOnWriteArrayList<ListenerRegistration>()
    private val syncMutex = Mutex()
    private var lastSyncTimestamp: Long = 0L

    companion object {
        private const val TAG = "CloudSyncEngine"
        const val COLLECTION_SCRAP = "scrap_entries"
        const val COLLECTION_TRANSFERS = "transfers"
        const val COLLECTION_CERTIFICATES = "certificates"
        const val COLLECTION_BID_REQUESTS = "bid_requests"
        const val COLLECTION_BIDS = "bids"
        const val COLLECTION_BINS = "smart_bins"
        const val COLLECTION_USERS = "users"
    }

    /**
     * Push a scrap entry immediately to Firestore and Room
     */
    fun pushScrapEntry(entry: ScrapEntryEntity) {
        scope.launch {
            try {
                scrapEntryDao.insert(entry)
                val data = mapOf(
                    "id" to entry.id,
                    "factoryId" to entry.factoryId,
                    "loggedByUserId" to entry.loggedByUserId,
                    "category" to entry.category,
                    "subCategory" to entry.subCategory,
                    "weightKg" to entry.weightKg,
                    "estimatedVolumeL" to entry.estimatedVolumeL,
                    "anomalyScore" to entry.anomalyScore,
                    "anomalyFlagged" to entry.anomalyFlagged,
                    "imageUri" to (entry.imageUri ?: ""),
                    "notes" to entry.notes,
                    "contentHash" to entry.contentHash,
                    "createdAt" to entry.createdAt
                )
                firestore.collection(COLLECTION_SCRAP).document(entry.id).set(data, SetOptions.merge()).await()
                scrapEntryDao.markSynced(entry.id)
                Log.d(TAG, "Successfully synced scrap entry: ${entry.id}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push scrap entry to cloud, queuing: ${e.message}")
                syncQueueDao.enqueue(
                    SyncQueueEntity(
                        entityType = "SCRAP_ENTRY",
                        entityId = entry.id,
                        action = "CREATE",
                        payload = gson.toJson(entry)
                    )
                )
            }
        }
    }

    /**
     * Push a transfer entity immediately to Firestore and Room
     */
    fun pushTransfer(transfer: TransferEntity) {
        scope.launch {
            try {
                transferDao.insert(transfer)
                val data = mapOf(
                    "id" to transfer.id,
                    "scrapEntryId" to transfer.scrapEntryId,
                    "fromFactoryId" to transfer.fromFactoryId,
                    "toRecyclerId" to transfer.toRecyclerId,
                    "supervisorId" to transfer.supervisorId,
                    "weightAtSource" to transfer.weightAtSource,
                    "weightAtDestination" to (transfer.weightAtDestination ?: 0f),
                    "weightDiscrepancy" to (transfer.weightDiscrepancy ?: 0f),
                    "vehicleNumber" to transfer.vehicleNumber,
                    "status" to transfer.status,
                    "contentHash" to transfer.contentHash,
                    "initiatedAt" to transfer.initiatedAt,
                    "completedAt" to (transfer.completedAt ?: 0L)
                )
                firestore.collection(COLLECTION_TRANSFERS).document(transfer.id).set(data, SetOptions.merge()).await()
                transferDao.markSynced(transfer.id)
                Log.d(TAG, "Successfully synced transfer: ${transfer.id}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push transfer to cloud, queuing: ${e.message}")
                syncQueueDao.enqueue(
                    SyncQueueEntity(
                        entityType = "TRANSFER",
                        entityId = transfer.id,
                        action = "CREATE",
                        payload = gson.toJson(transfer)
                    )
                )
            }
        }
    }

    /**
     * Push a certificate entity immediately to Firestore
     */
    fun pushCertificate(certificate: CertificateEntity) {
        scope.launch {
            try {
                certificateDao.insert(certificate)
                val data = mapOf(
                    "id" to certificate.id,
                    "transferId" to certificate.transferId,
                    "factoryId" to certificate.factoryId,
                    "type" to certificate.type,
                    "jsonPayload" to certificate.jsonPayload,
                    "digitalSignature" to certificate.digitalSignature,
                    "status" to certificate.status,
                    "generatedAt" to certificate.generatedAt
                )
                firestore.collection(COLLECTION_CERTIFICATES).document(certificate.id).set(data, SetOptions.merge()).await()
                Log.d(TAG, "Successfully synced certificate: ${certificate.id}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push certificate to cloud, queuing: ${e.message}")
                syncQueueDao.enqueue(
                    SyncQueueEntity(
                        entityType = "CERTIFICATE",
                        entityId = certificate.id,
                        action = "CREATE",
                        payload = gson.toJson(certificate)
                    )
                )
            }
        }
    }

    /**
     * Push a smart bin to Firestore
     */
    fun pushBin(bin: BinEntity) {
        scope.launch {
            try {
                binDao.insert(bin)
                val data = mapOf(
                    "id" to bin.id,
                    "factoryId" to bin.factoryId,
                    "scrapCategory" to bin.scrapCategory,
                    "capacityKg" to bin.capacityKg,
                    "currentFillKg" to bin.currentFillKg,
                    "fillPercentage" to bin.fillPercentage,
                    "predictedFullTimestamp" to (bin.predictedFullTimestamp ?: 0L),
                    "status" to bin.status,
                    "lastUpdated" to bin.lastUpdated
                )
                firestore.collection(COLLECTION_BINS).document(bin.id).set(data, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push bin to cloud: ${e.message}")
            }
        }
    }

    /**
     * Synchronize and pull all cloud records for the current authenticated user into Room DB.
     * Protected by Mutex to prevent ConcurrentModificationException.
     */
    suspend fun syncAllFromCloud(userId: String, factoryId: String, role: UserRole) {
        syncMutex.withLock {
            val now = System.currentTimeMillis()
            if (now - lastSyncTimestamp < 3000L) {
                // Debounce rapid repeated sync calls
                return
            }
            lastSyncTimestamp = now

            try {
                Log.d(TAG, "Starting full cloud rehydration for user=$userId, factory=$factoryId, role=$role")

                // 1. Pull Scrap Entries
                val scrapQuery = if (role == UserRole.SUPERVISOR) {
                    firestore.collection(COLLECTION_SCRAP).whereEqualTo("factoryId", factoryId).get().await()
                } else {
                    firestore.collection(COLLECTION_SCRAP).limit(100).get().await()
                }
                for (doc in scrapQuery.documents) {
                    val entity = ScrapEntryEntity(
                        id = doc.id,
                        factoryId = doc.getString("factoryId") ?: factoryId,
                        loggedByUserId = doc.getString("loggedByUserId") ?: userId,
                        category = doc.getString("category") ?: "METAL",
                        subCategory = doc.getString("subCategory") ?: "",
                        weightKg = doc.getDouble("weightKg")?.toFloat() ?: 0f,
                        estimatedVolumeL = doc.getDouble("estimatedVolumeL")?.toFloat() ?: 1000f,
                        anomalyScore = doc.getDouble("anomalyScore")?.toFloat() ?: 0f,
                        anomalyFlagged = doc.getBoolean("anomalyFlagged") ?: false,
                        imageUri = doc.getString("imageUri"),
                        notes = doc.getString("notes") ?: "",
                        syncStatus = "SYNCED",
                        contentHash = doc.getString("contentHash") ?: "",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                    scrapEntryDao.insert(entity)
                }

                // 2. Pull Bid Requests
                val bidRequestsQuery = firestore.collection(COLLECTION_BID_REQUESTS).get().await()
                for (doc in bidRequestsQuery.documents) {
                    val req = BidRequestEntity(
                        id = doc.id,
                        factoryId = doc.getString("factoryId") ?: factoryId,
                        createdByUserId = doc.getString("createdByUserId") ?: "",
                        scrapEntryId = doc.getString("scrapEntryId") ?: "",
                        scrapCategory = doc.getString("scrapCategory") ?: "METAL",
                        estimatedWeightKg = doc.getDouble("estimatedWeightKg")?.toFloat() ?: 0f,
                        reservePricePerKg = doc.getDouble("reservePricePerKg")?.toFloat() ?: 0f,
                        auctionStartTime = doc.getLong("auctionStartTime") ?: System.currentTimeMillis(),
                        auctionEndTime = doc.getLong("auctionEndTime") ?: (System.currentTimeMillis() + 86400000L),
                        status = doc.getString("status") ?: "OPEN"
                    )
                    bidDao.insertRequest(req)
                }

                // 3. Pull Bids
                val bidsQuery = firestore.collection(COLLECTION_BIDS).get().await()
                for (doc in bidsQuery.documents) {
                    val bid = BidEntity(
                        id = doc.id,
                        bidRequestId = doc.getString("bidRequestId") ?: "",
                        recyclerId = doc.getString("recyclerId") ?: "",
                        recyclerName = doc.getString("recyclerName") ?: "Certified Recycler",
                        pricePerKg = doc.getDouble("pricePerKg")?.toFloat() ?: 0f,
                        totalBidAmount = doc.getDouble("totalBidAmount")?.toFloat() ?: 0f,
                        isWinning = doc.getBoolean("isWinning") ?: false,
                        submittedAt = doc.getLong("submittedAt") ?: System.currentTimeMillis()
                    )
                    bidDao.insertBid(bid)
                }

                // 4. Pull Transfers
                val transfersQuery = if (role == UserRole.RECYCLER) {
                    firestore.collection(COLLECTION_TRANSFERS).whereEqualTo("toRecyclerId", userId).get().await()
                } else {
                    firestore.collection(COLLECTION_TRANSFERS).whereEqualTo("fromFactoryId", factoryId).get().await()
                }
                for (doc in transfersQuery.documents) {
                    val transfer = TransferEntity(
                        id = doc.id,
                        scrapEntryId = doc.getString("scrapEntryId") ?: "",
                        fromFactoryId = doc.getString("fromFactoryId") ?: factoryId,
                        toRecyclerId = doc.getString("toRecyclerId") ?: userId,
                        supervisorId = doc.getString("supervisorId") ?: "supervisor-001",
                        weightAtSource = doc.getDouble("weightAtSource")?.toFloat() ?: 0f,
                        weightAtDestination = doc.getDouble("weightAtDestination")?.toFloat(),
                        weightDiscrepancy = doc.getDouble("weightDiscrepancy")?.toFloat(),
                        vehicleNumber = doc.getString("vehicleNumber") ?: "MH-15-TR-2024",
                        status = doc.getString("status") ?: "IN_TRANSIT",
                        syncStatus = "SYNCED",
                        contentHash = doc.getString("contentHash") ?: "",
                        initiatedAt = doc.getLong("initiatedAt") ?: System.currentTimeMillis(),
                        completedAt = doc.getLong("completedAt")
                    )
                    transferDao.insert(transfer)
                }

                // 5. Pull Certificates
                val certsQuery = firestore.collection(COLLECTION_CERTIFICATES).whereEqualTo("factoryId", factoryId).get().await()
                for (doc in certsQuery.documents) {
                    val cert = CertificateEntity(
                        id = doc.id,
                        transferId = doc.getString("transferId") ?: "",
                        factoryId = doc.getString("factoryId") ?: factoryId,
                        type = doc.getString("type") ?: "MPCB_DISPOSAL",
                        jsonPayload = doc.getString("jsonPayload") ?: "{}",
                        digitalSignature = doc.getString("digitalSignature") ?: "",
                        status = doc.getString("status") ?: "GENERATED",
                        generatedAt = doc.getLong("generatedAt") ?: System.currentTimeMillis()
                    )
                    certificateDao.insert(cert)
                }

                // 6. Pull Smart Bins
                val binsQuery = firestore.collection(COLLECTION_BINS).whereEqualTo("factoryId", factoryId).get().await()
                for (doc in binsQuery.documents) {
                    val bin = BinEntity(
                        id = doc.id,
                        factoryId = doc.getString("factoryId") ?: factoryId,
                        scrapCategory = doc.getString("scrapCategory") ?: "METAL",
                        capacityKg = doc.getDouble("capacityKg")?.toFloat() ?: 1000f,
                        currentFillKg = doc.getDouble("currentFillKg")?.toFloat() ?: 0f,
                        fillPercentage = doc.getDouble("fillPercentage")?.toFloat() ?: 0f,
                        predictedFullTimestamp = doc.getLong("predictedFullTimestamp"),
                        status = doc.getString("status") ?: "ACTIVE",
                        lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis()
                    )
                    binDao.insert(bin)
                }

                Log.d(TAG, "Cloud rehydration completed successfully!")
            } catch (e: Exception) {
                Log.e(TAG, "Error rehydrating from cloud: ${e.message}", e)
            }
        }
    }

    /**
     * Start live realtime sync listeners (Thread-safe)
     */
    @Synchronized
    fun startRealtimeSync(factoryId: String, recyclerId: String) {
        stopRealtimeSync()

        try {
            // Listen for new/updated bid requests
            val bidListener = firestore.collection(COLLECTION_BID_REQUESTS).addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener
                scope.launch {
                    for (doc in snapshots.documents) {
                        val req = BidRequestEntity(
                            id = doc.id,
                            factoryId = doc.getString("factoryId") ?: factoryId,
                            createdByUserId = doc.getString("createdByUserId") ?: "",
                            scrapEntryId = doc.getString("scrapEntryId") ?: "",
                            scrapCategory = doc.getString("scrapCategory") ?: "METAL",
                            estimatedWeightKg = doc.getDouble("estimatedWeightKg")?.toFloat() ?: 0f,
                            reservePricePerKg = doc.getDouble("reservePricePerKg")?.toFloat() ?: 0f,
                            auctionStartTime = doc.getLong("auctionStartTime") ?: System.currentTimeMillis(),
                            auctionEndTime = doc.getLong("auctionEndTime") ?: (System.currentTimeMillis() + 86400000L),
                            status = doc.getString("status") ?: "OPEN"
                        )
                        bidDao.insertRequest(req)
                    }
                }
            }
            activeListeners.add(bidListener)

            // Listen for incoming transfer updates
            val transferListener = firestore.collection(COLLECTION_TRANSFERS).addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener
                scope.launch {
                    for (doc in snapshots.documents) {
                        val transfer = TransferEntity(
                            id = doc.id,
                            scrapEntryId = doc.getString("scrapEntryId") ?: "",
                            fromFactoryId = doc.getString("fromFactoryId") ?: factoryId,
                            toRecyclerId = doc.getString("toRecyclerId") ?: recyclerId,
                            supervisorId = doc.getString("supervisorId") ?: "supervisor-001",
                            weightAtSource = doc.getDouble("weightAtSource")?.toFloat() ?: 0f,
                            weightAtDestination = doc.getDouble("weightAtDestination")?.toFloat(),
                            weightDiscrepancy = doc.getDouble("weightDiscrepancy")?.toFloat(),
                            vehicleNumber = doc.getString("vehicleNumber") ?: "MH-15-TR-2024",
                            status = doc.getString("status") ?: "IN_TRANSIT",
                            syncStatus = "SYNCED",
                            contentHash = doc.getString("contentHash") ?: "",
                            initiatedAt = doc.getLong("initiatedAt") ?: System.currentTimeMillis(),
                            completedAt = doc.getLong("completedAt")
                        )
                        transferDao.insert(transfer)
                    }
                }
            }
            activeListeners.add(transferListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting realtime sync: ${e.message}", e)
        }
    }

    @Synchronized
    fun stopRealtimeSync() {
        val iterator = activeListeners.iterator()
        while (iterator.hasNext()) {
            val listener = iterator.next()
            try {
                listener.remove()
            } catch (e: Exception) {
                // Ignore removal failure
            }
        }
        activeListeners.clear()
    }
}
