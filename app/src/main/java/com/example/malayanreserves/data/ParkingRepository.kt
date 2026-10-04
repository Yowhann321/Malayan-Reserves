package com.example.malayanreserves.data

/**
 * Everything the app needs from a backend. [LocalRepository] runs fully on the device (demo mode);
 * [RemoteRepository] talks to the PHP API in /backend. Both enforce the same rules from [Rules].
 * Failures are thrown as [AppError] with a message that can be shown to the user.
 */
interface ParkingRepository {
    val isDemo: Boolean

    suspend fun register(reg: Registration): User
    suspend fun login(email: String, password: String): User
    suspend fun demoLogin(): User
    suspend fun currentUser(): User?
    suspend fun logout()
    suspend fun updateVehicle(vehicle: Vehicle): User

    suspend fun lot(): Lot
    suspend fun myReservation(): Reservation?
    suspend fun history(): List<Reservation>

    /** Books [slotId] from [start] for [durationMin]. A start at or before now means "park now". */
    suspend fun reserve(slotId: String, start: Long, durationMin: Int): Reservation
    suspend fun arrive(): Reservation
    suspend fun extend(): Reservation
    /** Leaves the slot if parked, or cancels the reservation if not yet parked. */
    suspend fun end(): Reservation
}
