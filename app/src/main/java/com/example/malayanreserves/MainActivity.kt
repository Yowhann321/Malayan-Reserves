package com.example.malayanreserves

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.malayanreserves.data.ResStatus
import com.example.malayanreserves.ui.AuthFlow
import com.example.malayanreserves.ui.LotScreen
import com.example.malayanreserves.ui.MineScreen
import com.example.malayanreserves.ui.ProfileScreen
import com.example.malayanreserves.ui.StatusBarIcons
import com.example.malayanreserves.ui.theme.MalayanReservesTheme
import com.example.malayanreserves.ui.theme.Paper

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MalayanReservesTheme { App(vm) }
        }
    }
}

@Composable
private fun App(vm: AppViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    if (state.user != null || state.loading) StatusBarIcons(light = false)
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        containerColor = Paper,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (state.user != null) {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(
                        selected = state.tab == Tab.LOT, onClick = { vm.select(Tab.LOT) },
                        icon = { Icon(Icons.Filled.Map, null) }, label = { Text("Lot") }
                    )
                    NavigationBarItem(
                        selected = state.tab == Tab.MINE, onClick = { vm.select(Tab.MINE) },
                        icon = { Icon(Icons.Filled.LocalParking, null) },
                        label = { Text(if (state.mine?.status == ResStatus.ACTIVE) "Parked" else "My parking") }
                    )
                    NavigationBarItem(
                        selected = state.tab == Tab.PROFILE, onClick = { vm.select(Tab.PROFILE) },
                        icon = { Icon(Icons.Filled.Person, null) }, label = { Text("Profile") }
                    )
                }
            }
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            state.user == null -> AuthFlow(
                isDemo = vm.repo.isDemo, busy = state.busy,
                onLogin = vm::login, onDemo = vm::demoLogin, onRegister = vm::register
            )
            else -> Box(Modifier.padding(padding)) {
                when (state.tab) {
                    Tab.LOT -> LotScreen(state, onOpenMine = { vm.select(Tab.MINE) }, onReserve = vm::reserve)
                    Tab.MINE -> MineScreen(state, onFind = { vm.select(Tab.LOT) }, onArrive = vm::arrive, onExtend = vm::extend, onEnd = vm::end)
                    Tab.PROFILE -> ProfileScreen(state, vm.repo.isDemo, onSaveVehicle = vm::updateVehicle, onLogout = vm::logout)
                }
            }
        }
    }
}
