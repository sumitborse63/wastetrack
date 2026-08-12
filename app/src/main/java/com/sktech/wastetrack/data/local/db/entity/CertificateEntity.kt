package com.sktech.wastetrack.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "certificates")
data class CertificateEntity(
    @PrimaryKey val id: String,
    val transferId: String,
    val factoryId: String,
    val type: String,
    val pdfUri: String? = null,
    val jsonPayload: String = "",
    val digitalSignature: String = "",
    val status: String = "DRAFT",
    val generatedAt: Long = System.currentTimeMillis(),
    val submittedAt: Long? = null
)
