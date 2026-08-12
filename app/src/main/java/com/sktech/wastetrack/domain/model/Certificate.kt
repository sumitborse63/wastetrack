package com.sktech.wastetrack.domain.model

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

enum class CertificateStatus(val displayName: String) {
    DRAFT("Draft"),
    GENERATED("Generated"),
    SUBMITTED("Submitted"),
    ACCEPTED("Accepted")
}
