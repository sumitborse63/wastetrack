package com.sktech.wastetrack.domain.model

import com.sktech.wastetrack.R

enum class UserRole(
    val displayName: String,
    val fullName: String,
    val nameRes: Int,
    val descRes: Int
) {
    SUPERVISOR(
        displayName = "Plant Supervisor",
        fullName = "Factory Floor Supervisor & Plant Manager",
        nameRes = R.string.role_supervisor,
        descRes = R.string.role_supervisor_desc
    ),
    RECYCLER(
        displayName = "Authorized Recycler",
        fullName = "Authorized Recycling Agency & Procurement Partner",
        nameRes = R.string.role_recycler,
        descRes = R.string.role_recycler_desc
    ),
    DRIVER(
        displayName = "Fleet Driver",
        fullName = "Logistics Carrier & Fleet Truck Driver",
        nameRes = R.string.role_driver,
        descRes = R.string.role_driver_desc
    ),
    ADMIN(
        displayName = "Factory Admin",
        fullName = "Industrial Plant Administrator",
        nameRes = R.string.role_admin,
        descRes = R.string.role_admin_desc
    )
}

