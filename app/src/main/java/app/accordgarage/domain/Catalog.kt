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
    SUSPENSION("Suspension and steering", Zone.NONE),
    TIRES("Wheels and tires", Zone.FRONT_WHEELS),
    FUEL_EXHAUST("Fuel and exhaust", Zone.NONE),
    ELECTRICAL("Electrical", Zone.NONE),
    CLIMATE("Climate", Zone.NONE),
    BODY("Body and exterior", Zone.NONE),
    INTERIOR("Interior", Zone.NONE),
    AUDIO("Audio", Zone.NONE),
    SAFETY("Safety systems", Zone.NONE),
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
        // Engine
        Job("oil_change", "Engine oil", CarSystem.ENGINE, "Replace", 5_000, 6, tutorialId = "oil_change"),
        Job("oil_filter", "Oil filter", CarSystem.ENGINE, "Replace", 10_000, 12, tutorialId = "oil_change"),
        Job("engine_air_filter", "Engine air filter", CarSystem.ENGINE, "Replace", 15_000, 24, tutorialId = "engine_air_filter"),
        Job("drive_belt", "Drive belt", CarSystem.ENGINE, "Inspect", 15_000, 24),
        Job("spark_plugs", "Spark plugs", CarSystem.ENGINE, "Replace", 105_000, null, catchUp = true, tutorialId = "spark_plugs"),
        Job("valve_clearance", "Valve clearance", CarSystem.ENGINE, "Inspect", 105_000, null, catchUp = true, tutorialId = "valve_clearance"),
        Job("belt_tensioner", "Drive belt tensioner", CarSystem.ENGINE, "Replace"),
        Job("pcv_valve", "PCV valve", CarSystem.ENGINE, "Replace"),
        Job("valve_cover_gasket", "Valve cover gasket", CarSystem.ENGINE, "Replace"),
        Job("ignition_coils", "Ignition coils", CarSystem.ENGINE, "Replace"),
        Job("throttle_body", "Throttle body", CarSystem.ENGINE, "Clean"),
        Job("vtc_actuator", "VTC actuator", CarSystem.ENGINE, "Replace"),
        Job("timing_chain", "Timing chain and tensioner", CarSystem.ENGINE, "Inspect"),
        Job("engine_mounts", "Engine mounts", CarSystem.ENGINE, "Inspect"),
        Job("oil_consumption_test", "Oil consumption test", CarSystem.ENGINE, "Test"),
        // Transmission
        Job("atf", "Automatic transmission fluid", CarSystem.TRANSMISSION, "Replace", 30_000, 36, catchUp = true, tutorialId = "atf"),
        Job("transmission_mount", "Transmission mount", CarSystem.TRANSMISSION, "Inspect"),
        Job("shift_solenoids", "Shift solenoids", CarSystem.TRANSMISSION, "Inspect"),
        // Cooling
        Job("coolant", "Engine coolant", CarSystem.COOLING, "Replace", 60_000, 60, catchUp = true, tutorialId = "coolant"),
        Job("thermostat", "Thermostat", CarSystem.COOLING, "Replace"),
        Job("radiator", "Radiator", CarSystem.COOLING, "Replace"),
        Job("coolant_hoses", "Coolant hoses", CarSystem.COOLING, "Replace"),
        Job("water_pump", "Water pump", CarSystem.COOLING, "Replace"),
        // Brakes. Rear pads get a shorter check than the fronts: this model wears them early.
        Job("front_brakes", "Front brake pads", CarSystem.BRAKES_FRONT, "Inspect", 15_000, 12, tutorialId = "front_brakes"),
        Job("front_rotors", "Front rotors", CarSystem.BRAKES_FRONT, "Replace"),
        Job("rear_brakes", "Rear brake pads", CarSystem.BRAKES_REAR, "Inspect", 7_500, 6, catchUp = true, tutorialId = "rear_brakes"),
        Job("rear_rotors", "Rear rotors", CarSystem.BRAKES_REAR, "Replace"),
        Job("parking_brake", "Parking brake", CarSystem.BRAKES_REAR, "Adjust"),
        Job("brake_fluid", "Brake fluid", CarSystem.BRAKES, "Replace", null, 36),
        Job("caliper_service", "Caliper slide pins", CarSystem.BRAKES, "Service"),
        Job("brake_lines", "Brake hoses and lines", CarSystem.BRAKES, "Inspect"),
        // Suspension and steering
        Job("struts_front", "Front struts", CarSystem.SUSPENSION, "Replace"),
        Job("struts_rear", "Rear shocks", CarSystem.SUSPENSION, "Replace"),
        Job("sway_links", "Sway bar links", CarSystem.SUSPENSION, "Replace"),
        Job("control_arm_bushings", "Control arm bushings", CarSystem.SUSPENSION, "Replace"),
        Job("tie_rods", "Tie rod ends", CarSystem.SUSPENSION, "Inspect"),
        Job("cv_axles", "Driveshaft boots and axles", CarSystem.SUSPENSION, "Inspect"),
        Job("power_steering_fluid", "Power steering fluid", CarSystem.SUSPENSION, "Replace"),
        Job("alignment", "Wheel alignment", CarSystem.SUSPENSION, "Check"),
        // Wheels and tires
        Job("tire_rotation", "Tire rotation", CarSystem.TIRES, "Rotate", 7_500, 12, tutorialId = "tire_rotation"),
        Job("tires", "Tires", CarSystem.TIRES, "Replace"),
        Job("wheel_balance", "Wheel balance", CarSystem.TIRES, "Balance"),
        Job("tire_repair", "Tire repair", CarSystem.TIRES, "Repair"),
        Job("tpms_sensor", "TPMS sensors", CarSystem.TIRES, "Replace", tutorialId = "tpms"),
        // Fuel and exhaust
        Job("o2_sensors", "Oxygen sensors", CarSystem.FUEL_EXHAUST, "Replace"),
        Job("catalytic_converter", "Catalytic converter", CarSystem.FUEL_EXHAUST, "Inspect"),
        Job("exhaust", "Exhaust system", CarSystem.FUEL_EXHAUST, "Repair"),
        Job("fuel_system", "Fuel filter and pump", CarSystem.FUEL_EXHAUST, "Service"),
        Job("emissions_test", "Emissions or safety inspection", CarSystem.FUEL_EXHAUST, "Pass"),
        // Electrical
        Job("battery", "Battery", CarSystem.ELECTRICAL, "Test", null, 12, tutorialId = "battery"),
        Job("bulbs", "Bulbs", CarSystem.ELECTRICAL, "Replace", tutorialId = "headlight_bulb"),
        Job("alternator", "Alternator", CarSystem.ELECTRICAL, "Replace"),
        Job("starter", "Starter", CarSystem.ELECTRICAL, "Replace"),
        Job("fuses", "Fuses", CarSystem.ELECTRICAL, "Replace"),
        // Climate
        Job("cabin_filter", "Cabin air filter", CarSystem.CLIMATE, "Replace", 15_000, 12, tutorialId = "cabin_filter"),
        Job("ac_check", "A/C performance", CarSystem.CLIMATE, "Check"),
        Job("blend_door", "Blend door actuator", CarSystem.CLIMATE, "Replace"),
        // Body and exterior
        Job("wipers", "Wiper blades", CarSystem.BODY, "Replace", null, 12, tutorialId = "wipers"),
        Job("door_lube", "Door hinges and locks", CarSystem.BODY, "Lubricate"),
        Job("headlight_restore", "Headlight lenses", CarSystem.BODY, "Restore"),
        Job("touch_up_paint", "Touch-up paint", CarSystem.BODY, "Apply"),
        Job("seals", "Door and trunk seals", CarSystem.BODY, "Replace"),
        // Interior
        Job("trim", "Interior trim", CarSystem.INTERIOR, "Repair"),
        Job("seats", "Seats", CarSystem.INTERIOR, "Repair"),
        Job("mirror_dashcam", "Auto-dimming mirror and dash cam", CarSystem.INTERIOR, "Install"),
        // Audio
        Job("head_unit", "Head unit install", CarSystem.AUDIO, "Install", tutorialId = "head_unit"),
        Job("speakers", "Speakers", CarSystem.AUDIO, "Replace"),
        // Safety systems
        Job("airbag_recall", "Airbag recall work", CarSystem.SAFETY, "Dealer"),
        Job("seat_belts", "Seat belts", CarSystem.SAFETY, "Inspect"),
        Job("other", "Other work", CarSystem.OTHER, "Do"),
    )

    /** Shown first when logging a service. */
    val common: List<String> = listOf(
        "oil_change", "oil_filter", "tire_rotation", "engine_air_filter", "cabin_filter",
        "rear_brakes", "front_brakes", "wipers", "battery", "atf",
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
