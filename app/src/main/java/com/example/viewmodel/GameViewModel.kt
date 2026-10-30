package com.example.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.data.PlayerProfileDoc
import com.example.data.RunRecordDoc
import com.example.data.repository.HorizonDashRepository
import com.example.model.AmbientParticle
import com.example.model.ConfettiParticle
import com.example.model.EntityType
import com.example.model.FlashMission
import com.example.model.FlashMissionType
import com.example.model.FloatingPopup
import com.example.model.HOVERBOARDS
import com.example.model.HoverboardSpec
import com.example.model.ModalType
import com.example.model.PowerupKind
import com.example.model.RUNNER_CHARACTERS
import com.example.model.RadioTransmission
import com.example.model.RunnerCharacter
import com.example.model.SceneryRow
import com.example.model.ScreenState
import com.example.model.WORLD_THEMES
import com.example.model.WorldEntity
import com.example.model.WorldTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class GameViewModel(
    private val repository: HorizonDashRepository? = null,
    val currentUserId: String? = null,
    val userDisplayName: String = "Explorer Leo",
    val onRequestSignIn: () -> Unit = {}
) : ViewModel() {

    val soundEngine = SoundEngine()

    // Persistent Profile State
    var coins by mutableIntStateOf(24180)
        private set
    var rubies by mutableIntStateOf(142)
        private set
    var selectedWorldIndex by mutableIntStateOf(0)
        private set
    var selectedCharId by mutableStateOf("leo")
        private set
    var selectedBoardId by mutableStateOf("solar")
        private set

    val unlockedBoards = mutableStateListOf("solar")
    val highScores = mutableStateListOf(98420, 64200, 51800, 42900)

    var challengeDist by mutableIntStateOf(1200)
        private set
    var challengeGems by mutableIntStateOf(0)
        private set

    var magnetLevel by mutableIntStateOf(2)
        private set
    var shieldLevel by mutableIntStateOf(2)
        private set
    var boostLevel by mutableIntStateOf(1)
        private set
    var multiplierLevel by mutableIntStateOf(3)
        private set

    var freeStartBoost by mutableStateOf(false)
        private set
    var showSwipeHints by mutableStateOf(true)
        private set

    // Audio Settings
    var soundEnabled by mutableStateOf(true)
        private set
    var bgmVolume by mutableFloatStateOf(0.65f)
        private set
    var sfxVolume by mutableFloatStateOf(0.85f)
        private set
    var footstepVolume by mutableFloatStateOf(0.75f)
        private set

    // Navigation & Modals
    var screenState by mutableStateOf(ScreenState.MAIN_MENU)
        private set
    var activeModal by mutableStateOf(ModalType.NONE)
        private set
    var toastMessage by mutableStateOf<String?>(null)
        private set
    private var toastTimestamp by mutableLongStateOf(0L)

    // 3D Showcase / Inspect Camera State
    var inspectYawRad by mutableFloatStateOf(0f)

    // Live Run State
    var isPaused by mutableStateOf(false)
        private set
    var score by mutableIntStateOf(0)
        private set
    var runCoins by mutableIntStateOf(0)
        private set
    var runRubies by mutableIntStateOf(0)
        private set
    var distanceMeters by mutableFloatStateOf(0f)
        private set
    var aura by mutableIntStateOf(75)
        private set

    // Physics & Movement
    private val laneWidth = 3.4f
    private val baseSpeed = 0.30f
    private val maxSpeed = 0.64f
    private val acceleration = 0.000045f
    private val gravity = 0.017f
    private val jumpPower = 0.37f

    var targetLane by mutableIntStateOf(0)
        private set
    var playerX by mutableFloatStateOf(0f)
        private set
    var playerY by mutableFloatStateOf(0f)
        private set
    var verticalVelocity by mutableFloatStateOf(0f)
        private set
    var isJumping by mutableStateOf(false)
        private set
    var isSliding by mutableStateOf(false)
        private set
    private var slideTimer = 0f

    var speed by mutableFloatStateOf(baseSpeed)
        private set
    var scrollDistance by mutableFloatStateOf(0f)
        private set
    var runCycle by mutableFloatStateOf(0f)
        private set
    var animTimeSec by mutableFloatStateOf(0f)
        private set
    private var lastStepSign = 0

    // Active Powerup Timers
    var hasShield by mutableStateOf(false)
        private set
    var magnetTimer by mutableFloatStateOf(0f)
        private set
    var magnetMax by mutableFloatStateOf(500f)
        private set
    var boostTimer by mutableFloatStateOf(0f)
        private set
    var boostMax by mutableFloatStateOf(320f)
        private set

    // --- GEN-Z DOPAMINE, FLOW MULTIPLIER, NEAR-MISS & FLASH MISSIONS ---
    var dailyStreakDays by mutableIntStateOf(5)
        private set
    var flowMeter by mutableFloatStateOf(0f) // 0f..100f
        private set
    var isHyperRun by mutableStateOf(false)
        private set
    var comboChain by mutableIntStateOf(0)
        private set
    var maxComboChain by mutableIntStateOf(0)
        private set
    var nearMissCount by mutableIntStateOf(0)
        private set
    var nearMissSlowMoTimer by mutableFloatStateOf(0f)
        private set
    var nearMissFlashTimer by mutableFloatStateOf(0f)
        private set
    private var lastLaneSwitchTimeSec = -10f
    private var lastPreviousLane = 0

    // Camera Juice (Dutch Roll, FOV Warp, Screen Shake)
    var cameraRollRad by mutableFloatStateOf(0f)
        private set
    var dynamicFovMultiplier by mutableFloatStateOf(1.0f)
        private set
    var screenShakeIntensity by mutableFloatStateOf(0f)
        private set
    var screenShakeX by mutableFloatStateOf(0f)
        private set
    var screenShakeY by mutableFloatStateOf(0f)
        private set

    // 30-Second Flash Micro-Missions & Confetti
    var activeFlashMission by mutableStateOf<FlashMission?>(null)
        private set
    var flashMissionsCompleted by mutableIntStateOf(0)
        private set
    private var flashMissionCursor = 0
    private var cleanDashAccumulator = 0f
    val confettiParticles = mutableStateListOf<ConfettiParticle>()

    // Close-Call Defeat & Social Viral Scorecard State
    var closeCallHeadline by mutableStateOf("")
        private set
    var closeCallSubtext by mutableStateOf("")
        private set
    var isNewPersonalBest by mutableStateOf(false)
        private set

    // --- 1. INTRO CUTSCENE (IntroCinematic: 3.0s Sequence + Tap-to-Skip) ---
    var introCutsceneTimerSec by mutableFloatStateOf(0f) // 3.0f -> 0.0f
        private set
    var introSurgePlayed by mutableStateOf(false)
        private set

    val isIntroCutsceneActive: Boolean
        get() = screenState == ScreenState.PLAYING && introCutsceneTimerSec > 0f

    val introElapsedSec: Float
        get() = (3.0f - introCutsceneTimerSec).coerceIn(0f, 3.0f)

    // --- 2. DIMENSIONAL PORTAL WARP FLASH ---
    var portalMorphFlashTimer by mutableFloatStateOf(0f)
        private set

    // --- 3. IN-GAME VOICE LINES / RADIO CHIMES & MILESTONES ---
    var activeRadioTransmission by mutableStateOf<RadioTransmission?>(null)
        private set
    private var nextRadioMilestoneMeters = 100
    private var nextPortalSpawnMeters = 350

    val flowMultiplier: Int
        get() = when {
            isHyperRun || flowMeter >= 90f -> 8
            flowMeter >= 60f -> 4
            flowMeter >= 30f -> 2
            else -> 1
        }

    val flowTierLabel: String
        get() = when {
            isHyperRun || flowMeter >= 90f -> "🔥 8x HYPER RUN"
            flowMeter >= 60f -> "⚡ 4x OVERDRIVE"
            flowMeter >= 30f -> "✨ 2x VIBE FLOW"
            else -> "1x FLOW"
        }

    val runRankTier: String
        get() = when {
            score >= 95000 || nearMissCount >= 6 -> "SS+ HYPER GOD"
            score >= 55000 || nearMissCount >= 4 -> "S-TIER LEGEND"
            score >= 25000 || nearMissCount >= 2 -> "A-TIER FLOW MASTER"
            else -> "B-TIER STREET RUNNER"
        }

    // 3D World Scene Lists
    val entities = mutableStateListOf<WorldEntity>()
    val sceneryRows = mutableStateListOf<SceneryRow>()
    val particles = mutableStateListOf<AmbientParticle>()
    val floatingPopups = mutableStateListOf<FloatingPopup>()

    private var nextEntityId = 1L
    private var spawnAccumulator = 0f
    private var nextSpawnGap = 24f

    // Two-tier Kotlin Flow error handling for Firestore Cloud Run Records
    val runRecordsState: StateFlow<UiState<List<RunRecordDoc>>> =
        (if (repository != null && currentUserId != null) {
            repository.observeUserRunRecords(currentUserId)
        } else {
            flowOf(emptyList())
        })
            .map<List<RunRecordDoc>, UiState<List<RunRecordDoc>>> { list ->
                val sorted = list.sortedByDescending { it.score }
                UiState.Success(sorted)
            }
            .catch { error ->
                Log.w("GameVM", "Error observing cloud run records", error)
                emit(UiState.Error(error.message ?: "Failed to observe run records"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val currentWorld: WorldTheme
        get() = WORLD_THEMES[selectedWorldIndex.coerceIn(0, WORLD_THEMES.lastIndex)]

    val currentCharacter: RunnerCharacter
        get() = RUNNER_CHARACTERS.find { it.id == selectedCharId } ?: RUNNER_CHARACTERS.first()

    val currentBoard: HoverboardSpec
        get() = HOVERBOARDS.find { it.id == selectedBoardId } ?: HOVERBOARDS.first()

    init {
        rebuildSceneryAndParticles()
        observeCloudProfileIfAuthenticated()
    }

    private fun observeCloudProfileIfAuthenticated() {
        val repo = repository ?: return
        val uid = currentUserId ?: return
        repo.observePlayerProfile(uid)
            .onEach { doc ->
                if (doc != null) {
                    coins = doc.coins.toInt()
                    rubies = doc.rubies.toInt()
                    val wIdx = doc.selectedWorld.toInt().coerceIn(0, 3)
                    if (wIdx != selectedWorldIndex) {
                        selectedWorldIndex = wIdx
                        rebuildSceneryAndParticles()
                    }
                    selectedCharId = doc.selectedChar
                    selectedBoardId = doc.selectedBoard
                    challengeDist = doc.challengeDist.toInt().coerceIn(0, 2000)
                    challengeGems = doc.challengeGems.toInt().coerceIn(0, 5)
                    magnetLevel = doc.magnetLevel.toInt().coerceIn(1, 50)
                    shieldLevel = doc.shieldLevel.toInt().coerceIn(1, 50)
                    boostLevel = doc.boostLevel.toInt().coerceIn(1, 50)
                    multiplierLevel = doc.multiplierLevel.toInt().coerceIn(1, 50)
                    highScores[0] = doc.highScoreW0.toInt()
                    highScores[1] = doc.highScoreW1.toInt()
                    highScores[2] = doc.highScoreW2.toInt()
                    highScores[3] = doc.highScoreW3.toInt()
                } else {
                    // Seed initial cloud profile for new authenticated user
                    syncProfileToCloud()
                }
            }
            .catch { error ->
                Log.w("GameVM", "Error observing cloud player profile", error)
            }
            .launchIn(viewModelScope)
    }

    private fun syncProfileToCloud() {
        val repo = repository ?: return
        val uid = currentUserId ?: return
        val doc = PlayerProfileDoc(
            id = uid,
            userId = uid,
            displayName = userDisplayName.take(50).ifBlank { "Explorer Leo" },
            coins = coins.toLong().coerceIn(0L, 100000000L),
            rubies = rubies.toLong().coerceIn(0L, 1000000L),
            selectedWorld = selectedWorldIndex.toLong().coerceIn(0L, 3L),
            selectedChar = selectedCharId.take(32).ifBlank { "leo" },
            selectedBoard = selectedBoardId.take(32).ifBlank { "solar" },
            challengeDist = challengeDist.toLong().coerceIn(0L, 2000L),
            challengeGems = challengeGems.toLong().coerceIn(0L, 5L),
            magnetLevel = magnetLevel.toLong().coerceIn(1L, 50L),
            shieldLevel = shieldLevel.toLong().coerceIn(1L, 50L),
            boostLevel = boostLevel.toLong().coerceIn(1L, 50L),
            multiplierLevel = multiplierLevel.toLong().coerceIn(1L, 50L),
            highScoreW0 = highScores[0].toLong().coerceIn(0L, 100000000L),
            highScoreW1 = highScores[1].toLong().coerceIn(0L, 100000000L),
            highScoreW2 = highScores[2].toLong().coerceIn(0L, 100000000L),
            highScoreW3 = highScores[3].toLong().coerceIn(0L, 100000000L)
        )
        viewModelScope.launch {
            repo.savePlayerProfile(doc)
        }
    }

    fun loadFromPrefs(context: Context) {
        val prefs = context.getSharedPreferences("horizon_dash_prefs", Context.MODE_PRIVATE)
        if (repository == null) {
            coins = prefs.getInt("coins", 24180)
            rubies = prefs.getInt("rubies", 142)
            selectedWorldIndex = prefs.getInt("world", 0).coerceIn(0, 3)
            selectedCharId = prefs.getString("char", "leo") ?: "leo"
            selectedBoardId = prefs.getString("board", "solar") ?: "solar"
            challengeDist = prefs.getInt("chal_dist", 1200)
            challengeGems = prefs.getInt("chal_gems", 0)
            magnetLevel = prefs.getInt("upg_magnet", 2)
            shieldLevel = prefs.getInt("upg_shield", 2)
            boostLevel = prefs.getInt("upg_boost", 1)
            multiplierLevel = prefs.getInt("upg_mult", 3)
            for (i in 0..3) {
                highScores[i] = prefs.getInt("hs_$i", highScores[i])
            }
        }
        soundEnabled = prefs.getBoolean("sound_on", true)
        bgmVolume = prefs.getFloat("bgm_vol", 0.65f)
        sfxVolume = prefs.getFloat("sfx_vol", 0.85f)
        footstepVolume = prefs.getFloat("step_vol", 0.75f)
        syncAudioSettings()
    }

    fun saveToPrefs(context: Context) {
        val prefs = context.getSharedPreferences("horizon_dash_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("coins", coins)
            putInt("rubies", rubies)
            putInt("world", selectedWorldIndex)
            putString("char", selectedCharId)
            putString("board", selectedBoardId)
            putInt("chal_dist", challengeDist)
            putInt("chal_gems", challengeGems)
            putInt("upg_magnet", magnetLevel)
            putInt("upg_shield", shieldLevel)
            putInt("upg_boost", boostLevel)
            putInt("upg_mult", multiplierLevel)
            putBoolean("sound_on", soundEnabled)
            putFloat("bgm_vol", bgmVolume)
            putFloat("sfx_vol", sfxVolume)
            putFloat("step_vol", footstepVolume)
            for (i in 0..3) {
                putInt("hs_$i", highScores[i])
            }
            apply()
        }
    }

    private fun syncAudioSettings() {
        soundEngine.soundEnabled = soundEnabled
        soundEngine.bgmVolume = bgmVolume
        soundEngine.sfxVolume = sfxVolume
        soundEngine.footstepVolume = footstepVolume
        if (soundEnabled) {
            soundEngine.startWorldBgm(selectedWorldIndex)
        } else {
            soundEngine.stopBgm()
        }
    }

    fun showToast(msg: String) {
        toastMessage = msg
        toastTimestamp = System.currentTimeMillis()
    }

    private fun rebuildSceneryAndParticles() {
        sceneryRows.clear()
        var rowCount = 0
        var z = -10f
        while (z >= -118f) {
            sceneryRows.add(
                SceneryRow(
                    z = z,
                    variant = rowCount,
                    hasArchway = (rowCount % 4 == 2)
                )
            )
            z -= 16f
            rowCount++
        }

        particles.clear()
        for (i in 0 until 75) {
            particles.add(
                AmbientParticle(
                    x = (Random.nextFloat() - 0.5f) * 26f,
                    y = Random.nextFloat() * 11f + 0.5f,
                    z = -Random.nextFloat() * 100f + 4f,
                    size = 0.09f + Random.nextFloat() * 0.11f,
                    speedMultiplier = 0.7f + Random.nextFloat() * 0.6f
                )
            )
        }
    }

    fun selectWorld(index: Int) {
        selectedWorldIndex = index.coerceIn(0, WORLD_THEMES.lastIndex)
        soundEngine.playUiClick()
        soundEngine.startWorldBgm(selectedWorldIndex)
        rebuildSceneryAndParticles()
        syncProfileToCloud()
        showToast("World: ${currentWorld.shortName}")
    }

    fun selectCharacter(id: String) {
        selectedCharId = id
        soundEngine.playUiClick()
        syncProfileToCloud()
        showToast("Runner: ${currentCharacter.name}")
    }

    fun selectOrBuyBoard(spec: HoverboardSpec) {
        soundEngine.playUiClick()
        if (unlockedBoards.contains(spec.id)) {
            selectedBoardId = spec.id
            syncProfileToCloud()
            showToast("Equipped ${spec.name}!")
        } else if (coins >= spec.costCoins) {
            coins -= spec.costCoins
            unlockedBoards.add(spec.id)
            selectedBoardId = spec.id
            soundEngine.playPowerupSurge()
            syncProfileToCloud()
            showToast("Unlocked & Equipped ${spec.name}!")
        } else {
            showToast("Need ${spec.costCoins} Coins!")
        }
    }

    fun openModal(modal: ModalType) {
        soundEngine.playUiClick()
        activeModal = modal
        if (screenState == ScreenState.PLAYING && modal != ModalType.NONE) {
            isPaused = true
        }
    }

    fun closeModal() {
        soundEngine.playUiClick()
        activeModal = ModalType.NONE
        if (screenState == ScreenState.PLAYING) {
            isPaused = false
        }
    }

    fun navigateToWorldsScreen() {
        soundEngine.playUiClick()
        isPaused = false
        activeModal = ModalType.NONE
        screenState = ScreenState.WORLDS_AND_CHALLENGES
    }

    fun navigateToMainMenu() {
        soundEngine.playUiClick()
        isPaused = false
        activeModal = ModalType.NONE
        screenState = ScreenState.MAIN_MENU
        playerX = 0f
        playerY = 0f
        targetLane = 0
    }

    fun claimDailyChest() {
        soundEngine.playPowerupSurge()
        dailyStreakDays += 1
        coins += 1000 + dailyStreakDays * 100
        rubies += 5
        spawnConfettiBurst()
        syncProfileToCloud()
        showToast("🔥 ${dailyStreakDays}-Day Streak Chest: +${1000 + dailyStreakDays * 100} Coins & +5 Rubies!")
    }

    fun spawnConfettiBurst() {
        val palette = listOf(
            Color(0xFF64FFDA),
            Color(0xFFFF6EC7),
            Color(0xFFFFE266),
            Color(0xFFB57CFF),
            Color(0xFF00F0FF),
            Color(0xFF56E364)
        )
        confettiParticles.clear()
        for (i in 0 until 42) {
            confettiParticles.add(
                ConfettiParticle(
                    xRatio = 0.5f + (Random.nextFloat() - 0.5f) * 0.28f,
                    yRatio = 0.32f + (Random.nextFloat() - 0.5f) * 0.14f,
                    vx = (Random.nextFloat() - 0.5f) * 0.022f,
                    vy = -0.016f - Random.nextFloat() * 0.018f,
                    color = palette[i % palette.size],
                    rotationDeg = Random.nextFloat() * 360f,
                    sizeDp = 6f + Random.nextFloat() * 6f
                )
            )
        }
    }

    private fun spawnNextFlashMission() {
        val idx = flashMissionCursor % 4
        flashMissionCursor++
        activeFlashMission = when (idx) {
            0 -> FlashMission(
                id = flashMissionCursor,
                type = FlashMissionType.COINS_SPRINT,
                title = "FLASH: Collect 12 Coins in 30s!",
                hiTitle = "30 सेकंड में 12 सिक्के चुनें!",
                target = 12,
                rewardCoins = 400,
                rewardRubies = 2
            )
            1 -> FlashMission(
                id = flashMissionCursor,
                type = FlashMissionType.NEAR_MISS_STREAK,
                title = "FLASH: 2 Near-Miss Dodges in 30s!",
                hiTitle = "30 सेकंड में 2 करीबी बचाव करें!",
                target = 2,
                rewardCoins = 500,
                rewardRubies = 3
            )
            2 -> FlashMission(
                id = flashMissionCursor,
                type = FlashMissionType.ACROBAT_COINS,
                title = "FLASH: 6 Coins Jumping/Sliding!",
                hiTitle = "कूदते/फिसलते हुए 6 सिक्के लें!",
                target = 6,
                rewardCoins = 450,
                rewardRubies = 2
            )
            else -> FlashMission(
                id = flashMissionCursor,
                type = FlashMissionType.CLEAN_DASH_METERS,
                title = "FLASH: Clean Dash 200m in 30s!",
                hiTitle = "30 सेकंड में 200m बेदाग दौड़ें!",
                target = 200,
                rewardCoins = 450,
                rewardRubies = 2
            )
        }
        cleanDashAccumulator = 0f
    }

    private fun advanceFlashMission(type: FlashMissionType, delta: Int = 1) {
        val mission = activeFlashMission ?: return
        if (mission.type != type) return
        val newProg = (mission.progress + delta).coerceAtMost(mission.target)
        activeFlashMission = mission.copy(progress = newProg)
        if (newProg >= mission.target) {
            flashMissionsCompleted += 1
            coins += mission.rewardCoins
            rubies += mission.rewardRubies
            runCoins += mission.rewardCoins / 10
            runRubies += mission.rewardRubies
            score += 1200 * flowMultiplier
            gainFlow(32f)
            spawnConfettiBurst()
            soundEngine.playFlashMissionComplete()
            addPopup("🎉 FLASH CLEAR! +${mission.rewardCoins}★ +${mission.rewardRubies}♦", isPositive = true, isCoin = true)
            spawnNextFlashMission()
        }
    }

    private fun gainFlow(amount: Float) {
        val wasHyper = isHyperRun
        flowMeter = (flowMeter + amount).coerceIn(0f, 100f)
        if (flowMeter >= 90f && !wasHyper) {
            isHyperRun = true
            screenShakeIntensity = max(screenShakeIntensity, 7.5f)
            soundEngine.playHyperRunDrop()
            addPopup("🔥 8x HYPER RUN UNLEASHED!", isPositive = true)
        }
    }

    fun equipFreeStartBoost() {
        soundEngine.playPowerupSurge()
        freeStartBoost = true
        showToast("⚡ Speed Boost Equipped for Next Run!")
    }

    fun upgradeStat(statKey: String) {
        val cost = when (statKey) {
            "magnet" -> 1500
            "shield" -> 1500
            "boost" -> 2000
            "mult" -> 2500
            else -> 1500
        }
        if (coins >= cost) {
            coins -= cost
            when (statKey) {
                "magnet" -> magnetLevel = min(50, magnetLevel + 1)
                "shield" -> shieldLevel = min(50, shieldLevel + 1)
                "boost" -> boostLevel = min(50, boostLevel + 1)
                "mult" -> multiplierLevel = min(50, multiplierLevel + 1)
            }
            soundEngine.playPowerupSurge()
            syncProfileToCloud()
            showToast("Upgrade Purchased!")
        } else {
            soundEngine.playUiClick()
            showToast("Not enough Coins!")
        }
    }

    fun buyShopCoinsPack() {
        if (rubies >= 10) {
            rubies -= 10
            coins += 5000
            soundEngine.playCoinChime()
            syncProfileToCloud()
            showToast("+5,000 Gold Coins Added!")
        } else {
            soundEngine.playUiClick()
            showToast("Need 10 Rubies!")
        }
    }

    fun buyShopHeadstart() {
        if (coins >= 500) {
            coins -= 500
            freeStartBoost = true
            soundEngine.playPowerupSurge()
            syncProfileToCloud()
            showToast("Headstart Speed Boost Ready!")
        } else {
            soundEngine.playUiClick()
            showToast("Need 500 Coins!")
        }
    }

    fun toggleMasterSound() {
        soundEnabled = !soundEnabled
        syncAudioSettings()
    }

    fun updateBgmVolume(vol: Float) {
        bgmVolume = vol.coerceIn(0f, 1f)
        syncAudioSettings()
    }

    fun updateSfxVolume(vol: Float) {
        sfxVolume = vol.coerceIn(0f, 1f)
        syncAudioSettings()
    }

    fun updateFootstepVolume(vol: Float) {
        footstepVolume = vol.coerceIn(0f, 1f)
        syncAudioSettings()
    }

    fun toggleSwipeHints() {
        showSwipeHints = !showSwipeHints
        soundEngine.playUiClick()
    }

    // --- START & END RUN ---
    fun startGame() {
        soundEngine.playRelicGrab()
        soundEngine.startWorldBgm(selectedWorldIndex)
        activeModal = ModalType.NONE
        screenState = ScreenState.PLAYING
        isPaused = false

        score = 0
        runCoins = 0
        runRubies = 0
        distanceMeters = 0f
        aura = min(100, 70 + currentCharacter.auraBonus)
        speed = baseSpeed + (currentCharacter.speedRating - 90) * 0.002f
        targetLane = 0
        playerX = 0f
        playerY = 0f
        verticalVelocity = 0f
        isJumping = false
        isSliding = false
        slideTimer = 0f
        runCycle = 0f

        flowMeter = 18f
        isHyperRun = false
        comboChain = 0
        maxComboChain = 0
        nearMissCount = 0
        nearMissSlowMoTimer = 0f
        nearMissFlashTimer = 0f
        flashMissionsCompleted = 0
        cameraRollRad = 0f
        dynamicFovMultiplier = 1.0f
        screenShakeIntensity = 4.5f
        screenShakeX = 0f
        screenShakeY = 0f
        confettiParticles.clear()
        spawnNextFlashMission()

        // Initialize 3-Second Intro Cutscene & Portal Milestones
        introCutsceneTimerSec = 3.0f
        introSurgePlayed = false
        portalMorphFlashTimer = 0f
        nextRadioMilestoneMeters = 100
        nextPortalSpawnMeters = 300

        broadcastRadioTransmission(
            speaker = "ORACLE-7 COMMS",
            callSign = "CHRONO-LINK // 94.2 MHz",
            message = "Chrono Core acquired! ${currentCharacter.name}'s ${currentCharacter.signatureGear} is supercharged for hyper-speed!",
            hiMessage = "क्रोनो कोर मिल गया! ${currentCharacter.hiName} पूरी रफ़्तार से दौड़ने के लिए तैयार है!",
            accentColor = Color(0xFF64FFDA)
        )

        magnetMax = 450f + magnetLevel * 85f
        magnetTimer = magnetMax
        hasShield = false

        if (freeStartBoost) {
            freeStartBoost = false
            boostMax = 280f + boostLevel * 75f
            boostTimer = boostMax
        } else {
            boostTimer = 0f
        }

        entities.clear()
        floatingPopups.clear()
        spawnWave(-44f)
        spawnWave(-78f)
        spawnWave(-112f)
        spawnAccumulator = 0f
    }

    fun skipIntroCutscene() {
        if (!isIntroCutsceneActive) return
        soundEngine.playSwipeWhoosh()
        introCutsceneTimerSec = 0f
        introSurgePlayed = true
        cameraRollRad = 0f
        dynamicFovMultiplier = 1.0f
        addPopup("⏭ INTRO SKIPPED — RUN!", isPositive = true)
    }

    fun broadcastRadioTransmission(
        speaker: String,
        callSign: String,
        message: String,
        hiMessage: String,
        accentColor: Color = Color(0xFF00F0FF)
    ) {
        soundEngine.playRadioCommsChime()
        activeRadioTransmission = RadioTransmission(
            id = System.nanoTime(),
            speaker = speaker,
            callSign = callSign,
            message = message,
            hiMessage = hiMessage,
            accentColor = accentColor
        )
    }

    fun triggerDimensionalPortalNow() {
        if (screenState != ScreenState.PLAYING) return
        if (introCutsceneTimerSec > 0f) {
            skipIntroCutscene()
        }
        // Spawn a Dimensional Rift Portal directly ahead in the player's current lane
        entities.add(
            WorldEntity(
                id = nextEntityId++,
                lane = targetLane,
                x = targetLane * laneWidth,
                y = 0f,
                z = -24f,
                type = EntityType.DIMENSIONAL_PORTAL
            )
        )
        broadcastRadioTransmission(
            speaker = "ORACLE-7 COMMS",
            callSign = "RIFT TELEMETRY",
            message = "Chrono Core resonating... Dimensional Portal opening right ahead!",
            hiMessage = "क्रोनो कोर गूंज रहा है... सामने आयामी पोर्टल खुल रहा है!",
            accentColor = Color(0xFFB57CFF)
        )
    }

    private fun enterDimensionalPortal() {
        val nextWorld = (selectedWorldIndex + 1) % WORLD_THEMES.size
        selectedWorldIndex = nextWorld
        soundEngine.playPortalWarp()
        soundEngine.startWorldBgm(selectedWorldIndex)
        rebuildSceneryAndParticles()
        portalMorphFlashTimer = 28f
        screenShakeIntensity = max(screenShakeIntensity, 10.5f)
        val bonus = 1500 * flowMultiplier
        score += bonus
        gainFlow(30f)
        spawnConfettiBurst()
        addPopup("🌀 RIFT WARP: ${currentWorld.shortName}! +$bonus", isPositive = true)
        broadcastRadioTransmission(
            speaker = "ORACLE-7 COMMS",
            callSign = "DIMENSIONAL SHIFT // WORLD ${selectedWorldIndex + 1}",
            message = "Rift crossed into ${currentWorld.shortName}! ${currentCharacter.name} gains +$bonus Rift Bonus!",
            hiMessage = "पोर्टल पार! अब आप ${currentWorld.hiName} में दौड़ रहे हैं! +$bonus बोनस!",
            accentColor = currentCharacter.accentGlow
        )
        syncProfileToCloud()
    }

    fun togglePause() {
        if (screenState != ScreenState.PLAYING) return
        soundEngine.playUiClick()
        isPaused = !isPaused
        activeModal = if (isPaused) ModalType.PAUSE else ModalType.NONE
    }

    fun moveLane(delta: Int) {
        if (screenState != ScreenState.PLAYING || isPaused) return
        if (isIntroCutsceneActive) {
            skipIntroCutscene()
        }
        val prev = targetLane
        targetLane = (targetLane + delta).coerceIn(-1, 1)
        if (targetLane != prev) {
            lastPreviousLane = prev
            lastLaneSwitchTimeSec = animTimeSec
            // Juicy Dutch-angle camera roll & micro haptic shake on fast lane swipe
            cameraRollRad = (if (delta > 0) -0.085f else 0.085f)
            screenShakeIntensity = max(screenShakeIntensity, 2.8f)
            soundEngine.playSwipeWhoosh()
        }
    }

    fun triggerJump() {
        if (screenState != ScreenState.PLAYING || isPaused) return
        if (isIntroCutsceneActive) {
            skipIntroCutscene()
        }
        if (!isJumping) {
            isSliding = false
            slideTimer = 0f
            verticalVelocity = jumpPower
            isJumping = true
            screenShakeIntensity = max(screenShakeIntensity, 3.8f)
            soundEngine.playJumpWhoosh()
        }
    }

    fun triggerSlide() {
        if (screenState != ScreenState.PLAYING || isPaused) return
        if (isIntroCutsceneActive) {
            skipIntroCutscene()
        }
        if (isJumping) {
            verticalVelocity = -0.36f
        }
        isSliding = true
        slideTimer = 38f
        screenShakeIntensity = max(screenShakeIntensity, 4.2f)
        soundEngine.playSlideScrape()
    }

    fun triggerManualBoost() {
        if (screenState != ScreenState.PLAYING || isPaused) return
        if (isIntroCutsceneActive) {
            skipIntroCutscene()
        }
        boostMax = 260f + boostLevel * 70f
        boostTimer = boostMax
        gainFlow(25f)
        screenShakeIntensity = max(screenShakeIntensity, 6.5f)
        soundEngine.playPowerupSurge()
        addPopup("⚡ SPEED BOOST!", isPositive = true)
    }

    private fun addPopup(text: String, isPositive: Boolean, isCoin: Boolean = false) {
        floatingPopups.add(
            FloatingPopup(
                id = System.nanoTime(),
                text = text,
                isPositive = isPositive,
                isCoin = isCoin
            )
        )
        if (floatingPopups.size > 5) {
            floatingPopups.removeAt(0)
        }
    }

    private fun spawnWave(zPos: Float) {
        val lanes = mutableListOf(-1, 0, 1).apply { shuffle() }
        val roll = Random.nextFloat()

        // Check if it's time to spawn a Dimensional Rift Portal
        if (distanceMeters >= nextPortalSpawnMeters) {
            nextPortalSpawnMeters += 350
            entities.add(
                WorldEntity(
                    id = nextEntityId++,
                    lane = 0,
                    x = 0f,
                    y = 0f,
                    z = zPos,
                    type = EntityType.DIMENSIONAL_PORTAL
                )
            )
            nextSpawnGap = 32f
            return
        }

        if (roll < 0.16f) {
            entities.add(
                WorldEntity(
                    id = nextEntityId++,
                    lane = lanes[0],
                    x = lanes[0] * laneWidth,
                    y = 0f,
                    z = zPos,
                    type = EntityType.GATE_BOOST
                )
            )
            entities.add(
                WorldEntity(
                    id = nextEntityId++,
                    lane = lanes[1],
                    x = lanes[1] * laneWidth,
                    y = 0f,
                    z = zPos,
                    type = EntityType.GATE_HAZARD
                )
            )
            nextSpawnGap = 30f
        } else if (roll < 0.58f) {
            val firstType = if (Random.nextBoolean()) EntityType.HURDLE_LOW else EntityType.BARRIER_HIGH
            entities.add(
                WorldEntity(
                    id = nextEntityId++,
                    lane = lanes[0],
                    x = lanes[0] * laneWidth,
                    y = 0f,
                    z = zPos,
                    type = firstType
                )
            )
            if (Random.nextFloat() > 0.32f) {
                val secondType = if (Random.nextBoolean()) EntityType.HURDLE_LOW else EntityType.BARRIER_HIGH
                entities.add(
                    WorldEntity(
                        id = nextEntityId++,
                        lane = lanes[1],
                        x = lanes[1] * laneWidth,
                        y = 0f,
                        z = zPos,
                        type = secondType
                    )
                )
            }
            val collectibleType = if (selectedWorldIndex == 1) EntityType.DIAMOND else EntityType.COIN
            for (i in 0 until 4) {
                entities.add(
                    WorldEntity(
                        id = nextEntityId++,
                        lane = lanes[2],
                        x = lanes[2] * laneWidth,
                        y = 1.05f,
                        z = zPos - i * 2.5f,
                        type = collectibleType
                    )
                )
            }
            nextSpawnGap = 27f
        } else {
            val mainType = if (selectedWorldIndex == 1 && Random.nextBoolean()) EntityType.DIAMOND else EntityType.COIN
            for (i in 0 until 5) {
                entities.add(
                    WorldEntity(
                        id = nextEntityId++,
                        lane = lanes[0],
                        x = lanes[0] * laneWidth,
                        y = 1.05f,
                        z = zPos - i * 2.3f,
                        type = mainType
                    )
                )
            }
            if (Random.nextFloat() < 0.48f) {
                entities.add(
                    WorldEntity(
                        id = nextEntityId++,
                        lane = lanes[1],
                        x = lanes[1] * laneWidth,
                        y = 1.18f,
                        z = zPos - 3f,
                        type = EntityType.RUBY
                    )
                )
            }
            if (Random.nextFloat() < 0.30f) {
                val kind = PowerupKind.entries[Random.nextInt(PowerupKind.entries.size)]
                entities.add(
                    WorldEntity(
                        id = nextEntityId++,
                        lane = lanes[2],
                        x = lanes[2] * laneWidth,
                        y = 1.25f,
                        z = zPos - 2f,
                        type = EntityType.POWERUP,
                        powerupKind = kind
                    )
                )
            }
            nextSpawnGap = 24f
        }
    }

    fun stepFrame(dtNormalized: Float) {
        val rawDt = dtNormalized.coerceIn(0.1f, 2.4f)
        animTimeSec += rawDt * 0.01667f

        // Update 2D Confetti Particles
        for (i in confettiParticles.size - 1 downTo 0) {
            val c = confettiParticles[i]
            c.xRatio += c.vx * rawDt
            c.yRatio += c.vy * rawDt
            c.vy += 0.00095f * rawDt
            c.rotationDeg = (c.rotationDeg + 9f * rawDt) % 360f
            if (c.yRatio > 1.15f) {
                confettiParticles.removeAt(i)
            }
        }

        // Decay screen shake & spring camera roll back to 0
        if (screenShakeIntensity > 0.05f) {
            screenShakeX = (Random.nextFloat() * 2f - 1f) * screenShakeIntensity
            screenShakeY = (Random.nextFloat() * 2f - 1f) * screenShakeIntensity
            screenShakeIntensity *= 0.82f
        } else {
            screenShakeIntensity = 0f
            screenShakeX = 0f
            screenShakeY = 0f
        }
        cameraRollRad *= 0.84f
        if (portalMorphFlashTimer > 0f) {
            portalMorphFlashTimer = max(0f, portalMorphFlashTimer - rawDt)
        }

        val now = System.currentTimeMillis()
        if (toastMessage != null && now - toastTimestamp > 2400L) {
            toastMessage = null
        }
        if (activeRadioTransmission != null && now - (activeRadioTransmission?.createdAtMs ?: 0L) > 5200L) {
            activeRadioTransmission = null
        }
        floatingPopups.removeAll { now - it.createdAt > 900L }

        // Time-dilation slow-mo during split-second Near-Miss spark or Stumble Zoom
        val timeScale = if (nearMissSlowMoTimer > 0f) {
            nearMissSlowMoTimer = max(0f, nearMissSlowMoTimer - rawDt)
            0.44f
        } else {
            1.0f
        }
        if (nearMissFlashTimer > 0f) {
            nearMissFlashTimer = max(0f, nearMissFlashTimer - rawDt)
        }
        val dt = rawDt * timeScale

        // Dynamic FOV Warp: Zoom punch on Near-Miss, Wide-Angle Speed Warp on HYPER RUN / Boost
        val targetFov = when {
            nearMissSlowMoTimer > 0f -> 1.16f
            isHyperRun -> 0.80f
            boostTimer > 0f -> 0.86f
            else -> 1.0f
        }
        dynamicFovMultiplier += (targetFov - dynamicFovMultiplier) * min(1f, 0.18f * rawDt)

        for (i in particles.indices) {
            val pt = particles[i]
            if (selectedWorldIndex == 2) {
                pt.y += 0.045f * dt * pt.speedMultiplier
                if (pt.y > 12f) pt.y = 0.4f
            } else {
                pt.y -= 0.04f * dt * pt.speedMultiplier
                if (pt.y < 0.2f) pt.y = 12f
            }
            if (screenState == ScreenState.PLAYING && !isPaused) {
                pt.z += speed * dt * (if (isHyperRun) 1.35f else 0.85f)
                if (pt.z > 8f) pt.z = -105f
            }
        }

        if (screenState != ScreenState.PLAYING) {
            val previewSpeed = 0.24f * rawDt
            scrollDistance += previewSpeed
            runCycle += previewSpeed * 1.25f
            for (i in sceneryRows.indices) {
                val r = sceneryRows[i]
                r.z += previewSpeed
                if (r.z > 8f) r.z -= 128f
            }
            return
        }

        if (isPaused) return

        // --- 1. STEP 3-SECOND INTRO CUTSCENE SEQUENCE (IntroCinematic) ---
        if (introCutsceneTimerSec > 0f) {
            introCutsceneTimerSec = max(0f, introCutsceneTimerSec - rawDt * 0.01667f)
            val elapsed = introElapsedSec
            if (elapsed >= 0.95f && !introSurgePlayed) {
                introSurgePlayed = true
                screenShakeIntensity = 8f
                soundEngine.playPowerupSurge()
                addPopup("⚡ ${currentCharacter.name.uppercase()} READY!", isPositive = true)
            }
            // During phase 3 (2.0s..3.0s), player starts sprinting as camera transitions to follow-cam
            if (elapsed >= 1.95f) {
                val introMoveSpeed = speed * ((elapsed - 1.95f) / 1.05f).coerceIn(0.15f, 1f) * rawDt
                scrollDistance += introMoveSpeed
                runCycle += introMoveSpeed * 1.35f
            } else {
                runCycle += 0.08f * rawDt
            }
            return
        }

        // --- 2. STEP NARRATIVE MILESTONE VOICE LINES / RADIO CHIMES ---
        val curMetersInt = distanceMeters.toInt()
        if (curMetersInt >= nextRadioMilestoneMeters) {
            val milestone = nextRadioMilestoneMeters
            nextRadioMilestoneMeters = when (milestone) {
                100 -> 280
                280 -> 600
                600 -> 1000
                else -> milestone + 500
            }
            when (milestone) {
                100 -> broadcastRadioTransmission(
                    speaker = "ORACLE-7 COMMS",
                    callSign = "TELEMETRY // 100m",
                    message = "${currentCharacter.name} hitting top stride! Chain Near-Miss dodges to ignite 8x HYPER RUN!",
                    hiMessage = "${currentCharacter.hiName} शानदार रफ़्तार में है! 8x हाइपर रन के लिए करीबी बचाव करें!",
                    accentColor = Color(0xFF64FFDA)
                )
                280 -> broadcastRadioTransmission(
                    speaker = "ORACLE-7 COMMS",
                    callSign = "RIFT DETECTED // 280m",
                    message = "Chrono Core resonating! A Dimensional Rift Portal is opening ahead—leap through to shift worlds!",
                    hiMessage = "क्रोनो कोर गूंज रहा है! आगे आयामी पोर्टल खुल रहा है—दुनिया बदलने के लिए उसमें कूदें!",
                    accentColor = Color(0xFFB57CFF)
                )
                600 -> broadcastRadioTransmission(
                    speaker = currentCharacter.name.uppercase(),
                    callSign = "RUNNER LOG // 600m",
                    message = "600m cleared in ${currentWorld.shortName}! Keep the combo multiplier alive!",
                    hiMessage = "600m पार! कॉम्बो मल्टीप्लायर को बनाए रखें!",
                    accentColor = Color(0xFFFFD03B)
                )
                1000 -> {
                    triggerDimensionalPortalNow()
                    broadcastRadioTransmission(
                        speaker = "ORACLE-7 ALERT",
                        callSign = "CRITICAL // 1,000m",
                        message = "1,000m Milestone! Chrono Core resonating... Portal opening!",
                        hiMessage = "1,000m पूरा! क्रोनो कोर गूंज रहा है... पोर्टल खुल रहा है!",
                        accentColor = Color(0xFFFF6EC7)
                    )
                }
                else -> broadcastRadioTransmission(
                    speaker = "ORACLE-7 COMMS",
                    callSign = "OVERDRIVE // ${milestone}m",
                    message = "Unbelievable pace at ${milestone}m! Dimensional Rift opening ahead!",
                    hiMessage = "${milestone}m पर शानदार रफ़्तार! आगे नया आयामी पोर्टल खुल रहा है!",
                    accentColor = Color(0xFF00F0FF)
                )
            }
        }

        // Update 30-Second Flash Micro-Mission Timer
        activeFlashMission?.let { mission ->
            val nextTime = mission.timeRemainingSec - rawDt * 0.01667f
            if (nextTime <= 0f) {
                addPopup("⏱️ NEW FLASH MISSION!", isPositive = false)
                spawnNextFlashMission()
            } else {
                activeFlashMission = mission.copy(timeRemainingSec = nextTime)
            }
        }

        // Flow Meter passive momentum & Hyper-Run decay
        if (isHyperRun) {
            flowMeter = max(0f, flowMeter - 0.11f * rawDt)
            if (flowMeter <= 25f) {
                isHyperRun = false
            }
        } else {
            flowMeter = (flowMeter + 0.035f * dt).coerceIn(0f, 100f)
        }

        if (speed < maxSpeed) {
            speed = min(maxSpeed, speed + acceleration * dt)
        }
        val hyperSpeedFactor = if (isHyperRun) 1.25f else 1.0f
        val effectiveSpeed = (if (boostTimer > 0f) speed * 1.38f else speed) * hyperSpeedFactor * dt
        scrollDistance += effectiveSpeed

        for (i in sceneryRows.indices) {
            val r = sceneryRows[i]
            r.z += effectiveSpeed
            if (r.z > 8f) {
                r.z -= 128f
            }
        }

        for (i in entities.size - 1 downTo 0) {
            val e = entities[i]
            e.z += effectiveSpeed
            e.rotationY += 0.075f * dt
            if (e.z > 10f) {
                entities.removeAt(i)
            }
        }

        val metersDelta = effectiveSpeed * 0.85f
        distanceMeters += metersDelta
        cleanDashAccumulator += metersDelta
        if (cleanDashAccumulator >= 10f) {
            val wholeSteps = (cleanDashAccumulator / 10f).toInt()
            cleanDashAccumulator -= wholeSteps * 10f
            advanceFlashMission(FlashMissionType.CLEAN_DASH_METERS, wholeSteps * 10)
        }

        score += (effectiveSpeed * 3.6f * multiplierLevel * flowMultiplier).toInt()

        val desiredX = targetLane * laneWidth
        val dx = desiredX - playerX
        playerX += dx * min(1f, 0.28f * rawDt)

        if (isJumping) {
            playerY += verticalVelocity * dt
            verticalVelocity -= gravity * dt
            if (playerY <= 0f) {
                playerY = 0f
                verticalVelocity = 0f
                isJumping = false
                screenShakeIntensity = max(screenShakeIntensity, 3.2f)
                soundEngine.playFootstep(selectedWorldIndex)
            }
        }

        if (isSliding) {
            slideTimer -= dt
            if (slideTimer <= 0f) {
                isSliding = false
            }
        } else if (!isJumping && boostTimer <= 0f) {
            runCycle += effectiveSpeed * 1.32f
            val curSign = if (sin(runCycle) >= 0f) 1 else -1
            if (lastStepSign != 0 && lastStepSign != curSign) {
                soundEngine.playFootstep(selectedWorldIndex)
            }
            lastStepSign = curSign
        }

        if (magnetTimer > 0f) magnetTimer = max(0f, magnetTimer - dt)
        if (boostTimer > 0f) boostTimer = max(0f, boostTimer - dt)

        spawnAccumulator += effectiveSpeed
        if (spawnAccumulator >= nextSpawnGap) {
            spawnWave(-112f)
            spawnAccumulator = 0f
        }

        checkCollisions()
    }

    private fun triggerNearMissReward(reasonTag: String) {
        nearMissCount += 1
        comboChain += 1
        maxComboChain = max(maxComboChain, comboChain)
        nearMissSlowMoTimer = 11f
        nearMissFlashTimer = 20f
        screenShakeIntensity = max(screenShakeIntensity, 8.5f)
        gainFlow(24f)
        val bonus = 500 * flowMultiplier
        score += bonus
        soundEngine.playNearMissSpark()
        addPopup("⚡ $reasonTag +$bonus (${flowMultiplier}x)", isPositive = true)
        advanceFlashMission(FlashMissionType.NEAR_MISS_STREAK, 1)
    }

    private fun checkCollisions() {
        val playerBottomY = playerY
        val playerTopY = playerY + if (isSliding) 1.0f else 2.1f

        for (i in entities.indices) {
            val e = entities[i]
            if (!e.active) continue

            if (magnetTimer > 0f && (e.type == EntityType.COIN || e.type == EntityType.RUBY || e.type == EntityType.DIAMOND)) {
                if (e.z > -22f && e.z < 4f) {
                    e.x += (playerX - e.x) * 0.20f
                    e.z += (0f - e.z) * 0.16f
                }
            }

            // Dimensional Portal spans a wider capture radius so player can leap right into the rift
            if (e.type == EntityType.DIMENSIONAL_PORTAL && e.z in -1.6f..1.6f && abs(e.x - playerX) < 3.8f) {
                e.active = false
                enterDimensionalPortal()
                continue
            }

            // Split-Second Near-Miss Detection as obstacle crosses player plane
            val isObstacle = e.type == EntityType.HURDLE_LOW || e.type == EntityType.BARRIER_HIGH || e.type == EntityType.GATE_HAZARD
            if (isObstacle && !e.nearMissChecked && e.z in -0.25f..1.45f) {
                val lateralDist = abs(e.x - playerX)
                val timeSinceSwipe = animTimeSec - lastLaneSwitchTimeSec
                if (lateralDist in 1.15f..3.65f && timeSinceSwipe < 0.55f && e.lane == lastPreviousLane) {
                    e.nearMissChecked = true
                    triggerNearMissReward("NEAR MISS!")
                } else if (lateralDist < 1.15f && e.type == EntityType.HURDLE_LOW && playerBottomY in 0.85f..1.65f) {
                    e.nearMissChecked = true
                    triggerNearMissReward("CLUTCH JUMP!")
                } else if (lateralDist < 1.15f && e.type == EntityType.BARRIER_HIGH && isSliding) {
                    e.nearMissChecked = true
                    triggerNearMissReward("SLICK SLIDE!")
                }
            }

            if (e.z > -1.25f && e.z < 1.25f && abs(e.x - playerX) < 1.15f) {
                var consumed = false
                when (e.type) {
                    EntityType.DIMENSIONAL_PORTAL -> {
                        consumed = true
                        enterDimensionalPortal()
                    }
                    EntityType.COIN, EntityType.DIAMOND -> {
                        consumed = true
                        val cVal = if (e.type == EntityType.DIAMOND) 2 else 1
                        runCoins += cVal
                        comboChain += 1
                        maxComboChain = max(maxComboChain, comboChain)
                        gainFlow(3.2f)
                        score += 50 * multiplierLevel * flowMultiplier
                        soundEngine.playCoinChime()
                        advanceFlashMission(FlashMissionType.COINS_SPRINT, cVal)
                        if (isJumping || isSliding) {
                            advanceFlashMission(FlashMissionType.ACROBAT_COINS, cVal)
                        }
                    }
                    EntityType.RUBY -> {
                        consumed = true
                        runRubies += 1
                        comboChain += 2
                        maxComboChain = max(maxComboChain, comboChain)
                        gainFlow(12f)
                        score += 250 * multiplierLevel * flowMultiplier
                        challengeGems = min(5, challengeGems + 1)
                        soundEngine.playRubyChime()
                        addPopup("+1 RUBY ♦ (${flowMultiplier}x)", isPositive = true)
                    }
                    EntityType.POWERUP -> {
                        consumed = true
                        gainFlow(18f)
                        screenShakeIntensity = max(screenShakeIntensity, 5.5f)
                        when (e.powerupKind) {
                            PowerupKind.MAGNET -> {
                                magnetMax = 420f + magnetLevel * 90f
                                magnetTimer = magnetMax
                                soundEngine.playPowerupSurge()
                                addPopup("🧲 MAGNET ACTIVE!", isPositive = true, isCoin = true)
                            }
                            PowerupKind.SHIELD -> {
                                hasShield = true
                                soundEngine.playPowerupSurge()
                                addPopup("🛡️ SHIELD READY!", isPositive = true)
                            }
                            PowerupKind.BOOST, null -> {
                                boostMax = 260f + boostLevel * 70f
                                boostTimer = boostMax
                                soundEngine.playPowerupSurge()
                                addPopup("⚡ SPEED BOOST!", isPositive = true)
                            }
                        }
                    }
                    EntityType.GATE_BOOST -> {
                        consumed = true
                        score += 400 * multiplierLevel * flowMultiplier
                        aura = min(100, aura + 15)
                        gainFlow(22f)
                        screenShakeIntensity = max(screenShakeIntensity, 6f)
                        soundEngine.playPowerupSurge()
                        addPopup("🔥 FLOW BOOST!", isPositive = true)
                    }
                    EntityType.GATE_HAZARD -> {
                        consumed = true
                        aura = max(10, aura - 18)
                        flowMeter = max(0f, flowMeter - 30f)
                        isHyperRun = false
                        comboChain = 0
                        screenShakeIntensity = max(screenShakeIntensity, 8f)
                        soundEngine.playImpact(isShatter = true)
                        addPopup("⚠️ HAZARD DRAIN!", isPositive = false)
                    }
                    EntityType.HURDLE_LOW -> {
                        if (playerBottomY < 0.85f) {
                            consumed = true
                            if (boostTimer > 0f || isHyperRun) {
                                screenShakeIntensity = max(screenShakeIntensity, 7.5f)
                                score += 300 * flowMultiplier
                                addPopup("💥 HYPER SMASH!", isPositive = true)
                            } else if (hasShield) {
                                hasShield = false
                                screenShakeIntensity = max(screenShakeIntensity, 9f)
                                soundEngine.playImpact(isShatter = true)
                                addPopup("🛡️ SHIELD SAVED YOU!", isPositive = false)
                            } else {
                                endGame()
                                return
                            }
                        }
                    }
                    EntityType.BARRIER_HIGH -> {
                        if (playerTopY > 1.75f) {
                            consumed = true
                            if (boostTimer > 0f || isHyperRun) {
                                screenShakeIntensity = max(screenShakeIntensity, 7.5f)
                                score += 300 * flowMultiplier
                                addPopup("💥 HYPER SMASH!", isPositive = true)
                            } else if (hasShield) {
                                hasShield = false
                                screenShakeIntensity = max(screenShakeIntensity, 9f)
                                soundEngine.playImpact(isShatter = true)
                                addPopup("🛡️ SHIELD SAVED YOU!", isPositive = false)
                            } else {
                                endGame()
                                return
                            }
                        }
                    }
                }
                if (consumed) {
                    e.active = false
                }
            }
        }
    }

    private fun endGame() {
        soundEngine.playImpact(isShatter = false)
        screenShakeIntensity = 12f
        val runDist = distanceMeters.toInt()
        val finalScore = score
        val prevBest = highScores[selectedWorldIndex]

        coins += runCoins
        rubies += runRubies
        challengeDist = min(2000, challengeDist + runDist)

        if (finalScore > prevBest) {
            isNewPersonalBest = true
            val diff = finalScore - prevBest
            highScores[selectedWorldIndex] = finalScore
            closeCallHeadline = "🏆 NEW WORLD RECORD!"
            closeCallSubtext = "You shattered your ${currentWorld.shortName} record by +%,d pts!".format(diff)
            spawnConfettiBurst()
        } else {
            isNewPersonalBest = false
            val pointsShort = (prevBest - finalScore).coerceAtLeast(1)
            val estMetersShort = max(3, (pointsShort / max(1, 18 * multiplierLevel)).coerceAtMost(140))
            closeCallHeadline = "🔥 SO CLOSE! ONLY ${estMetersShort}m AWAY!"
            closeCallSubtext = "Just ${estMetersShort}m (~%,d pts) from beating your ${currentWorld.shortName} Best!".format(pointsShort)
        }

        screenState = ScreenState.GAME_OVER

        syncProfileToCloud()
        val repo = repository
        if (repo != null && currentUserId != null) {
            viewModelScope.launch {
                repo.createRunRecord(
                    runnerName = currentCharacter.name,
                    worldId = selectedWorldIndex,
                    characterId = selectedCharId,
                    score = finalScore.toLong(),
                    distanceMeters = runDist.toLong(),
                    coinsCollected = runCoins.toLong(),
                    rubiesCollected = runRubies.toLong()
                )
            }
        }
    }

    fun shareViralScorecard(context: Context) {
        soundEngine.playPowerupSurge()
        val shareText = buildString {
            appendLine("⚡ HORIZON DASH: 3D LEGENDS ⚡")
            appendLine("🏅 Rank: $runRankTier")
            appendLine("🌍 World: ${currentWorld.shortName}")
            appendLine("👟 Runner: ${currentCharacter.badgeEmoji} ${currentCharacter.name} (${currentBoard.name})")
            appendLine("🔥 Score: %,d pts (%,dm)".format(score, distanceMeters.toInt()))
            appendLine("💥 Max Combo: ${maxComboChain}x | ⚡ Near-Misses: $nearMissCount")
            appendLine("🎯 Flash Missions Cleared: $flashMissionsCompleted | 🔥 Streak: ${dailyStreakDays}d")
            append("#HorizonDash #3DRunner #HyperRun")
        }
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Horizon Dash — $runRankTier Scorecard")
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(sendIntent, "Share Viral Run Scorecard").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            showToast("📸 Viral Scorecard Copied & Ready to Flex!")
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundEngine.stopBgm()
    }
}
