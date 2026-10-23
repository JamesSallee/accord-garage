package app.accordgarage.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ServiceRecord::class,
        FuelEntry::class,
        OilTopUp::class,
        OdometerReading::class,
        MinderReading::class,
        Attachment::class,
        DocumentItem::class,
        ManualDoc::class,
        ManualPage::class,
        ManualBookmark::class,
        RecallStatus::class,
        IntervalOverride::class,
        TutorialNote::class,
        StepCheck::class,
        SpecValue::class,
        PartUsed::class,
        WearReading::class,
        JobSnooze::class,
        ManualSection::class,
        ManualNote::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun services(): ServiceDao
    abstract fun fuel(): FuelDao
    abstract fun oil(): OilDao
    abstract fun odometer(): OdometerDao
    abstract fun minder(): MinderDao
    abstract fun attachments(): AttachmentDao
    abstract fun documents(): DocumentDao
    abstract fun manual(): ManualDao
    abstract fun settings(): SettingsDao
    abstract fun parts(): PartDao
    abstract fun wear(): WearDao

    companion object {
        const val FILE_NAME = "garage.db"

        /** Itemised parts, wear readings, snoozes, manual chapters and page notes. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `service_records` ADD COLUMN `shopEstimate` REAL")
                db.execSQL("ALTER TABLE `service_records` ADD COLUMN `tutorialId` TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `parts_used` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`recordId` INTEGER NOT NULL, `name` TEXT NOT NULL, `brand` TEXT NOT NULL, `partNumber` TEXT NOT NULL, " +
                        "`quantity` REAL NOT NULL, `unit` TEXT NOT NULL, `price` REAL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `wear_readings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`dateEpochDay` INTEGER NOT NULL, `odometer` INTEGER NOT NULL, `kind` TEXT NOT NULL, " +
                        "`value` REAL NOT NULL, `serviceId` INTEGER)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `job_snoozes` (`jobId` TEXT NOT NULL, `untilEpochDay` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`jobId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `manual_sections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`manualId` INTEGER NOT NULL, `title` TEXT NOT NULL, `pageNumber` INTEGER NOT NULL, `level` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `manual_notes` (`docId` INTEGER NOT NULL, `pageNumber` INTEGER NOT NULL, " +
                        "`text` TEXT NOT NULL, PRIMARY KEY(`docId`, `pageNumber`))"
                )
            }
        }

        fun open(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, FILE_NAME)
                // One file on disk, so a backup is a plain copy.
                .setJournalMode(JournalMode.TRUNCATE)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
