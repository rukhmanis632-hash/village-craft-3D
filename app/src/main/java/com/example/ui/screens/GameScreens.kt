package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ModalType
import com.example.model.WORLD_THEMES
import com.example.ui.components.CoinBadge
import com.example.ui.components.CurrencyPill
import com.example.ui.components.GoldLight
import com.example.ui.components.GoldMid
import com.example.ui.components.GreenBevelButton
import com.example.ui.components.HorizonDashTitlePlaque
import com.example.ui.components.RubyBadge
import com.example.ui.components.StoneGoldBilingualButton
import com.example.ui.components.WorldSelectionBannerCard
import com.example.viewmodel.GameViewModel

@Composable
fun MainMenuOverlay(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0x280F1C14),
                        Color(0xA608100C)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 430.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Currency Bar (Coins, Rubies, Daily Chest)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurrencyPill(
                    valueText = "%,d".format(viewModel.coins),
                    isRuby = false,
                    modifier = Modifier.testTag("menu_coins_pill")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CurrencyPill(
                        valueText = "%,d".format(viewModel.rubies),
                        isRuby = true,
                        modifier = Modifier.testTag("menu_rubies_pill")
                    )
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFFB86F28), Color(0xFF6E3B0C))
                                )
                            )
                            .border(2.dp, Color(0xFFFFD15C), RoundedCornerShape(10.dp))
                            .clickable { viewModel.claimDailyChest() }
                            .testTag("claim_chest_btn")
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "🧰", fontSize = 16.sp)
                            Text(
                                text = "🔥${viewModel.dailyStreakDays}d",
                                color = Color(0xFFFFE266),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. HORIZON DASH Title Plaque + Quick World Switcher
            HorizonDashTitlePlaque(
                activeWorldName = viewModel.currentWorld.name,
                selectedWorldIndex = viewModel.selectedWorldIndex,
                onWorldBadgeClick = { viewModel.navigateToWorldsScreen() },
                onQuickWorldSelect = { viewModel.selectWorld(it) },
                onInspect3DClick = { viewModel.openModal(ModalType.INSPECT_3D) }
            )

            // Unobstructed viewport window to admire the live 3D Runner & World
            Spacer(modifier = Modifier.height(48.dp))

            // 2B. Subtle Main Menu "STORY / LORE: THE CHRONO CORE" Card
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .shadow(8.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xE6182924), Color(0xE6281A3D))
                        )
                    )
                    .border(2.dp, Color(0xFF64FFDA), RoundedCornerShape(14.dp))
                    .clickable { viewModel.openModal(ModalType.STORY_LORE) }
                    .testTag("open_story_lore_btn")
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x4400F0FF))
                            .border(1.5.dp, Color(0xFF64FFDA), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "💎", fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            text = "📜 STORY & 3D HEROES: ${viewModel.currentCharacter.name.uppercase()}",
                            color = Color(0xFF64FFDA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${viewModel.currentCharacter.badgeEmoji} ${viewModel.currentCharacter.signatureGear}",
                            color = Color(0xFFEAEAEA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "VIEW ▶",
                    color = Color(0xFFFFD43B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Giant Green Beveled PLAY Button
            GreenBevelButton(
                text = "PLAY",
                iconText = "▶",
                testTag = "start_btn",
                onClick = { viewModel.startGame() },
                modifier = Modifier.fillMaxWidth(0.84f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. 2x2 Stone & Gold Navigation Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StoneGoldBilingualButton(
                        enLabel = "UPGRADES",
                        hiLabel = "अपग्रेड",
                        testTag = "open_upgrades_btn",
                        onClick = { viewModel.openModal(ModalType.UPGRADES) },
                        modifier = Modifier.weight(1f)
                    )
                    StoneGoldBilingualButton(
                        enLabel = "CHARACTERS",
                        hiLabel = "पात्र",
                        testTag = "open_characters_btn",
                        onClick = { viewModel.openModal(ModalType.CHARACTERS) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StoneGoldBilingualButton(
                        enLabel = "WORLDS & BOARDS",
                        hiLabel = "दुनिया और चुनौतियाँ",
                        testTag = "open_worlds_btn",
                        onClick = { viewModel.navigateToWorldsScreen() },
                        modifier = Modifier.weight(1f)
                    )
                    StoneGoldBilingualButton(
                        enLabel = "LEADERBOARD",
                        hiLabel = "लीडरबोर्ड",
                        testTag = "open_leaderboard_btn",
                        onClick = { viewModel.openModal(ModalType.LEADERBOARD) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Bottom Icon Bar (Settings, Story/Lore, Hoverboards, Shop)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomCircleIconAction(
                    emoji = "⚙️",
                    enSub = "SETTINGS",
                    hiSub = "सेटिंग्स",
                    testTag = "open_settings_btn",
                    onClick = { viewModel.openModal(ModalType.SETTINGS) }
                )
                BottomCircleIconAction(
                    emoji = "📜",
                    enSub = "STORY",
                    hiSub = "कहानी",
                    testTag = "open_story_circle_btn",
                    onClick = { viewModel.openModal(ModalType.STORY_LORE) }
                )
                BottomCircleIconAction(
                    emoji = "🛹",
                    enSub = "BOARDS",
                    hiSub = "बोर्ड",
                    testTag = "open_boards_btn",
                    onClick = { viewModel.openModal(ModalType.BOARDS) }
                )
                BottomCircleIconAction(
                    emoji = "🛒",
                    enSub = "SHOP",
                    hiSub = "शॉप",
                    testTag = "open_shop_btn",
                    onClick = { viewModel.openModal(ModalType.SHOP) }
                )
            }
        }
    }
}

@Composable
private fun BottomCircleIconAction(
    emoji: String,
    enSub: String,
    hiSub: String,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF524B42), Color(0xFF2B2722))
                    )
                )
                .border(2.8.dp, Color(0xFFD49B35), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = enSub,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = hiSub,
            color = Color(0xFFFFD269),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun InGameHudOverlay(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val activePowerupTitle = when {
        viewModel.boostTimer > 0f -> "SPEED BOOST"
        viewModel.magnetTimer > 0f -> "MAGNET"
        viewModel.hasShield -> "SHIELD ACTIVE"
        else -> null
    }
    val activePowerupEmoji = when {
        viewModel.boostTimer > 0f -> "🥾"
        viewModel.magnetTimer > 0f -> "🧲"
        viewModel.hasShield -> "🛡️"
        else -> "🧲"
    }
    val activePowerupRatio = when {
        viewModel.boostTimer > 0f -> (viewModel.boostTimer / viewModel.boostMax).coerceIn(0f, 1f)
        viewModel.magnetTimer > 0f -> (viewModel.magnetTimer / viewModel.magnetMax).coerceIn(0f, 1f)
        viewModel.hasShield -> 1f
        else -> 0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 1. Top HUD Bar (Score, Multiplier & Aura on Left; Distance, Coins & Rubies on Right)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 560.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Score & Multiplier
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "%,d".format(viewModel.score),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.testTag("hud_score"),
                        style = TextStyle(
                            shadow = Shadow(color = Color.Black, offset = Offset(0f, 3f), blurRadius = 4f)
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val effectiveMult = viewModel.multiplierLevel * viewModel.flowMultiplier
                        Text(
                            text = "x$effectiveMult",
                            color = if (viewModel.isHyperRun) Color(0xFFFF2E93) else Color(0xFFFFCC29),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black,
                            style = TextStyle(
                                shadow = Shadow(color = Color.Black, offset = Offset(0f, 2f), blurRadius = 2f)
                            )
                        )
                        // Arcade Flow & Combo Multiplier Bar (1x -> 2x -> 4x -> HYPER RUN)
                        Column {
                            Box(
                                modifier = Modifier
                                    .width(108.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x99000000))
                                    .border(
                                        1.5.dp,
                                        if (viewModel.isHyperRun) Color(0xFFFF2E93) else Color.White,
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth((viewModel.flowMeter / 100f).coerceIn(0.06f, 1f))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFF00E5FF),
                                                    Color(0xFF7B2CBF),
                                                    Color(0xFFFF2E93),
                                                    Color(0xFFFFCC00)
                                                )
                                            )
                                        )
                                )
                            }
                            Text(
                                text = viewModel.flowTierLabel,
                                color = if (viewModel.isHyperRun) Color(0xFFFF66B2) else Color(0xFF80F8FF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    if (viewModel.comboChain > 0 || viewModel.nearMissCount > 0) {
                        Text(
                            text = "⚡ ${viewModel.comboChain} COMBO • ${viewModel.nearMissCount} NEAR-MISS",
                            color = Color(0xFFFFD54F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Distance, Coins & Rubies
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "%,dm".format(viewModel.distanceMeters.toInt()),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.testTag("hud_distance"),
                        style = TextStyle(
                            shadow = Shadow(color = Color.Black, offset = Offset(0f, 3f), blurRadius = 4f)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "%,d".format(viewModel.runCoins),
                            color = Color(0xFFFFE266),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            style = TextStyle(
                                shadow = Shadow(color = Color.Black, offset = Offset(0f, 2f), blurRadius = 2f)
                            )
                        )
                        CoinBadge(20.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%,d".format(viewModel.runRubies),
                            color = Color(0xFFFF8FA3),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            style = TextStyle(
                                shadow = Shadow(color = Color.Black, offset = Offset(0f, 2f), blurRadius = 2f)
                            )
                        )
                        RubyBadge(19.dp)
                    }
                }
            }

            // Active Powerup Meter (Matching Screen 2 reference image)
            if (activePowerupTitle != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = 290.dp)
                        .fillMaxWidth(0.82f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = activePowerupTitle,
                        color = Color(0xFFFFE266),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        style = TextStyle(
                            shadow = Shadow(color = Color.Black, offset = Offset(0f, 2f), blurRadius = 2f)
                        )
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Progress track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 26.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                                .background(Color(0xD9141A26))
                                .border(
                                    2.dp,
                                    Color(0xFFE6A832),
                                    RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(activePowerupRatio)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFFFF7B00),
                                                Color(0xFFFFAE00),
                                                Color(0xFF249CFF)
                                            )
                                        )
                                    )
                            )
                        }
                        // Left circular powerup icon badge
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(6.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF5B6975), Color(0xFF252C33))
                                    )
                                )
                                .border(2.8.dp, Color(0xFFE6A832), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = activePowerupEmoji, fontSize = 21.sp)
                        }
                    }
                }
            }

            // 30-Second Flash Micro-Mission HUD Banner
            val flashMission = viewModel.activeFlashMission
            if (flashMission != null && !viewModel.isIntroCutsceneActive) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = 330.dp)
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCC161126))
                        .border(1.5.dp, Color(0xFF64FFDA), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("hud_flash_mission_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎯 ${flashMission.title}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${flashMission.progress}/${flashMission.target} • ${flashMission.timeRemainingSec.toInt()}s",
                            color = Color(0xFF64FFDA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x66000000))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((flashMission.timeRemainingSec / flashMission.maxTimeSec).coerceIn(0.03f, 1f))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFFF6EC7), Color(0xFF64FFDA))
                                    )
                                )
                        )
                    }
                }
            }

            // In-Game Voice Lines / Radio Comms Transmission Banner
            val radio = viewModel.activeRadioTransmission
            if (radio != null && !viewModel.isIntroCutsceneActive) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = 350.dp)
                        .fillMaxWidth(0.95f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xE60B131E))
                        .border(1.5.dp, radio.accentColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("hud_radio_comms_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📻 ${radio.speaker}",
                            color = radio.accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = radio.callSign,
                            color = Color(0xFF99AABB),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = radio.message,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = radio.hiMessage,
                        color = Color(0xFFFFD269),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 1B. 3-Second Intro Cutscene Cinematic Letterbox & Tap-to-Skip Overlay (IntroCinematic)
        if (viewModel.isIntroCutsceneActive) {
            val elapsed = viewModel.introElapsedSec
            val phaseHeadline = when {
                elapsed < 0.95f -> "💎 PHASE 1: GRABBING THE CHRONO CORE"
                elapsed < 2.0f -> "⚡ PHASE 2: ${viewModel.currentCharacter.name.uppercase()} CHARGED!"
                else -> "🎥 PHASE 3: LOCKING 3RD-PERSON FOLLOW-CAM..."
            }
            val phaseSub = when {
                elapsed < 0.95f -> "${viewModel.currentCharacter.name} lifts the glowing Chrono Core from the ancient stone pedestal..."
                elapsed < 2.0f -> "Dimensional energy surges through ${viewModel.currentCharacter.signatureGear}!"
                else -> "Speed & Flow systems online — Let's run!"
            }
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 370.dp)
                    .fillMaxWidth(0.94f)
                    .shadow(14.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xE6101B24), Color(0xE61D1128))
                        )
                    )
                    .border(2.2.dp, Color(0xFF64FFDA), RoundedCornerShape(18.dp))
                    .clickable { viewModel.skipIntroCutscene() }
                    .padding(14.dp)
                    .testTag("hud_intro_cutscene_banner"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎬 INTRO CUTSCENE • ${"%.1f".format(viewModel.introCutsceneTimerSec)}s",
                    color = Color(0xFFFFD43B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = phaseHeadline,
                    color = Color(0xFF64FFDA),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = phaseSub,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF2B193E))
                        .border(1.5.dp, Color(0xFFFF6EC7), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                        .testTag("skip_intro_btn")
                ) {
                    Text(
                        text = "⏭ TAP ANYWHERE TO SKIP TO RUN",
                        color = Color(0xFFFF6EC7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // 2. Bilingual Swipe Prompts (SWIPE UP / ऊपर स्वाइप करें, SWIPE DOWN / नीचे स्वाइप करें)
        if (!viewModel.isIntroCutsceneActive && viewModel.showSwipeHints && viewModel.distanceMeters < 180f) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight(0.54f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { viewModel.triggerJump() }
                        .padding(8.dp)
                ) {
                    Text(
                        text = "▲",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "SWIPE UP",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(
                            shadow = Shadow(color = Color.Black, offset = Offset(0f, 3f), blurRadius = 4f)
                        )
                    )
                    Text(
                        text = "ऊपर स्वाइप करें",
                        color = Color(0xFFFFE57F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            shadow = Shadow(color = Color.Black, offset = Offset(0f, 2f), blurRadius = 2f)
                        )
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { viewModel.triggerSlide() }
                        .padding(8.dp)
                ) {
                    Text(
                        text = "SWIPE DOWN",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(
                            shadow = Shadow(color = Color.Black, offset = Offset(0f, 3f), blurRadius = 4f)
                        )
                    )
                    Text(
                        text = "नीचे स्वाइप करें",
                        color = Color(0xFFFFE57F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            shadow = Shadow(color = Color.Black, offset = Offset(0f, 2f), blurRadius = 2f)
                        )
                    )
                    Text(
                        text = "▼",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // 3. Floating Combat & Pickup Popups
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            viewModel.floatingPopups.forEach { pop ->
                val col = when {
                    pop.isCoin -> Color(0xFFFFE266)
                    pop.isPositive -> Color(0xFF5CE63C)
                    else -> Color(0xFFFF4757)
                }
                Text(
                    text = pop.text,
                    color = col,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(
                        shadow = Shadow(color = Color.Black, offset = Offset(0f, 3f), blurRadius = 6f)
                    )
                )
            }
        }

        // 4. Bottom Action Controls + Gold Pause Button
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HudControlChip("◀", "btn_left") { viewModel.moveLane(-1) }
                HudControlChip("▲", "btn_jump") { viewModel.triggerJump() }
                HudControlChip("▼", "btn_slide") { viewModel.triggerSlide() }
                HudControlChip("▶", "btn_right") { viewModel.moveLane(1) }
                HudControlChip("⚡", "btn_boost") { viewModel.triggerManualBoost() }
                HudControlChip("🌀 RIFT", "btn_portal") { viewModel.triggerDimensionalPortalNow() }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFF5B625), Color(0xFFA86808))
                        )
                    )
                    .border(2.8.dp, Color(0xFFFFE885), RoundedCornerShape(12.dp))
                    .clickable { viewModel.togglePause() }
                    .testTag("pause_btn"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "||",
                    color = Color(0xFF2B1700),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun HudControlChip(
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC231E19))
            .border(2.dp, Color(0xFFD49B35), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun WorldsAndChallengesOverlay(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0x660F1C14), Color(0xD908100C))
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 430.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StoneGoldBilingualButton(
                    enLabel = "◀ BACK",
                    testTag = "worlds_back_btn",
                    onClick = { viewModel.navigateToMainMenu() },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                )
                Text(
                    text = "SELECT WORLD & JOURNEY",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(color = Color.Black, offset = Offset(0f, 2f), blurRadius = 3f)
                    )
                )
                GreenBevelButton(
                    text = "PLAY",
                    iconText = "▶",
                    testTag = "worlds_play_now_btn",
                    onClick = { viewModel.startGame() },
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 World Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WORLD_THEMES.forEachIndexed { idx, w ->
                    WorldSelectionBannerCard(
                        world = w,
                        isSelected = viewModel.selectedWorldIndex == idx,
                        bestScore = viewModel.highScores[idx],
                        onClick = { viewModel.selectWorld(idx) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Daily Challenges Carved Stone Tablet
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF595249), Color(0xFF3D3731))
                        )
                    )
                    .border(3.dp, Color(0xFF9E8C76), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DAILY CHALLENGES",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "रोजाना की चुनौतियां",
                        color = Color(0xFFFFD269),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Challenge 1: Run 2,000 meters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Run 2,000 meters",
                        color = Color(0xFFF0ECE1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "%,d / 2,000m".format(viewModel.challengeDist),
                        color = Color(0xFFFFD43B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(13.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF221E1A))
                        .border(1.5.dp, Color(0xFF8C7853), RoundedCornerShape(8.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((viewModel.challengeDist / 2000f).coerceIn(0.05f, 1f))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFF5A000), Color(0xFFFFE047))
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Challenge 2: Collect 5 Red Gems
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Collect 5 Red Gems",
                        color = Color(0xFFF0ECE1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${viewModel.challengeGems} / 5",
                        color = Color(0xFFFF8FA3),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(13.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF221E1A))
                        .border(1.5.dp, Color(0xFF8C7853), RoundedCornerShape(8.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((viewModel.challengeGems / 5f).coerceIn(0.12f, 1f))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFB8142C), Color(0xFFFF4760))
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0x661C1228), Color(0xE608060E))
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Header + Zero-Wait Instant Respawn Pill
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (viewModel.isNewPersonalBest) "🏆 NEW WORLD RECORD!" else "RUN COMPLETE",
                    color = Color(0xFF64FFDA),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.8.sp
                )
                Text(
                    text = "GAME OVER",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.testTag("game_over_header"),
                    style = TextStyle(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFF394),
                                Color(0xFFFFBB1C),
                                Color(0xFFCF6A00)
                            )
                        ),
                        shadow = Shadow(color = Color(0xFF3B1B00), offset = Offset(0f, 4f), blurRadius = 6f)
                    )
                )
            }

            // 2. "Close-Call" Competitive Hook Banner
            if (viewModel.closeCallHeadline.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = if (viewModel.isNewPersonalBest) {
                                    listOf(Color(0xFF143D34), Color(0xFF1E2246))
                                } else {
                                    listOf(Color(0xFF42122B), Color(0xFF2B143D))
                                }
                            )
                        )
                        .border(
                            2.dp,
                            if (viewModel.isNewPersonalBest) Color(0xFF64FFDA) else Color(0xFFFF6EC7),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("close_call_banner"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = viewModel.closeCallHeadline,
                        color = if (viewModel.isNewPersonalBest) Color(0xFF64FFDA) else Color(0xFFFF6EC7),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = viewModel.closeCallSubtext,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 3. Gen-Z Social Story Viral Scorecard
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(14.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF261C3D),
                                Color(0xFF191528),
                                Color(0xFF120F1D)
                            )
                        )
                    )
                    .border(
                        2.8.dp,
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF64FFDA), Color(0xFFB57CFF), Color(0xFFFF6EC7))
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
                    .testTag("viral_story_scorecard"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Story Header: Player Handle & Rank Tier Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "@${viewModel.userDisplayName.replace(" ", "").lowercase()}",
                            color = Color(0xFF64FFDA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${viewModel.currentCharacter.name} • ${viewModel.currentWorld.shortName}",
                            color = Color(0xFFCCCCCC),
                            fontSize = 11.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFB57CFF), Color(0xFFFF6EC7))
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = viewModel.runRankTier,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Giant Score & Distance Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "FINAL SCORE",
                            color = Color(0xFFAAAAAA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "%,d".format(viewModel.score),
                            color = Color(0xFFFFE266),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DISTANCE",
                            color = Color(0xFFAAAAAA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "%,dm".format(viewModel.distanceMeters.toInt()),
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // 2x2 Viral Flex Badges (Max Combo, Near-Miss Dodges, Flash Missions, Loot)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ViralStatChip(
                        label = "MAX COMBO",
                        value = "${viewModel.maxComboChain}x 🔥",
                        accent = Color(0xFFFF6EC7),
                        modifier = Modifier.weight(1f)
                    )
                    ViralStatChip(
                        label = "NEAR-MISSES",
                        value = "${viewModel.nearMissCount} ⚡",
                        accent = Color(0xFF64FFDA),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ViralStatChip(
                        label = "FLASH CLEARS",
                        value = "${viewModel.flashMissionsCompleted} 🎯",
                        accent = Color(0xFFB57CFF),
                        modifier = Modifier.weight(1f)
                    )
                    ViralStatChip(
                        label = "LOOT HAUL",
                        value = "${viewModel.runCoins}★ ${viewModel.runRubies}♦",
                        accent = Color(0xFFFFE266),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Share Story Card Action
                StoneGoldBilingualButton(
                    enLabel = "📸 SHARE VIRAL STORY CARD",
                    hiLabel = "स्कोरकार्ड शेयर करें",
                    testTag = "share_story_btn",
                    isHighlighted = true,
                    onClick = { viewModel.shareViralScorecard(context) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                )
            }

            // 4. Item Unlocked / Free Headstart Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF3C352D), Color(0xFF24201B))
                        )
                    )
                    .border(2.5.dp, Color(0xFFE0A32E), RoundedCornerShape(14.dp))
                    .clickable { viewModel.equipFreeStartBoost() }
                    .testTag("equip_unlock_card")
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0x59FFD250), Color(0x66000000))
                            )
                        )
                        .border(2.dp, Color(0xFFFFCC4D), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🥾", fontSize = 24.sp)
                }
                Column {
                    Text(
                        text = "HEADSTART BOOST UNLOCKED!",
                        color = Color(0xFFFFD43B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (viewModel.freeStartBoost) {
                            "Speed Boost EQUIPPED for next run! ⚡"
                        } else {
                            "Tap to equip free Speed Boost for next run"
                        },
                        color = Color(0xFFEAEAEA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 5. Zero-Wait Instant Respawn & Main Menu Buttons
            GreenBevelButton(
                text = "⚡ INSTANT RETRY",
                iconText = "↻",
                testTag = "restart_btn",
                onClick = { viewModel.startGame() },
                modifier = Modifier.fillMaxWidth(0.92f)
            )

            StoneGoldBilingualButton(
                enLabel = "MAIN MENU",
                hiLabel = "मुख्य मेन्यू",
                testTag = "gameover_menu_btn",
                onClick = { viewModel.navigateToMainMenu() },
                modifier = Modifier.fillMaxWidth(0.62f)
            )
        }
    }
}

@Composable
private fun ViralStatChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x66000000))
            .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color(0xFFBBBBBB),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black
        )
    }
}

