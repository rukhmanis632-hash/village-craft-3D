package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.signOutUser
import com.example.model.CHRONO_LORE_PANELS
import com.example.model.HOVERBOARDS
import com.example.model.ModalType
import com.example.model.RUNNER_CHARACTERS
import com.example.model.WORLD_THEMES
import com.example.ui.components.FirebaseDiagnosticModal
import com.example.ui.components.GoldLight
import com.example.ui.components.GoldMid
import com.example.ui.components.GreenBevelButton
import com.example.ui.components.StoneGoldBilingualButton
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.UiState
import kotlin.math.PI
import kotlin.math.roundToInt

@Composable
fun GameModalOverlay(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val modal = viewModel.activeModal
    if (modal == ModalType.NONE) return

    if (modal == ModalType.INSPECT_3D) {
        Inspect3DShowcaseOverlay(viewModel = viewModel, modifier = modifier)
        return
    }

    val title = when (modal) {
        ModalType.UPGRADES -> "UPGRADES / अपग्रेड"
        ModalType.CHARACTERS -> "3D CHARACTERS / पात्र"
        ModalType.BOARDS -> "HOVERBOARDS / बोर्ड"
        ModalType.LEADERBOARD -> "LEADERBOARD / लीडरबोर्ड"
        ModalType.SETTINGS -> "SETTINGS & AUDIO / सेटिंग्स"
        ModalType.SHOP -> "SHOP / शॉप"
        ModalType.PAUSE -> "GAME PAUSED / खेल रुका है"
        ModalType.STORY_LORE -> "STORY & 3D HEROES / कहानी"
        else -> ""
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xC8000000))
            .clickable(enabled = true, onClick = {})
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 430.dp)
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF4F4840), Color(0xFF2D2924))
                    )
                )
                .border(3.5.dp, Color(0xFFD49B35), RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color(0xFFFFE270),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB82828))
                        .border(2.dp, Color(0xFFFF9494), CircleShape)
                        .clickable { viewModel.closeModal() }
                        .testTag("modal_close_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(Color(0x33FFFFFF))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (modal) {
                    ModalType.UPGRADES -> UpgradesModalBody(viewModel)
                    ModalType.CHARACTERS -> CharactersModalBody(viewModel)
                    ModalType.BOARDS -> BoardsModalBody(viewModel)
                    ModalType.LEADERBOARD -> LeaderboardModalBody(viewModel)
                    ModalType.SETTINGS -> SettingsModalBody(viewModel)
                    ModalType.SHOP -> ShopModalBody(viewModel)
                    ModalType.PAUSE -> PauseModalBody(viewModel)
                    ModalType.STORY_LORE -> StoryLoreModalBody(viewModel)
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun ModalRowCard(
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x59000000))
            .border(1.5.dp, Color(0x73D49B35), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun UpgradesModalBody(viewModel: GameViewModel) {
    val items = listOf(
        Triple("magnet", "🧲 Magnet Duration (Lv.${viewModel.magnetLevel})", "Pulls coins & rubies from all lanes" to "1,500 ★"),
        Triple("shield", "🛡️ Shield Mastery (Lv.${viewModel.shieldLevel})", "Protects against obstacle crashes" to "1,500 ★"),
        Triple("boost", "🥾 Speed Boost Boot (Lv.${viewModel.boostLevel})", "Unstoppable hover-dash duration" to "2,000 ★"),
        Triple("mult", "✖️ Score Multiplier (x${viewModel.multiplierLevel})", "Increases all run points earned" to "2,500 ★")
    )
    items.forEach { (key, title, descAndCost) ->
        ModalRowCard {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(text = title, color = GoldLight, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Text(text = descAndCost.first, color = Color(0xFFCCCCCC), fontSize = 12.sp)
            }
            StoneGoldBilingualButton(
                enLabel = descAndCost.second,
                testTag = "upg_$key",
                onClick = { viewModel.upgradeStat(key) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun CharactersModalBody(viewModel: GameViewModel) {
    RUNNER_CHARACTERS.forEach { char ->
        val isSelected = viewModel.selectedCharId == char.id
        ModalRowCard {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(char.shirtColor)
                        .border(2.2.dp, char.accentGlow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = char.badgeEmoji, fontSize = 20.sp)
                }
                Column {
                    Text(text = char.name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text(text = "${char.hiName} • ${char.roleTitle}", color = Color(0xFFFFD269), fontSize = 11.sp)
                    Text(
                        text = char.signatureGear,
                        color = Color(0xFF80F8FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Speed ${char.speedRating} • Aura +${char.auraBonus}",
                        color = Color(0xFFB8E986),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            StoneGoldBilingualButton(
                enLabel = if (isSelected) "SELECTED ✓" else "SELECT",
                testTag = "char_select_${char.id}",
                isHighlighted = isSelected,
                onClick = { viewModel.selectCharacter(char.id) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun BoardsModalBody(viewModel: GameViewModel) {
    HOVERBOARDS.forEach { board ->
        val isUnlocked = viewModel.unlockedBoards.contains(board.id)
        val isSelected = viewModel.selectedBoardId == board.id
        ModalRowCard {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(board.primaryColor)
                        .border(2.dp, board.glowColor, RoundedCornerShape(8.dp))
                )
                Column {
                    Text(text = board.name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text(text = board.hiName, color = Color(0xFFFFD269), fontSize = 12.sp)
                }
            }
            val btnText = when {
                isSelected -> "EQUIPPED ✓"
                isUnlocked -> "EQUIP"
                else -> "${board.costCoins} ★"
            }
            StoneGoldBilingualButton(
                enLabel = btnText,
                testTag = "board_select_${board.id}",
                isHighlighted = isSelected,
                onClick = { viewModel.selectOrBuyBoard(board) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun LeaderboardModalBody(viewModel: GameViewModel) {
    val recordsState by viewModel.runRecordsState.collectAsStateWithLifecycle()

    WORLD_THEMES.forEachIndexed { idx, w ->
        ModalRowCard {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = w.name, color = GoldLight, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text(text = "${w.hiName} • Champion Record", color = Color(0xFFCCCCCC), fontSize = 12.sp)
            }
            Text(
                text = "%,d".format(viewModel.highScores[idx]),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }
    }

    // Cloud Run History from Firestore
    Text(
        text = "☁️ CLOUD RUN HISTORY (FIRESTORE)",
        color = GoldLight,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(top = 6.dp)
    )
    when (val state = recordsState) {
        is UiState.Loading -> {
            Text(
                text = "Syncing cloud run records...",
                color = Color(0xFFCCCCCC),
                fontSize = 12.sp
            )
        }
        is UiState.Error -> {
            Text(
                text = state.message,
                color = Color(0xFFFF8FA3),
                fontSize = 12.sp
            )
        }
        is UiState.Success -> {
            if (viewModel.currentUserId == null) {
                Text(
                    text = "Local Preview Mode active — world high scores above are saved on-device. Sign in with Google via Settings to sync run history to Firestore.",
                    color = Color(0xFFFFD269),
                    fontSize = 12.sp
                )
            } else if (state.data.isEmpty()) {
                Text(
                    text = "Complete a run to record your score to Firebase Cloud!",
                    color = Color(0xFFCCCCCC),
                    fontSize = 12.sp
                )
            } else {
                state.data.take(5).forEach { rec ->
                    val wName = WORLD_THEMES.getOrNull(rec.worldId.toInt())?.shortName ?: "RUINS"
                    ModalRowCard {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${rec.runnerName} • $wName",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${rec.distanceMeters}m • ${rec.coinsCollected} ★ • ${rec.rubiesCollected} ♦",
                                color = Color(0xFFFFD269),
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "%,d".format(rec.score),
                            color = GoldLight,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsModalBody(viewModel: GameViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var showDiagnostics by remember { mutableStateOf(false) }

    if (showDiagnostics) {
        FirebaseDiagnosticModal(
            onDismiss = { showDiagnostics = false }
        )
        return
    }

    // Google Account & Cloud Sync Status Row
    if (viewModel.currentUserId != null) {
        ModalRowCard {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "☁️ ${viewModel.userDisplayName}",
                    color = GoldLight,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Firebase Cloud Sync Active",
                    color = Color(0xFF98F584),
                    fontSize = 11.sp
                )
            }
            StoneGoldBilingualButton(
                enLabel = "SIGN OUT",
                hiLabel = "लॉग आउट",
                testTag = "sign_out_btn",
                onClick = {
                    viewModel.closeModal()
                    signOutUser(
                        credentialManager = credentialManager,
                        onSignOutComplete = {},
                        scope = scope
                    )
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    } else {
        ModalRowCard {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "📱 Local Preview Mode",
                    color = GoldLight,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "On-device storage • Sign in for Cloud Sync",
                    color = Color(0xFFFFD269),
                    fontSize = 11.sp
                )
            }
            StoneGoldBilingualButton(
                enLabel = "SIGN IN",
                hiLabel = "लॉग इन",
                testTag = "settings_sign_in_btn",
                isHighlighted = true,
                onClick = {
                    viewModel.closeModal()
                    viewModel.onRequestSignIn()
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }

    ModalRowCard {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = "🩺 Auth & Network Diagnostics",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "Check Firebase SDK, OAuth & Session Error Logs",
                color = Color(0xFFCCCCCC),
                fontSize = 11.sp
            )
        }
        StoneGoldBilingualButton(
            enLabel = "INSPECT",
            hiLabel = "जाँचें",
            testTag = "settings_diagnostics_btn",
            onClick = { showDiagnostics = true },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        )
    }

    ModalRowCard {
        Text(
            text = "🔊 Master Audio (मुख्य ध्वनि)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        StoneGoldBilingualButton(
            enLabel = if (viewModel.soundEnabled) "ON" else "MUTED",
            testTag = "toggle_sound_btn",
            isHighlighted = viewModel.soundEnabled,
            onClick = { viewModel.toggleMasterSound() },
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        )
    }

    ModalRowCard {
        Text(
            text = "👆 Bilingual Swipe Prompts",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        StoneGoldBilingualButton(
            enLabel = if (viewModel.showSwipeHints) "SHOWN" else "HIDDEN",
            testTag = "toggle_hints_btn",
            isHighlighted = viewModel.showSwipeHints,
            onClick = { viewModel.toggleSwipeHints() },
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x59000000))
            .border(1.5.dp, Color(0x73D49B35), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "🥁 Tribal BGM Volume", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = "${(viewModel.bgmVolume * 100).roundToInt()}%", color = GoldLight, fontSize = 13.sp)
        }
        Slider(
            value = viewModel.bgmVolume,
            onValueChange = { viewModel.updateBgmVolume(it) },
            colors = SliderDefaults.colors(thumbColor = GoldMid, activeTrackColor = GoldMid)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "💨 SFX & Whooshes Volume", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = "${(viewModel.sfxVolume * 100).roundToInt()}%", color = GoldLight, fontSize = 13.sp)
        }
        Slider(
            value = viewModel.sfxVolume,
            onValueChange = { viewModel.updateSfxVolume(it) },
            colors = SliderDefaults.colors(thumbColor = GoldMid, activeTrackColor = GoldMid)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "🥾 Surface Footsteps Volume", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = "${(viewModel.footstepVolume * 100).roundToInt()}%", color = GoldLight, fontSize = 13.sp)
        }
        Slider(
            value = viewModel.footstepVolume,
            onValueChange = { viewModel.updateFootstepVolume(it) },
            colors = SliderDefaults.colors(thumbColor = GoldMid, activeTrackColor = GoldMid)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x59000000))
            .border(1.5.dp, Color(0x73D49B35), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "🎧 Test Procedural Acoustic SFX",
            color = GoldLight,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StoneGoldBilingualButton(
                enLabel = "🥾 Step",
                testTag = "test_step_btn",
                onClick = { viewModel.soundEngine.playFootstep(viewModel.selectedWorldIndex) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            )
            StoneGoldBilingualButton(
                enLabel = "💨 Whoosh",
                testTag = "test_whoosh_btn",
                onClick = { viewModel.soundEngine.playSwipeWhoosh() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            )
            StoneGoldBilingualButton(
                enLabel = "★ Coin",
                testTag = "test_coin_btn",
                onClick = { viewModel.soundEngine.playCoinChime() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ShopModalBody(viewModel: GameViewModel) {
    ModalRowCard {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = "💰 5,000 Gold Coins Pack", color = GoldLight, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(text = "Instant treasure exchange", color = Color(0xFFCCCCCC), fontSize = 12.sp)
        }
        StoneGoldBilingualButton(
            enLabel = "10 ♦",
            testTag = "buy_coins_btn",
            onClick = { viewModel.buyShopCoinsPack() },
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
        )
    }

    ModalRowCard {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = "🥾 Headstart Speed Boost", color = Color(0xFFFFB700), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(text = "Start next run with Speed Boost", color = Color(0xFFCCCCCC), fontSize = 12.sp)
        }
        StoneGoldBilingualButton(
            enLabel = "500 ★",
            testTag = "buy_headstart_btn",
            onClick = { viewModel.buyShopHeadstart() },
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun PauseModalBody(viewModel: GameViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GreenBevelButton(
            text = "RESUME",
            iconText = "▶",
            testTag = "resume_run_btn",
            onClick = { viewModel.closeModal() },
            modifier = Modifier.fillMaxWidth(),
            fontSize = 20.sp
        )
        StoneGoldBilingualButton(
            enLabel = "SWITCH WORLD",
            hiLabel = "दुनिया बदलें",
            testTag = "pause_worlds_btn",
            onClick = { viewModel.navigateToWorldsScreen() },
            modifier = Modifier.fillMaxWidth()
        )
        StoneGoldBilingualButton(
            enLabel = "MAIN MENU",
            hiLabel = "मुख्य मेन्यू",
            testTag = "pause_menu_btn",
            onClick = { viewModel.navigateToMainMenu() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Inspect3DShowcaseOverlay(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xCC1E1A16))
                .border(2.dp, GoldMid, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "3D WORLD & LEGEND SHOWCASE",
                    color = GoldLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${viewModel.currentCharacter.name} in ${viewModel.currentWorld.shortName}",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
            StoneGoldBilingualButton(
                enLabel = "DONE ✕",
                testTag = "close_inspect_btn",
                onClick = {
                    viewModel.inspectYawRad = 0f
                    viewModel.closeModal()
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xD91E1A16))
                .border(2.5.dp, GoldMid, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🔄 360° Character Orbit Angle",
                    color = GoldLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${((viewModel.inspectYawRad / PI.toFloat()) * 180f).roundToInt()}°",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = viewModel.inspectYawRad,
                onValueChange = { viewModel.inspectYawRad = it },
                valueRange = -PI.toFloat()..PI.toFloat(),
                colors = SliderDefaults.colors(thumbColor = GoldMid, activeTrackColor = GoldMid)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RUNNER_CHARACTERS.forEach { c ->
                    val sel = viewModel.selectedCharId == c.id
                    StoneGoldBilingualButton(
                        enLabel = c.name.substringAfterLast(" "),
                        testTag = "showcase_char_${c.id}",
                        isHighlighted = sel,
                        onClick = { viewModel.selectCharacter(c.id) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WORLD_THEMES.forEach { w ->
                    val sel = viewModel.selectedWorldIndex == w.id
                    StoneGoldBilingualButton(
                        enLabel = w.shortName.substringBefore(" "),
                        testTag = "showcase_world_${w.id}",
                        isHighlighted = sel,
                        onClick = { viewModel.selectWorld(w.id) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryLoreModalBody(viewModel: GameViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "THE CHRONO CORE PROTOCOL • COMIC ARCHIVE",
            color = Color(0xFF64FFDA),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )

        // 4 Comic-Style Story Panels
        CHRONO_LORE_PANELS.forEach { panel ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(panel.primaryColor, Color(0xFF14111D))
                        )
                    )
                    .border(2.dp, panel.accentColor, RoundedCornerShape(14.dp))
                    .padding(12.dp)
                    .testTag("lore_comic_panel_${panel.panelNumber}"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = panel.title,
                            color = panel.accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = panel.hiTitle,
                            color = Color(0xFFFFD269),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x55000000))
                            .border(1.5.dp, panel.accentColor, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = panel.badgeEmoji, fontSize = 22.sp)
                    }
                }
                Text(
                    text = panel.caption,
                    color = Color(0xFFEEEEEE),
                    fontSize = 12.sp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x66000000))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = panel.dialogueQuote,
                        color = panel.accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Iconic 3D Runner Roster Encyclopedia Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x66000000))
                .border(1.5.dp, GoldMid, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "👟 ICONIC 3D RUNNER CREW (TAP TO EQUIP)",
                color = GoldLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            RUNNER_CHARACTERS.forEach { hero ->
                val isSelected = viewModel.selectedCharId == hero.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0x4464FFDA) else Color(0x33000000))
                        .border(
                            1.dp,
                            if (isSelected) hero.accentGlow else Color(0x44FFFFFF),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            viewModel.selectCharacter(hero.id)
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = hero.badgeEmoji, fontSize = 20.sp)
                        Column {
                            Text(
                                text = "${hero.name} • ${hero.roleTitle}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = hero.signatureGear,
                                color = hero.accentGlow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (isSelected) {
                        Text(
                            text = "EQUIPPED ✓",
                            color = Color(0xFF64FFDA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        GreenBevelButton(
            text = "▶ PLAY 3s INTRO & RUN",
            iconText = "🎬",
            testTag = "lore_start_run_btn",
            onClick = { viewModel.startGame() },
            modifier = Modifier.fillMaxWidth(),
            fontSize = 16.sp
        )
    }
}
