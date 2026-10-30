package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.auth.FirebaseDiagnostics
import com.example.auth.findActivity
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signInWithGoogleWebOAuth
import com.example.graphics.Camera3D
import com.example.graphics.Character3DRenderer.renderRunnerCharacter3D
import com.example.graphics.WorldEnvironmentRenderer.renderRoad3D
import com.example.graphics.WorldEnvironmentRenderer.renderSkyAndHorizon
import com.example.model.HOVERBOARDS
import com.example.model.RUNNER_CHARACTERS
import com.example.model.WORLD_THEMES
import com.example.ui.components.FirebaseDiagnosticModal
import com.example.ui.components.GoldLight
import com.example.ui.components.GoldMid
import com.example.ui.components.GreenDark
import com.example.ui.components.GreenMid
import com.example.ui.components.StoneGoldBilingualButton
import kotlinx.coroutines.isActive

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onEnterLocalPreviewMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDiagnosticsModal by remember { mutableStateOf(false) }

    // Automatically run dependency & network diagnostics on launch to populate Logcat ('FirebaseDiag')
    LaunchedEffect(Unit) {
        FirebaseDiagnostics.runFullDiagnostics(context)
    }

    // Animate 3D background vista on AuthScreen
    var animTime by remember { mutableFloatStateOf(0f) }
    var scrollDist by remember { mutableFloatStateOf(0f) }
    var lastNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { now ->
                val dt = if (lastNanos == 0L) 1f else ((now - lastNanos) / 16_666_667f).coerceIn(0.1f, 2f)
                lastNanos = now
                animTime += dt * 0.016f
                scrollDist += dt * 0.22f
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("auth_screen_root")
    ) {
        // Live 3D Ancient Ruins & Sculpted Runner Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val world = WORLD_THEMES[0]
            val character = RUNNER_CHARACTERS[0]
            val board = HOVERBOARDS[0]
            val cam = Camera3D(screenWidth = size.width, screenHeight = size.height)
            renderSkyAndHorizon(cam = cam, world = world, timeSec = animTime)
            renderRoad3D(cam = cam, world = world, scrollDistance = scrollDist)
            renderRunnerCharacter3D(
                cam = cam,
                character = character,
                board = board,
                playerX = 0f,
                playerY = 0f,
                playerZ = 0f,
                runCycle = scrollDist * 1.3f,
                isJumping = false,
                isSliding = false,
                isBoosting = false,
                hasShield = false,
                laneTiltRad = 0f,
                orbitYawRad = 0f,
                timeSec = animTime
            )
        }

        // Vignette & Sign-In Plaque
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x440F1C14), Color(0xCC08100C))
                    )
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 410.dp)
                    .shadow(16.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF5D564E), Color(0xFF3D3832), Color(0xFF25211D))
                        )
                    )
                    .border(3.5.dp, Color(0xFFD49B35), RoundedCornerShape(22.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    text = "HORIZON\nDASH",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = 34.sp,
                    lineHeight = 34.sp,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFF7B2),
                                Color(0xFFFFD03B),
                                Color(0xFFD97C0B)
                            )
                        ),
                        shadow = Shadow(color = Color(0xFF3A1D00), offset = Offset(0f, 5f), blurRadius = 4f)
                    )
                )

                Text(
                    text = "WORLDS & LEGENDS • CLOUD SYNC",
                    color = GoldLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )

                Text(
                    text = "Sign in with Google to sync your coins, rubies, upgrades, and high scores to Firebase Cloud, or launch Preview Mode to test in the emulator immediately.",
                    color = Color(0xFFEAE5DC),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFFFD269),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x66000000))
                            .border(1.dp, Color(0xFFFF8FA3), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                            .testTag("auth_error_text")
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 1. Primary Google Sign-In via Jetpack Credential Manager
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = coroutineScope,
                            onAuthCancelled = {
                                isLoading = false
                                errorMessage =
                                    "Google Play Services sign-in was dismissed or has no OS account in the preview emulator. " +
                                        "Tap 'PLAY IN PREVIEW MODE' or 'DIAGNOSTICS' below!"
                            }
                        )
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenMid,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .minimumInteractiveComponentSize()
                        .border(2.5.dp, GoldMid, RoundedCornerShape(32.dp))
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Sign in with Google",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            style = TextStyle(
                                shadow = Shadow(color = GreenDark, offset = Offset(0f, 2f), blurRadius = 2f)
                            )
                        )
                    }
                }

                // 2. Browser OAuth Fallback (bypasses Play Services OS account screen on emulators)
                StoneGoldBilingualButton(
                    enLabel = "🌐 SIGN IN VIA BROWSER (WEB OAUTH)",
                    hiLabel = "ब्राउज़र से गूगल लॉग इन करें",
                    testTag = "google_web_sign_in_button",
                    onClick = {
                        val activity = context.findActivity()
                        if (activity != null) {
                            isLoading = true
                            errorMessage = null
                            signInWithGoogleWebOAuth(
                                activity = activity,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = coroutineScope
                            )
                        } else {
                            errorMessage = "Activity context unavailable for browser sign-in."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                )

                // 3. Emulator Preview / Local Offline Mode (zero unauthenticated Firestore calls)
                StoneGoldBilingualButton(
                    enLabel = "▶ PLAY IN PREVIEW MODE (LOCAL)",
                    hiLabel = "प्रिव्यू मोड में अभी खेलें (बिना लॉग इन)",
                    testTag = "preview_offline_mode_button",
                    isHighlighted = true,
                    onClick = onEnterLocalPreviewMode,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
                )

                // 4. Firebase Auth, Google Sign-In & Network Diagnostic Inspector
                StoneGoldBilingualButton(
                    enLabel = "🩺 DIAGNOSTICS & NETWORK LOG",
                    hiLabel = "फ़ायरबेस और नेटवर्क डायग्नोस्टिक्स",
                    testTag = "open_auth_diagnostics_button",
                    onClick = { showDiagnosticsModal = true },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }

        if (showDiagnosticsModal) {
            FirebaseDiagnosticModal(
                onDismiss = { showDiagnosticsModal = false }
            )
        }
    }
}
