package app.accordgarage.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/** One visit or job. [jobIds] is a comma-separated list of catalog job ids. */
@Entity(tableName = "service_records")
data class ServiceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobIds: String,
    val title: String = "",
    val dateEpochDay: Long,
    val odometer: Int,
    val shop: String = "",
    val partsCost: Double? = null,
    val laborCost: Double? = null,
    val minderCode: String = "",
    val parts: String = "",
    val notes: String = "",
) {
    val jobs: List<String> get() = jobIds.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    val totalCost: Double get() = (partsCost ?: 0.0) + (laborCost ?: 0.0)
    val isDiy: Boolean get() = shop.isBlank()
}

@Entity(tableName = "fuel_entries")
data class FuelEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val odometer: Int,
    val gallons: Double,
    val pricePerGallon: Double? = null,
    val fullTank: Boolean = true,
    val station: String = "",
)

@Entity(tableName = "oil_topups")
data class OilTopUp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val odometer: Int,
    val quarts: Double,
)

@Entity(tableName = "odometer_readings")
data class OdometerReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val odometer: Int,
)

/** What the dash's Maintenance Minder showed, e.g. oil life 15% and code "B13". */
@Entity(tableName = "minder_readings")
data class MinderReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val odometer: Int,
    val oilLifePercent: Int? = null,
    val code: String = "",
)

/** A photo or receipt. [ownerType] is "service" or "document". */
@Entity(tableName = "attachments")
data class Attachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerType: String,
    val ownerId: Long,
    val fileName: String,
    val ocrText: String = "",
)

@Entity(tableName = "documents")
data class DocumentItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val title: String,
    val expiryEpochDay: Long? = null,
    val notes: String = "",
)

@Entity(tableName = "manual_docs")
data class ManualDoc(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val fileName: String,
    val pageCount: Int,
    val importedEpochDay: Long,
)

@Fts4
@Entity(tableName = "manual_pages")
data class ManualPage(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Int,
    val docId: Long,
    val pageNumber: Int,
    val text: String,
)

@Entity(tableName = "manual_bookmarks", primaryKeys = ["docId", "pageNumber"])
data class ManualBookmark(
    val docId: Long,
    val pageNumber: Int,
    val label: String = "",
)

@Entity(tableName = "recall_status")
data class RecallStatus(
    @PrimaryKey val campaign: String,
    val completedEpochDay: Long? = null,
    val notes: String = "",
)

@Entity(tableName = "interval_overrides")
data class IntervalOverride(
    @PrimaryKey val jobId: String,
    val miles: Int? = null,
    val months: Int? = null,
)

@Entity(tableName = "tutorial_notes", primaryKeys = ["tutorialId", "stepIndex"])
data class TutorialNote(
    val tutorialId: String,
    val stepIndex: Int,
    val text: String,
)

/** A ticked checklist item inside a tutorial step. key = "tutorialId:step:item". */
@Entity(tableName = "step_checks")
data class StepCheck(
    @PrimaryKey val key: String,
)

/** A quick-spec value the owner entered or confirmed from the manual. */
@Entity(tableName = "spec_values")
data class SpecValue(
    @PrimaryKey val key: String,
    val value: String,
    val confirmed: Boolean = false,
)
