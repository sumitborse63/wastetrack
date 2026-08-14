package com.sktech.wastetrack.domain.model

import com.sktech.wastetrack.R

enum class UserRole(val displayName: String, val nameRes: Int) {
    SUPERVISOR("Floor Supervisor", R.string.role_supervisor),
    DRIVER("Truck Driver", R.string.role_driver),
    ADMIN("Factory Admin", R.string.role_admin),
    RECYCLER("Recycler", R.string.role_recycler)
}

