package com.sktech.wastetrack.domain.model

enum class ScrapCategory(val displayName: String, val icon: String) {
    METAL("Metal", "🔩"),
    PLASTIC("Plastic", "♻️"),
    RUBBER("Rubber", "⚫"),
    EWASTE("E-Waste", "💻"),
    CHEMICAL("Chemical", "⚗️"),
    WOOD("Wood", "🪵"),
    PAPER("Paper", "📄"),
    GLASS("Glass", "🪟"),
    OTHER("Other", "📦")
}
