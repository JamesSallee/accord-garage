package app.accordgarage.data

/**
 * The one car this app is for. Decoded from the VIN with NHTSA's vPIC decoder
 * (error code 0, check digit valid). Nothing here is user-editable.
 */
object Vehicle {
    const val VIN = "1HGCS12838A014367"
    const val YEAR = 2008
    const val MAKE = "Honda"
    const val MODEL = "Accord"
    const val BODY = "Coupe"
    const val DOORS = 2
    const val TRIM = "EX-L"
    const val ENGINE = "2.4L K24Z3 I4, DOHC"
    const val ENGINE_CODE = "K24Z3"
    const val HORSEPOWER = "190 hp"
    const val TRANSMISSION = "5-speed automatic"
    const val TPMS = "Direct (sensor in each wheel)"
    const val PLANT = "Marysville, Ohio"
    const val STARTING_ODOMETER = 102_000

    val headline get() = "$YEAR ${MODEL.uppercase()} $TRIM ${BODY.uppercase()}"
    val subline get() = "$ENGINE_CODE 2.4L · 5-SPEED AT · VIN …${VIN.takeLast(6)}"

    const val NHTSA_VIN_LOOKUP = "https://www.nhtsa.gov/recalls?vin=$VIN"
    const val RECALLS_API =
        "https://api.nhtsa.gov/recalls/recallsByVehicle?make=HONDA&model=ACCORD&modelYear=2008"
    const val HONDA_OWNERS_SITE = "https://owners.honda.com/"
}
