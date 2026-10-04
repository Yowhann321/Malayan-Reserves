package com.example.malayanreserves.data

import java.util.Calendar

/** Parking rules shared by the demo database and the app UI. The PHP backend enforces the same rules. */
object Rules {
    const val EMAIL_DOMAIN = "@live.mcl.edu.ph"
    const val ARRIVAL_WINDOW_MIN = 20
    const val MIN_HOURS = 1
    const val MAX_HOURS = 4
    const val EXTEND_MIN = 60
    const val DAYS_AHEAD = 7
    const val STEP_MIN = 30
    const val MINUTE = 60_000L
    /** Advance bookings run during campus hours. Walk-in "park now" works any time. */
    const val OPEN_HOUR = 6
    const val CLOSE_HOUR = 22

    val VEHICLE_TYPES = listOf("Sedan", "SUV", "Hatchback", "Coupe", "Pickup", "Van")
    val VEHICLE_COLORS = listOf("White", "Black", "Silver", "Gray", "Red", "Blue", "Green", "Yellow", "Orange", "Brown")

    /** The campus lot: four rows of three. Row 1 is reserved for faculty and staff. */
    val SLOTS: List<Slot> = (1..12).map { n ->
        val row = (n - 1) / 3
        val note = when (row) {
            0 -> "Faculty and staff priority, nearest the gate"
            1 -> "Near the gate"
            2 -> "Middle of the lot"
            else -> "Covered parking"
        }
        Slot("P" + n.toString().padStart(2, '0'), row, staffOnly = row == 0, note = note)
    }

    fun slot(id: String): Slot? = SLOTS.firstOrNull { it.id == id }

    fun validEmail(email: String): Boolean {
        val e = email.trim().lowercase()
        return e.endsWith(EMAIL_DOMAIN) && Regex("^[a-z0-9._%+-]+$").matches(e.removeSuffix(EMAIL_DOMAIN))
    }

    /** Philippine plates: "ABC 1234" for cars, "AB 12345" or "123 ABC" style variants are normalized. */
    fun normalizePlate(plate: String): String =
        plate.trim().uppercase().replace(Regex("[^A-Z0-9]+"), " ").replace(Regex("^([A-Z]{2,3})(\\d)"), "$1 $2").trim()

    fun validPlate(plate: String): Boolean = Regex("^[A-Z]{2,3} \\d{3,5}$").matches(normalizePlate(plate))

    /** Other drivers only see part of a plate: "ABC 1234" becomes "ABC ••34". */
    fun maskPlate(plate: String): String {
        val p = normalizePlate(plate)
        val parts = p.split(" ")
        if (parts.size != 2) return "•••"
        val digits = parts[1]
        return parts[0] + " " + "•".repeat((digits.length - 2).coerceAtLeast(1)) + digits.takeLast(2)
    }

    fun passwordStrength(password: String): Int = when {
        password.length >= 10 && password.any { it.isDigit() } && password.any { it.isUpperCase() } -> 3
        password.length >= 8 && password.any { it.isDigit() } -> 2
        password.length >= 6 -> 1
        else -> 0
    }

    fun overlaps(aStart: Long, aEnd: Long, bStart: Long, bEnd: Long) = aStart < bEnd && bStart < aEnd

    fun startOfDay(t: Long): Long = Calendar.getInstance().apply {
        timeInMillis = t
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun roundUp(t: Long, stepMin: Int = STEP_MIN): Long {
        val step = stepMin * MINUTE
        return ((t + step - 1) / step) * step
    }

    /** True when an advance booking from [start] to [end] falls inside campus hours on one day. */
    fun withinHours(start: Long, end: Long): Boolean {
        val day = startOfDay(start)
        return start >= day + OPEN_HOUR * 60 * MINUTE && end <= day + CLOSE_HOUR * 60 * MINUTE
    }

    /** Start times on a day (every 30 minutes, campus hours) where a booking of [durationMin] fits around [bookings]. */
    fun availableStarts(dayStart: Long, durationMin: Int, bookings: List<Booking>, now: Long): List<Long> {
        val first = maxOf(dayStart + OPEN_HOUR * 60 * MINUTE, roundUp(now + 5 * MINUTE))
        val last = dayStart + CLOSE_HOUR * 60 * MINUTE - durationMin * MINUTE
        val out = mutableListOf<Long>()
        var t = first
        while (t <= last) {
            val end = t + durationMin * MINUTE
            if (bookings.none { overlaps(t, end, it.start, it.end) }) out.add(t)
            t += STEP_MIN * MINUTE
        }
        return out
    }
}
