package com.example

import com.example.graphics.Camera3D
import com.example.model.CHRONO_LORE_PANELS
import com.example.model.CharacterStyle
import com.example.model.HOVERBOARDS
import com.example.model.RUNNER_CHARACTERS
import com.example.model.WORLD_THEMES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `verify worlds 3D character archetypes and lore catalog integrity`() {
        assertEquals(4, WORLD_THEMES.size)
        assertEquals(4, CHRONO_LORE_PANELS.size)
        assertEquals(6, RUNNER_CHARACTERS.size)
        assertTrue(HOVERBOARDS.size >= 4)

        // Verify all 6 distinct 3D character styles inspired by top 3D runner games
        val styles = RUNNER_CHARACTERS.map { it.style }.toSet()
        assertEquals(CharacterStyle.entries.toSet(), styles)
        assertEquals("Street Rebel Leo", RUNNER_CHARACTERS[0].name)
        assertEquals("Cyber Ninja Kai", RUNNER_CHARACTERS[1].name)
        assertEquals("Solar Valkyrie Aria", RUNNER_CHARACTERS[2].name)
        assertEquals("Blaze Striker Ignis", RUNNER_CHARACTERS[3].name)
        assertEquals("Kitsune Spirit Nova", RUNNER_CHARACTERS[4].name)
        assertEquals("Royal Legend Kairo", RUNNER_CHARACTERS[5].name)
    }

    @Test
    fun `verify 3D camera projection and FOV warp math`() {
        val camStandard = Camera3D(
            screenWidth = 1080f,
            screenHeight = 1920f,
            camX = 0f,
            camY = 5.0f,
            camZ = 8.6f,
            pitchRad = 0.23f,
            fovMultiplier = 1.0f
        )
        val projectedCenter = camStandard.project(0f, 1.0f, 0f)
        assertTrue(projectedCenter.visible)
        assertEquals(540f, projectedCenter.x, 1.0f)

        // Behind camera clipping plane should not project as visible
        val behindCamera = camStandard.project(0f, 1.0f, 15.0f)
        assertEquals(false, behindCamera.visible)
    }
}
