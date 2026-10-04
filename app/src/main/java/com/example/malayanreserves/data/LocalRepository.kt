package com.example.malayanreserves.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.random.Random

/**
 * Demo mode: an on-device SQLite database with the same rules as the server, plus a handful of
 * simulated drivers who arrive, leave and book ahead so the lot feels alive.
 */
class LocalRepository(context: Context, private val clock: () -> Long = System::currentTimeMillis) : ParkingRepository {

    override val isDemo = true

    private val helper = Db(context.applicationContext)
    private val db: SQLiteDatabase get() = helper.writableDatabase
    private var seeded = false
    private val prefs = context.applicationContext.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val rng = Random(clock())
    private var lastSim = 0L

    // ---------- accounts ----------

    override suspend fun register(reg: Registration): User = io {
        val email = reg.email.trim().lowercase()
        val name = reg.name.trim()
        val plate = Rules.normalizePlate(reg.vehicle.plate)
        when {
            name.length < 2 -> fail("Enter your full name.")
            !Rules.validEmail(email) -> fail("Use your school email ending in ${Rules.EMAIL_DOMAIN}.")
            reg.password.length < 8 -> fail("Use a password with at least 8 characters.")
            reg.vehicle.type !in Rules.VEHICLE_TYPES -> fail("Choose your vehicle type.")
            reg.vehicle.color !in Rules.VEHICLE_COLORS -> fail("Choose your vehicle color.")
            !Rules.validPlate(plate) -> fail("Enter a plate number like ABC 1234.")
        }
        if (count("SELECT COUNT(*) FROM users WHERE email = ?", email) > 0) fail("An account with this email already exists.")
        if (count("SELECT COUNT(*) FROM users WHERE plate = ?", plate) > 0) fail("This plate number is already registered.")
        val id = insertUser(name, email, reg.password, reg.type, reg.vehicle.copy(plate = plate), sim = false)
        signIn(id)
    }

    override suspend fun login(email: String, password: String): User = io {
        val row = db.rawQuery("SELECT id, pw_hash, salt FROM users WHERE email = ? AND sim = 0", arrayOf(email.trim().lowercase()))
            .use { if (it.moveToFirst()) Triple(it.getLong(0), it.getString(1), it.getString(2)) else null }
        if (row == null || !MessageDigest.isEqual(hash(password, row.third).toByteArray(), row.second.toByteArray())) {
            fail("Incorrect email or password.")
        }
        signIn(row!!.first)
    }

    override suspend fun demoLogin(): User = io {
        val id = db.rawQuery("SELECT id FROM users WHERE email = ?", arrayOf(DEMO_EMAIL)).use { if (it.moveToFirst()) it.getLong(0) else null }
            ?: fail("The demo account is missing.")
        signIn(id)
    }

    override suspend fun currentUser(): User? = io { userId()?.let { user(it) } }

    override suspend fun logout() = io { prefs.edit().remove("user").apply() }

    override suspend fun updateVehicle(vehicle: Vehicle): User = io {
        val me = requireUser()
        val plate = Rules.normalizePlate(vehicle.plate)
        when {
            vehicle.type !in Rules.VEHICLE_TYPES -> fail("Choose your vehicle type.")
            vehicle.color !in Rules.VEHICLE_COLORS -> fail("Choose your vehicle color.")
            !Rules.validPlate(plate) -> fail("Enter a plate number like ABC 1234.")
        }
        if (count("SELECT COUNT(*) FROM users WHERE plate = ? AND id != ?", plate, me.id.toString()) > 0) {
            fail("This plate number is already registered.")
        }
        db.update("users", ContentValues().apply {
            put("v_type", vehicle.type); put("v_color", vehicle.color); put("plate", plate)
        }, "id = ?", arrayOf(me.id.toString()))
        user(me.id)!!
    }

    // ---------- parking ----------

    override suspend fun lot(): Lot = io {
        val now = clock()
        refresh(now)
        simulate(now)
        val me = userId()
        val live = liveReservations()
        val slots = Rules.SLOTS.map { slot ->
            val mine = live.filter { it.slotId == slot.id }
            val current = mine.firstOrNull { it.start <= now && (it.status == ResStatus.HELD || it.status == ResStatus.ACTIVE) }
            val state = when {
                current == null -> SlotState.AVAILABLE
                current.userId == me -> SlotState.MINE
                current.status == ResStatus.ACTIVE -> SlotState.OCCUPIED
                else -> SlotState.HELD
            }
            val vehicle = current?.takeIf { it.status == ResStatus.ACTIVE || it.userId == me }?.let { r ->
                user(r.userId)?.vehicle?.let { v -> if (r.userId == me) v else v.copy(plate = Rules.maskPlate(v.plate)) }
            }
            SlotView(
                slot = slot,
                state = state,
                vehicle = vehicle,
                until = current?.end,
                nextStart = mine.filter { it.start > now }.minOfOrNull { it.start },
                bookings = mine.sortedBy { it.start }.map { Booking(it.start, it.end, it.userId == me) }
            )
        }
        Lot(slots, now)
    }

    override suspend fun myReservation(): Reservation? = io {
        refresh(clock())
        val me = requireUser()
        liveReservations().firstOrNull { it.userId == me.id }
    }

    override suspend fun history(): List<Reservation> = io {
        val me = requireUser()
        query("SELECT * FROM reservations WHERE user_id = ? AND status IN (?, ?, ?) ORDER BY start DESC LIMIT 20",
            me.id.toString(), ResStatus.COMPLETED.name, ResStatus.CANCELLED.name, ResStatus.NO_SHOW.name)
    }

    override suspend fun reserve(slotId: String, start: Long, durationMin: Int): Reservation = io {
        val now = clock()
        refresh(now)
        val me = requireUser()
        val slot = Rules.slot(slotId) ?: fail("That parking slot doesn't exist.")
        if (slot.staffOnly && !me.type.isStaff) fail("${slot.id} is reserved for faculty and staff.")
        if (durationMin < Rules.MIN_HOURS * 60 || durationMin > Rules.MAX_HOURS * 60 || durationMin % Rules.STEP_MIN != 0) {
            fail("Choose a duration between ${Rules.MIN_HOURS} and ${Rules.MAX_HOURS} hours.")
        }
        val parkNow = start <= now + Rules.MINUTE
        val begin = if (parkNow) now else start
        if (!parkNow) {
            if (begin % (Rules.STEP_MIN * Rules.MINUTE) != 0L) fail("Start times are on the hour or half hour.")
            if (begin > now + Rules.DAYS_AHEAD * 24 * 60 * Rules.MINUTE) fail("You can book up to ${Rules.DAYS_AHEAD} days ahead.")
        }
        val end = begin + durationMin * Rules.MINUTE
        if (!parkNow && !Rules.withinHours(begin, end)) fail("Advance bookings run from 6 AM to 10 PM.")
        if (liveReservations().any { it.userId == me.id }) fail("You already have a reservation. Finish or cancel it first.")
        if (conflict(slot.id, begin, end, exceptId = null)) fail("${slot.id} is already booked for part of that time.")
        val id = db.insert("reservations", null, ContentValues().apply {
            put("slot", slot.id); put("user_id", me.id); put("start", begin); put("end_time", end)
            put("status", (if (parkNow) ResStatus.HELD else ResStatus.UPCOMING).name); put("extended", 0)
        })
        reservation(id)!!
    }

    override suspend fun arrive(): Reservation = io {
        val now = clock()
        refresh(now)
        val r = mine() ?: fail("You don't have a reservation.")
        when (r.status) {
            ResStatus.ACTIVE -> fail("You're already checked in.")
            ResStatus.UPCOMING -> {
                // Early arrival is fine within 15 minutes if the slot is free until the booking starts.
                if (r.start - now > 15 * Rules.MINUTE) fail("You can check in up to 15 minutes before your start time.")
                if (conflict(r.slotId, now, r.start, exceptId = r.id)) fail("${r.slotId} is still in use. Please wait until your start time.")
            }
            else -> {}
        }
        db.update("reservations", ContentValues().apply {
            put("status", ResStatus.ACTIVE.name); put("arrived_at", now)
            if (r.start > now) put("start", now)
        }, "id = ?", arrayOf(r.id.toString()))
        reservation(r.id)!!
    }

    override suspend fun extend(): Reservation = io {
        refresh(clock())
        val r = mine() ?: fail("You don't have a reservation.")
        if (r.extended) fail("A reservation can only be extended once.")
        val newEnd = r.end + Rules.EXTEND_MIN * Rules.MINUTE
        if (conflict(r.slotId, r.end, newEnd, exceptId = r.id)) fail("Someone has booked ${r.slotId} right after you, so it can't be extended.")
        db.update("reservations", ContentValues().apply { put("end_time", newEnd); put("extended", 1) }, "id = ?", arrayOf(r.id.toString()))
        reservation(r.id)!!
    }

    override suspend fun end(): Reservation = io {
        val now = clock()
        refresh(now)
        val r = mine() ?: fail("You don't have a reservation.")
        val status = if (r.status == ResStatus.ACTIVE) ResStatus.COMPLETED else ResStatus.CANCELLED
        db.update("reservations", ContentValues().apply { put("status", status.name); put("ended_at", now) }, "id = ?", arrayOf(r.id.toString()))
        reservation(r.id)!!
    }

    // ---------- rules over time ----------

    /** Moves reservations along as time passes. The PHP API runs the same step on every request. */
    private fun refresh(now: Long) {
        val arrivalDeadline = now - Rules.ARRIVAL_WINDOW_MIN * Rules.MINUTE
        db.execSQL("UPDATE reservations SET status = ? WHERE status = ? AND start <= ?",
            arrayOf(ResStatus.HELD.name, ResStatus.UPCOMING.name, now))
        // Simulated drivers always turn up a few minutes after their start time.
        db.execSQL("UPDATE reservations SET status = ?, arrived_at = start + 180000 WHERE status = ? AND start + 180000 <= ? " +
            "AND user_id IN (SELECT id FROM users WHERE sim = 1)", arrayOf(ResStatus.ACTIVE.name, ResStatus.HELD.name, now))
        db.execSQL("UPDATE reservations SET status = ?, ended_at = start + ? WHERE status = ? AND start <= ?",
            arrayOf(ResStatus.NO_SHOW.name, Rules.ARRIVAL_WINDOW_MIN * Rules.MINUTE, ResStatus.HELD.name, arrivalDeadline))
        db.execSQL("UPDATE reservations SET status = ?, ended_at = end_time WHERE status = ? AND end_time <= ?",
            arrayOf(ResStatus.COMPLETED.name, ResStatus.ACTIVE.name, now))
    }

    /** Keeps roughly half the lot busy and a few bookings ahead, with simulated drivers. */
    private fun simulate(now: Long) {
        if (now - lastSim < 20_000) return
        lastSim = now
        val sims = simUsers()
        if (sims.isEmpty()) return
        val live = liveReservations()
        val busyUsers = live.map { it.userId }.toSet()
        val idle = sims.filter { it.id !in busyUsers }.shuffled(rng)
        val parkedNow = live.count { it.start <= now }
        val ahead = live.count { it.start > now }
        var i = 0
        if (parkedNow < 6 && i < idle.size && rng.nextInt(3) == 0) {
            val u = idle[i++]
            val minutes = (2 + rng.nextInt(5)) * 30
            freeSlotFor(u, now, now + minutes * Rules.MINUTE)?.let { addSim(u, it, now - 4 * Rules.MINUTE, minutes, ResStatus.ACTIVE) }
        }
        if (ahead < 4 && i < idle.size) {
            val u = idle[i]
            val start = Rules.roundUp(now) + (1 + rng.nextInt(10)) * 30 * Rules.MINUTE
            val minutes = (2 + rng.nextInt(5)) * 30
            if (Rules.withinHours(start, start + minutes * Rules.MINUTE)) {
                freeSlotFor(u, start, start + minutes * Rules.MINUTE)?.let { addSim(u, it, start, minutes, ResStatus.UPCOMING) }
            }
        }
    }

    private fun freeSlotFor(u: User, start: Long, end: Long): String? =
        Rules.SLOTS.filter { u.type.isStaff || !it.staffOnly }.shuffled(rng).firstOrNull { !conflict(it.id, start, end, null) }?.id

    private fun addSim(u: User, slot: String, start: Long, minutes: Int, status: ResStatus) {
        db.insert("reservations", null, ContentValues().apply {
            put("slot", slot); put("user_id", u.id); put("start", start); put("end_time", start + minutes * Rules.MINUTE)
            put("status", status.name); put("extended", 0)
            if (status == ResStatus.ACTIVE) put("arrived_at", start)
        })
    }

    private fun conflict(slot: String, start: Long, end: Long, exceptId: Long?): Boolean =
        liveReservations().any { it.slotId == slot && it.id != exceptId && Rules.overlaps(start, end, it.start, it.end) }

    // ---------- seed data ----------

    private fun seed() {
        val now = clock()
        val demo = insertUser("Demo Driver", DEMO_EMAIL, "demo1234", UserType.STUDENT, Vehicle("Sedan", "Blue", "NBC 1234"), sim = false)
        val drivers = listOf(
            Triple("Carla M.", UserType.FACULTY, Vehicle("SUV", "White", "NDA 4821")),
            Triple("Paolo R.", UserType.STUDENT, Vehicle("Hatchback", "Red", "WQX 902")),
            Triple("Bea S.", UserType.STUDENT, Vehicle("Sedan", "Silver", "AAB 7315")),
            Triple("Engr. Tan", UserType.FACULTY, Vehicle("Pickup", "Black", "NFE 6640")),
            Triple("Miguel D.", UserType.STUDENT, Vehicle("Sedan", "Gray", "DCA 2297")),
            Triple("Rina L.", UserType.EMPLOYEE, Vehicle("Van", "White", "NHG 5108")),
            Triple("Jules A.", UserType.STUDENT, Vehicle("Hatchback", "Yellow", "ZTR 481")),
            Triple("Ms. Cruz", UserType.EMPLOYEE, Vehicle("Sedan", "Black", "NAQ 3390")),
            Triple("Kyle B.", UserType.STUDENT, Vehicle("SUV", "Green", "BKL 7724")),
            Triple("Andrea V.", UserType.STUDENT, Vehicle("Sedan", "Orange", "VRA 1185")),
            Triple("Sir Ramos", UserType.FACULTY, Vehicle("SUV", "Brown", "NJB 6052"))
        )
        val ids = drivers.mapIndexed { n, (name, type, v) ->
            insertUser(name, "driver${n + 1}@live.mcl.edu.ph", randomToken(), type, v, sim = true)
        }
        val m = Rules.MINUTE
        val base = Rules.roundUp(now)
        fun add(driver: Int, slot: String, start: Long, minutes: Int, status: ResStatus) =
            addSim(user(ids[driver])!!, slot, start, minutes, status)
        add(0, "P01", now - 70 * m, 180, ResStatus.ACTIVE)
        add(3, "P02", now - 40 * m, 150, ResStatus.ACTIVE)
        add(1, "P05", now - 25 * m, 120, ResStatus.ACTIVE)
        add(2, "P07", now - 95 * m, 150, ResStatus.ACTIVE)
        add(4, "P08", now - 10 * m, 90, ResStatus.ACTIVE)
        add(5, "P11", now - 50 * m, 240, ResStatus.ACTIVE)
        add(6, "P04", base + 60 * m, 120, ResStatus.UPCOMING)
        add(7, "P09", base + 30 * m, 180, ResStatus.UPCOMING)
        add(8, "P06", base + 150 * m, 90, ResStatus.UPCOMING)
        add(10, "P03", base + 24 * 60 * m, 120, ResStatus.UPCOMING)
        // A bit of history for the demo account.
        listOf(Triple("P06", 26 * 60, ResStatus.COMPLETED), Triple("P10", 50 * 60, ResStatus.COMPLETED), Triple("P12", 74 * 60, ResStatus.CANCELLED))
            .forEach { (slot, ago, status) ->
                val start = Rules.roundUp(now - ago * m)
                db.insert("reservations", null, ContentValues().apply {
                    put("slot", slot); put("user_id", demo); put("start", start); put("end_time", start + 120 * m)
                    put("status", status.name); put("extended", 0)
                    if (status == ResStatus.COMPLETED) { put("arrived_at", start + 5 * m); put("ended_at", start + 110 * m) }
                    else put("ended_at", start - 60 * m)
                })
            }
    }

    // ---------- helpers ----------

    private fun insertUser(name: String, email: String, password: String, type: UserType, v: Vehicle, sim: Boolean): Long {
        val salt = randomToken()
        return db.insert("users", null, ContentValues().apply {
            put("name", name); put("email", email); put("type", type.name)
            put("v_type", v.type); put("v_color", v.color); put("plate", Rules.normalizePlate(v.plate))
            put("pw_hash", if (sim) "!" else hash(password, salt)); put("salt", salt); put("sim", if (sim) 1 else 0)
        })
    }

    private fun signIn(id: Long): User {
        prefs.edit().putLong("user", id).apply()
        return user(id)!!
    }

    private fun userId(): Long? = prefs.getLong("user", -1).takeIf { it > 0 && user(it) != null }
    private fun requireUser(): User = userId()?.let { user(it) } ?: fail("Please sign in again.")
    private fun mine(): Reservation? = requireUser().let { me -> liveReservations().firstOrNull { it.userId == me.id } }

    private fun user(id: Long): User? =
        db.rawQuery("SELECT * FROM users WHERE id = ?", arrayOf(id.toString())).use { if (it.moveToFirst()) it.toUser() else null }

    private fun simUsers(): List<User> =
        db.rawQuery("SELECT * FROM users WHERE sim = 1", null).use { c -> buildList { while (c.moveToNext()) add(c.toUser()) } }

    private fun reservation(id: Long): Reservation? = query("SELECT * FROM reservations WHERE id = ?", id.toString()).firstOrNull()

    private fun liveReservations(): List<Reservation> =
        query("SELECT * FROM reservations WHERE status IN (?, ?, ?) ORDER BY start",
            ResStatus.UPCOMING.name, ResStatus.HELD.name, ResStatus.ACTIVE.name)

    private fun query(sql: String, vararg args: String): List<Reservation> =
        db.rawQuery(sql, args).use { c -> buildList { while (c.moveToNext()) add(c.toReservation()) } }

    private fun count(sql: String, vararg args: String): Long = db.rawQuery(sql, args).use { it.moveToFirst(); it.getLong(0) }

    private fun Cursor.str(col: String) = getString(getColumnIndexOrThrow(col))
    private fun Cursor.long(col: String) = getLong(getColumnIndexOrThrow(col))
    private fun Cursor.longOrNull(col: String) = getColumnIndexOrThrow(col).let { if (isNull(it)) null else getLong(it) }

    private fun Cursor.toUser() = User(
        id = long("id"), name = str("name"), email = str("email"), type = UserType.of(str("type")),
        vehicle = Vehicle(str("v_type"), str("v_color"), str("plate"))
    )

    private fun Cursor.toReservation() = Reservation(
        id = long("id"), slotId = str("slot"), userId = long("user_id"), start = long("start"), end = long("end_time"),
        status = ResStatus.valueOf(str("status")), arrivedAt = longOrNull("arrived_at"), endedAt = longOrNull("ended_at"),
        extended = long("extended") == 1L
    )

    /** Runs on a background thread. The first call opens the database and seeds it, which can take a moment. */
    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) {
        synchronized(this@LocalRepository) {
            if (!seeded) {
                if (count("SELECT COUNT(*) FROM users") == 0L) seed()
                seeded = true
            }
            block()
        }
    }

    private fun fail(message: String): Nothing = throw AppError(message)

    private class Db(context: Context) : SQLiteOpenHelper(context, "malayan_reserves.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, email TEXT NOT NULL UNIQUE, type TEXT NOT NULL,
                v_type TEXT NOT NULL, v_color TEXT NOT NULL, plate TEXT NOT NULL UNIQUE,
                pw_hash TEXT NOT NULL, salt TEXT NOT NULL, sim INTEGER NOT NULL DEFAULT 0)""")
            db.execSQL("""CREATE TABLE reservations (
                id INTEGER PRIMARY KEY AUTOINCREMENT, slot TEXT NOT NULL, user_id INTEGER NOT NULL REFERENCES users(id),
                start INTEGER NOT NULL, end_time INTEGER NOT NULL, status TEXT NOT NULL,
                arrived_at INTEGER, ended_at INTEGER, extended INTEGER NOT NULL DEFAULT 0)""")
            db.execSQL("CREATE INDEX idx_res_slot ON reservations(slot, status)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }

    companion object {
        const val DEMO_EMAIL = "demo@live.mcl.edu.ph"

        private fun randomToken(): String =
            ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }

        private fun hash(password: String, salt: String): String {
            val spec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), 20_000, 256)
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
                .joinToString("") { "%02x".format(it) }
        }
    }
}
