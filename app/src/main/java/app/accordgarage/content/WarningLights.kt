package app.accordgarage.content

/** How soon the owner needs to act, read from the manual's wording for each light. */
enum class Urgency(val label: String) {
    STOP("Stop safely"),
    CHECK("Check soon"),
    INFO("Reminder"),
}

/** The usual lamp color in the cluster, used to tint the drawn symbol. */
enum class LampColor { RED, AMBER, GREEN, BLUE }

/** Drawn symbols for lights that aren't plain words on the cluster. */
enum class Symbol { SEAT_BELT, BATTERY, OIL, ENGINE, TIRE, FUEL, WRENCH, KEY, HIGH_BEAM, LIGHTS_ON, TURN, DOOR, SKID, LED }

/**
 * One instrument-panel light or message on this car (2008 Accord EX-L coupe, 2.4L, automatic,
 * U.S. model). Text comes from the 2008 Accord Coupe owner's manual, pages 55–62 and the pages
 * they point to. Lights for other trims are left out: fuel economy (V6 automatic), fog lights
 * (V6), and the daytime running lights and washer level indicators (Canada only).
 *
 * A light shows either a drawn [symbol] or the [word] printed on the lamp.
 */
data class WarningLight(
    val id: String,
    val name: String,
    val urgency: Urgency,
    val color: LampColor,
    val symbol: Symbol? = null,
    val word: String? = null,
    val looksLike: String,
    val meaning: String,
    val steps: List<String>,
    val normal: String? = null,
    val caution: String? = null,
    val pages: String,
    val manualQuery: String,
    /** An app screen that helps, as label to route. */
    val action: Pair<String, String>? = null,
    val keywords: String = "",
) {
    fun matches(q: String): Boolean {
        val words = q.lowercase().split(' ').filter { it.isNotBlank() }
        val hay = "$name ${word.orEmpty()} $looksLike $keywords".lowercase()
        return words.all { it in hay }
    }
}

private const val BULB_CHECK = "Comes on for a few seconds when you turn the ignition to ON (II), then goes off."

object WarningLights {
    val all = listOf(
        WarningLight(
            "oil", "Low oil pressure", Urgency.STOP, LampColor.RED, symbol = Symbol.OIL,
            looksLike = "An oil can with a drip from the spout",
            meaning = "Oil pressure has dropped very low or been lost. It should never come on with the engine running. Serious engine damage is possible.",
            steps = listOf(
                "Safely pull off the road, shut off the engine and turn on the hazard lights.",
                "Let the car sit for a minute, then open the hood and check the oil level.",
                "If it's low, add oil to bring it to the full mark on the dipstick (5W-20).",
                "Start the engine and watch the light. If it doesn't go out within 10 seconds, turn the engine off and have the car towed.",
            ),
            caution = "Running the engine with low oil pressure can cause serious damage almost immediately. Turn it off as soon as you can safely stop.",
            pages = "p. 56, 361", manualQuery = "Low Oil Pressure Indicator",
            keywords = "oil can lamp red engine pressure flashing",
        ),
        WarningLight(
            "charging", "Charging system", Urgency.STOP, LampColor.RED, symbol = Symbol.BATTERY,
            looksLike = "A battery with + and − marks",
            meaning = "The battery is not being charged. The car is running on what's left in the battery.",
            steps = listOf(
                "Turn off every electrical accessory you can (A/C, fan, radio, heated seats) and avoid the power windows.",
                "Keep the engine running. Restarting it drains the battery quickly.",
                "Drive straight to a service station or garage for help.",
            ),
            pages = "p. 56, 361", manualQuery = "Charging System Indicator",
            keywords = "battery alternator red electrical",
        ),
        WarningLight(
            "brake", "Parking brake and brake system", Urgency.STOP, LampColor.RED, word = "BRAKE",
            looksLike = "The word BRAKE",
            meaning = "With the parking brake on, it's a reminder to release it (a beeper sounds if you drive with it on). If it stays on with the parking brake fully released, or comes on while driving, there may be a problem with the brakes. The brake fluid is probably low.",
            normal = "Comes on when you turn the ignition to ON (II) and whenever the parking brake is set.",
            steps = listOf(
                "Make sure the parking brake is fully released.",
                "If it's still on, press lightly on the brake pedal. If the pedal feels normal, check the brake fluid level at your next stop. If it's low, have the brakes checked for leaks or worn pads.",
                "If the pedal sinks farther than usual or feels soft, slow down by shifting to a lower gear, pull over when safe, and have the car towed.",
                "If the ABS and VSA lights come on along with it, have the car inspected right away.",
            ),
            caution = "With a soft pedal you only have braking at two wheels and need much more distance to stop.",
            pages = "p. 57, 363", manualQuery = "Brake System Indicator",
            keywords = "parking brake handbrake fluid red exclamation",
        ),
        WarningLight(
            "srs", "Supplemental restraint system (airbags)", Urgency.CHECK, LampColor.RED, word = "SRS",
            looksLike = "The letters SRS",
            meaning = "A possible problem with the airbags or seat belt tensioners. It also covers the side airbags, the passenger's side airbag cutoff, side curtain airbags, the driver's seat position sensor and the front passenger weight sensors.",
            normal = BULB_CHECK,
            steps = listOf(
                "Have the car checked by a dealer as soon as possible if it stays on after starting, comes on or flashes while driving, or doesn't come on at all at ON (II).",
            ),
            caution = "The airbags and tensioners may not work in a crash while this light is warning you.",
            pages = "p. 29, 57", manualQuery = "SRS Indicator",
            keywords = "airbag air bag srs person seat belt tensioner",
        ),
        WarningLight(
            "mil", "Malfunction indicator lamp (check engine)", Urgency.CHECK, LampColor.AMBER, symbol = Symbol.ENGINE,
            looksLike = "An engine outline",
            meaning = "One of the engine's emissions control systems may have a problem. You may feel no difference, but it can cost fuel economy and raise emissions, and continued driving may cause serious damage. With the automatic, it may come on together with a flashing D.",
            normal = "At ON (II) it stays on for about 20 seconds. If it then blinks five times, the emissions readiness codes aren't set yet (after a battery disconnect); don't take an emissions test until it just goes off.",
            steps = listOf(
                "If you just refueled, the fuel cap may be loose or missing. Tighten it until it clicks at least once. The light takes several days of normal driving to go off.",
                "Otherwise, or if the light comes back repeatedly, have the car checked by a dealer as soon as possible.",
            ),
            caution = "Driving on with this light on can damage the emissions controls and the engine.",
            pages = "p. 56, 362", manualQuery = "Malfunction Indicator Lamp",
            keywords = "check engine cel mil engine light emissions amber yellow orange gas cap",
        ),
        WarningLight(
            "abs", "Anti-lock brakes (ABS)", Urgency.CHECK, LampColor.AMBER, word = "ABS",
            looksLike = "The letters ABS in a circle",
            meaning = "There's a problem with the ABS. You still have normal braking, but no anti-lock function.",
            normal = BULB_CHECK,
            steps = listOf("Have the car checked by a dealer.", "If the BRAKE and VSA lights are on with it, have it inspected right away."),
            pages = "p. 58", manualQuery = "Anti-lock Brake System",
            keywords = "abs antilock brakes amber",
        ),
        WarningLight(
            "vsa", "VSA system", Urgency.CHECK, LampColor.AMBER, word = "VSA",
            looksLike = "The letters VSA",
            meaning = "There's a problem with Vehicle Stability Assist. You still have normal driving, braking and cornering, but no traction and stability help.",
            normal = BULB_CHECK,
            steps = listOf(
                "Pull over when it's safe and turn the engine off.",
                "Restart the engine to reset the system.",
                "If the light stays on or comes back while driving, have the VSA checked by a dealer.",
            ),
            pages = "p. 62, 282", manualQuery = "VSA System Indicator",
            keywords = "vsa stability traction control amber",
        ),
        WarningLight(
            "vsa_active", "VSA activation", Urgency.INFO, LampColor.AMBER, symbol = Symbol.SKID,
            looksLike = "A car with two wavy skid lines behind it",
            meaning = "Flashing: VSA is working to control wheel spin or a skid. Steady: you've turned VSA off with the switch under the driver's side vent. Steady with the VSA light: a problem with the VSA.",
            normal = BULB_CHECK,
            steps = listOf(
                "If it's flashing, ease off and drive for the conditions.",
                "If it's on steadily and you didn't mean to turn VSA off, press and hold the VSA OFF switch until it beeps. VSA also turns back on every time you start the engine.",
            ),
            pages = "p. 62, 282–283", manualQuery = "VSA Activation Indicator",
            keywords = "skid slip traction vsa off car squiggle wavy",
        ),
        WarningLight(
            "low_tire", "Low tire pressure", Urgency.CHECK, LampColor.AMBER, symbol = Symbol.TIRE,
            looksLike = "A tire cross-section (horseshoe) with an ! inside",
            meaning = "One or more tires are significantly low on pressure. Cold weather can also set it off when the tires were filled in a warm place.",
            normal = BULB_CHECK,
            steps = listOf(
                "Pull to the side of the road when it's safe and find which tire is low.",
                "If you can safely drive a short way, go slowly to a station and fill to 32 psi front and rear (cold).",
                "If it's flat, fit the compact spare. The light stays on with the spare fitted; that's normal.",
                "If two or more tires are low, call a tow.",
                "If it won't go out after filling the tires, have the dealer check the system.",
            ),
            caution = "Don't use a can of puncture sealant; it ruins the pressure sensor inside the tire.",
            pages = "p. 59, 284–286", manualQuery = "Low Tire Pressure Indicator",
            action = "Flat tire steps" to "roadside",
            keywords = "tpms tire tyre pressure flat horseshoe exclamation amber",
        ),
        WarningLight(
            "tpms", "Tire pressure monitoring system (TPMS)", Urgency.CHECK, LampColor.AMBER, word = "TPMS",
            looksLike = "The letters TPMS",
            meaning = "There's a problem with the tire pressure monitoring system, so it has stopped watching the tires. The low tire pressure light won't warn you while this is on. It also comes on after driving several miles on the compact spare.",
            normal = BULB_CHECK,
            steps = listOf(
                "Check all four tires with a gauge until it's fixed (32 psi cold).",
                "If you're not on the spare, have the dealer check the system. The original sensors are about 18 years old, and worn-out sensor batteries are a common cause at that age.",
            ),
            pages = "p. 59, 285", manualQuery = "TPMS Indicator",
            keywords = "tpms sensor tire tyre amber",
        ),
        WarningLight(
            "d_flash", "D indicator flashing", Urgency.CHECK, LampColor.GREEN, word = "D",
            looksLike = "The D in the gear position display, blinking",
            meaning = "A possible problem in the transmission. If the check-engine light is on with it, there's a problem with the transmission control system.",
            normal = BULB_CHECK,
            steps = listOf("Avoid rapid acceleration.", "Have the transmission checked by a dealer as soon as possible."),
            pages = "p. 273", manualQuery = "Shift Lever Position Indicators",
            keywords = "transmission gear drive d flashing blinking automatic",
        ),
        WarningLight(
            "low_fuel", "Low fuel", Urgency.CHECK, LampColor.AMBER, symbol = Symbol.FUEL,
            looksLike = "A fuel pump",
            meaning = "Time to refuel soon. About 2.8 US gal (10.5 L) is left when it comes on.",
            steps = listOf("Fill up with regular unleaded (87 octane or higher)."),
            pages = "p. 60", manualQuery = "Low Fuel Indicator",
            action = "Log a fill-up" to "fuel/0",
            keywords = "gas fuel pump empty amber",
        ),
        WarningLight(
            "fuel_cap", "CHECK FUEL CAP message", Urgency.CHECK, LampColor.AMBER, word = "CHECK\nFUEL CAP",
            looksLike = "The words CHECK FUEL CAP in the information display",
            meaning = "The fuel cap is loose or missing; the system sees it as an evaporative leak.",
            steps = listOf(
                "Turn the engine off and make sure the cap is there.",
                "Loosen it, then tighten until it clicks at least once.",
                "The message shows at each start until the system clears it after several days of normal driving.",
            ),
            pages = "p. 67, 251", manualQuery = "Check Fuel Cap Message",
            keywords = "gas cap fuel cap message display",
        ),
        WarningLight(
            "minder", "Maintenance Minder", Urgency.INFO, LampColor.AMBER, symbol = Symbol.WRENCH,
            looksLike = "A wrench",
            meaning = "It's time for scheduled maintenance. The main and sub items show in the information display.",
            normal = "Comes on for a few seconds at ON (II) when service is due.",
            steps = listOf("Read the code from the information display and decode it in the app.", "It goes off when the Minder is reset after the service."),
            pages = "p. 61, 297", manualQuery = "Maintenance Minder",
            action = "Decode the Minder code" to "minder",
            keywords = "wrench spanner service maintenance oil life amber",
        ),
        WarningLight(
            "seat_belt", "Seat belt reminder", Urgency.INFO, LampColor.RED, symbol = Symbol.SEAT_BELT,
            looksLike = "A seated person wearing a seat belt",
            meaning = "The driver or front passenger hasn't buckled up. A beeper sounds and the light flashes, then flashes again at intervals while driving.",
            steps = listOf("Fasten your seat belt and have your front passenger fasten theirs."),
            pages = "p. 17, 56", manualQuery = "Seat Belt Reminder",
            keywords = "seatbelt belt person red beeper chime",
        ),
        WarningLight(
            "side_airbag_off", "Side airbag off", Urgency.INFO, LampColor.AMBER, word = "SIDE\nAIRBAG\nOFF",
            looksLike = "The words SIDE AIRBAG OFF",
            meaning = "The front passenger's side airbag has been shut off because the passenger is leaning into its path. It does not mean there's a problem.",
            normal = BULB_CHECK,
            steps = listOf(
                "Have the passenger sit upright. The airbag turns back on and the light goes out after a short delay.",
                "If it doesn't come on at ON (II), stays on, or comes on with nobody in the seat, have the system checked.",
            ),
            pages = "p. 29, 57", manualQuery = "Side Airbag Off Indicator",
            keywords = "side airbag off passenger amber",
        ),
        WarningLight(
            "passenger_airbag_off", "Passenger airbag off", Urgency.INFO, LampColor.AMBER, word = "PASSENGER\nAIRBAG\nOFF",
            looksLike = "The words PASSENGER AIRBAG OFF, in the center of the dashboard",
            meaning = "The weight sensors detect about 65 lb (29 kg) or less on the front passenger seat, so the front passenger airbag is off. It does not mean there's a problem. It can flicker if the weight is near the cutoff.",
            steps = listOf(
                "For an adult or teenager in front: move the seat all the way back, sit upright and wear the belt properly.",
                "If it's on with an adult in the seat or nobody there, remove anything under the seat, hanging on it, in the seat-back pocket or touching the back of the seat.",
                "If nothing is in the way, have it checked by a dealer as soon as possible.",
            ),
            pages = "p. 26, 30", manualQuery = "Passenger Airbag Off Indicator",
            keywords = "passenger airbag off weight child dashboard",
        ),
        WarningLight(
            "immobilizer", "Immobilizer", Urgency.INFO, LampColor.GREEN, symbol = Symbol.KEY,
            looksLike = "A key",
            meaning = "Blinking means the car doesn't recognize the key's coding, so the fuel system is disabled and the engine won't start.",
            normal = "Comes on briefly at ON (II), then goes off with a properly coded key.",
            steps = listOf(
                "Turn the key to LOCK (0), take it out, put it back in and turn to ON (II) again.",
                "Keep other immobilizer keys and metal key chains away from the ignition.",
                "If you've lost the key or it still won't start, contact a dealer.",
            ),
            pages = "p. 58, 78", manualQuery = "Immobilizer System Indicator",
            keywords = "key immobiliser wont start no start blinking green",
        ),
        WarningLight(
            "security", "Security system", Urgency.INFO, LampColor.RED, symbol = Symbol.LED,
            looksLike = "A small red light that blinks",
            meaning = "The security system is set. It starts blinking as soon as you lock up, and sets after 15 seconds.",
            steps = listOf("If the system won't set, check the door and trunk open lights to see what isn't fully closed."),
            pages = "p. 61, 204", manualQuery = "Security System Indicator",
            keywords = "alarm security blinking red dot led",
        ),
        WarningLight(
            "door", "Door and trunk open", Urgency.INFO, LampColor.RED, symbol = Symbol.DOOR,
            looksLike = "A car seen from above with a door or the trunk lid open",
            meaning = "A door or the trunk isn't closed tightly.",
            normal = "All of them come on for a few seconds at ON (II).",
            steps = listOf("Close the door or trunk shown firmly."),
            pages = "p. 61", manualQuery = "Door and Trunk Open Indicator",
            keywords = "door trunk boot open ajar car",
        ),
        WarningLight(
            "turn", "Turn signals and hazards", Urgency.INFO, LampColor.GREEN, symbol = Symbol.TURN,
            looksLike = "Green arrows pointing left and right",
            meaning = "Your turn signal or the hazard lights are on. If an arrow doesn't blink or blinks fast, a turn signal bulb is probably burned out.",
            steps = listOf("Replace a burned-out bulb soon so other drivers can see you signal."),
            pages = "p. 58, 324, 326", manualQuery = "Turn Signal and Hazard Warning Indicators",
            keywords = "arrow blinker indicator hazard green fast blinking",
        ),
        WarningLight(
            "lights_on", "Lights on", Urgency.INFO, LampColor.GREEN, symbol = Symbol.LIGHTS_ON,
            looksLike = "Two small lamps facing away from each other",
            meaning = "The exterior lights are on, either from the switch or because AUTO turned them on. If it stays on after you switch the ignition off, the lights are still on, and a chime sounds when you open the driver's door.",
            steps = listOf("Turn the lights off when you park so the battery doesn't go flat."),
            pages = "p. 60", manualQuery = "Lights On Indicator",
            keywords = "headlights parking lights auto green",
        ),
        WarningLight(
            "high_beam", "High beams", Urgency.INFO, LampColor.BLUE, symbol = Symbol.HIGH_BEAM,
            looksLike = "A blue headlamp with straight beams",
            meaning = "The high beams are on.",
            steps = listOf("Dip them for oncoming traffic."),
            pages = "p. 60", manualQuery = "High Beam Indicator",
            keywords = "brights headlights high beam blue",
        ),
        WarningLight(
            "cruise_main", "Cruise main", Urgency.INFO, LampColor.AMBER, word = "CRUISE\nMAIN",
            looksLike = "The words CRUISE MAIN",
            meaning = "Cruise control is switched on with the CRUISE button, ready to set.",
            steps = listOf("Press CRUISE again to switch it off."),
            pages = "p. 62, 210–211", manualQuery = "Cruise Main Indicator",
            keywords = "cruise control main",
        ),
        WarningLight(
            "cruise_control", "Cruise control", Urgency.INFO, LampColor.GREEN, word = "CRUISE\nCONTROL",
            looksLike = "The words CRUISE CONTROL",
            meaning = "A cruising speed is set.",
            steps = listOf("Tap the brake or press CANCEL to stop cruising."),
            pages = "p. 62, 210–211", manualQuery = "Cruise Control Indicator",
            keywords = "cruise control set speed",
        ),
    )

    fun byId(id: String): WarningLight? = all.firstOrNull { it.id == id }
}
