package app.accordgarage.data

import android.content.Context
import android.content.SharedPreferences
import app.accordgarage.domain.DueEngine
import app.accordgarage.domain.GarageSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/** Everything the screens need, built once per process. */
class Container(context: Context) {
    private val app = context.applicationContext

    val db: AppDatabase by lazy { AppDatabase.open(app) }
    val prefs: SharedPreferences = app.getSharedPreferences("garage", Context.MODE_PRIVATE)
    val recalls = RecallRepository(prefs)
    val manual by lazy { ManualRepository(app, db) }
    val attachments = AttachmentFiles(app)
    val backup by lazy { BackupService(app) { db } }

    val snapshot: Flow<GarageSnapshot> by lazy {
        val base = combine(
            db.services().all(),
            db.fuel().all(),
            db.oil().all(),
            db.odometer().all(),
            db.minder().all(),
        ) { s, f, o, r, m -> Inputs(s, f, o, r, m) }
        combine(base, db.settings().overrides(), db.settings().snoozes()) { i, ov, sn ->
            DueEngine.snapshot(i.services, i.fuel, i.oil, i.readings, i.minder, ov, LocalDate.now(), sn)
        }
    }

    suspend fun snapshotNow(): GarageSnapshot = DueEngine.snapshot(
        db.services().allNow(),
        db.fuel().allNow(),
        db.oil().allNow(),
        db.odometer().allNow(),
        db.minder().allNow(),
        db.settings().overridesNow(),
        LocalDate.now(),
        db.settings().snoozesNow(),
    )

    private data class Inputs(
        val services: List<ServiceRecord>,
        val fuel: List<FuelEntry>,
        val oil: List<OilTopUp>,
        val readings: List<OdometerReading>,
        val minder: List<MinderReading>,
    )

    // Small settings kept in preferences.
    var lightTheme: Boolean
        get() = prefs.getBoolean("light_theme", false)
        set(v) = prefs.edit().putBoolean("light_theme", v).apply()

    /** Warn when oil use since the last change is worse than this many miles per quart; 0 = off. */
    var oilAlertMilesPerQuart: Int
        get() = prefs.getInt("oil_alert_mpq", 1_000)
        set(v) = prefs.edit().putInt("oil_alert_mpq", v).apply()
}
