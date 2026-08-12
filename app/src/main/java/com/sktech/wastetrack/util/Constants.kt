package com.sktech.wastetrack.util

object Constants {
    // Factory IDs for pilot deployment
    const val DEFAULT_FACTORY_ID = "ambad-midc-pilot-001"
    const val DEFAULT_FACTORY_NAME = "Ambad MIDC Pilot"

    // Sync Configuration
    const val SYNC_INTERVAL_MINUTES = 15L
    const val SYNC_BATCH_SIZE = 50
    const val MAX_RETRY_COUNT = 5

    // Bin Thresholds
    const val BIN_WARNING_THRESHOLD = 60f  // Orange warning at 60%
    const val BIN_CRITICAL_THRESHOLD = 85f // Red critical at 85%
    const val BIN_DISPATCH_THRESHOLD = 90f // Auto-dispatch at 90%

    // Anomaly Detection
    const val ANOMALY_SCORE_THRESHOLD = 0.7f // Flag if anomaly score > 0.7
    const val WEIGHT_VOLUME_SIGMA = 2.0f     // Flag if deviation > 2σ

    // ML Kit
    const val ML_CONFIDENCE_THRESHOLD = 0.6f // Min confidence for auto-classification

    // QR
    const val QR_EXPIRY_MINUTES = 30L // QR code valid for 30 minutes

    // Bid Auction
    const val AUCTION_DURATION_HOURS = 24L

    // API (mock for now)
    const val BASE_URL = "https://api.wastetrack.mock/"

    // DataStore
    const val PREFERENCES_NAME = "wastetrack_prefs"
    const val KEY_USER_ID = "user_id"
    const val KEY_FACTORY_ID = "factory_id"
    const val KEY_USER_ROLE = "user_role"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    const val KEY_LANGUAGE = "language"
}
