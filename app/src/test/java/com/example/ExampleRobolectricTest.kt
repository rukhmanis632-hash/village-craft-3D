package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ModalType
import com.example.model.ScreenState
import com.example.viewmodel.GameViewModel
import org.junit.Assert.assertEquals
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
    fun `verify worlds characters and gameplay state transitions`() {
        val vm = GameViewModel()
        vm.soundEngine.soundEnabled = false
        assertEquals(ScreenState.MAIN_MENU, vm.screenState)
        assertEquals(0, vm.selectedWorldIndex)

        // Switch through all 4 worlds
        for (w in 0..3) {
            vm.selectWorld(w)
            assertEquals(w, vm.selectedWorldIndex)
        }

        // Select character
        vm.selectCharacter("kai")
        assertEquals("kai", vm.selectedCharId)

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
        vm.moveLane(1)
        vm.triggerJump()
        assertTrue(vm.isJumping)
        vm.stepFrame(1.0f)
        assertTrue(vm.distanceMeters > 0f)
    }
}
