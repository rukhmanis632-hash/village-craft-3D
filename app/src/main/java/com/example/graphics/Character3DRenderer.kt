package com.example.graphics

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.CharacterStyle
import com.example.model.HoverboardSpec
import com.example.model.RunnerCharacter
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

object Character3DRenderer {

    private data class Joint3D(val x: Float, val y: Float, val z: Float)

    private fun rotateAroundPlayer(
        localX: Float,
        localY: Float,
        localZ: Float,
        px: Float,
        py: Float,
        pz: Float,
        yawRad: Float,
        leanZRad: Float
    ): Joint3D {
        // Apply lateral banking lean around Z
        val cosBank = cos(leanZRad)
        val sinBank = sin(leanZRad)
        val bx = localX * cosBank - localY * sinBank
        val by = localX * sinBank + localY * cosBank

        // Apply yaw rotation around Y
        val cosY = cos(yawRad)
        val sinY = sin(yawRad)
        val rx = bx * cosY + localZ * sinY
        val rz = -bx * sinY + localZ * cosY
        return Joint3D(px + rx, py + by, pz + rz)
    }

    fun DrawScope.renderRunnerCharacter3D(
        cam: Camera3D,
        character: RunnerCharacter,
        board: HoverboardSpec,
        playerX: Float,
        playerY: Float,
        playerZ: Float,
        runCycle: Float,
        isJumping: Boolean,
        isSliding: Boolean,
        isBoosting: Boolean,
        hasShield: Boolean,
        laneTiltRad: Float,
        orbitYawRad: Float = 0f,
        timeSec: Float = 0f,
        isHyperRun: Boolean = false
    ) {
        val crouchScale = if (isSliding) 0.56f else 1.0f
        val strideSin = sin(runCycle)
        val strideCos = cos(runCycle)

        val bounceY = when {
            isJumping -> 0f
            isSliding -> 0.02f
            isBoosting -> 0.14f + sin(timeSec * 6f) * 0.04f
            else -> abs(strideCos) * 0.13f
        }
        val basePy = playerY + bounceY

        // 1. Ground Contact Shadow & Dynamic Character Glow Ring
        val shadowCenter = cam.project(playerX, 0.02f, playerZ)
        if (shadowCenter.visible) {
            val heightFactor = (1f / (1f + playerY * 0.35f)).coerceIn(0.4f, 1f)
            val sw = max(16f, 1.45f * shadowCenter.scale * heightFactor)
            val sh = sw * 0.38f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xAA000000),
                        character.accentGlow.copy(alpha = 0.28f),
                        Color.Transparent
                    ),
                    center = Offset(shadowCenter.x, shadowCenter.y),
                    radius = sw * 0.68f
                ),
                topLeft = Offset(shadowCenter.x - sw * 0.5f, shadowCenter.y - sh * 0.5f),
                size = Size(sw, sh)
            )
        }

        // 2. Hoverboard / Speed Boost Deck (when boosting)
        if (isBoosting) {
            drawGlow3D(cam, playerX, basePy + 0.08f, playerZ, 1.65f, board.glowColor)
            val bFront = rotateAroundPlayer(0f, 0.08f, -1.08f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
            val bBack = rotateAroundPlayer(0f, 0.08f, 1.08f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
            drawSculptedLimb3D(
                cam = cam,
                x1 = bFront.x, y1 = bFront.y, z1 = bFront.z,
                x2 = bBack.x, y2 = bBack.y, z2 = bBack.z,
                radius1 = 0.44f,
                radius2 = 0.54f,
                baseColor = board.primaryColor,
                shadowColor = shadeColor(board.primaryColor, 0.65f),
                highlightColor = board.glowColor
            )
        }

        // Compute 3D Joint Positions in Player Local Space (Z- is forward down the track, Z+ is facing camera)
        val legSwingL = when {
            isJumping -> 0.44f
            isSliding -> -0.56f
            isBoosting -> 0.18f
            else -> strideSin * 0.65f
        }
        val legSwingR = when {
            isJumping -> -0.38f
            isSliding -> 0.50f
            isBoosting -> -0.18f
            else -> -strideSin * 0.65f
        }

        val kneeLiftL = max(0f, -strideCos) * 0.34f
        val kneeLiftR = max(0f, strideCos) * 0.34f

        val hipL = rotateAroundPlayer(-0.21f, 0.94f * crouchScale, 0f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
        val hipR = rotateAroundPlayer(0.21f, 0.94f * crouchScale, 0f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)

        val kneeL = rotateAroundPlayer(
            -0.22f,
            (0.52f + kneeLiftL * 0.5f) * crouchScale,
            -legSwingL * 0.48f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )
        val kneeR = rotateAroundPlayer(
            0.22f,
            (0.52f + kneeLiftR * 0.5f) * crouchScale,
            -legSwingR * 0.48f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )

        val ankleL = rotateAroundPlayer(
            -0.22f,
            (0.15f + kneeLiftL) * crouchScale,
            -legSwingL * 0.74f + 0.14f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )
        val ankleR = rotateAroundPlayer(
            0.22f,
            (0.15f + kneeLiftR) * crouchScale,
            -legSwingR * 0.74f + 0.14f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )

        val toeL = rotateAroundPlayer(
            -0.22f,
            (0.06f + kneeLiftL * 0.85f) * crouchScale,
            -legSwingL * 0.74f - 0.20f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )
        val toeR = rotateAroundPlayer(
            0.22f,
            (0.06f + kneeLiftR * 0.85f) * crouchScale,
            -legSwingR * 0.74f - 0.20f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )

        // Torso & Shoulder Joints
        val pelvisCenter = rotateAroundPlayer(0f, 0.96f * crouchScale, 0f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
        val waistCenter = rotateAroundPlayer(0f, 1.24f * crouchScale, -0.04f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
        val chestCenter = rotateAroundPlayer(0f, 1.66f * crouchScale, -0.10f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
        val neckBase = rotateAroundPlayer(0f, 1.82f * crouchScale, -0.12f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
        val headCenter = rotateAroundPlayer(0f, 2.09f * crouchScale, -0.12f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)

        val armSwingL = -legSwingL * 0.88f
        val armSwingR = -legSwingR * 0.88f

        val shoulderL = rotateAroundPlayer(-0.39f, 1.68f * crouchScale, -0.08f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
        val shoulderR = rotateAroundPlayer(0.39f, 1.68f * crouchScale, -0.08f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)

        val elbowL = rotateAroundPlayer(
            -0.47f,
            1.34f * crouchScale,
            -0.06f - armSwingL * 0.38f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )
        val elbowR = rotateAroundPlayer(
            0.47f,
            1.34f * crouchScale,
            -0.06f - armSwingR * 0.38f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )

        val handL = rotateAroundPlayer(
            -0.43f,
            (1.16f + abs(armSwingL) * 0.18f) * crouchScale,
            -0.24f - armSwingL * 0.56f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )
        val handR = rotateAroundPlayer(
            0.43f,
            (1.16f + abs(armSwingR) * 0.18f) * crouchScale,
            -0.24f - armSwingR * 0.56f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )

        val pantsDark = shadeColor(character.pantsColor, 0.65f)
        val shirtDark = shadeColor(character.shirtColor, 0.65f)

        // Helper to draw a complete articulated leg (Thigh + Knee Pad + Calf + Oversized High-Top Sneaker)
        fun drawCompleteLeg(hip: Joint3D, knee: Joint3D, ankle: Joint3D, toe: Joint3D) {
            // Thigh (muscular taper)
            drawSculptedLimb3D(
                cam = cam,
                x1 = hip.x, y1 = hip.y, z1 = hip.z,
                x2 = knee.x, y2 = knee.y, z2 = knee.z,
                radius1 = 0.175f,
                radius2 = 0.14f,
                baseColor = character.pantsColor,
                shadowColor = pantsDark,
                highlightColor = character.pantsStripe
            )
            // Stylized Knee Guard / Neon Accent Joint
            val kp = cam.project(knee.x, knee.y, knee.z)
            if (kp.visible) {
                drawCircle(
                    color = character.pantsStripe,
                    radius = max(2.2f, 0.095f * kp.scale),
                    center = Offset(kp.x, kp.y)
                )
            }
            // Sculpted Calf (taper down to ankle)
            drawSculptedLimb3D(
                cam = cam,
                x1 = knee.x, y1 = knee.y, z1 = knee.z,
                x2 = ankle.x, y2 = ankle.y, z2 = ankle.z,
                radius1 = 0.135f,
                radius2 = 0.09f,
                baseColor = character.pantsColor,
                shadowColor = pantsDark,
                highlightColor = character.pantsStripe
            )
            // High-Top Sneaker Collar & Sock Cuff
            val ap = cam.project(ankle.x, ankle.y, ankle.z)
            if (ap.visible) {
                drawCircle(
                    color = Color(0xFFF2F6FA),
                    radius = max(2.5f, 0.105f * ap.scale),
                    center = Offset(ap.x, ap.y - 0.02f * ap.scale)
                )
            }
            // Oversized Stylized 3D High-Top Running Shoe (Subway Surfers / Arcade Proportions)
            drawSculptedLimb3D(
                cam = cam,
                x1 = ankle.x, y1 = ankle.y - 0.03f, z1 = ankle.z + 0.05f,
                x2 = toe.x, y2 = toe.y, z2 = toe.z,
                radius1 = 0.135f,
                radius2 = 0.115f,
                baseColor = character.shoeColor,
                shadowColor = shadeColor(character.shoeColor, 0.62f),
                highlightColor = character.shoeSoleColor
            )
            if (ap.visible) {
                val soleR = max(3f, 0.13f * ap.scale)
                // Chunky Neon Midsole Cushion
                drawLine(
                    color = character.shoeSoleColor,
                    start = Offset(ap.x - soleR * 0.92f, ap.y + soleR * 0.55f),
                    end = Offset(ap.x + soleR * 0.92f, ap.y + soleR * 0.55f),
                    strokeWidth = max(3f, soleR * 0.52f)
                )
                // Heel Counter Accent Badge
                drawCircle(
                    color = character.accentGlow,
                    radius = max(2f, soleR * 0.36f),
                    center = Offset(ap.x, ap.y + soleR * 0.15f)
                )

                // Futuristic 3D Neon-Pastel Sneaker Trail Ribbons
                val trailLengthZ = if (isHyperRun || isBoosting) 3.6f else 2.0f
                val trailTail = cam.project(
                    ankle.x + sin(timeSec * 9f + ankle.x) * 0.15f,
                    max(0.05f, ankle.y - 0.04f),
                    ankle.z - trailLengthZ
                )
                if (trailTail.visible) {
                    val trailColorPrimary = if (isHyperRun) Color(0xFF64FFDA) else character.accentGlow
                    val trailColorSecondary = if (isHyperRun) Color(0xFFFF6EC7) else character.shoeColor
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                trailColorPrimary.copy(alpha = if (isHyperRun || isBoosting) 0.92f else 0.72f),
                                trailColorSecondary.copy(alpha = 0.38f),
                                Color.Transparent
                            ),
                            start = Offset(ap.x, ap.y + soleR * 0.4f),
                            end = Offset(trailTail.x, trailTail.y)
                        ),
                        start = Offset(ap.x, ap.y + soleR * 0.4f),
                        end = Offset(trailTail.x, trailTail.y),
                        strokeWidth = max(3.2f, soleR * (if (isHyperRun || isBoosting) 1.3f else 0.85f))
                    )
                }
            }
        }

        // Depth-sort Left vs Right Leg based on camera Z
        if (ankleL.z < ankleR.z) {
            drawCompleteLeg(hipL, kneeL, ankleL, toeL)
            drawCompleteLeg(hipR, kneeR, ankleR, toeR)
        } else {
            drawCompleteLeg(hipR, kneeR, ankleR, toeR)
            drawCompleteLeg(hipL, kneeL, ankleL, toeL)
        }

        // 3. Sculpted Pelvis & Athletic V-Taper Jacket/Hoodie Torso
        drawSculptedLimb3D(
            cam = cam,
            x1 = hipL.x, y1 = hipL.y, z1 = hipL.z,
            x2 = hipR.x, y2 = hipR.y, z2 = hipR.z,
            radius1 = 0.18f,
            radius2 = 0.18f,
            baseColor = character.pantsColor,
            shadowColor = pantsDark,
            highlightColor = character.pantsStripe
        )

        // Lower Torso (Waist) to Upper Chest (V-Taper)
        drawSculptedLimb3D(
            cam = cam,
            x1 = pelvisCenter.x, y1 = pelvisCenter.y + 0.05f, z1 = pelvisCenter.z,
            x2 = waistCenter.x, y2 = waistCenter.y, z2 = waistCenter.z,
            radius1 = 0.255f,
            radius2 = 0.28f,
            baseColor = character.shirtColor,
            shadowColor = shirtDark,
            highlightColor = character.shirtHighlight
        )
        drawSculptedLimb3D(
            cam = cam,
            x1 = waistCenter.x, y1 = waistCenter.y, z1 = waistCenter.z,
            x2 = chestCenter.x, y2 = chestCenter.y, z2 = chestCenter.z,
            radius1 = 0.28f,
            radius2 = 0.36f,
            baseColor = character.shirtColor,
            shadowColor = shirtDark,
            highlightColor = character.shirtHighlight
        )
        // Broad Athletic Shoulder Girdle
        drawSculptedLimb3D(
            cam = cam,
            x1 = shoulderL.x, y1 = shoulderL.y, z1 = shoulderL.z,
            x2 = shoulderR.x, y2 = shoulderR.y, z2 = shoulderR.z,
            radius1 = 0.17f,
            radius2 = 0.17f,
            baseColor = character.shirtColor,
            shadowColor = shirtDark,
            highlightColor = character.shirtHighlight
        )

        // 4. Sculpted Muscular Arms (Deltoid -> Bicep -> Forearm -> Stylized Glove)
        fun drawCompleteArm(shoulder: Joint3D, elbow: Joint3D, hand: Joint3D) {
            drawSculptedLimb3D(
                cam = cam,
                x1 = shoulder.x, y1 = shoulder.y, z1 = shoulder.z,
                x2 = elbow.x, y2 = elbow.y, z2 = elbow.z,
                radius1 = 0.14f,
                radius2 = 0.11f,
                baseColor = character.skinColor,
                shadowColor = character.skinShadow,
                highlightColor = shadeColor(character.skinColor, 1.16f)
            )
            // Jacket / Hoodie Sleeve over upper deltoid
            val sleeveEnd = Joint3D(
                shoulder.x + (elbow.x - shoulder.x) * 0.45f,
                shoulder.y + (elbow.y - shoulder.y) * 0.45f,
                shoulder.z + (elbow.z - shoulder.z) * 0.45f
            )
            drawSculptedLimb3D(
                cam = cam,
                x1 = shoulder.x, y1 = shoulder.y, z1 = shoulder.z,
                x2 = sleeveEnd.x, y2 = sleeveEnd.y, z2 = sleeveEnd.z,
                radius1 = 0.155f,
                radius2 = 0.138f,
                baseColor = character.shirtColor,
                shadowColor = shirtDark,
                highlightColor = character.shirtHighlight
            )
            // Forearm
            drawSculptedLimb3D(
                cam = cam,
                x1 = elbow.x, y1 = elbow.y, z1 = elbow.z,
                x2 = hand.x, y2 = hand.y, z2 = hand.z,
                radius1 = 0.105f,
                radius2 = 0.082f,
                baseColor = character.skinColor,
                shadowColor = character.skinShadow,
                highlightColor = shadeColor(character.skinColor, 1.15f)
            )
            // Stylized Glove & Glowing Wristband
            val hp = cam.project(hand.x, hand.y, hand.z)
            if (hp.visible) {
                val gr = max(2.8f, 0.098f * hp.scale)
                drawCircle(
                    color = character.gloveColor,
                    radius = gr,
                    center = Offset(hp.x, hp.y)
                )
                drawCircle(
                    color = character.accentGlow,
                    radius = gr * 0.92f,
                    center = Offset(hp.x, hp.y),
                    style = Stroke(width = 1.6f)
                )
            }
        }

        drawCompleteArm(shoulderL, elbowL, handL)
        drawCompleteArm(shoulderR, elbowR, handR)

        // 5. Character-Specific 3D Back Gear, Wings, Scarves, Kitsune Tails & Packs
        val packBackCenter = rotateAroundPlayer(0f, 1.48f * crouchScale, 0.22f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
        val packTop = rotateAroundPlayer(0f, 1.72f * crouchScale, 0.20f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
        val packBottom = rotateAroundPlayer(0f, 1.20f * crouchScale, 0.22f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)

        val isBackVisible = cos(orbitYawRad) >= -0.25f
        if (isBackVisible) {
            val pTop = cam.project(packTop.x, packTop.y, packTop.z)
            val pBot = cam.project(packBottom.x, packBottom.y, packBottom.z)

            // Render Archetype-Specific 3D Back Silhouettes first (Wings / Scarf / Tails / Jet Thrusters)
            when (character.style) {
                CharacterStyle.VALKYRIE_HERO -> {
                    // Twin Hard-Light Energy Wings extending from Aria's back
                    val wingFlap = sin(timeSec * 8f) * 0.14f
                    for (side in listOf(-1f, 1f)) {
                        val wRoot = cam.project(
                            packBackCenter.x + side * 0.18f,
                            packBackCenter.y + 0.12f,
                            packBackCenter.z
                        )
                        val wTipUpper = cam.project(
                            packBackCenter.x + side * (1.15f + wingFlap),
                            packBackCenter.y + 0.68f + wingFlap,
                            packBackCenter.z + 0.12f
                        )
                        val wTipLower = cam.project(
                            packBackCenter.x + side * (0.85f + wingFlap * 0.5f),
                            packBackCenter.y - 0.28f,
                            packBackCenter.z + 0.10f
                        )
                        if (wRoot.visible && wTipUpper.visible && wTipLower.visible) {
                            val wingPath = Path().apply {
                                moveTo(wRoot.x, wRoot.y)
                                lineTo(wTipUpper.x, wTipUpper.y)
                                lineTo((wTipUpper.x + wTipLower.x) * 0.52f, (wTipUpper.y + wTipLower.y) * 0.5f)
                                lineTo(wTipLower.x, wTipLower.y)
                                close()
                            }
                            drawPath(
                                path = wingPath,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        character.accentGlow.copy(alpha = 0.88f),
                                        Color(0xFFFFE066).copy(alpha = 0.65f),
                                        Color.Transparent
                                    ),
                                    start = Offset(wRoot.x, wRoot.y),
                                    end = Offset(wTipUpper.x, wTipUpper.y)
                                )
                            )
                            drawPath(
                                path = wingPath,
                                color = Color.White.copy(alpha = 0.8f),
                                style = Stroke(width = 2.0f)
                            )
                        }
                    }
                }
                CharacterStyle.CYBER_NINJA -> {
                    // Twin Billowing 3D Neon Energy Scarf Ribbons trailing in the wind behind Kai
                    for (side in listOf(-1f, 1f)) {
                        val sStart = cam.project(
                            neckBase.x + side * 0.16f,
                            neckBase.y - 0.04f,
                            neckBase.z + 0.18f
                        )
                        val waveX = sin(timeSec * 12f + side * 1.5f) * 0.34f
                        val waveY = cos(timeSec * 10f + side) * 0.18f
                        val sMid = cam.project(
                            neckBase.x + side * 0.42f + waveX * 0.5f,
                            neckBase.y - 0.26f + waveY,
                            neckBase.z + 0.85f
                        )
                        val sEnd = cam.project(
                            neckBase.x + side * 0.62f + waveX,
                            neckBase.y - 0.52f - waveY,
                            neckBase.z + 1.65f
                        )
                        if (sStart.visible && sMid.visible && sEnd.visible) {
                            val scarfPath = Path().apply {
                                moveTo(sStart.x, sStart.y)
                                quadraticTo(sMid.x, sMid.y, sEnd.x, sEnd.y)
                            }
                            drawPath(
                                path = scarfPath,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFF007F),
                                        Color(0xFF00F0FF),
                                        Color.Transparent
                                    ),
                                    start = Offset(sStart.x, sStart.y),
                                    end = Offset(sEnd.x, sEnd.y)
                                ),
                                style = Stroke(width = max(5f, 0.18f * sStart.scale))
                            )
                        }
                    }
                }
                CharacterStyle.KITSUNE_MASCOT -> {
                    // Twin Swaying 3D Kitsune Energy Tails behind Nova's waist
                    for (side in listOf(-1f, 1f)) {
                        val tailBase = cam.project(
                            pelvisCenter.x + side * 0.12f,
                            pelvisCenter.y + 0.06f,
                            pelvisCenter.z + 0.20f
                        )
                        val sway = sin(timeSec * 7.5f + side * 1.2f) * 0.35f
                        val tailTip = cam.project(
                            pelvisCenter.x + side * 0.68f + sway,
                            pelvisCenter.y + 0.52f + abs(sway) * 0.25f,
                            pelvisCenter.z + 0.65f
                        )
                        if (tailBase.visible && tailTip.visible) {
                            val tailW = max(7f, 0.26f * tailBase.scale)
                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        character.hairColor,
                                        character.accentGlow,
                                        Color.White
                                    ),
                                    start = Offset(tailBase.x, tailBase.y),
                                    end = Offset(tailTip.x, tailTip.y)
                                ),
                                start = Offset(tailBase.x, tailBase.y),
                                end = Offset(tailTip.x, tailTip.y),
                                strokeWidth = tailW
                            )
                            drawCircle(
                                color = Color.White,
                                radius = tailW * 0.55f,
                                center = Offset(tailTip.x, tailTip.y)
                            )
                        }
                    }
                }
                CharacterStyle.MECHA_STRIKER -> {
                    // Dual Plasma Jetpack Thruster Flames shooting downward from Ignis's pack
                    if (pBot.visible) {
                        val flameFlicker = 0.22f + abs(sin(timeSec * 18f)) * 0.18f
                        for (side in listOf(-0.22f, 0.22f)) {
                            val nozzle = cam.project(
                                packBottom.x + side,
                                packBottom.y,
                                packBottom.z + 0.05f
                            )
                            val exhaust = cam.project(
                                packBottom.x + side * 1.25f,
                                packBottom.y - flameFlicker,
                                packBottom.z + 0.22f
                            )
                            if (nozzle.visible && exhaust.visible) {
                                val nw = max(4f, 0.15f * nozzle.scale)
                                drawLine(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White,
                                            Color(0xFFFFAE00),
                                            Color(0xFFFF3B00),
                                            Color.Transparent
                                        ),
                                        startY = nozzle.y,
                                        endY = exhaust.y
                                    ),
                                    start = Offset(nozzle.x, nozzle.y),
                                    end = Offset(exhaust.x, exhaust.y),
                                    strokeWidth = nw
                                )
                            }
                        }
                    }
                }
                else -> {}
            }

            // Main Sculpted Back Gear / Street Rucksack / Cyber Pack
            if (pTop.visible && pBot.visible) {
                val pw = max(10f, 0.54f * pTop.scale)
                val ph = max(12f, abs(pBot.y - pTop.y) + 0.16f * pTop.scale)
                val left = (pTop.x + pBot.x) * 0.5f - pw * 0.5f
                val top = minOf(pTop.y, pBot.y) - 0.05f * pTop.scale

                drawRoundRect(
                    color = shadeColor(character.backpackColor, 0.62f),
                    topLeft = Offset(left - 2f, top + 2f),
                    size = Size(pw + 4f, ph),
                    cornerRadius = CornerRadius(pw * 0.24f)
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            character.backpackFlapColor,
                            character.backpackColor,
                            shadeColor(character.backpackColor, 0.72f)
                        ),
                        startY = top,
                        endY = top + ph
                    ),
                    topLeft = Offset(left, top),
                    size = Size(pw, ph),
                    cornerRadius = CornerRadius(pw * 0.22f)
                )
                // Glowing Emblem / Core on Pack
                drawCircle(
                    color = character.accentGlow,
                    radius = max(2.8f, pw * 0.15f),
                    center = Offset(left + pw * 0.5f, top + ph * 0.45f)
                )
            }
        }

        // 6. Neck, Sculpted Head & Signature 3D Headgear (Headphones / Cap / Crown / Fox Ears / Halo / Spiky Hair)
        drawSculptedLimb3D(
            cam = cam,
            x1 = neckBase.x, y1 = neckBase.y, z1 = neckBase.z,
            x2 = headCenter.x, y2 = headCenter.y - 0.06f, z2 = headCenter.z,
            radius1 = 0.11f,
            radius2 = 0.105f,
            baseColor = character.skinColor,
            shadowColor = character.skinShadow,
            highlightColor = shadeColor(character.skinColor, 1.12f)
        )

        val hp = cam.project(headCenter.x, headCenter.y, headCenter.z)
        if (hp.visible) {
            val hr = max(6.5f, 0.26f * hp.scale)

            // Cel-Shaded Dark Outline around Head
            drawCircle(
                color = Color(0xFF14111A),
                radius = hr * 1.07f,
                center = Offset(hp.x, hp.y)
            )

            // Ears
            drawCircle(color = character.skinShadow, radius = hr * 0.28f, center = Offset(hp.x - hr * 0.92f, hp.y + hr * 0.08f))
            drawCircle(color = character.skinShadow, radius = hr * 0.28f, center = Offset(hp.x + hr * 0.92f, hp.y + hr * 0.08f))

            // Head Cranium base
            drawCircle(
                color = character.skinColor,
                radius = hr,
                center = Offset(hp.x, hp.y)
            )

            // Back / Three-Quarter Gameplay Camera View
            if (cos(orbitYawRad) >= -0.15f) {
                val hairPath = Path().apply {
                    moveTo(hp.x - hr * 1.02f, hp.y + hr * 0.18f)
                    quadraticTo(hp.x - hr * 1.08f, hp.y - hr * 1.05f, hp.x, hp.y - hr * 1.14f)
                    quadraticTo(hp.x + hr * 1.08f, hp.y - hr * 1.05f, hp.x + hr * 1.02f, hp.y + hr * 0.18f)
                    quadraticTo(hp.x, hp.y + hr * 0.65f, hp.x - hr * 1.02f, hp.y + hr * 0.18f)
                    close()
                }
                drawPath(hairPath, color = character.hairColor)

                // Hair crown highlight
                drawCircle(
                    color = shadeColor(character.hairColor, 1.35f),
                    radius = hr * 0.42f,
                    center = Offset(hp.x - hr * 0.18f, hp.y - hr * 0.42f)
                )
            } else {
                // Front 360° Showcase View: Hair Bangs + Expressive Eyes / Visors / Shades
                drawArc(
                    color = character.hairColor,
                    startAngle = 190f,
                    sweepAngle = 160f,
                    useCenter = true,
                    topLeft = Offset(hp.x - hr * 1.05f, hp.y - hr * 1.12f),
                    size = Size(hr * 2.1f, hr * 1.6f)
                )
                when (character.style) {
                    CharacterStyle.CYBER_NINJA, CharacterStyle.MECHA_STRIKER -> {
                        // Glowing Cyber Visor Strip
                        drawRoundRect(
                            color = character.accentGlow,
                            topLeft = Offset(hp.x - hr * 0.68f, hp.y - hr * 0.18f),
                            size = Size(hr * 1.36f, hr * 0.36f),
                            cornerRadius = CornerRadius(hr * 0.16f)
                        )
                    }
                    CharacterStyle.ROYAL_FLEX -> {
                        // Luxury Gold-Rimmed Shades
                        drawRoundRect(
                            color = Color(0xFF141118),
                            topLeft = Offset(hp.x - hr * 0.70f, hp.y - hr * 0.18f),
                            size = Size(hr * 1.40f, hr * 0.34f),
                            cornerRadius = CornerRadius(hr * 0.12f)
                        )
                        drawRoundRect(
                            color = Color(0xFFFFD700),
                            topLeft = Offset(hp.x - hr * 0.70f, hp.y - hr * 0.18f),
                            size = Size(hr * 1.40f, hr * 0.34f),
                            cornerRadius = CornerRadius(hr * 0.12f),
                            style = Stroke(width = 2.0f)
                        )
                    }
                    else -> {
                        // Expressive Stylized Eyes + White Catchlights
                        drawCircle(color = Color(0xFF1E1A17), radius = hr * 0.15f, center = Offset(hp.x - hr * 0.34f, hp.y - hr * 0.04f))
                        drawCircle(color = Color(0xFF1E1A17), radius = hr * 0.15f, center = Offset(hp.x + hr * 0.34f, hp.y - hr * 0.04f))
                        drawCircle(color = Color.White, radius = hr * 0.055f, center = Offset(hp.x - hr * 0.38f, hp.y - hr * 0.08f))
                        drawCircle(color = Color.White, radius = hr * 0.055f, center = Offset(hp.x + hr * 0.30f, hp.y - hr * 0.08f))
                    }
                }
            }

            // Signature 3D Headgear Silhouette per Character Style (Visible from both Back & Front!)
            when (character.style) {
                CharacterStyle.STREET_REBEL -> {
                    // Backwards Street Cap Dome + Brim + Studio Headphones on Ears
                    drawArc(
                        color = character.backpackColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(hp.x - hr * 1.04f, hp.y - hr * 1.12f),
                        size = Size(hr * 2.08f, hr * 1.35f)
                    )
                    // Backwards Cap Snapback Brim jutting out
                    drawRoundRect(
                        color = character.backpackFlapColor,
                        topLeft = Offset(hp.x - hr * 0.55f, hp.y - hr * 0.48f),
                        size = Size(hr * 1.10f, hr * 0.26f),
                        cornerRadius = CornerRadius(hr * 0.12f)
                    )
                    // Oversized Studio Headphones Cups on Left & Right
                    for (side in listOf(-1f, 1f)) {
                        drawRoundRect(
                            color = Color(0xFF191D26),
                            topLeft = Offset(hp.x + side * hr * 0.96f - hr * 0.18f, hp.y - hr * 0.22f),
                            size = Size(hr * 0.36f, hr * 0.56f),
                            cornerRadius = CornerRadius(hr * 0.16f)
                        )
                        drawCircle(
                            color = character.accentGlow,
                            radius = hr * 0.11f,
                            center = Offset(hp.x + side * hr * 0.96f, hp.y + hr * 0.06f)
                        )
                    }
                }
                CharacterStyle.CYBER_NINJA -> {
                    // Dynamic Sonic/Anime Spiky Hair Silhouette + Glowing Cyber Headband
                    for (spike in -2..2) {
                        val spPath = Path().apply {
                            val baseAngle = spike * 0.42f
                            moveTo(hp.x + sin(baseAngle - 0.22f) * hr * 0.85f, hp.y - cos(baseAngle - 0.22f) * hr * 0.75f)
                            lineTo(hp.x + sin(baseAngle) * hr * 1.58f, hp.y - cos(baseAngle) * hr * 1.52f)
                            lineTo(hp.x + sin(baseAngle + 0.22f) * hr * 0.85f, hp.y - cos(baseAngle + 0.22f) * hr * 0.75f)
                            close()
                        }
                        drawPath(spPath, color = character.hairColor)
                    }
                    drawLine(
                        color = character.accentGlow,
                        start = Offset(hp.x - hr * 0.98f, hp.y - hr * 0.15f),
                        end = Offset(hp.x + hr * 0.98f, hp.y - hr * 0.15f),
                        strokeWidth = max(2.8f, hr * 0.22f)
                    )
                }
                CharacterStyle.VALKYRIE_HERO -> {
                    // Levitating Golden/Cyan Valkyrie Halo Ring & Winged Tiara
                    val haloY = hp.y - hr * (1.38f + sin(timeSec * 6f) * 0.08f)
                    drawOval(
                        color = Color(0xFFFFE066),
                        topLeft = Offset(hp.x - hr * 0.82f, haloY - hr * 0.20f),
                        size = Size(hr * 1.64f, hr * 0.40f),
                        style = Stroke(width = max(2.8f, hr * 0.18f))
                    )
                    drawOval(
                        color = character.accentGlow,
                        topLeft = Offset(hp.x - hr * 0.68f, haloY - hr * 0.14f),
                        size = Size(hr * 1.36f, hr * 0.28f),
                        style = Stroke(width = max(1.5f, hr * 0.09f))
                    )
                }
                CharacterStyle.MECHA_STRIKER -> {
                    // Horned Volcanic Cyber-Helm Crest
                    for (side in listOf(-1f, 1f)) {
                        val horn = Path().apply {
                            moveTo(hp.x + side * hr * 0.45f, hp.y - hr * 0.65f)
                            lineTo(hp.x + side * hr * 1.35f, hp.y - hr * 1.42f)
                            lineTo(hp.x + side * hr * 0.88f, hp.y - hr * 0.15f)
                            close()
                        }
                        drawPath(horn, color = character.accentGlow)
                    }
                }
                CharacterStyle.KITSUNE_MASCOT -> {
                    // Pointed 3D Neon Kitsune Fox Ears with Inner Mint Glow
                    for (side in listOf(-1f, 1f)) {
                        val outerEar = Path().apply {
                            moveTo(hp.x + side * hr * 0.30f, hp.y - hr * 0.82f)
                            lineTo(hp.x + side * hr * 0.98f, hp.y - hr * 1.68f)
                            lineTo(hp.x + side * hr * 0.95f, hp.y - hr * 0.35f)
                            close()
                        }
                        drawPath(outerEar, color = character.hairColor)
                        val innerEar = Path().apply {
                            moveTo(hp.x + side * hr * 0.44f, hp.y - hr * 0.78f)
                            lineTo(hp.x + side * hr * 0.86f, hp.y - hr * 1.42f)
                            lineTo(hp.x + side * hr * 0.82f, hp.y - hr * 0.48f)
                            close()
                        }
                        drawPath(innerEar, color = character.accentGlow)
                    }
                }
                CharacterStyle.ROYAL_FLEX -> {
                    // Multi-Point 3D Golden Royal Crown with Emerald/Diamond Jewels
                    val crownBaseY = hp.y - hr * 0.82f
                    val crownTopY = hp.y - hr * 1.56f
                    val crownPath = Path().apply {
                        moveTo(hp.x - hr * 0.78f, crownBaseY)
                        lineTo(hp.x - hr * 0.88f, crownTopY + hr * 0.18f)
                        lineTo(hp.x - hr * 0.42f, crownBaseY - hr * 0.25f)
                        lineTo(hp.x, crownTopY)
                        lineTo(hp.x + hr * 0.42f, crownBaseY - hr * 0.25f)
                        lineTo(hp.x + hr * 0.88f, crownTopY + hr * 0.18f)
                        lineTo(hp.x + hr * 0.78f, crownBaseY)
                        close()
                    }
                    drawPath(crownPath, color = Color(0xFFFFD700))
                    drawPath(crownPath, color = Color(0xFFB87800), style = Stroke(width = 1.8f))
                    // Crown Center Diamond Gem
                    drawCircle(
                        color = Color(0xFF64FFDA),
                        radius = max(2.2f, hr * 0.14f),
                        center = Offset(hp.x, crownBaseY - hr * 0.22f)
                    )
                }
            }
        }

        // 7. Hexagonal Forcefield Shield Sphere (when Shield is active)
        if (hasShield) {
            val sc = cam.project(playerX, basePy + 1.12f * crouchScale, playerZ)
            if (sc.visible) {
                val sr = max(24f, 1.48f * sc.scale)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x1800F0FF),
                            Color(0x5500F0FF),
                            Color.Transparent
                        ),
                        center = Offset(sc.x, sc.y),
                        radius = sr
                    ),
                    radius = sr,
                    center = Offset(sc.x, sc.y)
                )
                drawCircle(
                    color = Color(0xAA00F0FF),
                    radius = sr * 0.92f,
                    center = Offset(sc.x, sc.y),
                    style = Stroke(width = 2.5f)
                )
            }
        }

        // 8. HYPER RUN Cyber-Pastel Overdrive Aura Ring
        if (isHyperRun) {
            val hc = cam.project(playerX, basePy + 1.05f * crouchScale, playerZ)
            if (hc.visible) {
                val hr = max(28f, (1.58f + sin(timeSec * 14f) * 0.12f) * hc.scale)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x4464FFDA),
                            Color(0x55FF6EC7),
                            Color.Transparent
                        ),
                        center = Offset(hc.x, hc.y),
                        radius = hr
                    ),
                    radius = hr,
                    center = Offset(hc.x, hc.y)
                )
            }
        }
    }

    /**
     * Renders the Ancient Stone Pedestal & Levitating "Chrono Core" Relic
     * during the 3-second Intro Cutscene (clean hero power-up sequence without any monster).
     */
    fun DrawScope.renderChronoPedestalAndCore3D(
        cam: Camera3D,
        pedestalZ: Float,
        coreGrabbed: Boolean,
        introElapsedSec: Float,
        playerX: Float,
        playerY: Float,
        timeSec: Float
    ) {
        // Carved Ancient Stone Altar Pedestal
        drawBox3D(
            cam = cam,
            cx = -0.85f,
            bottomY = 0f,
            cz = pedestalZ,
            width = 1.05f,
            height = 1.35f,
            depthZ = 1.05f,
            baseColor = Color(0xFF8C826B),
            fogColor = Color(0xFF425932),
            outlineColor = Color(0xFF64FFDA)
        )

        // Glowing Chrono Core Relic (levitating on pedestal or held by the runner!)
        val coreX = if (coreGrabbed) playerX + 0.35f else -0.85f
        val coreY = if (coreGrabbed) playerY + 1.48f else (1.85f + sin(timeSec * 8f) * 0.14f)
        val coreZ = if (coreGrabbed) -0.25f else pedestalZ

        drawGlow3D(cam, coreX, coreY, coreZ, 1.85f, Color(0xFF64FFDA))
        val cp = cam.project(coreX, coreY, coreZ)
        if (cp.visible) {
            val r = max(7f, 0.44f * cp.scale)
            val diamond = Path().apply {
                moveTo(cp.x, cp.y - r * 1.3f)
                lineTo(cp.x + r, cp.y)
                lineTo(cp.x, cp.y + r * 1.3f)
                lineTo(cp.x - r, cp.y)
                close()
            }
            drawPath(diamond, color = Color(0xFF00F0FF))
            drawCircle(color = Color.White, radius = r * 0.48f, center = Offset(cp.x, cp.y))
            drawCircle(
                color = Color(0xFFFFD03B),
                radius = r * 1.35f,
                center = Offset(cp.x, cp.y),
                style = Stroke(width = 2.2f)
            )
            if (coreGrabbed && introElapsedSec in 0.65f..2.2f) {
                val pulseR = r * (1.5f + (introElapsedSec - 0.65f) * 2.4f)
                drawCircle(
                    color = Color(0xAA64FFDA),
                    radius = pulseR,
                    center = Offset(cp.x, cp.y),
                    style = Stroke(width = 2.6f)
                )
            }
        }
    }
}
