package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.EntityType
import com.example.model.HOVERBOARDS
import com.example.model.ModalType
import com.example.model.PowerupKind
import com.example.model.RUNNER_CHARACTERS
import com.example.model.ScreenState
import com.example.model.WorldEntity
import com.example.viewmodel.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Horizon Dash", appName)
    }

    @Test
    fun `verify worlds 6 3D characters and gameplay state transitions`() {
        val vm = GameViewModel()
        vm.soundEngine.soundEnabled = false
        assertEquals(ScreenState.MAIN_MENU, vm.screenState)
        assertEquals(0, vm.selectedWorldIndex)

        // Switch through all 4 worlds
        for (w in 0..3) {
            vm.selectWorld(w)
            assertEquals(w, vm.selectedWorldIndex)
        }

        // Cycle through all 6 3D runner characters (Leo, Kai, Aria, Ignis, Nova, Kairo)
        for (hero in RUNNER_CHARACTERS) {
            vm.selectCharacter(hero.id)
            assertEquals(hero.id, vm.selectedCharId)
            assertEquals(hero.name, vm.currentCharacter.name)
        }

        // Select hoverboard
        vm.selectOrBuyBoard(HOVERBOARDS[1])
        assertEquals(HOVERBOARDS[1].id, vm.selectedBoardId)

        // Open Story & 3D Heroes modal
        vm.openModal(ModalType.STORY_LORE)
        assertEquals(ModalType.STORY_LORE, vm.activeModal)
        vm.closeModal()

        // Open Upgrades modal & upgrade magnet
        vm.openModal(ModalType.UPGRADES)
        assertEquals(ModalType.UPGRADES, vm.activeModal)
        val prevMagnet = vm.magnetLevel
        vm.upgradeStat("magnet")
        assertEquals(prevMagnet + 1, vm.magnetLevel)
        vm.closeModal()

        // Start a game run and advance frames
        vm.startGame()
        assertEquals(ScreenState.PLAYING, vm.screenState)
        assertTrue(vm.isIntroCutsceneActive)
        vm.moveLane(1)
        assertFalse(vm.isIntroCutsceneActive)
        assertEquals(1, vm.targetLane)
        vm.triggerJump()
        assertTrue(vm.isJumping)
        vm.stepFrame(1.0f)
        assertTrue(vm.distanceMeters > 0f)
    }

    @Test
    fun `verify 3-second intro cutscene lifecycle and tap-to-skip`() {
        val vm = GameViewModel()
        vm.soundEngine.soundEnabled = false
        vm.startGame()

        // Initially in 3.0s intro cutscene
        assertTrue(vm.isIntroCutsceneActive)
        assertEquals(3.0f, vm.introCutsceneTimerSec, 0.01f)
        assertFalse(vm.introSurgePlayed)

        // Step ~1.2 seconds (75 frames at dt=1.0) -> Chrono Core charge triggers
        repeat(75) { vm.stepFrame(1.0f) }
        assertTrue(vm.introSurgePlayed)
        assertTrue(vm.isIntroCutsceneActive)

        // Step remaining frames past 3.0 seconds -> Cutscene completes seamlessly into running
        repeat(120) { vm.stepFrame(1.0f) }
        assertFalse(vm.isIntroCutsceneActive)

        // Verify Tap-to-Skip on a fresh run
        vm.startGame()
        assertTrue(vm.isIntroCutsceneActive)
        vm.skipIntroCutscene()
        assertFalse(vm.isIntroCutsceneActive)
    }

    @Test
    fun `verify shield protection and clean obstacle collision without monster`() {
        val vm = GameViewModel()
        vm.soundEngine.soundEnabled = false
        vm.startGame()
        vm.skipIntroCutscene()
        vm.entities.clear()

        // 1. Grant Shield powerup and hit a low hurdle -> Shield absorbs crash!
        vm.entities.add(
            WorldEntity(
                id = 500L,
                lane = 0,
                x = 0f,
                y = 1.0f,
                z = 0f,
                type = EntityType.POWERUP,
                powerupKind = PowerupKind.SHIELD
            )
        )
        vm.stepFrame(0.2f)
        assertTrue(vm.hasShield)

        vm.entities.clear()
        vm.entities.add(
            WorldEntity(
                id = 501L,
                lane = 0,
                x = 0f,
                y = 0f,
                z = 0f,
                type = EntityType.HURDLE_LOW
            )
        )
        vm.stepFrame(0.2f)
        assertFalse(vm.hasShield)
        assertEquals(ScreenState.PLAYING, vm.screenState)

        // 2. Hit a barrier without Shield -> Clean arcade Game Over with Close-Call banner
        vm.entities.clear()
        vm.entities.add(
            WorldEntity(
                id = 502L,
                lane = 0,
                x = 0f,
                y = 0f,
                z = 0f,
                type = EntityType.BARRIER_HIGH
            )
        )
        vm.stepFrame(0.2f)
        assertEquals(ScreenState.GAME_OVER, vm.screenState)
        assertTrue(vm.closeCallHeadline.contains("SO CLOSE"))
    }

    @Test
    fun `verify dimensional rift portal world warp`() {
        val vm = GameViewModel()
        vm.soundEngine.soundEnabled = false
        vm.selectWorld(0)
        vm.startGame()
        vm.skipIntroCutscene()
        vm.entities.clear()

        assertEquals(0, vm.selectedWorldIndex)

        // Place a Dimensional Portal right at player's position (z = 0f, x = 0f) and step frame
        vm.entities.add(
            WorldEntity(
                id = 999L,
                lane = 0,
                x = 0f,
                y = 0f,
                z = 0f,
                type = EntityType.DIMENSIONAL_PORTAL
            )
        )
        vm.stepFrame(0.5f)

        // World should warp from 0 (Ancient Ruins) -> 1 (Cyber City)!
        assertEquals(1, vm.selectedWorldIndex)
        assertTrue(vm.portalMorphFlashTimer > 0f)
        assertNotNull(vm.activeRadioTransmission)
        assertTrue(vm.activeRadioTransmission!!.message.contains("CYBER CITY"))
    }

    @Test
    fun `verify distance milestone radio comms and preferences persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = GameViewModel()
        vm.soundEngine.soundEnabled = false
        vm.startGame()
        vm.skipIntroCutscene()

        // Step frames until distance crosses 100m milestone
        var safetyCounter = 0
        while (vm.distanceMeters < 105f && safetyCounter < 1000) {
            vm.entities.clear()
            vm.stepFrame(2.0f)
            safetyCounter++
        }
        assertTrue(vm.distanceMeters >= 100f)
        assertNotNull(vm.activeRadioTransmission)
        assertTrue(vm.activeRadioTransmission!!.callSign.contains("100m"))

        // Save and reload preferences
        vm.saveToPrefs(context)
        val vmReloaded = GameViewModel()
        vmReloaded.soundEngine.soundEnabled = false
        vmReloaded.loadFromPrefs(context)
        assertEquals(vm.coins, vmReloaded.coins)
        assertEquals(vm.magnetLevel, vmReloaded.magnetLevel)
    }
}
