package com.soundbuild.settings

import com.soundbuild.model.SoundEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Verifies the [PersistentStateComponent][com.intellij.openapi.components.PersistentStateComponent]
 * contract: that what [SoundSettingsState.getState] produces can be restored by
 * [SoundSettingsState.loadState] on a fresh instance — i.e. settings survive an
 * IDE restart.
 */
class SoundSettingsStatePersistenceTest {

    @Test
    fun `defaults are sensible`() {
        val state = SoundSettingsState()
        assertTrue(state.enabled)
        assertEquals(80, state.volume)
        SoundEvent.entries.forEach { assertEquals("", state.pathFor(it)) }
    }

    @Test
    fun `state round-trips through get and load`() {
        val original = SoundSettingsState().apply {
            enabled = false
            volume = 33
            setPath(SoundEvent.BUILD_SUCCESS, "/sounds/build-ok.wav")
            setPath(SoundEvent.BUILD_FAILURE, "/sounds/build-fail.mp3")
            setPath(SoundEvent.TEST_SUCCESS, "/sounds/test-ok.wav")
            setPath(SoundEvent.TEST_FAILURE, "/sounds/test-fail.mp3")
        }

        // `.copy()` simulates the serialize/deserialize boundary: a fresh,
        // independent State object equal in content to the persisted one.
        val persisted = original.getState().copy()
        val restored = SoundSettingsState().apply { loadState(persisted) }

        assertEquals(false, restored.enabled)
        assertEquals(33, restored.volume)
        assertEquals("/sounds/build-ok.wav", restored.pathFor(SoundEvent.BUILD_SUCCESS))
        assertEquals("/sounds/build-fail.mp3", restored.pathFor(SoundEvent.BUILD_FAILURE))
        assertEquals("/sounds/test-ok.wav", restored.pathFor(SoundEvent.TEST_SUCCESS))
        assertEquals("/sounds/test-fail.mp3", restored.pathFor(SoundEvent.TEST_FAILURE))
    }

    @Test
    fun `volume is clamped to 0-100`() {
        val state = SoundSettingsState()
        state.volume = 150
        assertEquals(100, state.volume)
        state.volume = -20
        assertEquals(0, state.volume)
    }

    @Test
    fun `setPath trims whitespace`() {
        val state = SoundSettingsState()
        state.setPath(SoundEvent.TEST_SUCCESS, "  /sounds/ok.wav  ")
        assertEquals("/sounds/ok.wav", state.pathFor(SoundEvent.TEST_SUCCESS))
    }
}
