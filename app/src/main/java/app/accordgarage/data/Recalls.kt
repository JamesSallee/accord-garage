package app.accordgarage.data

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Recall(
    val campaign: String,
    val component: String,
    val summary: String,
    val consequence: String,
    val remedy: String,
    val reportDate: String,
)

/**
 * Recalls for the 2008 Accord from NHTSA's public recalls API. The API is per model
 * year, so it can list campaigns that don't apply to this exact car; the owner marks
 * each one done, and the VIN lookup on nhtsa.gov is offered for a per-car answer.
 */
class RecallRepository(private val prefs: SharedPreferences) {

    fun cached(): List<Recall> = parse(prefs.getString(KEY_JSON, null))
    fun lastCheckedEpochDay(): Long? = prefs.getLong(KEY_CHECKED, -1L).takeIf { it >= 0 }

    /** Downloads the latest list. Returns campaigns that weren't in the previous list. */
    suspend fun refresh(todayEpochDay: Long): Result<List<Recall>> = withContext(Dispatchers.IO) {
        runCatching {
            val before = cached().map { it.campaign }.toSet()
            val conn = (URL(Vehicle.RECALLS_API).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("Accept", "application/json")
            }
            val body = try {
                if (conn.responseCode !in 200..299) error("NHTSA answered ${conn.responseCode}")
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
            val results = JSONObject(body).optJSONArray("results") ?: JSONArray()
            prefs.edit()
                .putString(KEY_JSON, results.toString())
                .putLong(KEY_CHECKED, todayEpochDay)
                .apply()
            parse(results.toString()).filter { it.campaign !in before && before.isNotEmpty() }
        }
    }

    private fun parse(json: String?): List<Recall> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Recall(
                    campaign = o.optString("NHTSACampaignNumber"),
                    component = o.optString("Component"),
                    summary = o.optString("Summary"),
                    consequence = o.optString("Consequence"),
                    remedy = o.optString("Remedy"),
                    reportDate = o.optString("ReportReceivedDate"),
                )
            }.filter { it.campaign.isNotBlank() }.distinctBy { it.campaign }.sortedByDescending { it.campaign }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val KEY_JSON = "recalls_json"
        private const val KEY_CHECKED = "recalls_checked_day"
    }
}
