package com.example.model

import androidx.compose.ui.graphics.Color

enum class ScreenState {
    MAIN_MENU,
    PLAYING,
    WORLDS_AND_CHALLENGES,
    GAME_OVER
}

enum class ModalType {
    NONE,
    UPGRADES,
    CHARACTERS,
    BOARDS,
    LEADERBOARD,
    SETTINGS,
    SHOP,
    PAUSE,
    INSPECT_3D
}

enum class RoadStyle {
    RUINS,
    CYBER,
    LAVA,
    SNOW
}

data class WorldTheme(
    val id: Int,
    val name: String,
    val shortName: String,
    val hiName: String,
    val subtitle: String,
    val roadStyle: RoadStyle,
    val skyTop: Color,
    val skyMid: Color,
    val skyHorizon: Color,
    val fogColor: Color,
    val groundOuter: Color,
    val groundInner: Color,
    val roadBase: Color,
    val roadTilePrimary: Color,
    val roadTileSecondary: Color,
    val roadSeam: Color,
    val curbColor: Color,
    val curbAccent: Color,
    val hurdlePrimary: Color,
    val hurdleSecondary: Color,
    val barrierPrimary: Color,
    val barrierAccent: Color,
    val particleColor: Color,
    val godRayColor: Color
)

val WORLD_THEMES = listOf(
    WorldTheme(
        id = 0,
        name = "1. ANCIENT RUINS",
        shortName = "ANCIENT RUINS",
        hiName = "प्राचीन खंडहर",
        subtitle = "Sunlit Jungle Temple & Mossy Flagstones",
        roadStyle = RoadStyle.RUINS,
        skyTop = Color(0xFF2E5B44),
        skyMid = Color(0xFF68A67D),
        skyHorizon = Color(0xFFCDE6B5),
        fogColor = Color(0xFF9FD4A7),
        groundOuter = Color(0xFF1E3F1B),
        groundInner = Color(0xFF356528),
        roadBase = Color(0xFF4B6933),
        roadTilePrimary = Color(0xFF9E967E),
        roadTileSecondary = Color(0xFF88816B),
        roadSeam = Color(0xFF425B2B),
        curbColor = Color(0xFFBDB092),
        curbAccent = Color(0xFF7D7258),
        hurdlePrimary = Color(0xFFD93829),
        hurdleSecondary = Color(0xFFF2ECE1),
        barrierPrimary = Color(0xFF8C826B),
        barrierAccent = Color(0xFF567D38),
        particleColor = Color(0xFFFFE875),
        godRayColor = Color(0x55FFE999)
    ),
    WorldTheme(
        id = 1,
        name = "2. CYBER CITY",
        shortName = "CYBER CITY",
        hiName = "साइबर सिटी",
        subtitle = "Neon Megacity Highway & Hologram Skyline",
        roadStyle = RoadStyle.CYBER,
        skyTop = Color(0xFF060919),
        skyMid = Color(0xFF11193B),
        skyHorizon = Color(0xFF281B54),
        fogColor = Color(0xFF1C224A),
        groundOuter = Color(0xFF070A17),
        groundInner = Color(0xFF0E1329),
        roadBase = Color(0xFF12172E),
        roadTilePrimary = Color(0xFF181F3D),
        roadTileSecondary = Color(0xFF141A33),
        roadSeam = Color(0xFF00F0FF),
        curbColor = Color(0xFF232C54),
        curbAccent = Color(0xFFFF00AA),
        hurdlePrimary = Color(0xFF00F0FF),
        hurdleSecondary = Color(0xFFFF00AA),
        barrierPrimary = Color(0xFFFFE600),
        barrierAccent = Color(0xFF00F0FF),
        particleColor = Color(0xFF00F0FF),
        godRayColor = Color(0x33FF00AA)
    ),
    WorldTheme(
        id = 2,
        name = "3. LAVA CAVE",
        shortName = "LAVA CAVE",
        hiName = "लावा गुफा",
        subtitle = "Abyssal Magma Cavern & Basalt Viaduct",
        roadStyle = RoadStyle.LAVA,
        skyTop = Color(0xFF1A0500),
        skyMid = Color(0xFF450E02),
        skyHorizon = Color(0xFF9E2A04),
        fogColor = Color(0xFF6B1902),
        groundOuter = Color(0xFFFF3B00),
        groundInner = Color(0xFFFF8800),
        roadBase = Color(0xFFFF4D00),
        roadTilePrimary = Color(0xFF2D2320),
        roadTileSecondary = Color(0xFF211917),
        roadSeam = Color(0xFFFF6600),
        curbColor = Color(0xFF3B2822),
        curbAccent = Color(0xFFFF5500),
        hurdlePrimary = Color(0xFFFF5500),
        hurdleSecondary = Color(0xFF4A2511),
        barrierPrimary = Color(0xFF3D2314),
        barrierAccent = Color(0xFFFF7700),
        particleColor = Color(0xFFFF8800),
        godRayColor = Color(0x44FF4500)
    ),
    WorldTheme(
        id = 3,
        name = "4. SNOWY PEAK",
        shortName = "SNOWY PEAK",
        hiName = "बर्फीली चोटी",
        subtitle = "Glacial Summit Trail & Timber Bridges",
        roadStyle = RoadStyle.SNOW,
        skyTop = Color(0xFF4B8BBE),
        skyMid = Color(0xFF8CC5F2),
        skyHorizon = Color(0xFFE6F5FF),
        fogColor = Color(0xFFD6EEFF),
        groundOuter = Color(0xFFDCEEFF),
        groundInner = Color(0xFFF2F9FF),
        roadBase = Color(0xFFE5F3FF),
        roadTilePrimary = Color(0xFF755236),
        roadTileSecondary = Color(0xFF61422A),
        roadSeam = Color(0xFFD8EEFF),
        curbColor = Color(0xFFFFFFFF),
        curbAccent = Color(0xFF6CCDF7),
        hurdlePrimary = Color(0xFF38B6FF),
        hurdleSecondary = Color(0xFFB8ECFF),
        barrierPrimary = Color(0xFF6E4726),
        barrierAccent = Color(0xFFEFF8FF),
        particleColor = Color(0xFFFFFFFF),
        godRayColor = Color(0x44FFFFFF)
    )
)

data class RunnerCharacter(
    val id: String,
    val name: String,
    val hiName: String,
    val roleTitle: String,
    val shirtColor: Color,
    val shirtHighlight: Color,
    val pantsColor: Color,
    val pantsStripe: Color,
    val backpackColor: Color,
    val backpackFlapColor: Color,
    val skinColor: Color,
    val skinShadow: Color,
    val hairColor: Color,
    val shoeColor: Color,
    val shoeSoleColor: Color,
    val gloveColor: Color,
    val accentGlow: Color,
    val speedRating: Int,
    val auraBonus: Int
)

val RUNNER_CHARACTERS = listOf(
    RunnerCharacter(
        id = "leo",
        name = "Explorer Leo",
        hiName = "खोजकर्ता लियो",
        roleTitle = "Relic Pathfinder",
        shirtColor = Color(0xFF1E6BE6),
        shirtHighlight = Color(0xFF4D94FF),
        pantsColor = Color(0xFF282B36),
        pantsStripe = Color(0xFF4C5266),
        backpackColor = Color(0xFF3E612E),
        backpackFlapColor = Color(0xFF527A3E),
        skinColor = Color(0xFFD99B72),
        skinShadow = Color(0xFFB37852),
        hairColor = Color(0xFF3B2314),
        shoeColor = Color(0xFF1E6BE6),
        shoeSoleColor = Color(0xFFF2F5FA),
        gloveColor = Color(0xFF2E2622),
        accentGlow = Color(0xFFFFD03B),
        speedRating = 92,
        auraBonus = 10
    ),
    RunnerCharacter(
        id = "kai",
        name = "Cyber Ninja Kai",
        hiName = "साइबर निंजा काई",
        roleTitle = "Neon Phantom",
        shirtColor = Color(0xFF0E2A47),
        shirtHighlight = Color(0xFF00F0FF),
        pantsColor = Color(0xFF141829),
        pantsStripe = Color(0xFF00F0FF),
        backpackColor = Color(0xFF2B133D),
        backpackFlapColor = Color(0xFFFF007F),
        skinColor = Color(0xFFE0AC8B),
        skinShadow = Color(0xFFB88465),
        hairColor = Color(0xFF1A1F33),
        shoeColor = Color(0xFF00E5FF),
        shoeSoleColor = Color(0xFFFF007F),
        gloveColor = Color(0xFF12182B),
        accentGlow = Color(0xFF00F0FF),
        speedRating = 98,
        auraBonus = 15
    ),
    RunnerCharacter(
        id = "ignis",
        name = "Magma Vanguard",
        hiName = "मैग्मा योद्धा",
        roleTitle = "Volcanic Titan",
        shirtColor = Color(0xFFC43000),
        shirtHighlight = Color(0xFFFF6A00),
        pantsColor = Color(0xFF241512),
        pantsStripe = Color(0xFFFF8800),
        backpackColor = Color(0xFF4A2210),
        backpackFlapColor = Color(0xFFFFAE00),
        skinColor = Color(0xFFB87A56),
        skinShadow = Color(0xFF8F593A),
        hairColor = Color(0xFF24110B),
        shoeColor = Color(0xFFFF4500),
        shoeSoleColor = Color(0xFFFFCC4D),
        gloveColor = Color(0xFF38190E),
        accentGlow = Color(0xFFFF6600),
        speedRating = 95,
        auraBonus = 20
    ),
    RunnerCharacter(
        id = "aria",
        name = "Glacier Ranger",
        hiName = "ग्लेशियर रेंजर",
        roleTitle = "Summit Valkyrie",
        shirtColor = Color(0xFFEAF6FF),
        shirtHighlight = Color(0xFF70D6FF),
        pantsColor = Color(0xFF1D4E89),
        pantsStripe = Color(0xFF80DCFF),
        backpackColor = Color(0xFF246A9E),
        backpackFlapColor = Color(0xFF4CC9F0),
        skinColor = Color(0xFFF0C4A8),
        skinShadow = Color(0xFFC99B7E),
        hairColor = Color(0xFF593825),
        shoeColor = Color(0xFF38B6FF),
        shoeSoleColor = Color(0xFFFFFFFF),
        gloveColor = Color(0xFF1B3B5C),
        accentGlow = Color(0xFF5CD6FF),
        speedRating = 96,
        auraBonus = 18
    )
)

data class HoverboardSpec(
    val id: String,
    val name: String,
    val hiName: String,
    val primaryColor: Color,
    val glowColor: Color,
    val costCoins: Int
)

val HOVERBOARDS = listOf(
    HoverboardSpec("solar", "Solar Relic Board", "सोलर रेलिक बोर्ड", Color(0xFFFFB700), Color(0xFFFF5500), 0),
    HoverboardSpec("cyber", "Neon Cyber Deck", "नियॉन साइबर डेक", Color(0xFF00F0FF), Color(0xFFFF00AA), 1200),
    HoverboardSpec("magma", "Obsidian Drake", "ओब्सीडियन ड्रेक", Color(0xFF3B1A10), Color(0xFFFF3B00), 1800),
    HoverboardSpec("frost", "Cryo Wing Glider", "क्रायो विंग ग्लाइडर", Color(0xFFDFF6FF), Color(0xFF38B6FF), 2200)
)

enum class EntityType {
    COIN,
    RUBY,
    DIAMOND,
    POWERUP,
    HURDLE_LOW,
    BARRIER_HIGH,
    GATE_BOOST,
    GATE_HAZARD
}

enum class PowerupKind(val label: String, val emoji: String, val color: Color) {
    MAGNET("MAGNET", "🧲", Color(0xFFFF3B30)),
    SHIELD("SHIELD ACTIVE", "🛡️", Color(0xFF00D2FF)),
    BOOST("SPEED BOOST", "🥾", Color(0xFFFFB700))
}

data class WorldEntity(
    val id: Long,
    val lane: Int, // -1, 0, 1
    var x: Float,
    var y: Float,
    var z: Float,
    val type: EntityType,
    val powerupKind: PowerupKind? = null,
    var active: Boolean = true,
    var rotationY: Float = 0f
)

data class SceneryRow(
    var z: Float,
    val variant: Int,
    val hasArchway: Boolean = false
)

data class AmbientParticle(
    var x: Float,
    var y: Float,
    var z: Float,
    val size: Float,
    val speedMultiplier: Float
)

data class FloatingPopup(
    val id: Long,
    val text: String,
    val isPositive: Boolean,
    val isCoin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
