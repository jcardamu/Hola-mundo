package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.AppViewModel
import com.example.ui.AuthState
import com.example.ui.HomeScreen
import com.example.ui.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SpotifyBlack
import com.example.ui.theme.SpotifyGreen

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra("OPEN_PLAYER", false) == true) {
            viewModel.setShowFullScreenPlayer(true)
        }
    }
}

@Composable
fun MainScreen(viewModel: AppViewModel) {
    val authState by viewModel.authState.collectAsState()
    var showGuestDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(SpotifyBlack)) {
        when (val state = authState) {
            is AuthState.Checking -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            }
            is AuthState.LoggedOut, is AuthState.Error -> {
                val errorMsg = if (state is AuthState.Error) state.message else null
                LoginScreen(
                    onLogin = { url, user, pass -> viewModel.login(url, user, pass) },
                    onGuestAccess = { showGuestDialog = true },
                    errorMessage = errorMsg
                )
            }
            is AuthState.LoggedIn -> {
                HomeScreen(
                    serverUrl = state.serverUrl,
                    viewModel = viewModel
                )
            }
        }

        if (showGuestDialog) {
            com.example.ui.QrGuestSearchDialog(
                viewModel = viewModel,
                onDismiss = { showGuestDialog = false }
            )
        }
    }
}
