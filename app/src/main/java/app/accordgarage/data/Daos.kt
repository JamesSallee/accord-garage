package app.accordgarage.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {
    @Query("SELECT * FROM service_records ORDER BY dateEpochDay DESC, odometer DESC, id DESC")
    fun all(): Flow<List<ServiceRecord>>

    @Query("SELECT * FROM service_records ORDER BY dateEpochDay DESC, odometer DESC, id DESC")
    suspend fun allNow(): List<ServiceRecord>

    @Query("SELECT * FROM service_records WHERE id = :id")
    suspend fun get(id: Long): ServiceRecord?

    @Upsert
    suspend fun upsert(record: ServiceRecord): Long

    @Delete
    suspend fun delete(record: ServiceRecord)
}

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_entries ORDER BY odometer DESC, id DESC")
    fun all(): Flow<List<FuelEntry>>

    @Query("SELECT * FROM fuel_entries ORDER BY odometer DESC, id DESC")
    suspend fun allNow(): List<FuelEntry>

    @Query("SELECT * FROM fuel_entries WHERE id = :id")
    suspend fun get(id: Long): FuelEntry?

    @Upsert
    suspend fun upsert(entry: FuelEntry): Long

    @Delete
    suspend fun delete(entry: FuelEntry)
}

@Dao
interface OilDao {
    @Query("SELECT * FROM oil_topups ORDER BY odometer DESC, id DESC")
    fun all(): Flow<List<OilTopUp>>

    @Query("SELECT * FROM oil_topups ORDER BY odometer DESC, id DESC")
    suspend fun allNow(): List<OilTopUp>

    @Query("SELECT * FROM oil_topups WHERE id = :id")
    suspend fun get(id: Long): OilTopUp?

    @Upsert
    suspend fun upsert(entry: OilTopUp): Long

    @Delete
    suspend fun delete(entry: OilTopUp)
}

@Dao
interface OdometerDao {
    @Query("SELECT * FROM odometer_readings ORDER BY dateEpochDay DESC, id DESC")
    fun all(): Flow<List<OdometerReading>>

    @Query("SELECT * FROM odometer_readings ORDER BY dateEpochDay DESC, id DESC")
    suspend fun allNow(): List<OdometerReading>

    @Insert
    suspend fun insert(reading: OdometerReading): Long
}

@Dao
interface MinderDao {
    @Query("SELECT * FROM minder_readings ORDER BY dateEpochDay DESC, id DESC")
    fun all(): Flow<List<MinderReading>>

    @Query("SELECT * FROM minder_readings ORDER BY dateEpochDay DESC, id DESC")
    suspend fun allNow(): List<MinderReading>

    @Insert
    suspend fun insert(reading: MinderReading): Long

    @Delete
    suspend fun delete(reading: MinderReading)
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE ownerType = :type AND ownerId = :ownerId ORDER BY id")
    fun forOwner(type: String, ownerId: Long): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE ownerType = :type AND ownerId = :ownerId ORDER BY id")
    suspend fun forOwnerNow(type: String, ownerId: Long): List<Attachment>

    @Insert
    suspend fun insert(attachment: Attachment): Long

    @Delete
    suspend fun delete(attachment: Attachment)

    @Query("UPDATE attachments SET ownerId = :newId WHERE ownerType = :type AND ownerId = :oldId")
    suspend fun reassign(type: String, oldId: Long, newId: Long)
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY CASE WHEN expiryEpochDay IS NULL THEN 1 ELSE 0 END, expiryEpochDay, title")
    fun all(): Flow<List<DocumentItem>>

    @Query("SELECT * FROM documents")
    suspend fun allNow(): List<DocumentItem>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun get(id: Long): DocumentItem?

    @Upsert
    suspend fun upsert(item: DocumentItem): Long

    @Delete
    suspend fun delete(item: DocumentItem)
}

data class PageHit(
    val docId: Long,
    val pageNumber: Int,
    val text: String,
)

@Dao
interface ManualDao {
    @Query("SELECT * FROM manual_docs ORDER BY id")
    fun docs(): Flow<List<ManualDoc>>

    @Query("SELECT * FROM manual_docs ORDER BY id")
    suspend fun docsNow(): List<ManualDoc>

    @Query("SELECT * FROM manual_docs WHERE id = :id")
    suspend fun doc(id: Long): ManualDoc?

    @Insert
    suspend fun insertDoc(doc: ManualDoc): Long

    @Query("DELETE FROM manual_docs WHERE id = :id")
    suspend fun deleteDoc(id: Long)

    @Query("INSERT INTO manual_pages(manualId, pageNumber, text) VALUES (:docId, :pageNumber, :text)")
    suspend fun insertPage(docId: Long, pageNumber: Int, text: String)

    @Query("DELETE FROM manual_pages WHERE manualId = :docId")
    suspend fun deletePages(docId: Long)

    @Query("SELECT manualId AS docId, pageNumber, text FROM manual_pages WHERE manual_pages MATCH :query LIMIT 400")
    suspend fun search(query: String): List<PageHit>

    @Query("SELECT text FROM manual_pages")
    suspend fun allText(): List<String>

    @Query("SELECT text FROM manual_pages WHERE manualId = :docId AND pageNumber = :pageNumber LIMIT 1")
    suspend fun pageText(docId: Long, pageNumber: Int): String?

    @Insert
    suspend fun insertSections(sections: List<ManualSection>)

    @Query("SELECT * FROM manual_sections WHERE manualId = :docId ORDER BY pageNumber, id")
    fun sections(docId: Long): Flow<List<ManualSection>>

    @Query("SELECT * FROM manual_sections WHERE manualId = :docId ORDER BY pageNumber, id")
    suspend fun sectionsNow(docId: Long): List<ManualSection>

    @Query("DELETE FROM manual_sections WHERE manualId = :docId")
    suspend fun deleteSections(docId: Long)

    @Query("SELECT * FROM manual_notes ORDER BY docId, pageNumber")
    fun notes(): Flow<List<ManualNote>>

    @Upsert
    suspend fun upsertNote(note: ManualNote)

    @Query("DELETE FROM manual_notes WHERE docId = :docId AND pageNumber = :pageNumber")
    suspend fun deleteNote(docId: Long, pageNumber: Int)

    @Query("DELETE FROM manual_notes WHERE docId = :docId")
    suspend fun deleteNotesFor(docId: Long)

    @Query("SELECT * FROM manual_bookmarks ORDER BY docId, pageNumber")
    fun bookmarks(): Flow<List<ManualBookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBookmark(bookmark: ManualBookmark)

    @Query("DELETE FROM manual_bookmarks WHERE docId = :docId AND pageNumber = :pageNumber")
    suspend fun removeBookmark(docId: Long, pageNumber: Int)

    @Query("DELETE FROM manual_bookmarks WHERE docId = :docId")
    suspend fun removeBookmarksFor(docId: Long)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM recall_status")
    fun recallStatuses(): Flow<List<RecallStatus>>

    @Query("SELECT * FROM recall_status")
    suspend fun recallStatusesNow(): List<RecallStatus>

    @Upsert
    suspend fun upsertRecall(status: RecallStatus)

    @Query("SELECT * FROM interval_overrides")
    fun overrides(): Flow<List<IntervalOverride>>

    @Query("SELECT * FROM interval_overrides")
    suspend fun overridesNow(): List<IntervalOverride>

    @Upsert
    suspend fun upsertOverride(override: IntervalOverride)

    @Query("DELETE FROM interval_overrides WHERE jobId = :jobId")
    suspend fun clearOverride(jobId: String)

    @Query("SELECT * FROM tutorial_notes WHERE tutorialId = :tutorialId")
    fun notes(tutorialId: String): Flow<List<TutorialNote>>

    @Upsert
    suspend fun upsertNote(note: TutorialNote)

    @Query("SELECT `key` FROM step_checks WHERE `key` LIKE :prefix || '%'")
    fun checks(prefix: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCheck(check: StepCheck)

    @Query("DELETE FROM step_checks WHERE `key` = :key")
    suspend fun removeCheck(key: String)

    @Query("SELECT * FROM spec_values")
    fun specs(): Flow<List<SpecValue>>

    @Query("SELECT * FROM spec_values")
    suspend fun specsNow(): List<SpecValue>

    @Query("SELECT * FROM job_snoozes")
    fun snoozes(): Flow<List<JobSnooze>>

    @Query("SELECT * FROM job_snoozes")
    suspend fun snoozesNow(): List<JobSnooze>

    @Upsert
    suspend fun upsertSnooze(snooze: JobSnooze)

    @Query("DELETE FROM job_snoozes WHERE jobId = :jobId")
    suspend fun clearSnooze(jobId: String)

    @Upsert
    suspend fun upsertSpec(value: SpecValue)
}

@Dao
interface PartDao {
    @Query("SELECT * FROM parts_used ORDER BY recordId, id")
    fun all(): Flow<List<PartUsed>>

    @Query("SELECT * FROM parts_used WHERE recordId = :recordId ORDER BY id")
    suspend fun forRecord(recordId: Long): List<PartUsed>

    @Insert
    suspend fun insertAll(parts: List<PartUsed>)

    @Query("DELETE FROM parts_used WHERE recordId = :recordId")
    suspend fun deleteFor(recordId: Long)
}

@Dao
interface WearDao {
    @Query("SELECT * FROM wear_readings ORDER BY odometer, dateEpochDay, id")
    fun all(): Flow<List<WearReading>>

    @Query("SELECT * FROM wear_readings WHERE serviceId = :serviceId ORDER BY id")
    suspend fun forService(serviceId: Long): List<WearReading>

    @Insert
    suspend fun insert(reading: WearReading): Long

    @Insert
    suspend fun insertAll(readings: List<WearReading>)

    @Delete
    suspend fun delete(reading: WearReading)

    @Query("DELETE FROM wear_readings WHERE serviceId = :serviceId")
    suspend fun deleteForService(serviceId: Long)
}
