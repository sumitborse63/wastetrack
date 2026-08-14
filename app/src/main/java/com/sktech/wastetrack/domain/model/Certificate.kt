package com.sktech.wastetrack.domain.model

import com.sktech.wastetrack.R

data class Certificate(
    val id: String,
    val transferId: String,
    val factoryId: String,
    val type: CertificateType,
    val pdfUri: String? = null,
    val jsonPayload: String = "",
    val digitalSignature: String = "",
    val status: CertificateStatus = CertificateStatus.DRAFT,
    val generatedAt: Long = System.currentTimeMillis(),
    val submittedAt: Long? = null
)

enum class CertificateStatus(val displayName: String, val nameRes: Int) {
    DRAFT("Draft", R.string.cert_status_draft),
    GENERATED("Generated", R.string.cert_status_generated),
    SUBMITTED("Submitted", R.string.cert_status_submitted),
    ACCEPTED("Accepted", R.string.cert_status_accepted)
}

