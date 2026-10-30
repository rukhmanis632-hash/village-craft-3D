package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.data.repository.HorizonDashRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HorizonDashApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var isLocalPreviewMode by rememberSaveable { mutableStateOf(false) }
    val user = currentUser

    if (user == null && !isLocalPreviewMode) {
        AuthScreen(
            onAuthSuccess = {},
            onEnterLocalPreviewMode = { isLocalPreviewMode = true }
        )
    } else if (user != null) {
        val currentUserId = user.uid
        val displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Explorer Leo"
        val gameViewModel: GameViewModel = viewModel(
            key = "cloud_$currentUserId",
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    GameViewModel(
                        repository = HorizonDashRepository(db),
                        currentUserId = currentUserId,
                        userDisplayName = displayName
                    )
                }
            }
        )
        HorizonDashApp(viewModel = gameViewModel)
    } else {
        // Local Preview Mode: strictly local SharedPreferences state, zero unauthenticated Firestore calls
        val localViewModel: GameViewModel = viewModel(
            key = "local_preview_session",
            factory = viewModelFactory {
                initializer {
                    GameViewModel(
                        repository = null,
                        currentUserId = null,
                        userDisplayName = "Preview Explorer (Local)",
                        onRequestSignIn = { isLocalPreviewMode = false }
                    )
                }
            }
        )
        HorizonDashApp(viewModel = localViewModel)
    }
}
