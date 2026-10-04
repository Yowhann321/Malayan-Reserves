package com.example.malayanreserves.data

enum class UserType(val label: String) {
    STUDENT("Student"), FACULTY("Faculty"), EMPLOYEE("Employee");

    /** Faculty and employees can use the staff-priority slots. */
    val isStaff: Boolean get() = this != STUDENT

    companion object {
        fun of(value: String): UserType =
            entries.firstOrNull { it.name.equals(value, true) || it.label.equals(value, true) } ?: STUDENT
    }
}

data class Vehicle(val type: String, val color: String, val plate: String)

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val type: UserType,
    val vehicle: Vehicle
)

data class Registration(
    val name: String,
    val email: String,
    val password: String,
    val type: UserType,
    val vehicle: Vehicle
)

enum class ResStatus {
    UPCOMING,   // scheduled for later
    HELD,       // the start time has come; waiting for the driver to arrive
    ACTIVE,     // parked
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    val isLive: Boolean get() = this == UPCOMING || this == HELD || this == ACTIVE
}

data class Reservation(
    val id: Long,
    val slotId: String,
    val userId: Long,
    val start: Long,
    val end: Long,
    val status: ResStatus,
    val arrivedAt: Long? = null,
    val endedAt: Long? = null,
    val extended: Boolean = false
)

data class Slot(val id: String, val row: Int, val staffOnly: Boolean, val note: String)

enum class SlotState { AVAILABLE, HELD, OCCUPIED, MINE }

/** A booking on a slot's timeline. Other drivers' details are not included. */
data class Booking(val start: Long, val end: Long, val mine: Boolean)

data class SlotView(
    val slot: Slot,
    val state: SlotState,
    val vehicle: Vehicle?,      // the parked car, with the plate masked unless it's yours
    val until: Long?,           // when the current reservation ends
    val nextStart: Long?,       // the next booking on this slot, if any
    val bookings: List<Booking>
)

data class Lot(val slots: List<SlotView>, val updatedAt: Long) {
    val free: Int get() = slots.count { it.state == SlotState.AVAILABLE }
}

/** An error with a message that is safe to show to the user. */
class AppError(message: String) : Exception(message)
