package com.sktech.wastetrack.domain.model

enum class UserRole(val displayName: String) {
    SUPERVISOR("Floor Supervisor"),
    DRIVER("Truck Driver"),
    ADMIN("Factory Admin"),
    RECYCLER("Recycler")
}
