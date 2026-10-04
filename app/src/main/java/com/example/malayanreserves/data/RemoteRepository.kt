package com.example.malayanreserves.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Talks to the PHP API in /backend. Every call is a JSON POST to `api.php?a=<action>`; the session
 * token from login is sent as a bearer token. Passwords never appear in URLs.
 */
class RemoteRepository(context: Context, baseUrl: String) : ParkingRepository {

    override val isDemo = false

    private val endpoint = baseUrl.trimEnd('/') + "/api.php"
    private val prefs = context.applicationContext.getSharedPreferences("session_remote", Context.MODE_PRIVATE)
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private var cachedUser: User? = null

    override suspend fun register(reg: Registration): User = signIn(call("register", JSONObject().apply {
        put("name", reg.name.trim()); put("email", reg.email.trim().lowercase()); put("password", reg.password)
        put("type", reg.type.name); put("vehicle", reg.vehicle.toJson())
    }))

    override suspend fun login(email: String, password: String): User =
        signIn(call("login", JSONObject().put("email", email.trim().lowercase()).put("password", password)))

    override suspend fun demoLogin(): User = throw AppError("Demo sign-in is only available in demo mode.")

    override suspend fun currentUser(): User? {
        if (prefs.getString("token", null) == null) return null
        return try {
            call("me").getJSONObject("user").toUser().also { cachedUser = it }
        } catch (e: AppError) {
            if (e.message == SESSION_EXPIRED) null else cachedUser ?: throw e
        }
    }

    override suspend fun logout() {
        runCatching { call("logout") }
        prefs.edit().remove("token").apply()
        cachedUser = null
    }

    override suspend fun updateVehicle(vehicle: Vehicle): User =
        call("vehicle", JSONObject().put("vehicle", vehicle.toJson())).getJSONObject("user").toUser().also { cachedUser = it }

    override suspend fun lot(): Lot {
        val res = call("lot")
        val slots = res.getJSONArray("slots").objects().map { o ->
            val slot = Rules.slot(o.getString("id")) ?: Slot(o.getString("id"), 0, false, "")
            SlotView(
                slot = slot,
                state = SlotState.valueOf(o.getString("state")),
                vehicle = o.optJSONObject("vehicle")?.toVehicle(),
                until = o.optLongOrNull("until"),
                nextStart = o.optLongOrNull("next_start"),
                bookings = o.getJSONArray("bookings").objects().map {
                    Booking(it.getLong("start"), it.getLong("end"), it.optBoolean("mine"))
                }
            )
        }
        return Lot(slots, res.optLong("now", System.currentTimeMillis()))
    }

    override suspend fun myReservation(): Reservation? = call("mine").optJSONObject("reservation")?.toReservation()

    override suspend fun history(): List<Reservation> = call("history").getJSONArray("reservations").objects().map { it.toReservation() }

    override suspend fun reserve(slotId: String, start: Long, durationMin: Int): Reservation =
        call("reserve", JSONObject().put("slot", slotId).put("start", start).put("duration", durationMin))
            .getJSONObject("reservation").toReservation()

    override suspend fun arrive(): Reservation = call("arrive").getJSONObject("reservation").toReservation()
    override suspend fun extend(): Reservation = call("extend").getJSONObject("reservation").toReservation()
    override suspend fun end(): Reservation = call("end").getJSONObject("reservation").toReservation()

    // ---------- transport ----------

    private fun signIn(res: JSONObject): User {
        prefs.edit().putString("token", res.getString("token")).apply()
        return res.getJSONObject("user").toUser().also { cachedUser = it }
    }

    private suspend fun call(action: String, body: JSONObject = JSONObject()): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$endpoint?a=$action")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .apply { prefs.getString("token", null)?.let { header("Authorization", "Bearer $it") } }
            .build()
        val text = try {
            http.newCall(request).execute().use { it.body?.string().orEmpty() }
        } catch (e: IOException) {
            throw AppError("Can't reach the parking server. Check your connection.")
        }
        val json = try { JSONObject(text) } catch (e: Exception) { throw AppError("The parking server sent an unexpected response.") }
        if (!json.optBoolean("ok")) {
            if (json.optString("code") == "auth") {
                prefs.edit().remove("token").apply()
                throw AppError(SESSION_EXPIRED)
            }
            throw AppError(json.optString("error", "Something went wrong."))
        }
        json
    }

    private fun Vehicle.toJson() = JSONObject().put("type", type).put("color", color).put("plate", plate)
    private fun JSONObject.toVehicle() = Vehicle(getString("type"), getString("color"), getString("plate"))
    private fun JSONObject.toUser() = User(
        id = getLong("id"), name = getString("name"), email = getString("email"),
        type = UserType.of(getString("type")), vehicle = getJSONObject("vehicle").toVehicle()
    )
    private fun JSONObject.toReservation() = Reservation(
        id = getLong("id"), slotId = getString("slot"), userId = getLong("user_id"),
        start = getLong("start"), end = getLong("end"), status = ResStatus.valueOf(getString("status")),
        arrivedAt = optLongOrNull("arrived_at"), endedAt = optLongOrNull("ended_at"), extended = optBoolean("extended")
    )
    private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    companion object {
        const val SESSION_EXPIRED = "Your session has expired. Please sign in again."
    }
}
