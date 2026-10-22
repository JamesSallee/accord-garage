package app.accordgarage.domain

import app.accordgarage.data.FuelEntry
import app.accordgarage.data.IntervalOverride
import app.accordgarage.data.MinderReading
import app.accordgarage.data.OdometerReading
import app.accordgarage.data.OilTopUp
import app.accordgarage.data.ServiceRecord
import app.accordgarage.data.Vehicle
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

enum class DueState(val rank: Int, val label: String) {
    OVERDUE(0, "OVERDUE"),
    MINDER(1, "MINDER"),
    CATCH_UP(2, "NOT LOGGED"),
    DUE_SOON(3, "DUE SOON"),
    OK(4, "OK"),
    NO_HISTORY(5, "NO HISTORY"),
    NO_INTERVAL(6, "AS NEEDED"),
}

data class JobStatus(
    val job: Job,
    val state: DueState,
    val lastDone: ServiceRecord?,
    val intervalMiles: Int?,
    val intervalMonths: Int?,
    val isCustomInterval: Boolean,
    val dueOdometer: Int?,
    val dueDate: LocalDate?,
    val milesLeft: Int?,
    val projectedDate: LocalDate?,
) {
    /** Needs action: shows on the home screen and the car illustration. */
    val needsAction: Boolean
        get() = state == DueState.OVERDUE || state == DueState.MINDER ||
            state == DueState.CATCH_UP || state == DueState.DUE_SOON

    val actionWord: String
        get() = when (state) {
            DueState.OVERDUE -> "OVERDUE"
            DueState.DUE_SOON -> "DUE SOON"
            DueState.MINDER -> "MINDER"
            else -> if (job.action == "Inspect") "INSPECT" else "DUE"
        }
}

data class GarageSnapshot(
    val odometer: Int,
    val milesPerDay: Double,
    val statuses: List<JobStatus>,
    val latestMinder: MinderReading?,
    val milesPerQuart: Double?,
) {
    val actionable: List<JobStatus> get() = statuses.filter { it.needsAction }.sortedBy { it.state.rank }
    val catchUp: List<JobStatus> get() = statuses.filter { it.job.catchUp }
    val catchUpDone: Int get() = catchUp.count { it.lastDone != null }

    fun zoneItems(zone: Zone): List<JobStatus> = actionable.filter { it.job.system.zone == zone }
}

object DueEngine {
    private const val SOON_MILES = 500
    private const val SOON_DAYS = 14L
    private const val DEFAULT_MILES_PER_DAY = 30.0

    fun currentOdometer(
        services: List<ServiceRecord>,
        fuel: List<FuelEntry>,
        oil: List<OilTopUp>,
        readings: List<OdometerReading>,
        minder: List<MinderReading>,
    ): Int = listOf(
        Vehicle.STARTING_ODOMETER,
        services.maxOfOrNull { it.odometer } ?: 0,
        fuel.maxOfOrNull { it.odometer } ?: 0,
        oil.maxOfOrNull { it.odometer } ?: 0,
        readings.maxOfOrNull { it.odometer } ?: 0,
        minder.maxOfOrNull { it.odometer } ?: 0,
    ).max()

    /** Average daily driving from every dated odometer point in the last year. */
    fun milesPerDay(points: List<Pair<Long, Int>>, today: LocalDate): Double {
        val cutoff = today.minusDays(365).toEpochDay()
        val recent = points.filter { it.first >= cutoff }.sortedBy { it.first }
        if (recent.size < 2) return DEFAULT_MILES_PER_DAY
        val days = recent.last().first - recent.first().first
        val miles = recent.last().second - recent.first().second
        if (days < 14 || miles <= 0) return DEFAULT_MILES_PER_DAY
        return miles.toDouble() / days
    }

    /** Miles driven per quart added since the last oil change. */
    fun milesPerQuart(services: List<ServiceRecord>, topUps: List<OilTopUp>, odometer: Int): Double? {
        val lastChange = services.filter { "oil_change" in it.jobs }.maxByOrNull { it.odometer }
        val since = topUps.filter { lastChange == null || it.odometer >= lastChange.odometer }
        val quarts = since.sumOf { it.quarts }
        if (quarts <= 0.0) return null
        val start = lastChange?.odometer ?: since.minOf { it.odometer }
        val miles = odometer - start
        if (miles <= 0) return null
        return miles / quarts
    }

    fun snapshot(
        services: List<ServiceRecord>,
        fuel: List<FuelEntry>,
        oil: List<OilTopUp>,
        readings: List<OdometerReading>,
        minder: List<MinderReading>,
        overrides: List<IntervalOverride>,
        today: LocalDate = LocalDate.now(),
    ): GarageSnapshot {
        val odometer = currentOdometer(services, fuel, oil, readings, minder)
        val points = buildList {
            services.forEach { add(it.dateEpochDay to it.odometer) }
            fuel.forEach { add(it.dateEpochDay to it.odometer) }
            oil.forEach { add(it.dateEpochDay to it.odometer) }
            readings.forEach { add(it.dateEpochDay to it.odometer) }
            minder.forEach { add(it.dateEpochDay to it.odometer) }
        }
        val perDay = milesPerDay(points, today)
        val latestMinder = minder.maxWithOrNull(compareBy({ it.dateEpochDay }, { it.id }))
        val overrideMap = overrides.associateBy { it.jobId }

        val statuses = Catalog.jobs.filter { it.id != "other" }.map { job ->
            val override = overrideMap[job.id]
            val miles = if (override != null) override.miles else job.defaultMiles
            val months = if (override != null) override.months else job.defaultMonths
            val last = services.filter { job.id in it.jobs }
                .maxWithOrNull(compareBy({ it.dateEpochDay }, { it.odometer }))

            val minderFlag = latestMinder != null &&
                job.id in Minder.jobsFor(latestMinder.code) &&
                (last == null || last.dateEpochDay < latestMinder.dateEpochDay)

            var dueOdo: Int? = null
            var dueDate: LocalDate? = null
            var milesLeft: Int? = null
            var projected: LocalDate? = null
            if (last != null) {
                if (miles != null) {
                    dueOdo = last.odometer + miles
                    milesLeft = dueOdo - odometer
                    projected = today.plusDays((max(milesLeft, 0) / perDay).roundToInt().toLong())
                }
                if (months != null) dueDate = LocalDate.ofEpochDay(last.dateEpochDay).plusMonths(months.toLong())
            }

            val state = when {
                minderFlag -> DueState.MINDER
                last == null && job.catchUp -> DueState.CATCH_UP
                last == null && miles == null && months == null -> DueState.NO_INTERVAL
                last == null -> DueState.NO_HISTORY
                miles == null && months == null -> DueState.NO_INTERVAL
                (milesLeft != null && milesLeft < 0) || (dueDate != null && dueDate.isBefore(today)) -> DueState.OVERDUE
                (milesLeft != null && milesLeft <= SOON_MILES) ||
                    (dueDate != null && !dueDate.isAfter(today.plusDays(SOON_DAYS))) -> DueState.DUE_SOON
                else -> DueState.OK
            }
            JobStatus(job, state, last, miles, months, override != null, dueOdo, dueDate, milesLeft, projected)
        }

        return GarageSnapshot(
            odometer = odometer,
            milesPerDay = perDay,
            statuses = statuses,
            latestMinder = latestMinder,
            milesPerQuart = milesPerQuart(services, oil, odometer),
        )
    }

    /** MPG for each full fill-up, measured against the previous full fill-up. */
    fun mpgByEntry(entries: List<FuelEntry>): Map<Long, Double> {
        val sorted = entries.sortedBy { it.odometer }
        val result = mutableMapOf<Long, Double>()
        var lastFullOdo: Int? = null
        var gallonsSince = 0.0
        for (e in sorted) {
            if (lastFullOdo == null) {
                if (e.fullTank) lastFullOdo = e.odometer
                continue
            }
            gallonsSince += e.gallons
            if (e.fullTank) {
                val miles = e.odometer - lastFullOdo
                if (miles > 0 && gallonsSince > 0) result[e.id] = miles / gallonsSince
                lastFullOdo = e.odometer
                gallonsSince = 0.0
            }
        }
        return result
    }
}
