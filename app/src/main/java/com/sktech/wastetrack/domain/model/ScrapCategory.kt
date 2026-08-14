package com.sktech.wastetrack.domain.model

import com.sktech.wastetrack.R

enum class ScrapCategory(
    val displayName: String,
    val icon: String,
    val nameRes: Int,
    val subCategories: List<String>
) {
    METAL(
        "Metal", "🔩", R.string.cat_metal,
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
        "Plastic", "♻️", R.string.cat_plastic,
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
        "Rubber", "⚫", R.string.cat_rubber,
        listOf(
            "Tire Shreds / Whole Tires",
            "Conveyor Belting",
            "Industrial Hoses & Seals",
            "Synthetic Rubber Scrap"
        )
    ),
    EWASTE(
        "E-Waste", "💻", R.string.cat_ewaste,
        listOf(
            "Printed Circuit Boards (PCB)",
            "Li-ion / Lead-Acid Batteries",
            "Hard Drives & Server Racks",
            "Copper Coils & Transformers",
            "Display Panels & Monitors"
        )
    ),
    CHEMICAL(
        "Chemical", "⚗️", R.string.cat_chemical,
        listOf(
            "Spent Solvent / Oil",
            "Acidic / Alkaline Residue",
            "Paint & Sludge Scrap",
            "Industrial Coolant"
        )
    ),
    WOOD(
        "Wood", "🪵", R.string.cat_wood,
        listOf(
            "Wooden Pallets",
            "Sawdust & Shavings",
            "Plywood & MDF Offcuts",
            "Untreated Timber Scrap"
        )
    ),
    PAPER(
        "Paper", "📄", R.string.cat_paper,
        listOf(
            "Corrugated Cardboard (OCC)",
            "Office White Paper Shreds",
            "Newsprint Scrap",
            "Kraft Paper Rolls"
        )
    ),
    GLASS(
        "Glass", "🪟", R.string.cat_glass,
        listOf(
            "Clear Cullet Glass",
            "Amber / Green Bottles",
            "Laminated Window Glass",
            "Laboratory Glassware"
        )
    ),
    OTHER(
        "Other", "📦", R.string.cat_other,
        listOf(
            "Textiles & Fabric Rags",
            "Mixed Solid Scrap",
            "Construction Debris",
            "Refuse Derived Fuel (RDF)"
        )
    )
}

