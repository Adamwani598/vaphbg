package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * High-quality architectural building templates for various building types in Kenya and East Africa.
 * Users can browse, preview, and fully customize any template (dimensions, county, finishes, roofing, extra wings).
 */
data class BuildingTemplate(
    val id: String,
    val title: String,
    val category: String, // Bungalow, Maisonette, Residential, Commercial, Apartment, Storey Building, Custom
    val subtitle: String,
    val description: String,
    val baseAreaSqm: Float,
    val bedrooms: Int,
    val bathrooms: Int,
    val recommendedPlot: String, // e.g. "50x100 ft (1/8 Acre)"
    val estimatedCostMinKes: Double,
    val estimatedCostMaxKes: Double,
    val recommendedRoof: String, // Mabati, Decra Tiles, Flat Slab, Shingles
    val recommendedFinish: String, // Nairobi Stone, Smooth Plaster, Brick, Timber
    val highlights: List<String>,
    val defaultRooms: List<RoomItem>,
    val badge: String = "Popular",
    val defaultStoreys: Int = 1,
    val minStoreys: Int = 1,
    val maxStoreys: Int = 1,
    val isMultiStorey: Boolean = false,
    val typicalFloorRooms: List<RoomItem> = emptyList(),
    val penthouseRooms: List<RoomItem> = emptyList(),
    val structuralEngineeringNotes: String = ""
)

object BuildingTemplateCatalog {

    val templates = listOf(
        BuildingTemplate(
            id = "template_maisonette_4bed",
            title = "Modern 4-Bedroom Maisonette",
            category = "Maisonette",
            subtitle = "Spacious Double-Storey Family Home with DSQ",
            description = "A double-storey contemporary Kenyan residence featuring a double-volume foyer, open-plan dining and lounge, ensuite guest wing on the ground floor, and three expansive bedrooms with a private family TV lounge on the upper level.",
            baseAreaSqm = 165f,
            bedrooms = 4,
            bathrooms = 4,
            recommendedPlot = "50x100 ft (1/8 Acre)",
            estimatedCostMinKes = 6800000.0,
            estimatedCostMaxKes = 8500000.0,
            recommendedRoof = "Decra Tiles",
            recommendedFinish = "Nairobi Blue Stone",
            highlights = listOf(
                "Double-Volume Ceiling Foyer",
                "Master Suite with Private Balcony",
                "Ground Floor Guest Bedroom Ensuite",
                "Detached Domestic Servants Quarter (DSQ)",
                "Separate Laundry & Pantry Yard"
            ),
            badge = "Featured",
            defaultRooms = listOf(
                RoomItem("Living Room & Foyer", 6.5f, 5.0f, 0f, 0f, "#2196F3"),
                RoomItem("Dining Area", 4.0f, 3.5f, 6.5f, 0f, "#00BCD4"),
                RoomItem("Chef Kitchen", 4.0f, 4.0f, 6.5f, 3.5f, "#4CAF50"),
                RoomItem("Guest Bedroom (Ensuite)", 4.2f, 3.8f, 0f, 5.0f, "#9C27B0"),
                RoomItem("Guest Bath", 2.3f, 2.0f, 4.2f, 5.0f, "#FF5722"),
                RoomItem("Master Suite & Balcony", 5.5f, 4.5f, 0f, 8.8f, "#E91E63"),
                RoomItem("Master Bath & Wardrobe", 3.5f, 3.0f, 5.5f, 8.8f, "#E040FB"),
                RoomItem("Bedroom 2", 4.0f, 3.8f, 9.0f, 8.8f, "#3F51B5"),
                RoomItem("Bedroom 3", 4.0f, 3.8f, 9.0f, 12.6f, "#009688"),
                RoomItem("Family TV Lounge", 4.5f, 3.8f, 4.5f, 12.6f, "#FF9800"),
                RoomItem("Common Bath", 2.2f, 2.2f, 2.3f, 12.6f, "#607D8B"),
                RoomItem("Verandah Porch", 6.5f, 2.0f, 0f, 16.4f, "#795548")
            )
        ),

        BuildingTemplate(
            id = "template_bungalow_3bed",
            title = "Classic 3-Bedroom Kenyan Bungalow",
            category = "Bungalow",
            subtitle = "Cost-Effective Single-Storey Masterpiece",
            description = "The quintessential Kenyan family home optimized for standard 50x100 plots. Delivers high spatial efficiency with a wide shade verandah, open lounge, spacious master ensuite, and convenient laundry access.",
            baseAreaSqm = 115f,
            bedrooms = 3,
            bathrooms = 2,
            recommendedPlot = "50x100 ft or 40x80 ft",
            estimatedCostMinKes = 4200000.0,
            estimatedCostMaxKes = 5600000.0,
            recommendedRoof = "Hip Roof Mabati",
            recommendedFinish = "Smooth Plaster",
            highlights = listOf(
                "Spacious Front Shaded Verandah",
                "Master Bedroom with Walk-in Shower",
                "Open-Plan Living & Dining Flow",
                "Dedicated Kitchen Pantry & Store",
                "Natural Cross-Ventilation Design"
            ),
            badge = "Most Popular",
            defaultRooms = listOf(
                RoomItem("Living Room", 5.5f, 4.5f, 0f, 0f, "#2196F3"),
                RoomItem("Dining Space", 3.5f, 3.5f, 5.5f, 0f, "#00BCD4"),
                RoomItem("Kitchen", 3.5f, 3.5f, 5.5f, 3.5f, "#4CAF50"),
                RoomItem("Master Bedroom", 4.5f, 4.0f, 0f, 4.5f, "#9C27B0"),
                RoomItem("Master Bath", 2.2f, 2.0f, 4.5f, 4.5f, "#FF5722"),
                RoomItem("Bedroom 2", 3.8f, 3.5f, 0f, 8.5f, "#E91E63"),
                RoomItem("Bedroom 3", 3.8f, 3.5f, 3.8f, 8.5f, "#3F51B5"),
                RoomItem("Common Bath", 2.4f, 2.0f, 6.7f, 7.0f, "#FF9800"),
                RoomItem("Front Verandah", 5.5f, 2.0f, 0f, 12.0f, "#795548")
            )
        ),

        BuildingTemplate(
            id = "template_villa_5bed",
            title = "Executive 5-Bedroom Luxury Villa",
            category = "Residential",
            subtitle = "Prestige Grand Villa with Rooftop Terrace",
            description = "High-end executive mansion engineered for luxury living on 1/4 acre to 1/2 acre plots. Features a grand sunken lounge, formal dining, chef's island kitchen, home study/library, private cinema/gym space, and rooftop entertainment terrace.",
            baseAreaSqm = 240f,
            bedrooms = 5,
            bathrooms = 5,
            recommendedPlot = "1/4 Acre or 100x100 ft",
            estimatedCostMinKes = 11500000.0,
            estimatedCostMaxKes = 15000000.0,
            recommendedRoof = "Flat Concrete Slab",
            recommendedFinish = "Coral Stone & Timber",
            highlights = listOf(
                "Grand Sunken Lounge with Fireplace",
                "Rooftop Sunset Lounge & Barbeque Deck",
                "All 5 Bedrooms with Ensuite Bathrooms",
                "Executive Home Office / Library",
                "2-Car Covered Portico & DSQ"
            ),
            badge = "Luxury",
            defaultRooms = listOf(
                RoomItem("Grand Sunken Lounge", 7.0f, 5.5f, 0f, 0f, "#2196F3"),
                RoomItem("Formal Dining", 4.5f, 4.0f, 7.0f, 0f, "#00BCD4"),
                RoomItem("Island Kitchen & Pantry", 5.0f, 4.5f, 7.0f, 4.0f, "#4CAF50"),
                RoomItem("Home Office / Study", 3.8f, 3.5f, 0f, 5.5f, "#607D8B"),
                RoomItem("Ground Guest Suite", 4.5f, 4.0f, 3.8f, 5.5f, "#9C27B0"),
                RoomItem("Guest Ensuite Bath", 2.5f, 2.2f, 8.3f, 5.5f, "#FF5722"),
                RoomItem("Master Palace Suite", 6.5f, 5.0f, 0f, 9.5f, "#E91E63"),
                RoomItem("Master Walk-in & Jacuzzi", 4.5f, 3.5f, 6.5f, 9.5f, "#E040FB"),
                RoomItem("Bedroom 3 (Ensuite)", 4.2f, 3.8f, 0f, 14.5f, "#3F51B5"),
                RoomItem("Bedroom 4 (Ensuite)", 4.2f, 3.8f, 4.2f, 14.5f, "#009688"),
                RoomItem("Bedroom 5 / Gym", 4.0f, 3.8f, 8.4f, 14.5f, "#FF9800"),
                RoomItem("Rooftop Lounge Terrace", 8.0f, 4.0f, 0f, 18.3f, "#795548")
            )
        ),

        BuildingTemplate(
            id = "template_starter_2bed",
            title = "Compact 2-Bedroom Starter Home",
            category = "Bungalow",
            subtitle = "Smart Budget Residential Layout",
            description = "An ultra-efficient residential plan engineered for low construction cost and fast build timelines. Ideal for young families, rental cottages, or phased construction with future extension capability.",
            baseAreaSqm = 68f,
            bedrooms = 2,
            bathrooms = 1,
            recommendedPlot = "40x60 ft or 50x100 ft",
            estimatedCostMinKes = 2300000.0,
            estimatedCostMaxKes = 3100000.0,
            recommendedRoof = "Mabati Box Profile",
            recommendedFinish = "Smooth Plaster",
            highlights = listOf(
                "Low-Cost High-Yield Architecture",
                "Central Plumbing Core for Cost Savings",
                "Spacious Living Room with Breakfast Bar",
                "Future Horizontal Expansion Friendly",
                "High Energy Efficiency"
            ),
            badge = "Affordable",
            defaultRooms = listOf(
                RoomItem("Living Room & Diner", 5.0f, 4.0f, 0f, 0f, "#2196F3"),
                RoomItem("Kitchenette", 3.0f, 3.0f, 5.0f, 0f, "#4CAF50"),
                RoomItem("Store / Utility", 1.8f, 2.0f, 5.0f, 3.0f, "#607D8B"),
                RoomItem("Master Bedroom", 3.8f, 3.5f, 0f, 4.0f, "#9C27B0"),
                RoomItem("Bedroom 2", 3.2f, 3.5f, 3.8f, 4.0f, "#E91E63"),
                RoomItem("Bathroom", 2.2f, 1.8f, 5.0f, 5.0f, "#FF9800"),
                RoomItem("Entry Porch", 2.5f, 1.5f, 0f, 7.5f, "#795548")
            )
        ),

        BuildingTemplate(
            id = "template_apartments_rental",
            title = "Multi-Unit Rental Apartments Block",
            category = "Apartment",
            subtitle = "High-Yield 4-Unit Floor Layout (Bedsitters & 1-Beds)",
            description = "A commercial residential rental layout comprising two 1-bedroom apartments and two modern studio bedsitters per floor. Optimized for urban centers, satellite towns, and student/corporate housing.",
            baseAreaSqm = 180f,
            bedrooms = 6,
            bathrooms = 4,
            recommendedPlot = "50x100 ft Commercial Plot",
            estimatedCostMinKes = 7500000.0,
            estimatedCostMaxKes = 9800000.0,
            recommendedRoof = "Flat Concrete Slab",
            recommendedFinish = "Machine Cut Stone",
            highlights = listOf(
                "High Monthly Rental ROI",
                "Self-Contained Studios with Kitchenettes",
                "Central Secure Staircase & Lobby",
                "Individual Water & Power Sub-metering",
                "Roof Clothes Drying & Tank Deck"
            ),
            badge = "Commercial ROI",
            defaultRooms = listOf(
                RoomItem("Apt 1: Living & Bed", 4.8f, 3.8f, 0f, 0f, "#2196F3"),
                RoomItem("Apt 1: Kitchen & Bath", 2.5f, 3.8f, 4.8f, 0f, "#4CAF50"),
                RoomItem("Apt 2: Studio Bedsitter", 4.2f, 3.8f, 7.3f, 0f, "#9C27B0"),
                RoomItem("Apt 2: Bath", 1.8f, 2.0f, 11.5f, 0f, "#FF5722"),
                RoomItem("Central Corridor & Stairs", 13.3f, 2.0f, 0f, 3.8f, "#607D8B"),
                RoomItem("Apt 3: 1-Bed Lounge", 4.5f, 3.8f, 0f, 5.8f, "#00BCD4"),
                RoomItem("Apt 3: Bedroom", 3.8f, 3.8f, 4.5f, 5.8f, "#E91E63"),
                RoomItem("Apt 3: Bath & Kitchen", 2.5f, 3.8f, 8.3f, 5.8f, "#FF9800"),
                RoomItem("Apt 4: Studio Bedsitter", 4.5f, 3.8f, 10.8f, 5.8f, "#3F51B5"),
                RoomItem("Apt 4: Bath", 1.8f, 2.0f, 13.3f, 5.8f, "#E040FB")
            )
        ),

        BuildingTemplate(
            id = "template_commercial_plaza",
            title = "Commercial Strip Plaza & Retail Shops",
            category = "Commercial",
            subtitle = "4 Road-Frontage Retail Units + Office & Washrooms",
            description = "A modern single-storey roadside retail commercial building designed for shopping centers, market centers, and busy high-traffic road frontage plots. Includes 4 lockable shops, an administration/security office, and public washrooms.",
            baseAreaSqm = 145f,
            bedrooms = 0,
            bathrooms = 3,
            recommendedPlot = "50x100 ft or Commercial Corner Plot",
            estimatedCostMinKes = 5200000.0,
            estimatedCostMaxKes = 6900000.0,
            recommendedRoof = "Parapet Hidden Roof",
            recommendedFinish = "Polished Plaster & Tile",
            highlights = listOf(
                "4 Wide Glass Storefront Units",
                "Covered Customer Walkway Arcades",
                "Dedicated Administration & Security Office",
                "Separate Male/Female Public Washrooms",
                "Rear Loading & Offloading Gate"
            ),
            badge = "Commercial",
            defaultRooms = listOf(
                RoomItem("Retail Shop 1", 4.5f, 5.5f, 0f, 0f, "#2196F3"),
                RoomItem("Retail Shop 2", 4.5f, 5.5f, 4.5f, 0f, "#00BCD4"),
                RoomItem("Retail Shop 3", 4.5f, 5.5f, 9.0f, 0f, "#4CAF50"),
                RoomItem("Retail Shop 4", 4.5f, 5.5f, 13.5f, 0f, "#9C27B0"),
                RoomItem("Admin & Security Office", 3.5f, 3.0f, 0f, 5.5f, "#607D8B"),
                RoomItem("Customer Washrooms", 3.5f, 2.5f, 3.5f, 5.5f, "#FF9800"),
                RoomItem("Storage & Utility", 4.0f, 3.0f, 7.0f, 5.5f, "#795548"),
                RoomItem("Arcade Covered Walkway", 18.0f, 2.0f, 0f, 8.5f, "#E91E63")
            )
        ),

        BuildingTemplate(
            id = "template_farmhouse_country",
            title = "Contemporary Country Farmhouse",
            category = "Residential",
            subtitle = "Upcountry Residence with Wrap-around Deck & Solar Room",
            description = "A charming rustic yet modern farmhouse designed for countryside living and agro-tourism. Features a high vaulted ceiling fireplace lounge, farm pantry, wrap-around veranda, and dedicated solar battery & agricultural store.",
            baseAreaSqm = 140f,
            bedrooms = 3,
            bathrooms = 2,
            recommendedPlot = "1/4 Acre to 5 Acres Farm",
            estimatedCostMinKes = 5400000.0,
            estimatedCostMaxKes = 7200000.0,
            recommendedRoof = "Steep Pitch Mabati",
            recommendedFinish = "Rustic Brick & Timber",
            highlights = listOf(
                "Spacious Wrap-Around Viewing Deck",
                "High Vaulted Lounge with Fireplace",
                "Country Kitchen with Food Pantry",
                "Dedicated Solar Inverter Room",
                "Outdoor Mudroom & Wash Area"
            ),
            badge = "Country Living",
            defaultRooms = listOf(
                RoomItem("Vaulted Farmhouse Lounge", 6.0f, 5.0f, 0f, 0f, "#2196F3"),
                RoomItem("Dining Hall", 4.0f, 3.5f, 6.0f, 0f, "#00BCD4"),
                RoomItem("Country Kitchen & Pantry", 4.5f, 4.0f, 6.0f, 3.5f, "#4CAF50"),
                RoomItem("Master Suite", 4.8f, 4.2f, 0f, 5.0f, "#9C27B0"),
                RoomItem("Master Bath", 2.5f, 2.2f, 4.8f, 5.0f, "#FF5722"),
                RoomItem("Bedroom 2", 3.8f, 3.8f, 0f, 9.2f, "#E91E63"),
                RoomItem("Bedroom 3", 3.8f, 3.8f, 3.8f, 9.2f, "#3F51B5"),
                RoomItem("Solar & Battery Inverter Room", 2.5f, 2.2f, 7.6f, 9.2f, "#FF9800"),
                RoomItem("Mudroom & Wash", 2.5f, 2.0f, 7.6f, 11.4f, "#607D8B"),
                RoomItem("Wrap-around Timber Deck", 10.5f, 2.5f, 0f, 13.4f, "#795548")
            )
        ),

        BuildingTemplate(
            id = "template_storey_tower_10",
            title = "10-Storey High-Rise Multi-Unit Tower",
            category = "Storey Building",
            subtitle = "High-Density Urban Residential Tower (1 to 10 Storeys)",
            description = "A comprehensive multi-storey architectural blueprint engineered for high-density urban plots across Kenyan metropolitan centers (Nairobi Kilimani/Westlands, Mombasa, Kisumu, Nakuru, Kiambu). Fully configurable from 1 up to a maximum of 10 storeys. Features ground-floor commercial reception, security concierge atrium, dual high-speed lift shafts, pressurized fire escape stairwells, typical multi-unit residential apartment floors on typical levels (1-9), and a panoramic executive rooftop penthouse with sky deck on Level 10. Conforms strictly with NCA high-rise structural regulations, including wet riser firefighting provisions, shear wall reinforced concrete cores, and dedicated transformer/generator rooms.",
            baseAreaSqm = 260f,
            bedrooms = 24,
            bathrooms = 18,
            recommendedPlot = "50x100 ft or 100x100 ft Commercial / High-Density Plot",
            estimatedCostMinKes = 24500000.0,
            estimatedCostMaxKes = 128000000.0,
            recommendedRoof = "Flat Concrete Slab & Rooftop Terrace",
            recommendedFinish = "Reinforced Concrete & Glass Curtain Wall",
            highlights = listOf(
                "Configurable from 1 up to a Maximum of 10 Storeys",
                "Dual High-Speed Passenger Elevator / Lift Shafts",
                "Dual Pressurized Fire Escape Staircases (East & West Wings)",
                "Ground Floor Reception, Security Desk & Commercial Retail",
                "NCA High-Rise Engineering: Wet Riser & Standby Generator",
                "10th Floor Executive Sky Penthouse & Rooftop Lounge"
            ),
            badge = "Up to 10 Storeys",
            defaultStoreys = 6,
            minStoreys = 1,
            maxStoreys = 10,
            isMultiStorey = true,
            structuralEngineeringNotes = "NCA Class A/B High-Rise standard: Reinforced concrete frame with C30/37 structural columns, core shear walls around lift and stairwell shafts, and suspended reinforced concrete slabs. Buildings exceeding 4 storeys require passenger lifts and dual fire exits pursuant to the Physical and Land Use Planning Act and Kenyan Building Code.",
            defaultRooms = listOf(
                RoomItem("Grand Entrance Lobby & Atrium", 6.5f, 4.5f, 0f, 0f, "#2196F3"),
                RoomItem("Security & Concierge Desk", 3.0f, 2.5f, 6.5f, 0f, "#00BCD4"),
                RoomItem("Dual High-Speed Lift Core", 3.5f, 2.5f, 9.5f, 0f, "#9C27B0"),
                RoomItem("Fire Escape Staircase 1", 2.8f, 4.5f, 13.0f, 0f, "#E91E63"),
                RoomItem("Commercial Retail Shop 1", 6.0f, 5.5f, 0f, 4.5f, "#4CAF50"),
                RoomItem("Commercial Retail Shop 2", 6.0f, 5.5f, 6.0f, 4.5f, "#8BC34A"),
                RoomItem("Building Management Office", 3.8f, 3.5f, 12.0f, 4.5f, "#607D8B"),
                RoomItem("Fire Escape Staircase 2", 2.8f, 4.5f, 13.0f, 8.0f, "#F44336"),
                RoomItem("MEP Generator & Transformer Room", 4.0f, 3.5f, 0f, 10.0f, "#FF9800"),
                RoomItem("Public Restrooms (M/F)", 3.5f, 2.5f, 4.0f, 10.0f, "#FF5722")
            ),
            typicalFloorRooms = listOf(
                RoomItem("Central Lift & Service Lobby", 4.0f, 3.2f, 5.5f, 0f, "#9C27B0"),
                RoomItem("Fire Escape Stairs West", 2.8f, 4.5f, 0f, 0f, "#E91E63"),
                RoomItem("Fire Escape Stairs East", 2.8f, 4.5f, 12.2f, 0f, "#F44336"),
                RoomItem("Apt A: 2-Bed Living & Dining", 5.5f, 4.5f, 0f, 4.5f, "#2196F3"),
                RoomItem("Apt A: Kitchen & Balcony", 3.5f, 3.0f, 5.5f, 4.5f, "#00BCD4"),
                RoomItem("Apt A: Master Ensuite", 4.2f, 3.8f, 0f, 9.0f, "#3F51B5"),
                RoomItem("Apt B: 2-Bed Living & Dining", 5.5f, 4.5f, 9.5f, 4.5f, "#4CAF50"),
                RoomItem("Apt B: Master Ensuite", 4.2f, 3.8f, 9.5f, 9.0f, "#009688"),
                RoomItem("Apt C: 1-Bed Studio Suite", 4.5f, 3.8f, 4.5f, 8.5f, "#FF9800"),
                RoomItem("Common Utility & Refuse Chute", 2.5f, 2.2f, 9.5f, 0f, "#607D8B")
            ),
            penthouseRooms = listOf(
                RoomItem("Executive Sky Lounge", 7.5f, 5.5f, 0f, 0f, "#2196F3"),
                RoomItem("Private Panoramic Terrace Deck", 8.0f, 4.0f, 7.5f, 0f, "#795548"),
                RoomItem("Penthouse Master Suite", 5.5f, 5.0f, 0f, 5.5f, "#E91E63"),
                RoomItem("Master Jacuzzi Spa & Bath", 3.5f, 3.0f, 5.5f, 5.5f, "#E040FB"),
                RoomItem("Central Lift Foyer", 3.5f, 2.5f, 9.0f, 4.0f, "#9C27B0"),
                RoomItem("Fire Escape Access", 2.8f, 4.5f, 12.5f, 4.0f, "#F44336"),
                RoomItem("Rooftop Water Reservoir & Solar Core", 4.5f, 3.5f, 0f, 10.5f, "#FF9800")
            )
        ),

        BuildingTemplate(
            id = "template_commercial_storey_tower_10",
            title = "10-Storey Commercial & Office Tower",
            category = "Storey Building",
            subtitle = "Grade-A Commercial Plaza & Corporate Suites (1 to 10 Storeys)",
            description = "A premier turnkey commercial office tower blueprint engineered up to 10 storeys. Features ground banking hall with double-height entrance atrium, flexible column-free open-plan office plates from 2nd to 9th floors, dual passenger elevators plus a dedicated goods service hoist, and a 10th floor executive boardroom with private rooftop terrace. Fully compliant with NCA commercial zoning, MEP riser ducting, and disabled-accessible washrooms on all levels.",
            baseAreaSqm = 300f,
            bedrooms = 0,
            bathrooms = 20,
            recommendedPlot = "Commercial High-Density Plot (1/4 to 1/2 Acre)",
            estimatedCostMinKes = 29000000.0,
            estimatedCostMaxKes = 152000000.0,
            recommendedRoof = "Flat Concrete Slab & Solar PV Deck",
            recommendedFinish = "Granite Cladding & Structural Glazing",
            highlights = listOf(
                "Configurable up to 10 Commercial Storeys",
                "Column-Free Open-Plan Floor Plates",
                "Dual Passenger Lifts + Goods Service Hoist",
                "Ground Floor Banking Hall & Retail Arcade",
                "Level 10 Executive Boardroom & Sky Conference",
                "Basement & Stilt Parking Integration"
            ),
            badge = "10-Storey Commercial",
            defaultStoreys = 8,
            minStoreys = 1,
            maxStoreys = 10,
            isMultiStorey = true,
            structuralEngineeringNotes = "Eurocode / BS 8110 reinforced concrete design for wind and seismic loading up to 10 storeys. Features basement/ground vehicular access and integrated wet riser fire suppression system.",
            defaultRooms = listOf(
                RoomItem("Grand Banking Hall / Atrium", 8.0f, 6.0f, 0f, 0f, "#2196F3"),
                RoomItem("Customer Service & Teller Desks", 5.5f, 4.0f, 8.0f, 0f, "#00BCD4"),
                RoomItem("Central Core Lift Bank (Dual Lifts)", 4.0f, 3.0f, 13.5f, 0f, "#9C27B0"),
                RoomItem("North Fire Escape Stairwell", 3.0f, 4.5f, 13.5f, 3.0f, "#E91E63"),
                RoomItem("Retail Arcade Suite 1", 6.0f, 5.0f, 0f, 6.0f, "#4CAF50"),
                RoomItem("Retail Arcade Suite 2", 6.0f, 5.0f, 6.0f, 6.0f, "#8BC34A"),
                RoomItem("Security & CCTV Dispatch Center", 3.5f, 3.0f, 12.0f, 7.5f, "#607D8B"),
                RoomItem("Public Restrooms (M/F/Accessible)", 4.0f, 3.0f, 0f, 11.0f, "#FF9800"),
                RoomItem("MEP Transformer & Generator Room", 4.5f, 3.5f, 4.0f, 11.0f, "#795548")
            ),
            typicalFloorRooms = listOf(
                RoomItem("Open-Plan Office Wing East", 8.5f, 6.5f, 0f, 0f, "#2196F3"),
                RoomItem("Open-Plan Office Wing West", 8.5f, 6.5f, 8.5f, 0f, "#00BCD4"),
                RoomItem("Executive Meeting Room", 4.5f, 3.5f, 0f, 6.5f, "#4CAF50"),
                RoomItem("Central Lift Lobby", 4.0f, 3.0f, 4.5f, 6.5f, "#9C27B0"),
                RoomItem("North Fire Escape", 3.0f, 4.5f, 14.0f, 6.5f, "#E91E63"),
                RoomItem("Kitchenette & Breakroom", 3.5f, 3.0f, 8.5f, 6.5f, "#FF9800"),
                RoomItem("Executive Restrooms", 4.0f, 3.0f, 0f, 10.0f, "#FF5722"),
                RoomItem("Server & IT Telecom Room", 3.0f, 2.5f, 4.0f, 10.0f, "#607D8B")
            ),
            penthouseRooms = listOf(
                RoomItem("Presidential Boardroom", 8.5f, 6.0f, 0f, 0f, "#2196F3"),
                RoomItem("Executive Sky Terrace Deck", 9.0f, 5.0f, 8.5f, 0f, "#795548"),
                RoomItem("VIP Lounge & Private Bar", 5.0f, 4.5f, 0f, 6.0f, "#9C27B0"),
                RoomItem("Private Executive Suite", 4.5f, 4.0f, 5.0f, 6.0f, "#4CAF50"),
                RoomItem("Lift Core & Fire Escape", 4.0f, 3.0f, 9.5f, 5.0f, "#E91E63"),
                RoomItem("Executive Boardroom Restroom", 3.0f, 2.5f, 9.5f, 8.0f, "#FF5722")
            )
        )
    )

    fun getTemplateById(id: String): BuildingTemplate? {
        return templates.find { it.id == id }
    }

    /**
     * Converts a customized template into a Room BlueprintEntity ready for insertion
     */
    fun createBlueprintFromCustomization(
        template: BuildingTemplate,
        customTitle: String,
        customAreaSqm: Float,
        county: String,
        finishesQuality: String,
        roofType: String,
        includedModules: List<String>,
        storeys: Int = template.defaultStoreys
    ): BlueprintEntity {
        val effectiveStoreys = storeys.coerceIn(template.minStoreys, template.maxStoreys)

        // Compute scaled rooms or append customized modules
        val scale = kotlin.math.sqrt((customAreaSqm / template.baseAreaSqm).toDouble()).toFloat()
        
        val modifiedRooms = mutableListOf<RoomItem>()
        template.defaultRooms.forEach { r ->
            modifiedRooms.add(
                r.copy(
                    width = (r.width * scale).roundTo(1),
                    height = (r.height * scale).roundTo(1),
                    x = (r.x * scale).roundTo(1),
                    y = (r.y * scale).roundTo(1)
                )
            )
        }

        // Add selected add-on modules
        val maxX = modifiedRooms.maxOfOrNull { it.x + it.width } ?: 12f
        val maxY = modifiedRooms.maxOfOrNull { it.y + it.height } ?: 10f

        var currentAddonY = maxY

        if (includedModules.contains("DSQ (Servants Quarter)")) {
            modifiedRooms.add(
                RoomItem("DSQ Room & Bath", 4.0f, 3.5f, maxX + 1.0f, 0f, "#795548")
            )
        }

        if (includedModules.contains("Covered Carport")) {
            modifiedRooms.add(
                RoomItem("Covered Carport", 5.5f, 3.2f, 0f, currentAddonY + 0.5f, "#607D8B")
            )
            currentAddonY += 3.7f
        }

        if (includedModules.contains("Solar Inverter Room") || includedModules.contains("Solar PV Rooftop Array")) {
            modifiedRooms.add(
                RoomItem("Solar Inverter & Battery Core", 3.0f, 2.5f, maxX + 1.0f, 4.0f, "#FF9800")
            )
        }

        if (includedModules.contains("Water Tower Base")) {
            modifiedRooms.add(
                RoomItem("Water Tower Base", 2.0f, 2.0f, maxX + 1.0f, 6.5f, "#00BCD4")
            )
        }

        if (includedModules.contains("Dual High-Speed Lifts / Elevators")) {
            modifiedRooms.add(
                RoomItem("Dual Elevator Shafts", 3.8f, 2.8f, maxX + 1.0f, 0f, "#9C27B0")
            )
        }

        if (includedModules.contains("Backup Industrial Generator")) {
            modifiedRooms.add(
                RoomItem("Industrial Generator (250kVA)", 4.0f, 3.0f, maxX + 1.0f, 3.0f, "#FF5722")
            )
        }

        if (includedModules.contains("Basement Parking Ramp")) {
            modifiedRooms.add(
                RoomItem("Basement Parking Access Ramp", 6.0f, 3.5f, 0f, currentAddonY + 0.5f, "#455A64")
            )
            currentAddonY += 4.0f
        }

        if (includedModules.contains("Wet Riser Firefighting Core")) {
            modifiedRooms.add(
                RoomItem("Wet Riser & Fire Pump Station", 3.0f, 2.2f, maxX + 1.0f, 6.5f, "#D32F2F")
            )
        }

        if (includedModules.contains("Rooftop Sky Lounge & Helipad")) {
            modifiedRooms.add(
                RoomItem("Rooftop Sky Lounge Deck", 7.0f, 4.5f, 0f, currentAddonY + 0.5f, "#0288D1")
            )
            currentAddonY += 5.0f
        }

        // Serialize rooms to JSON
        val roomsArray = JSONArray()
        modifiedRooms.forEach { r ->
            val o = JSONObject()
            o.put("name", r.name)
            o.put("width", r.width.toDouble())
            o.put("height", r.height.toDouble())
            o.put("x", r.x.toDouble())
            o.put("y", r.y.toDouble())
            o.put("color", r.color)
            roomsArray.put(o)
        }

        // Footprint area + addon footprint
        val footprintArea = modifiedRooms.sumOf { (it.width * it.height).toDouble() }.toFloat()
        // Multi-storey total gross floor area = footprint * storeys
        val totalGrossArea = if (effectiveStoreys > 1) {
            (footprintArea * effectiveStoreys).roundTo(1)
        } else {
            footprintArea
        }

        // Compute cost with multi-storey structural factoring
        val baseCostBreakdown = BlueprintEngine.calculateCostBreakdown(
            areaSqm = totalGrossArea,
            finishesQuality = finishesQuality,
            roofType = roofType,
            soilType = "Red Soil"
        )

        // For buildings > 4 storeys, high-rise factor (lift shafts, shear walls, pumping) increases cost slightly
        val highRiseMultiplier = when {
            effectiveStoreys >= 7 -> 1.15 // 7-10 storeys: dual lifts, shear walls, tower crane
            effectiveStoreys >= 5 -> 1.08 // 5-6 storeys: lift installation, structural frame
            effectiveStoreys >= 3 -> 1.04 // 3-4 storeys: reinforced concrete frame
            else -> 1.00
        }

        val finalMinCost = baseCostBreakdown.totalCostMin * highRiseMultiplier
        val finalMaxCost = baseCostBreakdown.totalCostMax * highRiseMultiplier

        val storeyTitle = if (template.isMultiStorey) {
            if (customTitle.isNotBlank()) customTitle else "${template.title} ($effectiveStoreys Storeys)"
        } else {
            customTitle.ifBlank { template.title }
        }

        val storeyDescription = if (effectiveStoreys > 1) {
            "Customized $effectiveStoreys-storey building plan (configurable up to 10 storeys) based on '${template.title}'. Total gross floor area of ${totalGrossArea.roundToInt()} m² (${footprintArea.roundToInt()} m² footprint x $effectiveStoreys floors) with $finishesQuality finishes under $roofType roofing in $county County."
        } else {
            "Customized ${template.category} plan based on '${template.title}'. Scaled to ${totalGrossArea.roundToInt()} m² with $finishesQuality finishes under $roofType roofing in $county County."
        }

        val storeyNotes = if (effectiveStoreys > 1) {
            "Configured for $effectiveStoreys storeys (maximum 10 storeys). NCA High-Rise code compliance: ${if (effectiveStoreys >= 5) "Mandatory lift shafts & dual pressurized fire escapes equipped." else "Reinforced concrete frame."} Includes ${modifiedRooms.size} zone rooms per level."
        } else {
            "Built from VAPHBG architectural template catalog. Includes ${modifiedRooms.size} room zones."
        }

        return BlueprintEntity(
            title = storeyTitle,
            description = storeyDescription,
            roomsJson = roomsArray.toString(),
            totalAreaSqm = totalGrossArea,
            estimatedCostMinKes = finalMinCost,
            estimatedCostMaxKes = finalMaxCost,
            plotSize = template.recommendedPlot,
            county = county,
            status = "Draft",
            category = template.category,
            tags = "${template.category}, ${template.badge}, ${effectiveStoreys}-Storey, High-Rise, Custom Template",
            notes = storeyNotes
        )
    }

    private fun Float.roundTo(decimals: Int): Float {
        var multiplier = 1.0f
        repeat(decimals) { multiplier *= 10f }
        return kotlin.math.round(this * multiplier) / multiplier
    }
}
