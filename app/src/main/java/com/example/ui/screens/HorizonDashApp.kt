package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graphics.Camera3D
import com.example.graphics.Character3DRenderer.renderChronoPedestalAndCore3D
import com.example.graphics.Character3DRenderer.renderRunnerCharacter3D
import com.example.graphics.WorldEnvironmentRenderer.renderAmbientParticles3D
import com.example.graphics.WorldEnvironmentRenderer.renderEntity3D
import com.example.graphics.WorldEnvironmentRenderer.renderRoad3D
import com.example.graphics.WorldEnvironmentRenderer.renderSceneryRow3D
import com.example.graphics.WorldEnvironmentRenderer.renderSkyAndHorizon
import com.example.model.ModalType
import com.example.model.ScreenState
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.isActive
import kotlin.math.abs

@Composable
fun HorizonDashApp(
    viewModel: GameViewModel
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadFromPrefs(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveToPrefs(context)
            viewModel.soundEngine.stopBgm()
        }
    }

    // Handle system Back button across modals and secondary screens
    BackHandler(
        enabled = viewModel.activeModal != ModalType.NONE || viewModel.screenState != ScreenState.MAIN_MENU
    ) {
        when {
            viewModel.activeModal != ModalType.NONE -> viewModel.closeModal()
            viewModel.screenState == ScreenState.PLAYING -> viewModel.togglePause()
            else -> viewModel.navigateToMainMenu()
        }
    }

    // Continuous 60fps 3D Animation & Physics Loop
    var lastFrameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { nowNanos ->
                val dtNormalized = if (lastFrameNanos == 0L) {
                    1f
                } else {
                    ((nowNanos - lastFrameNanos) / 16_666_667f).coerceIn(0.1f, 2.4f)
                }
                lastFrameNanos = nowNanos
                viewModel.stepFrame(dtNormalized)
            }
        }
    }

    // Swipe gesture accumulators
    var dragTotalX by remember { mutableFloatStateOf(0f) }
    var dragTotalY by remember { mutableFloatStateOf(0f) }
    var gestureConsumed by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("horizon_dash_root")
            .pointerInput(viewModel.screenState, viewModel.activeModal, viewModel.isIntroCutsceneActive) {
                if (viewModel.isIntroCutsceneActive) {
                    detectTapGestures(
                        onTap = { viewModel.skipIntroCutscene() }
                    )
                }
            }
            .pointerInput(viewModel.screenState, viewModel.activeModal) {
                detectDragGestures(
                    onDragStart = {
                        if (viewModel.isIntroCutsceneActive) {
                            viewModel.skipIntroCutscene()
                            gestureConsumed = 1f
                            return@detectDragGestures
                        }
                        dragTotalX = 0f
                        dragTotalY = 0f
                        gestureConsumed = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (viewModel.activeModal == ModalType.INSPECT_3D) {
                            viewModel.inspectYawRad = (viewModel.inspectYawRad + dragAmount.x * 0.012f)
                                .coerceIn(-3.14f, 3.14f)
                            return@detectDragGestures
                        }
                        if (viewModel.screenState == ScreenState.PLAYING && !viewModel.isPaused && gestureConsumed == 0f) {
                            dragTotalX += dragAmount.x
                            dragTotalY += dragAmount.y
                            val threshold = 42f
                            if (abs(dragTotalX) > abs(dragTotalY) && abs(dragTotalX) > threshold) {
                                viewModel.moveLane(if (dragTotalX > 0) 1 else -1)
                                gestureConsumed = 1f
                            } else if (abs(dragTotalY) >= abs(dragTotalX) && abs(dragTotalY) > threshold) {
                                if (dragTotalY < 0) viewModel.triggerJump() else viewModel.triggerSlide()
                                gestureConsumed = 1f
                            }
                        }
                    }
                )
            }
    ) {
        // 1. Live 3D World & Sculpted Runner Canvas Layer
        HorizonDash3DViewport(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Screen UI Layer (Hidden when in 3D Showcase mode so user has unobstructed view)
        if (viewModel.activeModal != ModalType.INSPECT_3D) {
            when (viewModel.screenState) {
                ScreenState.MAIN_MENU -> MainMenuOverlay(viewModel = viewModel)
                ScreenState.PLAYING -> InGameHudOverlay(viewModel = viewModel)
                ScreenState.WORLDS_AND_CHALLENGES -> WorldsAndChallengesOverlay(viewModel = viewModel)
                ScreenState.GAME_OVER -> GameOverOverlay(viewModel = viewModel)
            }
        }

        // 3. Universal Modal Overlay (Upgrades, Characters, Boards, Leaderboard, Settings, Shop, Pause, 3D Showcase, Story Lore)
        GameModalOverlay(viewModel = viewModel)

        // 4. Toast Banner
        val toast = viewModel.toastMessage
        if (!toast.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 12.dp)
                    .shadow(10.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF2B2620), Color(0xFF171411))
                        )
                    )
                    .border(2.2.dp, Color(0xFFFFD43B), RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .testTag("toast_banner")
            ) {
                Text(
                    text = toast,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun HorizonDash3DViewport(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    // Observe state values for smooth 60fps canvas redraw
    val world = viewModel.currentWorld
    val character = viewModel.currentCharacter
    val board = viewModel.currentBoard
    val scrollDist = viewModel.scrollDistance
    val timeSec = viewModel.animTimeSec
    val px = viewModel.playerX
    val py = viewModel.playerY
    val runCycle = viewModel.runCycle
    val isJumping = viewModel.isJumping
    val isSliding = viewModel.isSliding
    val isBoosting = viewModel.boostTimer > 0f
    val isHyperRun = viewModel.isHyperRun
    val hasShield = viewModel.hasShield
    val isInspect = viewModel.activeModal == ModalType.INSPECT_3D
    val isIntro = viewModel.isIntroCutsceneActive
    val introElapsed = viewModel.introElapsedSec
    val portalFlash = viewModel.portalMorphFlashTimer

    // Smooth camera blend during the 3-second Intro Cutscene (IntroCinematic)
    val introBlend = if (isIntro) {
        ((introElapsed - 1.7f) / 1.3f).coerceIn(0f, 1f)
    } else {
        1f
    }
    val orbitYaw = when {
        isInspect -> viewModel.inspectYawRad
        isIntro -> (1f - introBlend) * 0.52f
        else -> 0f
    }
    val camRoll = if (isInspect) 0f else viewModel.cameraRollRad
    val fovMult = if (isInspect) 1.0f else viewModel.dynamicFovMultiplier
    val shakeX = if (isInspect) 0f else viewModel.screenShakeX
    val shakeY = if (isInspect) 0f else viewModel.screenShakeY
    val nearMissFlash = viewModel.nearMissFlashTimer

    Canvas(
        modifier = modifier.testTag("canvas_3d_viewport")
    ) {
        val camZ = when {
            isInspect -> 5.2f
            isIntro -> 5.5f + introBlend * 3.1f
            else -> 8.6f
        }
        val camY = when {
            isInspect -> 3.2f
            isIntro -> 2.75f + introBlend * 2.25f
            else -> (5.0f + py * 0.28f)
        }
        val camX = when {
            isInspect -> 0f
            isIntro -> -0.65f * (1f - introBlend) + (px * 0.42f) * introBlend
            else -> (px * 0.42f)
        }
        val camPitch = when {
            isInspect -> 0.16f
            isIntro -> 0.14f + introBlend * 0.09f
            else -> 0.23f
        }

        val cam = Camera3D(
            screenWidth = size.width,
            screenHeight = size.height,
            camX = camX,
            camY = camY,
            camZ = camZ,
            pitchRad = camPitch,
            rollRad = camRoll,
            fovMultiplier = fovMult,
            shakeX = shakeX,
            shakeY = shakeY
        )

        // 1. Sky Dome, Horizon Monuments, Volumetric God-Rays / Flying Cars / Lava Falls / Peaks
        renderSkyAndHorizon(cam = cam, world = world, timeSec = timeSec)

        // 2. 3D Perspective Road, Curbs, Planks & Magma Cracks
        renderRoad3D(cam = cam, world = world, scrollDistance = scrollDist)

        // 3. Depth-Sorted Roadside 3D Scenery (Columns, Archways, Neon Billboards, Meteors, Pine Trees)
        val sortedRows = viewModel.sceneryRows.sortedBy { it.z }
        for (row in sortedRows) {
            renderSceneryRow3D(cam = cam, world = world, row = row, timeSec = timeSec)
        }

        // 4. Depth-Sorted 3D Obstacles & Collectibles Behind Player (z <= 0)
        val sortedEntities = viewModel.entities.sortedBy { it.z }
        for (entity in sortedEntities) {
            if (entity.z <= 0.1f) {
                renderEntity3D(cam = cam, world = world, entity = entity, timeSec = timeSec)
            }
        }

        // 4B. Ancient Stone Pedestal & Levitating Chrono Core Relic (during 3s Intro Cutscene)
        if (isIntro) {
            val pedestalZ = -1.25f + maxOf(0f, introElapsed - 1.9f) * 8.5f
            renderChronoPedestalAndCore3D(
                cam = cam,
                pedestalZ = pedestalZ,
                coreGrabbed = introElapsed >= 0.65f,
                introElapsedSec = introElapsed,
                playerX = px,
                playerY = py,
                timeSec = timeSec
            )
        }

        // 5. Sculpted Anatomical 3D Runner Character + Futuristic Sneaker Trails
        val targetX = viewModel.targetLane * 3.4f
        val laneTilt = ((px - targetX) * 0.08f).coerceIn(-0.22f, 0.22f)
        renderRunnerCharacter3D(
            cam = cam,
            character = character,
            board = board,
            playerX = if (isInspect) 0f else px,
            playerY = if (isInspect) 0f else py,
            playerZ = 0f,
            runCycle = runCycle,
            isJumping = isJumping,
            isSliding = isSliding,
            isBoosting = isBoosting,
            hasShield = hasShield,
            laneTiltRad = laneTilt,
            orbitYawRad = orbitYaw,
            timeSec = timeSec,
            isHyperRun = isHyperRun
        )

        // 6. Depth-Sorted 3D Entities Passing Camera (z > 0.1f)
        for (entity in sortedEntities) {
            if (entity.z > 0.1f) {
                renderEntity3D(cam = cam, world = world, entity = entity, timeSec = timeSec)
            }
        }

        // 7. 3D Ambient Particles
        renderAmbientParticles3D(cam = cam, world = world, particles = viewModel.particles)

        // 8B. Dimensional Portal Warp Flash Overlay
        if (portalFlash > 0f) {
            val pAlpha = (portalFlash / 28f).coerceIn(0f, 1f) * 0.55f
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00F0FF).copy(alpha = pAlpha),
                        Color(0xFFB57CFF).copy(alpha = pAlpha * 0.8f),
                        Color.Transparent
                    )
                )
            )
        }

        // 8C. Hyper-Dynamic Chromatic Aberration Edge Vignette & Radial Speed Lines (Boost / Hyper Run / Near-Miss)
        if (!isInspect && (isHyperRun || isBoosting || nearMissFlash > 0f)) {
            val intensity = when {
                nearMissFlash > 0f -> (nearMissFlash / 20f).coerceIn(0.25f, 1f)
                isHyperRun -> 0.85f
                else -> 0.55f
            }
            // Left Cyan & Right Acid-Magenta Chromatic Fringe
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF00F0FF).copy(alpha = 0.28f * intensity),
                        Color.Transparent,
                        Color.Transparent,
                        Color(0xFFFF00AA).copy(alpha = 0.28f * intensity)
                    )
                )
            )
            // Radial anime/cyber speed lines around screen edges
            val cx = size.width * 0.5f
            val cy = size.height * 0.42f
            for (i in 0 until 14) {
                val angle = i * 0.448f + (timeSec * 8f)
                val dx = kotlin.math.cos(angle)
                val dy = kotlin.math.sin(angle)
                val innerR = size.width * 0.36f
                val outerR = size.width * 0.68f
                val lineCol = if (i % 2 == 0) Color(0xFF64FFDA) else Color(0xFFFF6EC7)
                drawLine(
                    color = lineCol.copy(alpha = 0.36f * intensity),
                    start = androidx.compose.ui.geometry.Offset(cx + dx * innerR, cy + dy * innerR),
                    end = androidx.compose.ui.geometry.Offset(cx + dx * outerR, cy + dy * outerR),
                    strokeWidth = 3.2f
                )
            }
        }

        // 9. Celebratory Confetti Burst Particles (Flash Micro-Mission Clear & New World Record)
        for (c in viewModel.confettiParticles) {
            drawCircle(
                color = c.color,
                radius = c.sizeDp * 1.4f,
                center = androidx.compose.ui.geometry.Offset(size.width * c.xRatio, size.height * c.yRatio)
            )
        }
    }
}

