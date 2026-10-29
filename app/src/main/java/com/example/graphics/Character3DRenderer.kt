package com.example.graphics

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
        timeSec: Float = 0f
    ) {
        val crouchScale = if (isSliding) 0.56f else 1.0f
        val strideSin = sin(runCycle)
        val strideCos = cos(runCycle)

        val bounceY = when {
            isJumping -> 0f
            isSliding -> 0.02f
            isBoosting -> 0.14f + sin(timeSec * 6f) * 0.04f
            else -> abs(strideCos) * 0.12f
        }
        val basePy = playerY + bounceY

        // 1. Ground Contact Shadow & Ambient Occlusion Oval
        val shadowCenter = cam.project(playerX, 0.02f, playerZ)
        if (shadowCenter.visible) {
            val heightFactor = (1f / (1f + playerY * 0.35f)).coerceIn(0.4f, 1f)
            val sw = max(14f, 1.35f * shadowCenter.scale * heightFactor)
            val sh = sw * 0.38f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x99000000),
                        Color(0x44000000),
                        Color.Transparent
                    ),
                    center = Offset(shadowCenter.x, shadowCenter.y),
                    radius = sw * 0.65f
                ),
                topLeft = Offset(shadowCenter.x - sw * 0.5f, shadowCenter.y - sh * 0.5f),
                size = Size(sw, sh)
            )
        }

        // 2. Hoverboard / Speed Boost Deck (when boosting)
        if (isBoosting) {
            drawGlow3D(cam, playerX, basePy + 0.08f, playerZ, 1.65f, board.glowColor)
            val bFront = rotateAroundPlayer(0f, 0.08f, -1.05f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
            val bBack = rotateAroundPlayer(0f, 0.08f, 1.05f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
            drawSculptedLimb3D(
                cam = cam,
                x1 = bFront.x, y1 = bFront.y, z1 = bFront.z,
                x2 = bBack.x, y2 = bBack.y, z2 = bBack.z,
                radius1 = 0.42f,
                radius2 = 0.52f,
                baseColor = board.primaryColor,
                shadowColor = shadeColor(board.primaryColor, 0.65f),
                highlightColor = board.glowColor
            )
        }

        // Compute 3D Joint Positions in Player Local Space (Z- is forward down the track, Z+ is facing camera)
        val legSwingL = when {
            isJumping -> 0.42f
            isSliding -> -0.55f
            isBoosting -> 0.18f
            else -> strideSin * 0.62f
        }
        val legSwingR = when {
            isJumping -> -0.36f
            isSliding -> 0.48f
            isBoosting -> -0.18f
            else -> -strideSin * 0.62f
        }

        val kneeLiftL = max(0f, -strideCos) * 0.32f
        val kneeLiftR = max(0f, strideCos) * 0.32f

        val hipL = rotateAroundPlayer(-0.20f, 0.94f * crouchScale, 0f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
        val hipR = rotateAroundPlayer(0.20f, 0.94f * crouchScale, 0f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)

        val kneeL = rotateAroundPlayer(
            -0.21f,
            (0.52f + kneeLiftL * 0.5f) * crouchScale,
            -legSwingL * 0.48f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )
        val kneeR = rotateAroundPlayer(
            0.21f,
            (0.52f + kneeLiftR * 0.5f) * crouchScale,
            -legSwingR * 0.48f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )

        val ankleL = rotateAroundPlayer(
            -0.21f,
            (0.14f + kneeLiftL) * crouchScale,
            -legSwingL * 0.72f + 0.14f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )
        val ankleR = rotateAroundPlayer(
            0.21f,
            (0.14f + kneeLiftR) * crouchScale,
            -legSwingR * 0.72f + 0.14f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )

        val toeL = rotateAroundPlayer(
            -0.21f,
            (0.06f + kneeLiftL * 0.85f) * crouchScale,
            -legSwingL * 0.72f - 0.18f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )
        val toeR = rotateAroundPlayer(
            0.21f,
            (0.06f + kneeLiftR * 0.85f) * crouchScale,
            -legSwingR * 0.72f - 0.18f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )

        // Torso & Shoulder Joints
        val pelvisCenter = rotateAroundPlayer(0f, 0.96f * crouchScale, 0f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
        val waistCenter = rotateAroundPlayer(0f, 1.24f * crouchScale, -0.04f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
        val chestCenter = rotateAroundPlayer(0f, 1.66f * crouchScale, -0.10f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
        val neckBase = rotateAroundPlayer(0f, 1.82f * crouchScale, -0.12f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
        val headCenter = rotateAroundPlayer(0f, 2.08f * crouchScale, -0.12f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)

        val armSwingL = -legSwingL * 0.85f
        val armSwingR = -legSwingR * 0.85f

        val shoulderL = rotateAroundPlayer(-0.38f, 1.68f * crouchScale, -0.08f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
        val shoulderR = rotateAroundPlayer(0.38f, 1.68f * crouchScale, -0.08f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)

        val elbowL = rotateAroundPlayer(
            -0.46f,
            1.34f * crouchScale,
            -0.06f - armSwingL * 0.38f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )
        val elbowR = rotateAroundPlayer(
            0.46f,
            1.34f * crouchScale,
            -0.06f - armSwingR * 0.38f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )

        val handL = rotateAroundPlayer(
            -0.42f,
            (1.16f + abs(armSwingL) * 0.18f) * crouchScale,
            -0.24f - armSwingL * 0.56f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )
        val handR = rotateAroundPlayer(
            0.42f,
            (1.16f + abs(armSwingR) * 0.18f) * crouchScale,
            -0.24f - armSwingR * 0.56f,
            playerX, basePy, playerZ, orbitYawRad, laneTiltRad
        )

        val pantsDark = shadeColor(character.pantsColor, 0.68f)
        val shirtDark = shadeColor(character.shirtColor, 0.68f)

        // Helper to draw a complete articulated leg (Thigh + Calf + Multi-part Sneaker)
        fun drawCompleteLeg(hip: Joint3D, knee: Joint3D, ankle: Joint3D, toe: Joint3D) {
            // Thigh (muscular taper)
            drawSculptedLimb3D(
                cam = cam,
                x1 = hip.x, y1 = hip.y, z1 = hip.z,
                x2 = knee.x, y2 = knee.y, z2 = knee.z,
                radius1 = 0.17f,
                radius2 = 0.135f,
                baseColor = character.pantsColor,
                shadowColor = pantsDark,
                highlightColor = character.pantsStripe
            )
            // Sculpted Calf (gastrocnemius taper down to ankle)
            drawSculptedLimb3D(
                cam = cam,
                x1 = knee.x, y1 = knee.y, z1 = knee.z,
                x2 = ankle.x, y2 = ankle.y, z2 = ankle.z,
                radius1 = 0.13f,
                radius2 = 0.085f,
                baseColor = character.pantsColor,
                shadowColor = pantsDark,
                highlightColor = character.pantsStripe
            )
            // White athletic sock cuff
            val ap = cam.project(ankle.x, ankle.y, ankle.z)
            if (ap.visible) {
                drawCircle(
                    color = Color(0xFFE8EEF5),
                    radius = max(2f, 0.092f * ap.scale),
                    center = Offset(ap.x, ap.y)
                )
            }
            // Running Shoe (Cushioned Sole + Upper + Heel Counter)
            drawSculptedLimb3D(
                cam = cam,
                x1 = ankle.x, y1 = ankle.y - 0.03f, z1 = ankle.z + 0.04f,
                x2 = toe.x, y2 = toe.y, z2 = toe.z,
                radius1 = 0.115f,
                radius2 = 0.095f,
                baseColor = character.shoeColor,
                shadowColor = shadeColor(character.shoeColor, 0.65f),
                highlightColor = character.shoeSoleColor
            )
            if (ap.visible) {
                val soleR = max(2.5f, 0.115f * ap.scale)
                drawLine(
                    color = character.shoeSoleColor,
                    start = Offset(ap.x - soleR * 0.85f, ap.y + soleR * 0.55f),
                    end = Offset(ap.x + soleR * 0.85f, ap.y + soleR * 0.55f),
                    strokeWidth = max(2.5f, soleR * 0.45f)
                )
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

        // 3. Sculpted Pelvis / Shorts & Athletic V-Taper Torso
        drawSculptedLimb3D(
            cam = cam,
            x1 = hipL.x, y1 = hipL.y, z1 = hipL.z,
            x2 = hipR.x, y2 = hipR.y, z2 = hipR.z,
            radius1 = 0.175f,
            radius2 = 0.175f,
            baseColor = character.pantsColor,
            shadowColor = pantsDark,
            highlightColor = character.pantsStripe
        )

        // Lower Torso (Waist) to Upper Chest (V-Taper)
        drawSculptedLimb3D(
            cam = cam,
            x1 = pelvisCenter.x, y1 = pelvisCenter.y + 0.05f, z1 = pelvisCenter.z,
            x2 = waistCenter.x, y2 = waistCenter.y, z2 = waistCenter.z,
            radius1 = 0.25f,
            radius2 = 0.27f,
            baseColor = character.shirtColor,
            shadowColor = shirtDark,
            highlightColor = character.shirtHighlight
        )
        drawSculptedLimb3D(
            cam = cam,
            x1 = waistCenter.x, y1 = waistCenter.y, z1 = waistCenter.z,
            x2 = chestCenter.x, y2 = chestCenter.y, z2 = chestCenter.z,
            radius1 = 0.27f,
            radius2 = 0.35f,
            baseColor = character.shirtColor,
            shadowColor = shirtDark,
            highlightColor = character.shirtHighlight
        )
        // Broad Athletic Shoulder Girdle
        drawSculptedLimb3D(
            cam = cam,
            x1 = shoulderL.x, y1 = shoulderL.y, z1 = shoulderL.z,
            x2 = shoulderR.x, y2 = shoulderR.y, z2 = shoulderR.z,
            radius1 = 0.16f,
            radius2 = 0.16f,
            baseColor = character.shirtColor,
            shadowColor = shirtDark,
            highlightColor = character.shirtHighlight
        )

        // 4. Sculpted Muscular Arms (Deltoid -> Bicep/Tricep -> Forearm -> Fingerless Glove)
        fun drawCompleteArm(shoulder: Joint3D, elbow: Joint3D, hand: Joint3D) {
            // Deltoid & Bicep
            drawSculptedLimb3D(
                cam = cam,
                x1 = shoulder.x, y1 = shoulder.y, z1 = shoulder.z,
                x2 = elbow.x, y2 = elbow.y, z2 = elbow.z,
                radius1 = 0.135f,
                radius2 = 0.105f,
                baseColor = character.skinColor,
                shadowColor = character.skinShadow,
                highlightColor = shadeColor(character.skinColor, 1.16f)
            )
            // Short Sleeve Cuff over upper deltoid
            val sleeveEnd = Joint3D(
                shoulder.x + (elbow.x - shoulder.x) * 0.36f,
                shoulder.y + (elbow.y - shoulder.y) * 0.36f,
                shoulder.z + (elbow.z - shoulder.z) * 0.36f
            )
            drawSculptedLimb3D(
                cam = cam,
                x1 = shoulder.x, y1 = shoulder.y, z1 = shoulder.z,
                x2 = sleeveEnd.x, y2 = sleeveEnd.y, z2 = sleeveEnd.z,
                radius1 = 0.148f,
                radius2 = 0.132f,
                baseColor = character.shirtColor,
                shadowColor = shirtDark,
                highlightColor = character.shirtHighlight
            )
            // Forearm
            drawSculptedLimb3D(
                cam = cam,
                x1 = elbow.x, y1 = elbow.y, z1 = elbow.z,
                x2 = hand.x, y2 = hand.y, z2 = hand.z,
                radius1 = 0.102f,
                radius2 = 0.078f,
                baseColor = character.skinColor,
                shadowColor = character.skinShadow,
                highlightColor = shadeColor(character.skinColor, 1.15f)
            )
            // Adventure Glove & Wristband
            val hp = cam.project(hand.x, hand.y, hand.z)
            if (hp.visible) {
                drawCircle(
                    color = character.gloveColor,
                    radius = max(2.5f, 0.092f * hp.scale),
                    center = Offset(hp.x, hp.y)
                )
            }
        }

        drawCompleteArm(shoulderL, elbowL, handL)
        drawCompleteArm(shoulderR, elbowR, handR)

        // 5. Detailed 3D Expedition Backpack (When viewed from behind or side)
        val packBackCenter = rotateAroundPlayer(0f, 1.48f * crouchScale, 0.22f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
        val packTop = rotateAroundPlayer(0f, 1.72f * crouchScale, 0.20f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)
        val packBottom = rotateAroundPlayer(0f, 1.20f * crouchScale, 0.22f, playerX, basePy, playerZ, orbitYawRad, laneTiltRad)

        val isBackVisible = cos(orbitYawRad) >= -0.25f
        if (isBackVisible) {
            // Shoulder Harness Straps
            val strapL = cam.project(shoulderL.x * 0.72f + chestCenter.x * 0.28f, shoulderL.y, shoulderL.z + 0.12f)
            val strapR = cam.project(shoulderR.x * 0.72f + chestCenter.x * 0.28f, shoulderR.y, shoulderR.z + 0.12f)
            val packP = cam.project(packBackCenter.x, packBackCenter.y, packBackCenter.z)
            if (strapL.visible && strapR.visible && packP.visible) {
                val strapW = max(2f, 0.07f * packP.scale)
                drawLine(
                    color = Color(0xFF2B241E),
                    start = Offset(strapL.x, strapL.y),
                    end = Offset(strapL.x, packP.y + strapW * 2.5f),
                    strokeWidth = strapW
                )
                drawLine(
                    color = Color(0xFF2B241E),
                    start = Offset(strapR.x, strapR.y),
                    end = Offset(strapR.x, packP.y + strapW * 2.5f),
                    strokeWidth = strapW
                )
            }

            // Main Sculpted Rucksack Body
            val pTop = cam.project(packTop.x, packTop.y, packTop.z)
            val pBot = cam.project(packBottom.x, packBottom.y, packBottom.z)
            if (pTop.visible && pBot.visible) {
                val pw = max(10f, 0.56f * pTop.scale)
                val ph = max(12f, abs(pBot.y - pTop.y) + 0.18f * pTop.scale)
                val left = (pTop.x + pBot.x) * 0.5f - pw * 0.5f
                val top = minOf(pTop.y, pBot.y) - 0.06f * pTop.scale

                // Backpack Shadow & Outer Rucksack
                drawRoundRect(
                    color = shadeColor(character.backpackColor, 0.65f),
                    topLeft = Offset(left - 2f, top + 2f),
                    size = Size(pw + 4f, ph),
                    cornerRadius = CornerRadius(pw * 0.24f)
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            character.backpackFlapColor,
                            character.backpackColor,
                            shadeColor(character.backpackColor, 0.75f)
                        ),
                        startY = top,
                        endY = top + ph
                    ),
                    topLeft = Offset(left, top),
                    size = Size(pw, ph),
                    cornerRadius = CornerRadius(pw * 0.22f)
                )

                // Top Rucksack Flap & Dual Vertical Buckle Straps
                drawRoundRect(
                    color = character.backpackFlapColor,
                    topLeft = Offset(left + pw * 0.08f, top + ph * 0.05f),
                    size = Size(pw * 0.84f, ph * 0.36f),
                    cornerRadius = CornerRadius(pw * 0.18f)
                )
                for (bx in listOf(0.28f, 0.72f)) {
                    val sx = left + pw * bx
                    drawLine(
                        color = Color(0xFF261F19),
                        start = Offset(sx, top + ph * 0.12f),
                        end = Offset(sx, top + ph * 0.88f),
                        strokeWidth = max(2f, pw * 0.08f)
                    )
                    // Metallic Gold/Cyan Buckle Clasp
                    drawCircle(
                        color = character.accentGlow,
                        radius = max(2f, pw * 0.065f),
                        center = Offset(sx, top + ph * 0.42f)
                    )
                }

                // Lower Exterior Pocket
                drawRoundRect(
                    color = shadeColor(character.backpackColor, 0.82f),
                    topLeft = Offset(left + pw * 0.15f, top + ph * 0.48f),
                    size = Size(pw * 0.70f, ph * 0.38f),
                    cornerRadius = CornerRadius(pw * 0.14f)
                )
            }
        }

        // 6. Neck, Sculpted Head, Ears & Layered Hair / Character Headgear
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
            val hr = max(6f, 0.25f * hp.scale)
            // Ears
            drawCircle(color = character.skinShadow, radius = hr * 0.28f, center = Offset(hp.x - hr * 0.92f, hp.y + hr * 0.08f))
            drawCircle(color = character.skinShadow, radius = hr * 0.28f, center = Offset(hp.x + hr * 0.92f, hp.y + hr * 0.08f))

            // Head Cranium base
            drawCircle(
                color = character.skinColor,
                radius = hr,
                center = Offset(hp.x, hp.y)
            )

            // If viewing from the back/three-quarter, render sculpted layered hair locks & nape
            if (cos(orbitYawRad) >= -0.15f) {
                val hairPath = Path().apply {
                    moveTo(hp.x - hr * 1.02f, hp.y + hr * 0.18f)
                    quadraticBezierTo(hp.x - hr * 1.08f, hp.y - hr * 1.05f, hp.x, hp.y - hr * 1.14f)
                    quadraticBezierTo(hp.x + hr * 1.08f, hp.y - hr * 1.05f, hp.x + hr * 1.02f, hp.y + hr * 0.18f)
                    quadraticBezierTo(hp.x, hp.y + hr * 0.65f, hp.x - hr * 1.02f, hp.y + hr * 0.18f)
                    close()
                }
                drawPath(hairPath, color = character.hairColor)
                // Hair crown highlight
                drawCircle(
                    color = shadeColor(character.hairColor, 1.38f),
                    radius = hr * 0.45f,
                    center = Offset(hp.x - hr * 0.18f, hp.y - hr * 0.42f)
                )
                // Character-specific headband / cyber visor rim
                if (character.id != "leo") {
                    drawLine(
                        color = character.accentGlow,
                        start = Offset(hp.x - hr * 0.95f, hp.y - hr * 0.12f),
                        end = Offset(hp.x + hr * 0.95f, hp.y - hr * 0.12f),
                        strokeWidth = max(2.5f, hr * 0.22f)
                    )
                }
            } else {
                // Front face view when rotated in 360° Inspect Mode!
                drawArc(
                    color = character.hairColor,
                    startAngle = 190f,
                    sweepAngle = 160f,
                    useCenter = true,
                    topLeft = Offset(hp.x - hr * 1.05f, hp.y - hr * 1.12f),
                    size = Size(hr * 2.1f, hr * 1.6f)
                )
                // Eyes / Cyber Visor
                if (character.id == "kai") {
                    drawRoundRect(
                        color = Color(0xFF00F0FF),
                        topLeft = Offset(hp.x - hr * 0.65f, hp.y - hr * 0.18f),
                        size = Size(hr * 1.3f, hr * 0.34f),
                        cornerRadius = CornerRadius(hr * 0.15f)
                    )
                } else {
                    drawCircle(color = Color(0xFF1E1A17), radius = hr * 0.13f, center = Offset(hp.x - hr * 0.34f, hp.y - hr * 0.05f))
                    drawCircle(color = Color(0xFF1E1A17), radius = hr * 0.13f, center = Offset(hp.x + hr * 0.34f, hp.y - hr * 0.05f))
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
    }
}
