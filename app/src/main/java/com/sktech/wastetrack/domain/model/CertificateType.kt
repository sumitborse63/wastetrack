package com.sktech.wastetrack.domain.model

import com.sktech.wastetrack.R

enum class CertificateType(val displayName: String, val nameRes: Int) {
    MPCB_DISPOSAL("MPCB Disposal Certificate", R.string.cert_type_mpcb),
    ESG_CREDIT("ESG Credit Certificate", R.string.cert_type_esg),
    AUDIT_REPORT("Audit Report", R.string.cert_type_audit)
}

