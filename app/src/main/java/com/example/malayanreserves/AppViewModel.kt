package com.example.malayanreserves

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.malayanreserves.data.AppError
import com.example.malayanreserves.data.LocalRepository
import com.example.malayanreserves.data.Lot
import com.example.malayanreserves.data.ParkingRepository
import com.example.malayanreserves.data.Registration
import com.example.malayanreserves.data.RemoteRepository
import com.example.malayanreserves.data.Reservation
import com.example.malayanreserves.data.User
import com.example.malayanreserves.data.Vehicle
import com.example.malayanreserves.ui.Fmt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class Tab { LOT, MINE, PROFILE }

data class UiState(
    val loading: Boolean = true,
    val user: User? = null,
    val lot: Lot? = null,
    val mine: Reservation? = null,
    val history: List<Reservation> = emptyList(),
    val tab: Tab = Tab.LOT,
    val busy: Boolean = false,
    val message: String? = null,
    val now: Long = System.currentTimeMillis()
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val repo: ParkingRepository =
        if (BuildConfig.API_BASE_URL.isBlank()) LocalRepository(app) else RemoteRepository(app, BuildConfig.API_BASE_URL)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val user = runCatching { repo.currentUser() }.getOrNull()
            _state.update { it.copy(loading = false, user = user) }
            if (user != null) reload()
        }
        // A one-second clock for countdowns, and a refresh of the lot every 10 seconds.
        viewModelScope.launch {
            var n = 0
            while (isActive) {
                delay(1000)
                _state.update { it.copy(now = System.currentTimeMillis()) }
                if (++n % 10 == 0 && _state.value.user != null && !_state.value.busy) reload(quiet = true)
            }
        }
    }

    fun select(tab: Tab) {
        _state.update { it.copy(tab = tab) }
        reload(quiet = true)
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun reload(quiet: Boolean = false) = viewModelScope.launch {
        try {
            val lot = repo.lot()
            val mine = repo.myReservation()
            val history = repo.history()
            _state.update { it.copy(lot = lot, mine = mine, history = history, now = System.currentTimeMillis()) }
        } catch (e: AppError) {
            if (e.message == RemoteRepository.SESSION_EXPIRED) _state.update { UiState(loading = false, message = e.message) }
            else if (!quiet) _state.update { it.copy(message = e.message) }
        }
    }

    fun login(email: String, password: String) = auth { repo.login(email, password) }
    fun demoLogin() = auth { repo.demoLogin() }
    fun register(reg: Registration) = auth { repo.register(reg) }

    fun logout() = viewModelScope.launch {
        repo.logout()
        _state.update { UiState(loading = false) }
    }

    fun updateVehicle(vehicle: Vehicle, done: () -> Unit) = action("Vehicle updated.", done) {
        val user = repo.updateVehicle(vehicle)
        _state.update { it.copy(user = user) }
    }

    fun reserve(slotId: String, start: Long, durationMin: Int, done: () -> Unit) = action(null, {
        done(); _state.update { it.copy(tab = Tab.MINE) }
    }) {
        val r = repo.reserve(slotId, start, durationMin)
        _state.update {
            it.copy(message = if (r.start <= System.currentTimeMillis()) "${r.slotId} is held for you. Check in when you arrive."
            else "${r.slotId} is booked for ${Fmt.whenLabel(r.start, r.end, System.currentTimeMillis())}.")
        }
    }

    fun arrive() = action("Checked in. Enjoy your day!") { repo.arrive() }
    fun extend() = action("Extended by one hour.") { repo.extend() }
    fun end() = action(null) {
        val r = repo.end()
        _state.update { it.copy(message = if (r.status.name == "COMPLETED") "Thanks! ${r.slotId} is free again." else "Reservation cancelled.") }
    }

    private fun auth(block: suspend () -> User) = viewModelScope.launch {
        _state.update { it.copy(busy = true) }
        try {
            val user = block()
            _state.update { it.copy(user = user, busy = false, tab = Tab.LOT) }
            reload()
        } catch (e: AppError) {
            _state.update { it.copy(busy = false, message = e.message) }
        }
    }

    private fun action(success: String?, done: () -> Unit = {}, block: suspend () -> Unit) = viewModelScope.launch {
        if (_state.value.busy) return@launch
        _state.update { it.copy(busy = true) }
        try {
            block()
            _state.update { it.copy(busy = false, message = success ?: it.message) }
            done()
        } catch (e: AppError) {
            _state.update { it.copy(busy = false, message = e.message) }
        }
        reload(quiet = true)
    }
}
