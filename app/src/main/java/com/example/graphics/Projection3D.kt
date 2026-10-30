package com.example.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class ProjectedPoint(
    val x: Float,
    val y: Float,
    val scale: Float,
    val depth: Float,
    val visible: Boolean
)

class Camera3D(
    val screenWidth: Float,
    val screenHeight: Float,
    val camX: Float = 0f,
    val camY: Float = 5.0f,
    val camZ: Float = 8.6f,
    val pitchRad: Float = 0.23f,
    val rollRad: Float = 0f,
    val fovMultiplier: Float = 1.0f,
    val shakeX: Float = 0f,
    val shakeY: Float = 0f,
    val focalLength: Float = min(screenWidth, screenHeight) * 0.92f * fovMultiplier,
    val horizonRatio: Float = 0.34f
) {
    private val cosPitch = cos(pitchRad)
    private val sinPitch = sin(pitchRad)
    private val cosRoll = cos(rollRad)
    private val sinRoll = sin(rollRad)
    val horizonY: Float = screenHeight * horizonRatio

    fun project(wx: Float, wy: Float, wz: Float): ProjectedPoint {
        val dx = wx - camX
        val dy = wy - camY
        val dz = camZ - wz // Positive when in front of camera

        // Rotate by camera pitch around X axis
        val ry = dy * cosPitch + dz * sinPitch
        val rz = dz * cosPitch - dy * sinPitch

        if (rz <= 0.25f) {
            return ProjectedPoint(0f, 0f, 0f, rz, false)
        }
        val scale = focalLength / rz
        val rawX = dx * scale
        val rawY = -ry * scale + (camY * cosPitch) * (focalLength / 18f)

        // Apply dynamic camera Dutch-angle roll on lane swipes + screen shake
        val rolledX = rawX * cosRoll - rawY * sinRoll
        val rolledY = rawX * sinRoll + rawY * cosRoll

        val sx = screenWidth * 0.5f + rolledX + shakeX
        val sy = horizonY + rolledY + shakeY
        return ProjectedPoint(sx, sy, scale, rz, true)
    }
}

fun shadeColor(color: Color, factor: Float): Color {
    return Color(
        red = (color.red * factor).coerceIn(0f, 1f),
        green = (color.green * factor).coerceIn(0f, 1f),
        blue = (color.blue * factor).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}

fun fogBlend(color: Color, fogColor: Color, depth: Float, fogNear: Float = 26f, fogFar: Float = 115f): Color {
    val t = ((depth - fogNear) / (fogFar - fogNear)).coerceIn(0f, 0.88f)
    return lerp(color, fogColor, t * t)
}

fun DrawScope.drawQuad3D(
    cam: Camera3D,
    x1: Float, y1: Float, z1: Float,
    x2: Float, y2: Float, z2: Float,
    x3: Float, y3: Float, z3: Float,
    x4: Float, y4: Float, z4: Float,
    fillColor: Color,
    strokeColor: Color? = null,
    strokeWidth: Float = 1.5f
) {
    val p1 = cam.project(x1, y1, z1)
    val p2 = cam.project(x2, y2, z2)
    val p3 = cam.project(x3, y3, z3)
    val p4 = cam.project(x4, y4, z4)
    if (!p1.visible || !p2.visible || !p3.visible || !p4.visible) return

    val path = Path().apply {
        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        lineTo(p4.x, p4.y)
        close()
    }
    drawPath(path, color = fillColor, style = Fill)
    if (strokeColor != null) {
        drawPath(path, color = strokeColor, style = Stroke(width = strokeWidth))
    }
}

/**
 * Renders a full 3D perspective box centered at (cx, bottomY..bottomY+height, cz)
 * with realistic directional shading on Top, Left/Right Side, and Front faces.
 */
fun DrawScope.drawBox3D(
    cam: Camera3D,
    cx: Float,
    bottomY: Float,
    cz: Float,
    width: Float,
    height: Float,
    depthZ: Float,
    baseColor: Color,
    fogColor: Color,
    outlineColor: Color? = null
) {
    val hw = width * 0.5f
    val hd = depthZ * 0.5f
    val topY = bottomY + height
    val zFront = cz + hd
    val zBack = cz - hd
    val xLeft = cx - hw
    val xRight = cx + hw

    val avgDepth = max(1f, cam.camZ - cz)
    val foggedBase = fogBlend(baseColor, fogColor, avgDepth)

    val topCol = shadeColor(foggedBase, 1.22f)
    val frontCol = foggedBase
    val sideCol = shadeColor(foggedBase, 0.72f)

    // Side face visible towards center of screen
    if (cx < cam.camX - 0.1f) {
        // Right face of box is visible
        drawQuad3D(
            cam,
            xRight, bottomY, zFront,
            xRight, bottomY, zBack,
            xRight, topY, zBack,
            xRight, topY, zFront,
            sideCol, outlineColor
        )
    } else if (cx > cam.camX + 0.1f) {
        // Left face of box is visible
        drawQuad3D(
            cam,
            xLeft, bottomY, zBack,
            xLeft, bottomY, zFront,
            xLeft, topY, zFront,
            xLeft, topY, zBack,
            sideCol, outlineColor
        )
    }

    // Top face (visible if below camera height)
    if (topY < cam.camY) {
        drawQuad3D(
            cam,
            xLeft, topY, zFront,
            xRight, topY, zFront,
            xRight, topY, zBack,
            xLeft, topY, zBack,
            topCol, outlineColor
        )
    }

    // Front face
    drawQuad3D(
        cam,
        xLeft, bottomY, zFront,
        xRight, bottomY, zFront,
        xRight, topY, zFront,
        xLeft, topY, zFront,
        frontCol, outlineColor
    )
}

/**
 * Draws a sculpted 3D tapered capsule/limb segment between two 3D points with
 * shadow edge, main diffuse body, and specular highlight ridge.
 */
fun DrawScope.drawSculptedLimb3D(
    cam: Camera3D,
    x1: Float, y1: Float, z1: Float,
    x2: Float, y2: Float, z2: Float,
    radius1: Float,
    radius2: Float,
    baseColor: Color,
    shadowColor: Color,
    highlightColor: Color
) {
    val p1 = cam.project(x1, y1, z1)
    val p2 = cam.project(x2, y2, z2)
    if (!p1.visible || !p2.visible) return

    val r1 = max(2f, radius1 * p1.scale)
    val r2 = max(2f, radius2 * p2.scale)

    val dx = p2.x - p1.x
    val dy = p2.y - p1.y
    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
    val nx = -dy / len
    val ny = dx / len

    // Outer shadow silhouette
    val path = Path().apply {
        moveTo(p1.x + nx * r1, p1.y + ny * r1)
        lineTo(p2.x + nx * r2, p2.y + ny * r2)
        lineTo(p2.x - nx * r2, p2.y - ny * r2)
        lineTo(p1.x - nx * r1, p1.y - ny * r1)
        close()
    }
    drawPath(path, color = shadowColor)
    drawCircle(color = shadowColor, radius = r1, center = Offset(p1.x, p1.y))
    drawCircle(color = shadowColor, radius = r2, center = Offset(p2.x, p2.y))

    // Main diffuse anatomical volume
    val innerR1 = r1 * 0.86f
    val innerR2 = r2 * 0.86f
    val innerPath = Path().apply {
        moveTo(p1.x + nx * innerR1, p1.y + ny * innerR1)
        lineTo(p2.x + nx * innerR2, p2.y + ny * innerR2)
        lineTo(p2.x - nx * innerR2, p2.y - ny * innerR2)
        lineTo(p1.x - nx * innerR1, p1.y - ny * innerR1)
        close()
    }
    drawPath(innerPath, color = baseColor)
    drawCircle(color = baseColor, radius = innerR1, center = Offset(p1.x, p1.y))
    drawCircle(color = baseColor, radius = innerR2, center = Offset(p2.x, p2.y))

    // Specular muscle/cloth highlight ridge shifted slightly toward light source
    val hlOffset1 = r1 * 0.22f
    val hlOffset2 = r2 * 0.22f
    drawLine(
        color = highlightColor.copy(alpha = 0.55f),
        start = Offset(p1.x - nx * hlOffset1, p1.y - ny * hlOffset1),
        end = Offset(p2.x - nx * hlOffset2, p2.y - ny * hlOffset2),
        strokeWidth = max(1.5f, (r1 + r2) * 0.24f),
        cap = StrokeCap.Round
    )
}

/**
 * Draws a radial volumetric glow / bloom halo at a 3D world position.
 */
fun DrawScope.drawGlow3D(
    cam: Camera3D,
    wx: Float,
    wy: Float,
    wz: Float,
    worldRadius: Float,
    glowColor: Color
) {
    val p = cam.project(wx, wy, wz)
    if (!p.visible) return
    val r = (worldRadius * p.scale).coerceIn(2f, size.width * 0.6f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                glowColor.copy(alpha = 0.78f),
                glowColor.copy(alpha = 0.28f),
                Color.Transparent
            ),
            center = Offset(p.x, p.y),
            radius = r
        ),
        radius = r,
        center = Offset(p.x, p.y)
    )
}
