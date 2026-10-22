package app.accordgarage.content

import app.accordgarage.data.Vehicle

/**
 * A quick-spec row. [value] is a starting value with its [source]; values marked
 * [fromVin] come from the VIN decode and values marked [fromManual] were checked
 * against the 2008 Accord Coupe owner's manual. Everything else stays flagged until
 * the owner confirms it against the manual or the label named.
 */
data class SpecDef(
    val key: String,
    val label: String,
    val value: String = "",
    val source: String = "",
    val fromVin: Boolean = false,
    val fromManual: Boolean = false,
    val manualQuery: String? = null,
    val sensitive: Boolean = false,
)

object Specs {
    val groups: List<Pair<String, List<SpecDef>>> = listOf(
        "VEHICLE" to listOf(
            SpecDef("vin", "VIN", Vehicle.VIN, "NHTSA vPIC", fromVin = true),
            SpecDef("engine", "Engine", "${Vehicle.ENGINE}, ${Vehicle.HORSEPOWER}", "NHTSA vPIC", fromVin = true),
            SpecDef("transmission", "Transmission", Vehicle.TRANSMISSION, "NHTSA vPIC", fromVin = true),
            SpecDef("tpms", "Tire pressure monitoring", Vehicle.TPMS, "NHTSA vPIC", fromVin = true),
            SpecDef("paint_code", "Paint code", source = "Driver's door-jamb label"),
        ),
        "FLUIDS" to listOf(
            SpecDef("oil_type", "Engine oil", "SAE 5W-20, API certified", "Owner's manual p. 306", fromManual = true, manualQuery = "engine oil"),
            SpecDef("oil_capacity", "Oil capacity with filter", "4.4 US qt (4.2 L)", "Owner's manual p. 309 and 376", fromManual = true, manualQuery = "engine oil capacity"),
            SpecDef("atf", "Transmission fluid", "Honda ATF-Z1 (now sold as ATF DW-1)", "Owner's manual p. 314; DW-1 is Honda's replacement for Z1", fromManual = true, manualQuery = "transmission fluid"),
            SpecDef("coolant", "Coolant", "Honda Long-Life Antifreeze/Coolant Type 2", "Owner's manual, Engine Coolant", fromManual = true, manualQuery = "coolant"),
            SpecDef("brake_fluid", "Brake fluid", "Honda Heavy Duty Brake Fluid DOT 3", "Owner's manual, Brake Fluid", fromManual = true, manualQuery = "brake fluid"),
        ),
        "WHEELS AND TIRES" to listOf(
            SpecDef("tire_size", "Tire size", "P225/50R17 93V", "Owner's manual p. 336 and 377", fromManual = true, manualQuery = "tire size"),
            SpecDef("tire_pressure_front", "Pressure, front (cold)", "32 psi (220 kPa)", "Owner's manual p. 336", fromManual = true, manualQuery = "tire pressure"),
            SpecDef("tire_pressure_rear", "Pressure, rear (cold)", "32 psi (220 kPa)", "Owner's manual p. 336", fromManual = true, manualQuery = "tire pressure"),
            SpecDef("spare_pressure", "Spare tire pressure", "60 psi (420 kPa), T135/80D16 compact", "Owner's manual p. 336 and 377", fromManual = true, manualQuery = "spare tire"),
            SpecDef("lug_torque", "Lug nut torque", "80 lbf·ft (108 N·m)", "Owner's manual p. 352", fromManual = true, manualQuery = "wheel nut"),
        ),
        "PARTS" to listOf(
            SpecDef("drain_plug_torque", "Oil drain plug torque", "29 lbf·ft (39 N·m)", "Owner's manual p. 309", fromManual = true),
            SpecDef("battery_group", "Battery group size", source = "Label on the current battery"),
            SpecDef("wiper_driver", "Wiper, driver", manualQuery = "wiper blade"),
            SpecDef("wiper_passenger", "Wiper, passenger", manualQuery = "wiper blade"),
            SpecDef("bulb_low", "Headlight bulb, low beam", "H11 (12 V 55 W)", "Owner's manual p. 377", fromManual = true, manualQuery = "bulb"),
            SpecDef("bulb_high", "Headlight bulb, high beam", "HB3 / 9005 (12 V 60 W)", "Owner's manual p. 377", fromManual = true, manualQuery = "bulb"),
            SpecDef("spark_plug", "Spark plug part number", "NGK ILZKR7B11S or DENSO SXU22HCR11S; PZEV engine: NGK DILZKR7A11GS", "Owner's manual p. 376; PZEV or ULEV is on the under-hood emissions label", manualQuery = "spark plug"),
        ),
        "PERSONAL" to listOf(
            SpecDef("radio_code", "Factory radio anti-theft code", source = "Glovebox sticker, manual, or a Honda dealer", sensitive = true),
            SpecDef("roadside_phone", "Roadside assistance number", source = "Insurance card or auto club"),
            SpecDef("head_unit_firmware", "Head unit firmware version", source = "Head unit settings"),
        ),
    )

    val all: List<SpecDef> get() = groups.flatMap { it.second }
}

data class RoadsideGuide(val id: String, val title: String, val steps: List<String>, val caution: String? = null)

object Roadside {
    val guides = listOf(
        RoadsideGuide(
            "safety", "First: get safe",
            listOf(
                "Turn on the hazard lights.",
                "If you can, move fully off the road onto a level, firm surface away from traffic.",
                "On a busy road, get everyone out on the side away from traffic and stand clear of the car.",
                "If anyone is hurt or the car is in a dangerous spot, call 911.",
            ),
        ),
        RoadsideGuide(
            "flat", "Flat tire",
            listOf(
                "Park on level ground, set the parking brake, shift to Park, and chock the wheel diagonally opposite.",
                "The spare, jack and wrench are under the trunk floor.",
                "Loosen the lug nuts half a turn while the wheel is still on the ground.",
                "Jack at the lift point nearest the flat, as shown in the manual, until the tire is just off the ground.",
                "Remove the nuts and wheel, fit the spare, and snug the nuts in a star pattern.",
                "Lower the car, then tighten the nuts fully in a star pattern.",
                "Check the spare's pressure and follow the speed limit printed on a compact spare.",
            ),
            caution = "Never put any part of your body under a car held only by the jack.",
        ),
        RoadsideGuide(
            "jump", "Jump start",
            listOf(
                "Park the cars close but not touching, both engines off.",
                "Red clamp to the dead battery's + terminal.",
                "Other red clamp to the good battery's + terminal.",
                "Black clamp to the good battery's − terminal.",
                "Last black clamp to bare, unpainted metal on the dead car's engine, away from the battery.",
                "Start the good car, wait a few minutes, then start yours.",
                "Remove the clamps in reverse order. Drive for at least 20 minutes, then test the battery.",
                "If the factory radio asks for its code, it's in Quick specs.",
            ),
            caution = "Never let the red and black clamps touch each other.",
        ),
        RoadsideGuide(
            "overheat", "Overheating",
            listOf(
                "Turn the A/C off and the heater to full hot and full fan; it pulls heat from the engine.",
                "Pull over safely and switch the engine off.",
                "Wait until the engine has cooled, at least 30 minutes.",
                "With the engine cold, check the coolant reservoir level and look for leaks.",
                "If the gauge goes back into the red after a top-up, or coolant is pouring out, call for a tow.",
            ),
            caution = "Never open the radiator cap on a hot engine.",
        ),
    )
}
