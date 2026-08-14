package com.sktech.wastetrack.domain.model

import com.sktech.wastetrack.R

enum class ScrapCategory(
    val displayName: String,
    val icon: String,
    val sampleImageUrl: String,
    val nameRes: Int,
    val subCategories: List<String>
) {
    METAL(
        "Metal", "🔩",
        "https://images.unsplash.com/photo-1587293852726-70cdb56c2866?w=600&auto=format&fit=crop&q=80",
        R.string.cat_metal,
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
        "https://images.unsplash.com/photo-1530587191325-3db32d826c18?w=600&auto=format&fit=crop&q=80",
        R.string.cat_plastic,
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
        "https://images.unsplash.com/photo-1578844251758-2f71da64c96f?w=600&auto=format&fit=crop&q=80",
        R.string.cat_rubber,
        listOf(
            "Tire Shreds / Whole Tires",
            "Conveyor Belting",
            "Industrial Hoses & Seals",
            "Synthetic Rubber Scrap"
        )
    ),
    EWASTE(
        "E-Waste", "💻",
        "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&auto=format&fit=crop&q=80",
        R.string.cat_ewaste,
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
        "https://images.unsplash.com/photo-1603555501671-8f96b3fce8b4?w=600&auto=format&fit=crop&q=80",
        R.string.cat_chemical,
        listOf(
            "Spent Solvent / Oil",
            "Acidic / Alkaline Residue",
            "Paint & Sludge Scrap",
            "Industrial Coolant"
        )
    ),
    WOOD(
        "Wood", "🪵",
        "https://images.unsplash.com/photo-1589939705384-5185137a7f0f?w=600&auto=format&fit=crop&q=80",
        R.string.cat_wood,
        listOf(
            "Wooden Pallets",
            "Sawdust & Shavings",
            "Plywood & MDF Offcuts",
            "Untreated Timber Scrap"
        )
    ),
    PAPER(
        "Paper", "📄",
        "https://images.unsplash.com/photo-1607344645866-009c320c5ab8?w=600&auto=format&fit=crop&q=80",
        R.string.cat_paper,
        listOf(
            "Corrugated Cardboard (OCC)",
            "Office White Paper Shreds",
            "Newsprint Scrap",
            "Kraft Paper Rolls"
        )
    ),
    GLASS(
        "Glass", "🪟",
        "https://images.unsplash.com/photo-1514782831304-632d84503f6f?w=600&auto=format&fit=crop&q=80",
        R.string.cat_glass,
        listOf(
            "Clear Cullet Glass",
            "Amber / Green Bottles",
            "Laminated Window Glass",
            "Laboratory Glassware"
        )
    ),
    OTHER(
        "Other", "📦",
        "https://images.unsplash.com/photo-1532996122724-e3c354a0b15b?w=600&auto=format&fit=crop&q=80",
        R.string.cat_other,
        listOf(
            "Textiles & Fabric Rags",
            "Mixed Solid Scrap",
            "Construction Debris",
            "Refuse Derived Fuel (RDF)"
        )
    )
}
