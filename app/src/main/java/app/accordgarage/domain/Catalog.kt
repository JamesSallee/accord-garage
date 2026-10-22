package app.accordgarage.domain

/** Where a job shows on the home illustration. NONE = list only. */
enum class Zone(val label: String) {
    POWERTRAIN("POWERTRAIN"),
    FRONT_WHEELS("FRONT WHEELS"),
    REAR_BRAKES("REAR BRAKES"),
    NONE(""),
}

enum class CarSystem(val label: String, val zone: Zone) {
    ENGINE("Engine", Zone.POWERTRAIN),
    TRANSMISSION("Transmission", Zone.POWERTRAIN),
    COOLING("Cooling", Zone.POWERTRAIN),
    BRAKES_FRONT("Front brakes", Zone.FRONT_WHEELS),
    BRAKES_REAR("Rear brakes", Zone.REAR_BRAKES),
    BRAKES("Brakes", Zone.NONE),
    TIRES("Wheels and tires", Zone.FRONT_WHEELS),
    ELECTRICAL("Electrical", Zone.NONE),
    CLIMATE("Climate", Zone.NONE),
    BODY("Body and exterior", Zone.NONE),
    AUDIO("Audio", Zone.NONE),
    OTHER("Other", Zone.NONE),
}

/**
 * A maintenance job. [defaultMiles]/[defaultMonths] are starting points for owners who
 * don't go by the Maintenance Minder; the app labels them as defaults to check against
 * the owner's manual, and every one can be changed.
 */
data class Job(
    val id: String,
    val name: String,
    val system: CarSystem,
    val action: String,
    val defaultMiles: Int? = null,
    val defaultMonths: Int? = null,
    val catchUp: Boolean = false,
    val tutorialId: String? = null,
)

object Catalog {
    val jobs: List<Job> = listOf(
        Job("oil_change", "Engine oil", CarSystem.ENGINE, "Replace", 5_000, 6, tutorialId = "oil_change"),
        Job("oil_filter", "Oil filter", CarSystem.ENGINE, "Replace", 10_000, 12, tutorialId = "oil_change"),
        Job("tire_rotation", "Tire rotation", CarSystem.TIRES, "Rotate", 7_500, 12, tutorialId = "tire_rotation"),
        Job("engine_air_filter", "Engine air filter", CarSystem.ENGINE, "Replace", 15_000, 24, tutorialId = "engine_air_filter"),
        Job("cabin_filter", "Cabin air filter", CarSystem.CLIMATE, "Replace", 15_000, 12, tutorialId = "cabin_filter"),
        Job("drive_belt", "Drive belt", CarSystem.ENGINE, "Inspect", 15_000, 24),
        Job("atf", "Automatic transmission fluid", CarSystem.TRANSMISSION, "Replace", 30_000, 36, catchUp = true, tutorialId = "atf"),
        Job("spark_plugs", "Spark plugs", CarSystem.ENGINE, "Replace", 105_000, null, catchUp = true, tutorialId = "spark_plugs"),
        Job("valve_clearance", "Valve clearance", CarSystem.ENGINE, "Inspect", 105_000, null, catchUp = true, tutorialId = "valve_clearance"),
        Job("coolant", "Engine coolant", CarSystem.COOLING, "Replace", 60_000, 60, catchUp = true, tutorialId = "coolant"),
        Job("front_brakes", "Front brake pads", CarSystem.BRAKES_FRONT, "Inspect", 15_000, 12, tutorialId = "front_brakes"),
        Job("rear_brakes", "Rear brake pads", CarSystem.BRAKES_REAR, "Inspect", 15_000, 12, catchUp = true, tutorialId = "rear_brakes"),
        Job("brake_fluid", "Brake fluid", CarSystem.BRAKES, "Replace", null, 36),
        Job("wipers", "Wiper blades", CarSystem.BODY, "Replace", null, 12, tutorialId = "wipers"),
        Job("battery", "Battery", CarSystem.ELECTRICAL, "Test", null, 12, tutorialId = "battery"),
        Job("bulbs", "Bulbs", CarSystem.ELECTRICAL, "Replace", tutorialId = "headlight_bulb"),
        Job("tpms_sensor", "TPMS sensors", CarSystem.TIRES, "Replace", tutorialId = "tpms"),
        Job("head_unit", "Head unit install", CarSystem.AUDIO, "Install", tutorialId = "head_unit"),
        Job("other", "Other work", CarSystem.OTHER, "Do"),
    )

    private val byId = jobs.associateBy { it.id }
    fun job(id: String): Job? = byId[id]
    fun name(id: String): String = byId[id]?.name ?: id

    val catchUpJobs: List<Job> get() = jobs.filter { it.catchUp }
}

/** Honda Maintenance Minder codes as shown on the dash: a main letter plus sub-items 1–5. */
object Minder {
    data class Item(val code: String, val title: String, val details: List<String>, val jobIds: List<String>)

    val items: List<Item> = listOf(
        Item("A", "Replace engine oil", listOf("Replace engine oil"), listOf("oil_change")),
        Item(
            "B", "Oil, filter and inspection",
            listOf(
                "Replace engine oil and oil filter",
                "Inspect front and rear brakes",
                "Check parking brake adjustment",
                "Inspect tie rod ends, steering gearbox and boots",
                "Inspect suspension components and driveshaft boots",
                "Inspect brake hoses and lines, fluid levels, exhaust and fuel lines",
            ),
            listOf("oil_change", "oil_filter", "front_brakes", "rear_brakes"),
        ),
        Item("1", "Rotate tires", listOf("Rotate tires", "Check tire inflation and condition"), listOf("tire_rotation")),
        Item(
            "2", "Filters and drive belt",
            listOf("Replace engine air filter", "Replace cabin (dust and pollen) filter", "Inspect drive belt"),
            listOf("engine_air_filter", "cabin_filter", "drive_belt"),
        ),
        Item("3", "Transmission fluid", listOf("Replace automatic transmission fluid"), listOf("atf")),
        Item(
            "4", "Spark plugs and valves",
            listOf("Replace spark plugs", "Inspect valve clearance"),
            listOf("spark_plugs", "valve_clearance"),
        ),
        Item("5", "Engine coolant", listOf("Replace engine coolant"), listOf("coolant")),
    )

    private val byCode = items.associateBy { it.code }

    /** Parses "B13" -> [B, 1, 3]. Unknown characters are ignored. */
    fun parse(code: String): List<Item> =
        code.uppercase().filter { !it.isWhitespace() }.mapNotNull { byCode[it.toString()] }.distinctBy { it.code }

    fun jobsFor(code: String): List<String> = parse(code).flatMap { it.jobIds }.distinct()

    fun normalize(main: String?, subs: Set<Int>): String =
        (main ?: "") + subs.sorted().joinToString("")
}
