package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.data.PlayerProfileDoc
import com.example.data.RunRecordDoc
import com.example.data.repository.HorizonDashRepository
import com.example.model.AmbientParticle
import com.example.model.EntityType
import com.example.model.FloatingPopup
import com.example.model.HOVERBOARDS
import com.example.model.HoverboardSpec
import com.example.model.ModalType
import com.example.model.PowerupKind
import com.example.model.RUNNER_CHARACTERS
import com.example.model.RunnerCharacter
import com.example.model.SceneryRow
import com.example.model.ScreenState
import com.example.model.WORLD_THEMES
import com.example.model.WorldEntity
import com.example.model.WorldTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
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
    val userDisplayName: String = "Explorer Leo"
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
            emptyFlow()
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
        coins += 1000
        rubies += 5
        syncProfileToCloud()
        showToast("🧰 Daily Chest: +1,000 Coins & +5 Rubies!")
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
        soundEngine.playUiClick()
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
        spawnWave(-38f)
        spawnWave(-72f)
        spawnWave(-106f)
        spawnAccumulator = 0f
    }

    fun togglePause() {
        if (screenState != ScreenState.PLAYING) return
        soundEngine.playUiClick()
        isPaused = !isPaused
        activeModal = if (isPaused) ModalType.PAUSE else ModalType.NONE
    }

    fun moveLane(delta: Int) {
        if (screenState != ScreenState.PLAYING || isPaused) return
        val prev = targetLane
        targetLane = (targetLane + delta).coerceIn(-1, 1)
        if (targetLane != prev) {
            soundEngine.playSwipeWhoosh()
        }
    }

    fun triggerJump() {
        if (screenState != ScreenState.PLAYING || isPaused) return
        if (!isJumping) {
            isSliding = false
            slideTimer = 0f
            verticalVelocity = jumpPower
            isJumping = true
            soundEngine.playJumpWhoosh()
        }
    }

    fun triggerSlide() {
        if (screenState != ScreenState.PLAYING || isPaused) return
        if (isJumping) {
            verticalVelocity = -0.36f
        }
        isSliding = true
        slideTimer = 38f
        soundEngine.playSlideScrape()
    }

    fun triggerManualBoost() {
        if (screenState != ScreenState.PLAYING || isPaused) return
        boostMax = 260f + boostLevel * 70f
        boostTimer = boostMax
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
        val dt = dtNormalized.coerceIn(0.1f, 2.4f)
        animTimeSec += dt * 0.01667f

        val now = System.currentTimeMillis()
        if (toastMessage != null && now - toastTimestamp > 2400L) {
            toastMessage = null
        }
        floatingPopups.removeAll { now - it.createdAt > 850L }

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
                pt.z += speed * dt * 0.85f
                if (pt.z > 8f) pt.z = -105f
            }
        }

        if (screenState != ScreenState.PLAYING) {
            val previewSpeed = 0.24f * dt
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

        if (speed < maxSpeed) {
            speed = min(maxSpeed, speed + acceleration * dt)
        }
        val effectiveSpeed = (if (boostTimer > 0f) speed * 1.38f else speed) * dt
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

        distanceMeters += effectiveSpeed * 0.85f
        score += (effectiveSpeed * 3.6f * multiplierLevel).toInt()

        val desiredX = targetLane * laneWidth
        val dx = desiredX - playerX
        playerX += dx * min(1f, 0.26f * dt)

        if (isJumping) {
            playerY += verticalVelocity * dt
            verticalVelocity -= gravity * dt
            if (playerY <= 0f) {
                playerY = 0f
                verticalVelocity = 0f
                isJumping = false
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

            if (e.z > -1.25f && e.z < 1.25f && abs(e.x - playerX) < 1.15f) {
                var consumed = false
                when (e.type) {
                    EntityType.COIN, EntityType.DIAMOND -> {
                        consumed = true
                        runCoins += if (e.type == EntityType.DIAMOND) 2 else 1
                        score += 50 * multiplierLevel
                        soundEngine.playCoinChime()
                    }
                    EntityType.RUBY -> {
                        consumed = true
                        runRubies += 1
                        score += 250 * multiplierLevel
                        challengeGems = min(5, challengeGems + 1)
                        soundEngine.playRubyChime()
                        addPopup("+1 RUBY ♦", isPositive = true)
                    }
                    EntityType.POWERUP -> {
                        consumed = true
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
                        score += 400 * multiplierLevel
                        aura = min(100, aura + 15)
                        soundEngine.playPowerupSurge()
                        addPopup("AURA BOOST!", isPositive = true)
                    }
                    EntityType.GATE_HAZARD -> {
                        consumed = true
                        aura = max(10, aura - 18)
                        soundEngine.playImpact(isShatter = true)
                        addPopup("HAZARD!", isPositive = false)
                    }
                    EntityType.HURDLE_LOW -> {
                        if (playerBottomY < 0.85f) {
                            if (boostTimer > 0f) {
                                consumed = true
                                addPopup("SMASH!", isPositive = true)
                            } else if (hasShield) {
                                hasShield = false
                                consumed = true
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
                            if (boostTimer > 0f) {
                                consumed = true
                                addPopup("SMASH!", isPositive = true)
                            } else if (hasShield) {
                                hasShield = false
                                consumed = true
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
        val runDist = distanceMeters.toInt()
        val finalScore = score

        coins += runCoins
        rubies += runRubies
        challengeDist = min(2000, challengeDist + runDist)
        if (finalScore > highScores[selectedWorldIndex]) {
            highScores[selectedWorldIndex] = finalScore
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

    override fun onCleared() {
        super.onCleared()
        soundEngine.stopBgm()
    }
}
