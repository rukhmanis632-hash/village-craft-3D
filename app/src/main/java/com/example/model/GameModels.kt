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
    INSPECT_3D,
    STORY_LORE
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

/**
 * Iconic 3D Endless Runner Character Archetypes inspired by top-charting 3D mobile runner games
 * (Subway Surfers, Sonic Dash, Blades of Brim, Jetpack Joyride):
 * - STREET_REBEL: Backwards Street Cap + Studio Headphones + Layered Hoodie + Chunky High-Tops
 * - CYBER_NINJA: Spiky Anime Hair + Neon Visor + Twin Flowing 3D Energy Scarf Ribbons
 * - VALKYRIE_HERO: Floating Radiant Halo Crown + Hard-Light Energy Wings + Paladin Pauldrons
 * - MECHA_STRIKER: Horned Cyber-Helm + Dual Plasma Jetpack Thrusters with Live Flame Cones
 * - KITSUNE_MASCOT: Pointed 3D Neon Fox Ears + Cyber Mask + Twin Swaying 3D Kitsune Tails
 * - ROYAL_FLEX: Floating 3D Gold Crown + Luxury Varsity Jacket + Diamond Aura & Gold Kicks
 */
enum class CharacterStyle {
    STREET_REBEL,
    CYBER_NINJA,
    VALKYRIE_HERO,
    MECHA_STRIKER,
    KITSUNE_MASCOT,
    ROYAL_FLEX
}

data class RunnerCharacter(
    val id: String,
    val name: String,
    val hiName: String,
    val roleTitle: String,
    val signatureGear: String,
    val badgeEmoji: String,
    val style: CharacterStyle,
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
        name = "Street Rebel Leo",
        hiName = "स्ट्रीट रेबेल लियो",
        roleTitle = "Urban Parkour Surfer",
        signatureGear = "Street Cap • Studio Headphones • Spray Pack",
        badgeEmoji = "🎧",
        style = CharacterStyle.STREET_REBEL,
        shirtColor = Color(0xFF1E6BE6),
        shirtHighlight = Color(0xFF4D94FF),
        pantsColor = Color(0xFF222634),
        pantsStripe = Color(0xFF64FFDA),
        backpackColor = Color(0xFFD93829),
        backpackFlapColor = Color(0xFFFFB700),
        skinColor = Color(0xFFD99B72),
        skinShadow = Color(0xFFB37852),
        hairColor = Color(0xFF3B2314),
        shoeColor = Color(0xFFFF2E55),
        shoeSoleColor = Color(0xFFF2F5FA),
        gloveColor = Color(0xFF232733),
        accentGlow = Color(0xFFFFD03B),
        speedRating = 94,
        auraBonus = 12
    ),
    RunnerCharacter(
        id = "kai",
        name = "Cyber Ninja Kai",
        hiName = "साइबर निंजा काई",
        roleTitle = "Supersonic Shinobi",
        signatureGear = "Spiky Hair • Cyber Visor • Twin Energy Scarf",
        badgeEmoji = "⚡",
        style = CharacterStyle.CYBER_NINJA,
        shirtColor = Color(0xFF0E2A47),
        shirtHighlight = Color(0xFF00F0FF),
        pantsColor = Color(0xFF141829),
        pantsStripe = Color(0xFF00F0FF),
        backpackColor = Color(0xFF2B133D),
        backpackFlapColor = Color(0xFFFF007F),
        skinColor = Color(0xFFE0AC8B),
        skinShadow = Color(0xFFB88465),
        hairColor = Color(0xFF00E5FF),
        shoeColor = Color(0xFF00E5FF),
        shoeSoleColor = Color(0xFFFF007F),
        gloveColor = Color(0xFF12182B),
        accentGlow = Color(0xFF00F0FF),
        speedRating = 99,
        auraBonus = 18
    ),
    RunnerCharacter(
        id = "aria",
        name = "Solar Valkyrie Aria",
        hiName = "सोलर वाल्किरी आरिया",
        roleTitle = "Astral Wing Knight",
        signatureGear = "Floating Halo • Hard-Light Wings • Aegis Armor",
        badgeEmoji = "🪽",
        style = CharacterStyle.VALKYRIE_HERO,
        shirtColor = Color(0xFFEAF6FF),
        shirtHighlight = Color(0xFF70D6FF),
        pantsColor = Color(0xFF1D4E89),
        pantsStripe = Color(0xFFFFD43B),
        backpackColor = Color(0xFF246A9E),
        backpackFlapColor = Color(0xFFFFE066),
        skinColor = Color(0xFFF0C4A8),
        skinShadow = Color(0xFFC99B7E),
        hairColor = Color(0xFFFFD15C),
        shoeColor = Color(0xFF38B6FF),
        shoeSoleColor = Color(0xFFFFFFFF),
        gloveColor = Color(0xFF1B3B5C),
        accentGlow = Color(0xFF5CD6FF),
        speedRating = 97,
        auraBonus = 22
    ),
    RunnerCharacter(
        id = "ignis",
        name = "Blaze Striker Ignis",
        hiName = "ब्लेज़ स्ट्राइकर इग्निस",
        roleTitle = "Jetpack Vanguard",
        signatureGear = "Horned Visor Helm • Dual Plasma Jet-Thrusters",
        badgeEmoji = "🚀",
        style = CharacterStyle.MECHA_STRIKER,
        shirtColor = Color(0xFFC43000),
        shirtHighlight = Color(0xFFFF6A00),
        pantsColor = Color(0xFF241512),
        pantsStripe = Color(0xFFFF8800),
        backpackColor = Color(0xFF3A1E14),
        backpackFlapColor = Color(0xFFFFAE00),
        skinColor = Color(0xFFB87A56),
        skinShadow = Color(0xFF8F593A),
        hairColor = Color(0xFF24110B),
        shoeColor = Color(0xFFFF4500),
        shoeSoleColor = Color(0xFFFFCC4D),
        gloveColor = Color(0xFF38190E),
        accentGlow = Color(0xFFFF6600),
        speedRating = 96,
        auraBonus = 20
    ),
    RunnerCharacter(
        id = "nova",
        name = "Kitsune Spirit Nova",
        hiName = "कित्सुने स्पिरिट नोवा",
        roleTitle = "Neon Fox Mascot",
        signatureGear = "3D Neon Fox Ears • Twin Glowing Kitsune Tails",
        badgeEmoji = "🦊",
        style = CharacterStyle.KITSUNE_MASCOT,
        shirtColor = Color(0xFFB57CFF),
        shirtHighlight = Color(0xFF64FFDA),
        pantsColor = Color(0xFF18142C),
        pantsStripe = Color(0xFFFF6EC7),
        backpackColor = Color(0xFF2C1E4A),
        backpackFlapColor = Color(0xFF64FFDA),
        skinColor = Color(0xFFE8B49A),
        skinShadow = Color(0xFFC28C72),
        hairColor = Color(0xFFFF5E9E),
        shoeColor = Color(0xFFFF36AB),
        shoeSoleColor = Color(0xFF64FFDA),
        gloveColor = Color(0xFF1F1638),
        accentGlow = Color(0xFF64FFDA),
        speedRating = 99,
        auraBonus = 25
    ),
    RunnerCharacter(
        id = "kairo",
        name = "Royal Legend Kairo",
        hiName = "रॉयल लीजेंड काइरो",
        roleTitle = "Golden Crown Champion",
        signatureGear = "3D Gold Crown • Diamond Bomber • Gold Kicks",
        badgeEmoji = "👑",
        style = CharacterStyle.ROYAL_FLEX,
        shirtColor = Color(0xFF2A1F0D),
        shirtHighlight = Color(0xFFFFD700),
        pantsColor = Color(0xFF17130C),
        pantsStripe = Color(0xFFFFD700),
        backpackColor = Color(0xFF5C430E),
        backpackFlapColor = Color(0xFFFFE55C),
        skinColor = Color(0xFFC6865E),
        skinShadow = Color(0xFF99623F),
        hairColor = Color(0xFF1A1410),
        shoeColor = Color(0xFFFFC700),
        shoeSoleColor = Color(0xFF64FFDA),
        gloveColor = Color(0xFF2B200D),
        accentGlow = Color(0xFFFFE047),
        speedRating = 100,
        auraBonus = 30
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
    HoverboardSpec("frost", "Cryo Wing Glider", "क्रायो विंग ग्लाइडर", Color(0xFFDFF6FF), Color(0xFF38B6FF), 2200),
    HoverboardSpec("vapor", "Vaporwave Pulse", "वेपरवेव पल्स डेक", Color(0xFFB57CFF), Color(0xFF64FFDA), 1500)
)

enum class EntityType {
    COIN,
    RUBY,
    DIAMOND,
    POWERUP,
    HURDLE_LOW,
    BARRIER_HIGH,
    GATE_BOOST,
    GATE_HAZARD,
    DIMENSIONAL_PORTAL
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
    var rotationY: Float = 0f,
    var nearMissChecked: Boolean = false
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

enum class FlashMissionType {
    COINS_SPRINT,
    NEAR_MISS_STREAK,
    ACROBAT_COINS,
    CLEAN_DASH_METERS
}

data class FlashMission(
    val id: Int,
    val type: FlashMissionType,
    val title: String,
    val hiTitle: String,
    val target: Int,
    var progress: Int = 0,
    var timeRemainingSec: Float = 30f,
    val maxTimeSec: Float = 30f,
    val rewardCoins: Int = 350,
    val rewardRubies: Int = 2
)

data class ConfettiParticle(
    var xRatio: Float,
    var yRatio: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    var rotationDeg: Float,
    val sizeDp: Float
)

data class RadioTransmission(
    val id: Long,
    val speaker: String,
    val callSign: String,
    val message: String,
    val hiMessage: String,
    val accentColor: Color,
    val createdAtMs: Long = System.currentTimeMillis()
)

data class LoreComicPanel(
    val panelNumber: Int,
    val title: String,
    val hiTitle: String,
    val caption: String,
    val dialogueQuote: String,
    val primaryColor: Color,
    val accentColor: Color,
    val badgeEmoji: String
)

val CHRONO_LORE_PANELS = listOf(
    LoreComicPanel(
        panelNumber = 1,
        title = "PANEL I: THE CHRONO CORE",
        hiTitle = "१. क्रोनो कोर की खोज",
        caption = "Deep inside the overgrown Sunlit Temple of the Ancient Ruins, Street Rebel Leo unseals the levitating Chrono Core from its inscribed stone pedestal.",
        dialogueQuote = "\"Energy readings are off the charts! The artifact is supercharging our speed!\"",
        primaryColor = Color(0xFF1E3F2B),
        accentColor = Color(0xFF64FFDA),
        badgeEmoji = "💎"
    ),
    LoreComicPanel(
        panelNumber = 2,
        title = "PANEL II: LEGENDS OF THE HORIZON",
        hiTitle = "२. होराइजन के 3D दिग्गज",
        caption = "Six elite 3D runners unite—Street Rebel Leo, Cyber Ninja Kai, Solar Valkyrie Aria, Blaze Striker Ignis, Kitsune Spirit Nova, and Royal Legend Kairo!",
        dialogueQuote = "\"Pick your 3D Hero, lace up your neon kicks, and ignite the track!\"",
        primaryColor = Color(0xFF1F2442),
        accentColor = Color(0xFFFFD03B),
        badgeEmoji = "👟"
    ),
    LoreComicPanel(
        panelNumber = 3,
        title = "PANEL III: 8x HYPER RUN OVERDRIVE",
        hiTitle = "३. 8x हाइपर रन और नियर-मिस",
        caption = "Chain split-second Near-Miss dodges and Flash Missions to charge your Flow Meter from 1x to 8x HYPER RUN overdrive with custom 3D trails!",
        dialogueQuote = "\"Dodge at the last split-second to trigger slow-mo sparks and massive combos!\"",
        primaryColor = Color(0xFF3A1228),
        accentColor = Color(0xFFFF2E93),
        badgeEmoji = "⚡"
    ),
    LoreComicPanel(
        panelNumber = 4,
        title = "PANEL IV: DIMENSIONAL RIFT PORTALS",
        hiTitle = "४. आयामी पोर्टल और ४ दुनिया",
        caption = "As the Chrono Core resonates, Dimensional Portals tear open across Ancient Ruins, Cyber City, Lava Cave, and Snowy Peak!",
        dialogueQuote = "\"Chrono Core resonating... Portal opening ahead! Leap through the rift!\"",
        primaryColor = Color(0xFF141E46),
        accentColor = Color(0xFF00F0FF),
        badgeEmoji = "🌀"
    )
)
