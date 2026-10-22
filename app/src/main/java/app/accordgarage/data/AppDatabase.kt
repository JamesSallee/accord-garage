package app.accordgarage.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
    ],
    version = 1,
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

    companion object {
        const val FILE_NAME = "garage.db"

        fun open(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, FILE_NAME)
                // One file on disk, so a backup is a plain copy.
                .setJournalMode(JournalMode.TRUNCATE)
                .build()
    }
}
