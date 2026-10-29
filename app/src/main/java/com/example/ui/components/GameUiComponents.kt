package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RoadStyle
import com.example.model.WorldTheme

val GoldLight = Color(0xFFFFE875)
val GoldMid = Color(0xFFF5B625)
val GoldDark = Color(0xFF9C6208)
val StoneLight = Color(0xFF6E675F)
val StoneMid = Color(0xFF4A453F)
val StoneDark = Color(0xFF2C2824)
val GreenTop = Color(0xFF5CE63C)
val GreenMid = Color(0xFF28B818)
val GreenDark = Color(0xFF106E06)

@Composable
fun CoinBadge(size: Dp = 26.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF38F), Color(0xFFF5B000), Color(0xFFB36B00))
                )
            )
            .border(1.8.dp, Color(0xFFFFF099), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "★",
            color = Color(0xFF7A4100),
            fontSize = (size.value * 0.52f).sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun RubyBadge(size: Dp = 24.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFF9EB5), Color(0xFFE6193C), Color(0xFF7D0017))
                )
            )
            .border(1.8.dp, Color(0xFFFFBFD0), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "♦",
            color = Color.White,
            fontSize = (size.value * 0.52f).sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun CurrencyPill(
    valueText: String,
    isRuby: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF3B3127), Color(0xFF1F1914))
                )
            )
            .border(2.2.dp, Color(0xFFD49B35), RoundedCornerShape(30.dp))
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (isRuby) {
            RubyBadge(24.dp)
        } else {
            CoinBadge(24.dp)
        }
        Text(
            text = valueText,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            style = TextStyle(
                shadow = Shadow(color = Color.Black, offset = Offset(1f, 2f), blurRadius = 2f)
            )
        )
    }
}

@Composable
fun GreenBevelButton(
    text: String,
    iconText: String = "▶",
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 26.sp
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .shadow(10.dp, RoundedCornerShape(40.dp))
            .clip(RoundedCornerShape(40.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(GreenTop, GreenMid, GreenDark)
                )
            )
            .border(3.5.dp, Color(0xFFE5B142), RoundedCornerShape(40.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 26.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                style = TextStyle(
                    shadow = Shadow(color = Color(0xFF0D5205), offset = Offset(0f, 4f), blurRadius = 2f)
                )
            )
            Text(
                text = iconText,
                color = Color.White,
                fontSize = (fontSize.value * 0.78f).sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(
                    shadow = Shadow(color = Color(0xFF0D5205), offset = Offset(0f, 3f), blurRadius = 2f)
                )
            )
        }
    }
}

@Composable
fun StoneGoldBilingualButton(
    enLabel: String,
    hiLabel: String? = null,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
) {
    val borderColor = if (isHighlighted) GreenTop else Color(0xFFD49B35)
    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF635C54), Color(0xFF453F39), Color(0xFF2F2B27))
                )
            )
            .border(2.8.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = enLabel,
            color = Color(0xFFFFF7DF),
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(color = Color.Black, offset = Offset(0f, 2f), blurRadius = 2f)
            )
        )
        if (!hiLabel.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = hiLabel,
                color = Color(0xFFFFD269),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HorizonDashTitlePlaque(
    activeWorldName: String,
    selectedWorldIndex: Int,
    onWorldBadgeClick: () -> Unit,
    onQuickWorldSelect: (Int) -> Unit,
    onInspect3DClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF5D564E), Color(0xFF3D3832), Color(0xFF292521))
                )
            )
            .border(3.5.dp, Color(0xFFB58536), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🌿", fontSize = 18.sp)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1F1B17))
                    .border(1.5.dp, GoldMid, RoundedCornerShape(12.dp))
                    .clickable(onClick = onInspect3DClick)
                    .testTag("inspect_3d_btn")
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "🎥 3D SHOWCASE",
                    color = GoldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(text = "🌿", fontSize = 18.sp)
        }

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

        Spacer(modifier = Modifier.height(6.dp))

        // Active World Badge Button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF4F4840), Color(0xFF2B2621))
                    )
                )
                .border(2.dp, Color(0xFFD49B35), RoundedCornerShape(8.dp))
                .clickable(onClick = onWorldBadgeClick)
                .testTag("logo_world_btn")
                .padding(horizontal = 14.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = activeWorldName,
                color = Color(0xFFFFE28A),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Text(text = "▾", color = Color(0xFFFFE28A), fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4-World Quick Switcher Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            val chips = listOf(
                0 to "🏛️ Ruins",
                1 to "🌆 Cyber",
                2 to "🌋 Lava",
                3 to "🏔️ Snow"
            )
            chips.forEach { (idx, label) ->
                val isSelected = selectedWorldIndex == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isSelected) {
                                Brush.verticalGradient(listOf(Color(0xFF32B820), Color(0xFF16700B)))
                            } else {
                                Brush.verticalGradient(listOf(Color(0xDD191612), Color(0xDD12100D)))
                            }
                        )
                        .border(
                            1.8.dp,
                            if (isSelected) Color(0xFFFFE270) else Color(0x99D49B35),
                            RoundedCornerShape(18.dp)
                        )
                        .clickable { onQuickWorldSelect(idx) }
                        .testTag("chip_w$idx")
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Rich custom-painted World Banner Card for Screen 3 (SELECT WORLD & JOURNEY).
 */
@Composable
fun WorldSelectionBannerCard(
    world: WorldTheme,
    isSelected: Boolean,
    bestScore: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderCol = if (isSelected) Color(0xFF57E838) else Color(0xFF8A929E)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(94.dp)
            .shadow(8.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .border(3.dp, borderCol, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("world_card_${world.id}")
    ) {
        // Painted Scenic Background Canvas per World
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(world.skyTop, world.skyMid, world.groundOuter)
                )
            )
            when (world.roadStyle) {
                RoadStyle.RUINS -> {
                    // Temple silhouette & sunbeams
                    drawCircle(color = Color(0x55FFE875), radius = size.height * 0.8f, center = Offset(size.width * 0.78f, size.height * 0.2f))
                    drawRect(color = Color(0xFF3A5F32), topLeft = Offset(0f, size.height * 0.65f), size = Size(size.width, size.height * 0.35f))
                }
                RoadStyle.CYBER -> {
                    // Neon skyline bars
                    for (i in 0..8) {
                        val bx = size.width * (0.08f + i * 0.11f)
                        val bh = size.height * (0.4f + (i % 3) * 0.2f)
                        drawRect(color = Color(0xFF151D42), topLeft = Offset(bx, size.height - bh), size = Size(size.width * 0.08f, bh))
                    }
                }
                RoadStyle.LAVA -> {
                    // Lava waterfall & magma glow
                    drawRect(
                        brush = Brush.verticalGradient(listOf(Color(0xFFFFAA00), Color(0xFFFF3300))),
                        topLeft = Offset(size.width * 0.72f, 0f),
                        size = Size(size.width * 0.12f, size.height)
                    )
                }
                RoadStyle.SNOW -> {
                    // Alpine snow peak
                    val p = Path().apply {
                        moveTo(size.width * 0.45f, size.height)
                        lineTo(size.width * 0.70f, size.height * 0.15f)
                        lineTo(size.width * 0.95f, size.height)
                        close()
                    }
                    drawPath(p, color = Color(0xFFE6F4FF))
                }
            }
            // Dark readable overlay gradient
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xBB000000),
                        Color(0x44000000),
                        Color(0xAA000000)
                    )
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = world.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(
                        shadow = Shadow(color = Color.Black, offset = Offset(0f, 2f), blurRadius = 4f)
                    )
                )
                Text(
                    text = world.hiName,
                    color = Color(0xFFFFE270),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Best: %,d".format(bestScore),
                    color = Color(0xFFE5E5E5),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) {
                                Brush.verticalGradient(listOf(Color(0xFF4AE02B), Color(0xFF1D8A09)))
                            } else {
                                Brush.verticalGradient(listOf(Color(0xFFF5B625), Color(0xFFB36B00)))
                            }
                        )
                        .border(
                            1.5.dp,
                            if (isSelected) Color(0xFFB8FF9E) else Color(0xFFFFE885),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isSelected) "ACTIVE" else "SELECT WORLD",
                        color = if (isSelected) Color.White else Color(0xFF241300),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
