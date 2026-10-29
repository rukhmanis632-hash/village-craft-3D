package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.graphics.Camera3D
import com.example.graphics.Character3DRenderer.renderRunnerCharacter3D
import com.example.graphics.WorldEnvironmentRenderer.renderRoad3D
import com.example.graphics.WorldEnvironmentRenderer.renderSkyAndHorizon
import com.example.model.HOVERBOARDS
import com.example.model.RUNNER_CHARACTERS
import com.example.model.WORLD_THEMES
import com.example.ui.components.GoldLight
import com.example.ui.components.GoldMid
import com.example.ui.components.GreenDark
import com.example.ui.components.GreenMid
import kotlinx.coroutines.isActive

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Attempt silent auto-sign-in once on launch
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = coroutineScope
        )
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
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp)
                    .shadow(16.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF5D564E), Color(0xFF3D3832), Color(0xFF25211D))
                        )
                    )
                    .border(3.5.dp, Color(0xFFD49B35), RoundedCornerShape(22.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "HORIZON\nDASH",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = 38.sp,
                    lineHeight = 38.sp,
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
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )

                Text(
                    text = "Sign in with Google to sync your coins, rubies, character upgrades, daily challenges, and world high scores to Firebase Cloud.",
                    color = Color(0xFFEAE5DC),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFFF8FA3),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("auth_error_text")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

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
                        .height(54.dp)
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
            }
        }
    }
}
