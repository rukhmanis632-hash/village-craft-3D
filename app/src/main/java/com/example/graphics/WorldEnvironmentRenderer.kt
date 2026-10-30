package com.example.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.AmbientParticle
import com.example.model.EntityType
import com.example.model.PowerupKind
import com.example.model.RoadStyle
import com.example.model.SceneryRow
import com.example.model.WorldEntity
import com.example.model.WorldTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object WorldEnvironmentRenderer {

    fun DrawScope.renderSkyAndHorizon(
        cam: Camera3D,
        world: WorldTheme,
        timeSec: Float
    ) {
        val w = size.width
        val h = size.height
        val horizonY = cam.horizonY + h * 0.08f

        // 1. Sky Dome Multi-Stop Atmospheric Gradient
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to world.skyTop,
                0.55f to world.skyMid,
                1.0f to world.skyHorizon,
                startY = 0f,
                endY = horizonY
            ),
            topLeft = Offset.Zero,
            size = Size(w, horizonY)
        )

        // 2. Ground Plane Base with Distance Fog Gradient
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to world.fogColor,
                0.28f to world.groundInner,
                1.0f to world.groundOuter,
                startY = horizonY - 6f,
                endY = h
            ),
            topLeft = Offset(0f, horizonY - 6f),
            size = Size(w, h - horizonY + 6f)
        )

        // 3. World-Specific Horizon Monuments & Vista
        when (world.roadStyle) {
            RoadStyle.RUINS -> renderRuinsVista(w, horizonY, timeSec)
            RoadStyle.CYBER -> renderCyberVista(w, horizonY, timeSec)
            RoadStyle.LAVA -> renderLavaVista(w, h, horizonY, timeSec)
            RoadStyle.SNOW -> renderSnowVista(w, horizonY)
        }

        // 4. Soft Horizon Atmospheric Haze Blend
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    world.fogColor.copy(alpha = 0.65f),
                    Color.Transparent
                ),
                startY = horizonY - h * 0.09f,
                endY = horizonY + h * 0.08f
            ),
            topLeft = Offset(0f, horizonY - h * 0.09f),
            size = Size(w, h * 0.17f)
        )
    }

    private fun DrawScope.renderRuinsVista(w: Float, horizonY: Float, timeSec: Float) {
        // Distant jungle tree silhouettes
        val jungleCol = Color(0xFF3E6B48)
        for (i in -4..4) {
            val cx = w * 0.5f + i * (w * 0.13f)
            val r = w * (0.09f + (abs(i) % 2) * 0.03f)
            drawCircle(color = jungleCol, radius = r, center = Offset(cx, horizonY - r * 0.35f))
        }

        // Grand Stepped Ancient Temple Sanctuary & Dome in Center Horizon
        val cx = w * 0.5f
        val templeStone = Color(0xFF7D8C6E)
        val templeHighlight = Color(0xFFA5B58E)
        val templeDark = Color(0xFF4F5E46)

        // Base tier
        val baseW = w * 0.38f
        val baseH = horizonY * 0.32f
        drawRect(
            color = templeStone,
            topLeft = Offset(cx - baseW * 0.5f, horizonY - baseH),
            size = Size(baseW, baseH)
        )
        // Second stepped tier
        val midW = w * 0.28f
        val midH = horizonY * 0.22f
        drawRect(
            color = templeHighlight,
            topLeft = Offset(cx - midW * 0.5f, horizonY - baseH - midH),
            size = Size(midW, midH)
        )
        // Temple Dome (Stupa / Cupola)
        drawCircle(
            color = templeStone,
            radius = midW * 0.38f,
            center = Offset(cx, horizonY - baseH - midH)
        )
        // Golden Spire Finial
        val spirePath = Path().apply {
            moveTo(cx - 5f, horizonY - baseH - midH - midW * 0.35f)
            lineTo(cx + 5f, horizonY - baseH - midH - midW * 0.35f)
            lineTo(cx, horizonY - baseH - midH - midW * 0.54f)
            close()
        }
        drawPath(spirePath, color = Color(0xFFFFD03B))

        // Glowing Inner Temple Sanctuary Portal
        val portalW = baseW * 0.22f
        val portalH = baseH * 0.58f
        drawRect(
            color = templeDark,
            topLeft = Offset(cx - portalW * 0.5f, horizonY - portalH),
            size = Size(portalW, portalH)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xAAFFE27A), Color.Transparent),
                center = Offset(cx, horizonY - portalH * 0.45f),
                radius = portalW * 0.9f
            ),
            radius = portalW * 0.9f,
            center = Offset(cx, horizonY - portalH * 0.45f)
        )

        // Volumetric Sunbeams / God-Rays streaming from top-right canopy
        val pulse = 0.85f + 0.15f * sin(timeSec * 1.4f)
        for (ray in 0..4) {
            val startX = w * (0.72f + ray * 0.06f)
            val endLeftX = w * (0.12f + ray * 0.14f)
            val rayW = w * 0.09f
            val rayPath = Path().apply {
                moveTo(startX, 0f)
                lineTo(startX + rayW * 0.5f, 0f)
                lineTo(endLeftX + rayW, horizonY * 1.35f)
                lineTo(endLeftX, horizonY * 1.35f)
                close()
            }
            drawPath(
                path = rayPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x55FFF099).copy(alpha = 0.22f * pulse),
                        Color(0x33FFE066).copy(alpha = 0.10f * pulse),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = horizonY * 1.35f
                )
            )
        }
    }

    private fun DrawScope.renderCyberVista(w: Float, horizonY: Float, timeSec: Float) {
        // Towering Futuristic Skyscrapers with glowing window grids
        val towerSpecs = listOf(
            Triple(0.08f, 0.14f, 0.78f),
            Triple(0.22f, 0.12f, 0.65f),
            Triple(0.36f, 0.13f, 0.92f),
            Triple(0.50f, 0.15f, 0.86f),
            Triple(0.65f, 0.13f, 0.72f),
            Triple(0.79f, 0.14f, 0.88f),
            Triple(0.92f, 0.12f, 0.74f)
        )
        towerSpecs.forEachIndexed { idx, (cxRatio, wRatio, hRatio) ->
            val tw = w * wRatio
            val th = horizonY * hRatio
            val left = w * cxRatio - tw * 0.5f
            val top = horizonY - th
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0B112C), Color(0xFF1A1E4A)),
                    startY = top,
                    endY = horizonY
                ),
                topLeft = Offset(left, top),
                size = Size(tw, th)
            )
            // Neon roof trim
            val trimCol = if (idx % 2 == 0) Color(0xFF00F0FF) else Color(0xFFFF00AA)
            drawLine(
                color = trimCol.copy(alpha = 0.8f),
                start = Offset(left, top),
                end = Offset(left + tw, top),
                strokeWidth = 3f
            )
            // Illuminated window strips
            val rows = 9
            for (r in 1 until rows) {
                val wy = top + (th / rows) * r
                drawLine(
                    color = trimCol.copy(alpha = 0.35f),
                    start = Offset(left + tw * 0.18f, wy),
                    end = Offset(left + tw * 0.82f, wy),
                    strokeWidth = 2f
                )
            }
        }

        // Animated Flying Cyber-Cars cruising across the skyline
        for (carIdx in 0..2) {
            val dir = if (carIdx % 2 == 0) 1f else -1f
            val speed = 0.12f + carIdx * 0.04f
            val rawX = ((timeSec * speed * dir + carIdx * 0.37f) % 1.2f + 1.2f) % 1.2f - 0.1f
            val cx = w * rawX
            val cy = horizonY * (0.42f + carIdx * 0.16f)
            val carW = w * 0.065f
            val carH = carW * 0.34f

            // Thruster trail
            val glowCol = if (dir > 0) Color(0xFF00F0FF) else Color(0xFFFF00AA)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = if (dir > 0) listOf(Color.Transparent, glowCol) else listOf(glowCol, Color.Transparent),
                    startX = cx - carW * 1.4f,
                    endX = cx + carW * 1.4f
                ),
                start = Offset(cx - dir * carW * 1.1f, cy),
                end = Offset(cx, cy),
                strokeWidth = carH * 0.65f,
                cap = StrokeCap.Round
            )
            // Sleek spinner car body
            drawRoundRect(
                color = Color(0xFF1C2547),
                topLeft = Offset(cx - carW * 0.5f, cy - carH * 0.5f),
                size = Size(carW, carH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(carH * 0.5f)
            )
            // Neon canopy & thruster lights
            drawCircle(
                color = Color(0xFF00F0FF),
                radius = carH * 0.32f,
                center = Offset(cx - dir * carW * 0.35f, cy)
            )
            drawCircle(
                color = Color(0xFFFF2A6D),
                radius = carH * 0.28f,
                center = Offset(cx + dir * carW * 0.35f, cy)
            )
        }
    }

    private fun DrawScope.renderLavaVista(w: Float, h: Float, horizonY: Float, timeSec: Float) {
        // Cascading Molten Lava Waterfalls on Left and Right Cavern Walls
        val fallPositions = listOf(w * 0.12f to w * 0.06f, w * 0.88f to w * 0.085f)
        fallPositions.forEach { (fx, fw) ->
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFAA00),
                        Color(0xFFFF5500),
                        Color(0xFFFF2200)
                    ),
                    startY = 0f,
                    endY = horizonY * 1.15f
                ),
                topLeft = Offset(fx - fw * 0.5f, 0f),
                size = Size(fw, horizonY * 1.15f)
            )
            // Animated bright lava surges
            for (s in 0..3) {
                val sy = ((timeSec * 120f + s * 80f) % (horizonY * 1.1f))
                drawCircle(
                    color = Color(0xFFFFF088).copy(alpha = 0.75f),
                    radius = fw * 0.38f,
                    center = Offset(fx, sy)
                )
            }
        }

        // Jagged Upper Cavern Ceiling Stalactites
        val stalactitePath = Path().apply {
            moveTo(0f, 0f)
            val peaks = 10
            for (i in 0..peaks) {
                val px = (w / peaks) * i
                val py = if (i % 2 == 0) horizonY * (0.14f + (i % 3) * 0.07f) else horizonY * 0.04f
                lineTo(px, py)
            }
            lineTo(w, 0f)
            close()
        }
        drawPath(stalactitePath, color = Color(0xFF1C0802))

        // Churning Magma Waves on Left & Right of Bridge
        val waveCount = 6
        for (i in 0 until waveCount) {
            val wy = horizonY + (h - horizonY) * ((i + 0.5f) / waveCount)
            val shift = sin(timeSec * 2.2f + i) * 18f
            drawLine(
                color = Color(0xFFFFCC00).copy(alpha = 0.55f),
                start = Offset(w * 0.02f + shift, wy),
                end = Offset(w * 0.24f + shift, wy),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFFFCC00).copy(alpha = 0.55f),
                start = Offset(w * 0.76f - shift, wy),
                end = Offset(w * 0.98f - shift, wy),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }
    }

    private fun DrawScope.renderSnowVista(w: Float, horizonY: Float) {
        // Majestic Jagged Snow-Capped Alpine Peaks
        val peaks = listOf(
            Triple(0.18f, 0.42f, 0.68f),
            Triple(0.50f, 0.48f, 0.86f),
            Triple(0.82f, 0.44f, 0.74f)
        )
        peaks.forEach { (cxR, baseWR, hR) ->
            val cx = w * cxR
            val bw = w * baseWR
            val ph = horizonY * hR
            val mtnPath = Path().apply {
                moveTo(cx - bw * 0.5f, horizonY)
                lineTo(cx, horizonY - ph)
                lineTo(cx + bw * 0.5f, horizonY)
                close()
            }
            drawPath(
                path = mtnPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFB8DCFA), Color(0xFF7FAFD9)),
                    startY = horizonY - ph,
                    endY = horizonY
                )
            )
            // Shaded right slope for 3D alpine relief
            val shadowSlope = Path().apply {
                moveTo(cx, horizonY - ph)
                lineTo(cx + bw * 0.5f, horizonY)
                lineTo(cx + bw * 0.08f, horizonY)
                close()
            }
            drawPath(shadowSlope, color = Color(0x4446759E))
        }
    }

    /**
     * Renders the 3D perspective road surface, cobblestones / cyber neon lanes /
     * lava basalt cracks / wooden snow bridge planks, and raised 3D curbs/railings.
     */
    fun DrawScope.renderRoad3D(
        cam: Camera3D,
        world: WorldTheme,
        scrollDistance: Float
    ) {
        val roadHalfW = 5.4f
        val curbW = 0.85f
        val zStart = 8.0f
        val zEnd = -115f

        // 1. Main Road Base Quad
        drawQuad3D(
            cam,
            -roadHalfW, 0f, zStart,
            roadHalfW, 0f, zStart,
            roadHalfW, 0f, zEnd,
            -roadHalfW, 0f, zEnd,
            fillColor = world.roadBase
        )

        // 2. Raised 3D Left & Right Curbs
        for (side in listOf(-1f, 1f)) {
            val innerX = side * roadHalfW
            val outerX = side * (roadHalfW + curbW)
            drawQuad3D(
                cam,
                innerX, 0.28f, zStart,
                outerX, 0.28f, zStart,
                outerX, 0.28f, zEnd,
                innerX, 0.28f, zEnd,
                fillColor = world.curbColor
            )
            // Inner vertical curb wall
            drawQuad3D(
                cam,
                innerX, 0f, zStart,
                innerX, 0.28f, zStart,
                innerX, 0.28f, zEnd,
                innerX, 0f, zEnd,
                fillColor = world.curbAccent
            )
        }

        // 3. Detailed Scrolling 3D Road Slabs / Planks / Neon Strips (from back to front)
        val tileStep = 3.2f
        val phase = (scrollDistance % tileStep)
        var z = zEnd + phase
        var rowIdx = (scrollDistance / tileStep).toInt()

        while (z < zStart - 0.5f) {
            val nextZ = min(zStart, z + tileStep * 0.86f)
            val depth = max(1f, cam.camZ - z)

            when (world.roadStyle) {
                RoadStyle.RUINS -> {
                    // 3 lanes of staggered mossy temple flagstones
                    for (lane in -1..1) {
                        val lx = lane * 3.4f
                        val col = if ((rowIdx + lane) % 2 == 0) world.roadTilePrimary else world.roadTileSecondary
                        val fogged = fogBlend(col, world.fogColor, depth)
                        drawQuad3D(
                            cam,
                            lx - 1.52f, 0.01f, nextZ,
                            lx + 1.52f, 0.01f, nextZ,
                            lx + 1.52f, 0.01f, z,
                            lx - 1.52f, 0.01f, z,
                            fillColor = fogged,
                            strokeColor = fogBlend(world.roadSeam, world.fogColor, depth),
                            strokeWidth = 1.2f
                        )
                    }
                }
                RoadStyle.CYBER -> {
                    // Glowing cyan dashed lane lines & metallic street grates
                    for (div in listOf(-1.7f, 1.7f)) {
                        if (rowIdx % 2 == 0) {
                            drawQuad3D(
                                cam,
                                div - 0.10f, 0.02f, nextZ,
                                div + 0.10f, 0.02f, nextZ,
                                div + 0.10f, 0.02f, z,
                                div - 0.10f, 0.02f, z,
                                fillColor = Color(0xFF00F0FF)
                            )
                        }
                    }
                    // Neon edge strips
                    for (side in listOf(-5.2f, 5.2f)) {
                        drawQuad3D(
                            cam,
                            side - 0.08f, 0.03f, nextZ,
                            side + 0.08f, 0.03f, nextZ,
                            side + 0.08f, 0.03f, z,
                            side - 0.08f, 0.03f, z,
                            fillColor = if (side < 0) Color(0xFFFF00AA) else Color(0xFF00F0FF)
                        )
                    }
                }
                RoadStyle.LAVA -> {
                    // Dark cracked volcanic basalt slabs over glowing orange magma seams
                    for (lane in -1..1) {
                        val lx = lane * 3.4f
                        val shift = if (rowIdx % 2 == 0) 0.12f else -0.12f
                        val slabCol = fogBlend(world.roadTilePrimary, world.fogColor, depth)
                        drawQuad3D(
                            cam,
                            lx - 1.48f + shift, 0.02f, nextZ,
                            lx + 1.48f + shift, 0.02f, nextZ,
                            lx + 1.48f - shift, 0.02f, z,
                            lx - 1.48f - shift, 0.02f, z,
                            fillColor = slabCol,
                            strokeColor = Color(0xFFFF6600).copy(alpha = 0.7f),
                            strokeWidth = 1.4f
                        )
                    }
                }
                RoadStyle.SNOW -> {
                    // Weathered wooden bridge planks with snow patches on edges
                    val plankCol = fogBlend(
                        if (rowIdx % 2 == 0) world.roadTilePrimary else world.roadTileSecondary,
                        world.fogColor,
                        depth
                    )
                    drawQuad3D(
                        cam,
                        -4.8f, 0.02f, nextZ,
                        4.8f, 0.02f, nextZ,
                        4.8f, 0.02f, z,
                        -4.8f, 0.02f, z,
                        fillColor = plankCol
                    )
                    // Snow drift along bridge edges
                    for (side in listOf(-4.3f, 4.3f)) {
                        drawQuad3D(
                            cam,
                            side - 0.65f, 0.03f, nextZ,
                            side + 0.65f, 0.03f, nextZ,
                            side + 0.65f, 0.03f, z,
                            side - 0.65f, 0.03f, z,
                            fillColor = Color(0xFFF0F8FF)
                        )
                    }
                }
            }

            z += tileStep
            rowIdx++
        }
    }

    /**
     * Renders a single depth-sorted roadside scenery row at `row.z`.
     */
    fun DrawScope.renderSceneryRow3D(
        cam: Camera3D,
        world: WorldTheme,
        row: SceneryRow,
        timeSec: Float
    ) {
        val z = row.z
        if (z > cam.camZ - 0.5f || z < -118f) return

        when (world.roadStyle) {
            RoadStyle.RUINS -> {
                for (side in listOf(-1f, 1f)) {
                    // Fluted Stone Temple Column
                    val colX = side * 7.4f
                    drawBox3D(
                        cam = cam,
                        cx = colX,
                        bottomY = 0f,
                        cz = z,
                        width = 1.35f,
                        height = 6.2f,
                        depthZ = 1.35f,
                        baseColor = Color(0xFF9E947A),
                        fogColor = world.fogColor
                    )
                    // Column Capital
                    drawBox3D(
                        cam = cam,
                        cx = colX,
                        bottomY = 6.2f,
                        cz = z,
                        width = 1.75f,
                        height = 0.65f,
                        depthZ = 1.75f,
                        baseColor = Color(0xFFB5AA8E),
                        fogColor = world.fogColor
                    )

                    // Jungle Canopy Tree Crown behind column
                    val treeP = cam.project(side * 10.8f, 5.8f, z - 1.5f)
                    if (treeP.visible) {
                        val tr = (3.2f * treeP.scale).coerceAtLeast(4f)
                        val treeCol = fogBlend(Color(0xFF2B6329), world.fogColor, treeP.depth)
                        drawCircle(color = treeCol, radius = tr, center = Offset(treeP.x, treeP.y))
                        drawCircle(
                            color = shadeColor(treeCol, 1.22f),
                            radius = tr * 0.72f,
                            center = Offset(treeP.x - side * tr * 0.18f, treeP.y - tr * 0.18f)
                        )
                    }

                    // Flickering Stone Torch Brazier along curb
                    val torchX = side * 5.85f
                    drawBox3D(
                        cam = cam,
                        cx = torchX,
                        bottomY = 0.25f,
                        cz = z + 2.2f,
                        width = 0.55f,
                        height = 1.65f,
                        depthZ = 0.55f,
                        baseColor = Color(0xFF4A3D31),
                        fogColor = world.fogColor
                    )
                    val flicker = 0.88f + 0.16f * sin(timeSec * 9f + z)
                    drawGlow3D(cam, torchX, 2.35f, z + 2.2f, 1.45f * flicker, Color(0xFFFF8800))
                    val flameP = cam.project(torchX, 2.25f, z + 2.2f)
                    if (flameP.visible) {
                        val fr = max(3f, 0.45f * flameP.scale * flicker)
                        val flamePath = Path().apply {
                            moveTo(flameP.x - fr, flameP.y)
                            lineTo(flameP.x + fr, flameP.y)
                            lineTo(flameP.x, flameP.y - fr * 2.5f)
                            close()
                        }
                        drawPath(flamePath, color = Color(0xFFFF6A00))
                        drawCircle(color = Color(0xFFFFF088), radius = fr * 0.65f, center = Offset(flameP.x, flameP.y - fr * 0.5f))
                    }
                }

                // Grand Mossy Temple Archway spanning the road on archway rows
                if (row.hasArchway) {
                    drawBox3D(
                        cam = cam,
                        cx = 0f,
                        bottomY = 5.6f,
                        cz = z,
                        width = 16.2f,
                        height = 1.6f,
                        depthZ = 1.6f,
                        baseColor = Color(0xFF8C826A),
                        fogColor = world.fogColor
                    )
                    // Mossy keystone crest
                    drawBox3D(
                        cam = cam,
                        cx = 0f,
                        bottomY = 7.2f,
                        cz = z,
                        width = 6.5f,
                        height = 1.1f,
                        depthZ = 1.4f,
                        baseColor = Color(0xFF567D38),
                        fogColor = world.fogColor
                    )
                }
            }

            RoadStyle.CYBER -> {
                for (side in listOf(-1f, 1f)) {
                    val bldgX = side * 9.8f
                    val bldgH = 14f + (row.variant % 3) * 3.5f
                    drawBox3D(
                        cam = cam,
                        cx = bldgX,
                        bottomY = 0f,
                        cz = z,
                        width = 4.6f,
                        height = bldgH,
                        depthZ = 5.2f,
                        baseColor = Color(0xFF121836),
                        fogColor = world.fogColor,
                        outlineColor = Color(0x4400F0FF)
                    )

                    // Glowing Neon Hologram Billboard on inner building wall
                    val holoColor = if ((row.variant + (if (side > 0) 1 else 0)) % 2 == 0) {
                        Color(0xFFFF00AA)
                    } else {
                        Color(0xFF00F0FF)
                    }
                    val bx = side * 7.2f
                    drawQuad3D(
                        cam,
                        bx, 3.2f, z + 1.8f,
                        bx, 3.2f, z - 1.8f,
                        bx, 7.6f, z - 1.8f,
                        bx, 7.6f, z + 1.8f,
                        fillColor = holoColor.copy(alpha = 0.45f),
                        strokeColor = holoColor,
                        strokeWidth = 2.2f
                    )
                    drawGlow3D(cam, bx, 5.4f, z, 2.4f, holoColor.copy(alpha = 0.45f))
                }
            }

            RoadStyle.LAVA -> {
                for (side in listOf(-1f, 1f)) {
                    // 3D Iron Bridge Railing Posts & Handrails
                    val railX = side * 5.75f
                    drawBox3D(
                        cam = cam,
                        cx = railX,
                        bottomY = 0.25f,
                        cz = z,
                        width = 0.36f,
                        height = 1.65f,
                        depthZ = 0.36f,
                        baseColor = Color(0xFF2B201C),
                        fogColor = world.fogColor
                    )
                    // Horizontal Iron Handrail spanning to next post
                    drawQuad3D(
                        cam,
                        railX - 0.12f, 1.75f, z + 9f,
                        railX + 0.12f, 1.75f, z + 9f,
                        railX + 0.12f, 1.75f, z - 9f,
                        railX - 0.12f, 1.75f, z - 9f,
                        fillColor = Color(0xFF3D2E29)
                    )

                    // Levitating Incandescent Volcanic Meteor Boulder in the Cavern
                    val bob = sin(timeSec * 2.2f + row.variant + side) * 0.65f
                    val meteorX = side * 9.2f
                    val meteorY = 5.4f + (row.variant % 2) * 1.8f + bob
                    drawGlow3D(cam, meteorX, meteorY, z, 2.2f, Color(0xFFFF4500))
                    val mp = cam.project(meteorX, meteorY, z)
                    if (mp.visible) {
                        val mr = max(4f, 1.15f * mp.scale)
                        drawCircle(color = Color(0xFF2B140E), radius = mr, center = Offset(mp.x, mp.y))
                        drawCircle(
                            color = Color(0xFFFF6600),
                            radius = mr * 0.55f,
                            center = Offset(mp.x - mr * 0.2f, mp.y + mr * 0.15f)
                        )
                    }
                }
            }

            RoadStyle.SNOW -> {
                for (side in listOf(-1f, 1f)) {
                    // Snow-Topped Wooden Bridge Post
                    val postX = side * 5.75f
                    drawBox3D(
                        cam = cam,
                        cx = postX,
                        bottomY = 0.2f,
                        cz = z,
                        width = 0.48f,
                        height = 1.45f,
                        depthZ = 0.48f,
                        baseColor = Color(0xFF5C3D24),
                        fogColor = world.fogColor
                    )
                    // White snow cap on post
                    val capP = cam.project(postX, 1.72f, z)
                    if (capP.visible) {
                        drawCircle(
                            color = Color.White,
                            radius = max(2.5f, 0.34f * capP.scale),
                            center = Offset(capP.x, capP.y)
                        )
                    }

                    // Faceted Translucent Blue Ice Crystal Cluster
                    val cryX = side * 7.8f
                    val cBase = cam.project(cryX, 0f, z + 1.5f)
                    val cTip = cam.project(cryX - side * 0.4f, 3.6f, z + 1.5f)
                    if (cBase.visible && cTip.visible) {
                        val cw = max(3f, 0.85f * cBase.scale)
                        val icePath = Path().apply {
                            moveTo(cBase.x - cw, cBase.y)
                            lineTo(cTip.x, cTip.y)
                            lineTo(cBase.x + cw, cBase.y)
                            close()
                        }
                        drawPath(icePath, color = fogBlend(Color(0xFF5CD6FF), world.fogColor, cBase.depth))
                        drawPath(
                            icePath,
                            color = Color.White.copy(alpha = 0.65f),
                            style = Stroke(width = 1.5f)
                        )
                    }

                    // Snow-Covered Multi-Tiered Alpine Pine Tree
                    val treeX = side * 10.6f
                    val tBase = cam.project(treeX, 0.5f, z - 1.5f)
                    val tTop = cam.project(treeX, 7.2f, z - 1.5f)
                    if (tBase.visible && tTop.visible) {
                        val tw = max(5f, 2.4f * tBase.scale)
                        val th = tBase.y - tTop.y
                        for (tier in 0..2) {
                            val tyBottom = tBase.y - th * (tier * 0.28f)
                            val tyTop = tBase.y - th * (0.48f + tier * 0.26f)
                            val tierW = tw * (1f - tier * 0.22f)
                            val pinePath = Path().apply {
                                moveTo(tBase.x - tierW, tyBottom)
                                lineTo(tBase.x, tyTop)
                                lineTo(tBase.x + tierW, tyBottom)
                                close()
                            }
                            drawPath(
                                pinePath,
                                color = fogBlend(Color(0xFF2D5A4C), world.fogColor, tBase.depth)
                            )
                            // Snow blanket on upper pine needles
                            val snowPath = Path().apply {
                                moveTo(tBase.x - tierW * 0.68f, tyTop + (tyBottom - tyTop) * 0.65f)
                                lineTo(tBase.x, tyTop)
                                lineTo(tBase.x + tierW * 0.68f, tyTop + (tyBottom - tyTop) * 0.65f)
                                close()
                            }
                            drawPath(
                                snowPath,
                                color = fogBlend(Color(0xFFF2F9FF), world.fogColor, tBase.depth)
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Renders a 3D gameplay entity (Coin, Ruby, Diamond, Powerup, Low Hurdle, High Barrier, or Aura Gate).
     */
    fun DrawScope.renderEntity3D(
        cam: Camera3D,
        world: WorldTheme,
        entity: WorldEntity,
        timeSec: Float
    ) {
        if (!entity.active || entity.z > cam.camZ - 0.4f || entity.z < -115f) return

        when (entity.type) {
            EntityType.COIN -> {
                val bobY = entity.y + sin(timeSec * 5f + entity.id) * 0.12f
                val p = cam.project(entity.x, bobY, entity.z)
                if (!p.visible) return
                val r = max(4f, 0.46f * p.scale)
                val spinWidth = (abs(cos(entity.rotationY)) * 0.75f + 0.25f) * r

                // Golden ground shadow
                val sp = cam.project(entity.x, 0.03f, entity.z)
                if (sp.visible) {
                    drawOval(
                        color = Color(0x55000000),
                        topLeft = Offset(sp.x - r * 0.7f, sp.y - r * 0.25f),
                        size = Size(r * 1.4f, r * 0.5f)
                    )
                }

                // Coin metallic rim + face + star emboss
                drawOval(
                    color = Color(0xFFB36B00),
                    topLeft = Offset(p.x - spinWidth - 2f, p.y - r),
                    size = Size(spinWidth * 2f + 4f, r * 2f)
                )
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF38F), Color(0xFFF5B000), Color(0xFFC47A00)),
                        center = Offset(p.x - spinWidth * 0.2f, p.y - r * 0.2f),
                        radius = r * 1.1f
                    ),
                    topLeft = Offset(p.x - spinWidth, p.y - r),
                    size = Size(spinWidth * 2f, r * 2f)
                )
                // Inner star highlight
                drawCircle(
                    color = Color(0xFFFFFAE0),
                    radius = min(spinWidth, r) * 0.38f,
                    center = Offset(p.x, p.y)
                )
            }

            EntityType.RUBY, EntityType.DIAMOND -> {
                val isDiamond = entity.type == EntityType.DIAMOND
                val bobY = entity.y + sin(timeSec * 5.5f + entity.id) * 0.15f
                val mainCol = if (isDiamond) Color(0xFF00E5FF) else Color(0xFFFF1E42)
                val lightCol = if (isDiamond) Color(0xFFB8F8FF) else Color(0xFFFF9EB5)
                val darkCol = if (isDiamond) Color(0xFF006B8F) else Color(0xFF8F001A)

                drawGlow3D(cam, entity.x, bobY, entity.z, 1.05f, mainCol)
                val p = cam.project(entity.x, bobY, entity.z)
                if (!p.visible) return
                val r = max(5f, 0.52f * p.scale)

                val gemPath = Path().apply {
                    moveTo(p.x, p.y - r * 1.15f)
                    lineTo(p.x + r * 0.95f, p.y - r * 0.15f)
                    lineTo(p.x, p.y + r * 1.15f)
                    lineTo(p.x - r * 0.95f, p.y - r * 0.15f)
                    close()
                }
                drawPath(gemPath, color = mainCol)
                // Upper facet highlight
                val crownPath = Path().apply {
                    moveTo(p.x, p.y - r * 1.15f)
                    lineTo(p.x + r * 0.55f, p.y - r * 0.15f)
                    lineTo(p.x - r * 0.55f, p.y - r * 0.15f)
                    close()
                }
                drawPath(crownPath, color = lightCol)
                drawPath(gemPath, color = darkCol, style = Stroke(width = 1.5f))
            }

            EntityType.POWERUP -> {
                val kind = entity.powerupKind ?: PowerupKind.MAGNET
                val bobY = entity.y + sin(timeSec * 6f) * 0.18f
                drawGlow3D(cam, entity.x, bobY, entity.z, 1.45f, kind.color)
                val p = cam.project(entity.x, bobY, entity.z)
                if (!p.visible) return
                val r = max(6f, 0.65f * p.scale)
                drawCircle(
                    color = kind.color,
                    radius = r,
                    center = Offset(p.x, p.y)
                )
                drawCircle(
                    color = Color.White,
                    radius = r * 1.18f,
                    center = Offset(p.x, p.y),
                    style = Stroke(width = max(2f, r * 0.18f))
                )
            }

            EntityType.HURDLE_LOW -> {
                when (world.roadStyle) {
                    RoadStyle.LAVA -> {
                        // Flaming Magma Hazard Crate
                        drawBox3D(
                            cam = cam,
                            cx = entity.x,
                            bottomY = 0f,
                            cz = entity.z,
                            width = 2.1f,
                            height = 1.25f,
                            depthZ = 1.1f,
                            baseColor = Color(0xFF7A3012),
                            fogColor = world.fogColor,
                            outlineColor = Color(0xFFFF7700)
                        )
                        drawGlow3D(cam, entity.x, 1.6f, entity.z, 1.35f, Color(0xFFFF5500))
                    }
                    RoadStyle.SNOW -> {
                        // Jagged Blue Ice Spike Cluster
                        for (spikeIdx in -1..1) {
                            val sx = entity.x + spikeIdx * 0.72f
                            val b = cam.project(sx, 0f, entity.z)
                            val t = cam.project(sx, 1.38f, entity.z)
                            if (b.visible && t.visible) {
                                val sw = max(3f, 0.45f * b.scale)
                                val sp = Path().apply {
                                    moveTo(b.x - sw, b.y)
                                    lineTo(t.x, t.y)
                                    lineTo(b.x + sw, b.y)
                                    close()
                                }
                                drawPath(sp, color = Color(0xFF38B6FF))
                                drawPath(sp, color = Color.White, style = Stroke(width = 1.5f))
                            }
                        }
                    }
                    RoadStyle.CYBER -> {
                        // Glowing Neon Laser Hurdle
                        drawGlow3D(cam, entity.x, 0.8f, entity.z, 1.6f, world.hurdlePrimary)
                        drawBox3D(
                            cam = cam,
                            cx = entity.x,
                            bottomY = 0.35f,
                            cz = entity.z,
                            width = 2.65f,
                            height = 0.78f,
                            depthZ = 0.25f,
                            baseColor = world.hurdlePrimary.copy(alpha = 0.55f),
                            fogColor = world.fogColor,
                            outlineColor = world.hurdlePrimary
                        )
                    }
                    RoadStyle.RUINS -> {
                        // Striped Hurdle with Posts
                        drawBox3D(
                            cam = cam,
                            cx = entity.x - 1.15f,
                            bottomY = 0f,
                            cz = entity.z,
                            width = 0.24f,
                            height = 1.25f,
                            depthZ = 0.24f,
                            baseColor = Color(0xFF423B32),
                            fogColor = world.fogColor
                        )
                        drawBox3D(
                            cam = cam,
                            cx = entity.x + 1.15f,
                            bottomY = 0f,
                            cz = entity.z,
                            width = 0.24f,
                            height = 1.25f,
                            depthZ = 0.24f,
                            baseColor = Color(0xFF423B32),
                            fogColor = world.fogColor
                        )
                        drawBox3D(
                            cam = cam,
                            cx = entity.x,
                            bottomY = 0.62f,
                            cz = entity.z,
                            width = 2.65f,
                            height = 0.55f,
                            depthZ = 0.28f,
                            baseColor = world.hurdlePrimary,
                            fogColor = world.fogColor,
                            outlineColor = world.hurdleSecondary
                        )
                    }
                }
            }

            EntityType.BARRIER_HIGH -> {
                // High Slide-Under Overhead Arch Barrier
                for (side in listOf(-1.28f, 1.28f)) {
                    drawBox3D(
                        cam = cam,
                        cx = entity.x + side,
                        bottomY = 0f,
                        cz = entity.z,
                        width = 0.32f,
                        height = 3.6f,
                        depthZ = 0.32f,
                        baseColor = Color(0xFF5C5245),
                        fogColor = world.fogColor
                    )
                }
                drawBox3D(
                    cam = cam,
                    cx = entity.x,
                    bottomY = 1.95f,
                    cz = entity.z,
                    width = 2.95f,
                    height = 1.55f,
                    depthZ = 0.45f,
                    baseColor = world.barrierPrimary,
                    fogColor = world.fogColor,
                    outlineColor = world.barrierAccent
                )
            }

            EntityType.GATE_BOOST, EntityType.GATE_HAZARD -> {
                val isBoost = entity.type == EntityType.GATE_BOOST
                val gateCol = if (isBoost) Color(0xFF32B820) else Color(0xFFD92638)
                drawBox3D(
                    cam = cam,
                    cx = entity.x,
                    bottomY = 0f,
                    cz = entity.z,
                    width = 2.75f,
                    height = 3.4f,
                    depthZ = 0.22f,
                    baseColor = gateCol.copy(alpha = 0.58f),
                    fogColor = world.fogColor,
                    outlineColor = Color(0xFFFFD43B)
                )
            }

            EntityType.DIMENSIONAL_PORTAL -> {
                // Full-road-width or Center Dimensional Rift Vortex Portal
                val portalY = 2.35f + sin(timeSec * 3.5f) * 0.14f
                drawGlow3D(cam, entity.x, portalY, entity.z, 3.8f, Color(0xFFB57CFF))
                drawGlow3D(cam, entity.x, portalY, entity.z, 2.4f, Color(0xFF00F0FF))
                // Left & Right Rift Pillars
                for (side in listOf(-2.25f, 2.25f)) {
                    drawBox3D(
                        cam = cam,
                        cx = entity.x + side,
                        bottomY = 0f,
                        cz = entity.z,
                        width = 0.48f,
                        height = 4.6f,
                        depthZ = 0.48f,
                        baseColor = Color(0xFF281B4D),
                        fogColor = world.fogColor,
                        outlineColor = Color(0xFF00F0FF)
                    )
                }
                val p = cam.project(entity.x, portalY, entity.z)
                if (p.visible) {
                    val rx = max(12f, 2.15f * p.scale)
                    val ry = max(16f, 2.55f * p.scale)
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFF00F0FF),
                                Color(0xFFB57CFF),
                                Color(0x33280B45),
                                Color.Transparent
                            ),
                            center = Offset(p.x, p.y),
                            radius = max(rx, ry)
                        ),
                        topLeft = Offset(p.x - rx, p.y - ry),
                        size = Size(rx * 2f, ry * 2f)
                    )
                    drawOval(
                        color = Color(0xFF64FFDA),
                        topLeft = Offset(p.x - rx * 0.85f, p.y - ry * 0.85f),
                        size = Size(rx * 1.7f, ry * 1.7f),
                        style = Stroke(width = max(2.5f, p.scale * 0.14f))
                    )
                }
            }
        }
    }

    fun DrawScope.renderAmbientParticles3D(
        cam: Camera3D,
        world: WorldTheme,
        particles: List<AmbientParticle>
    ) {
        for (pt in particles) {
            val p = cam.project(pt.x, pt.y, pt.z)
            if (!p.visible) continue
            val r = (pt.size * p.scale).coerceIn(1.5f, 8f)
            drawCircle(
                color = world.particleColor.copy(alpha = 0.78f),
                radius = r,
                center = Offset(p.x, p.y)
            )
        }
    }
}
