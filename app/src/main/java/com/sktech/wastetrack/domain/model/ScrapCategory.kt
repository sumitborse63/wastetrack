package com.sktech.wastetrack.domain.model

enum class ScrapCategory(
    val displayName: String,
    val icon: String,
    val subCategories: List<String>
) {
    METAL(
        "Metal", "🔩",
        listOf(
            "Copper Wire / Cable",
            "Aluminum Ingot / Extrusion",
            "Brass Scrap",
            "Heavy Melting Steel (HMS)",
            "Stainless Steel (304/316)",
            "Cast Iron",
            "Mild Steel Sheets",
            "Lead / Battery Plates",
            "Zinc Scrap"
        )
    ),
    PLASTIC(
        "Plastic", "♻️",
        listOf(
            "PET Bottles & Sheets",
            "HDPE Drums & Containers",
            "PVC Pipes & Sheaths",
            "LDPE Film & Wrap",
            "PP Moulded Scrap",
            "ABS Electronic Casings"
        )
    ),
    RUBBER(
        "Rubber", "⚫",
        listOf(
            "Tire Shreds / Whole Tires",
            "Conveyor Belting",
            "Industrial Hoses & Seals",
            "Synthetic Rubber Scrap"
        )
    ),
    EWASTE(
        "E-Waste", "💻",
        listOf(
            "Printed Circuit Boards (PCB)",
            "Li-ion / Lead-Acid Batteries",
            "Hard Drives & Server Racks",
            "Copper Coils & Transformers",
            "Display Panels & Monitors"
        )
    ),
    CHEMICAL(
        "Chemical", "⚗️",
        listOf(
            "Spent Solvent / Oil",
            "Acidic / Alkaline Residue",
            "Paint & Sludge Scrap",
            "Industrial Coolant"
        )
    ),
    WOOD(
        "Wood", "🪵",
        listOf(
            "Wooden Pallets",
            "Sawdust & Shavings",
            "Plywood & MDF Offcuts",
            "Untreated Timber Scrap"
        )
    ),
    PAPER(
        "Paper", "📄",
        listOf(
            "Corrugated Cardboard (OCC)",
            "Office White Paper Shreds",
            "Newsprint Scrap",
            "Kraft Paper Rolls"
        )
    ),
    GLASS(
        "Glass", "🪟",
        listOf(
            "Clear Cullet Glass",
            "Amber / Green Bottles",
            "Laminated Window Glass",
            "Laboratory Glassware"
        )
    ),
    OTHER(
        "Other", "📦",
        listOf(
            "Textiles & Fabric Rags",
            "Mixed Solid Scrap",
            "Construction Debris",
            "Refuse Derived Fuel (RDF)"
        )
    )
}

