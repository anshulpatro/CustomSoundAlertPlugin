package com.soundbuild.services

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import java.util.concurrent.Executor

/**
 * Tests the service's threading/lifecycle behaviour using a synchronous
 * executor, so playback runs inline and assertions are deterministic without
 * starting the IDE or requiring an audio device.
 */
class SoundPlayerServiceTest {

    /** Runs submitted work immediately on the calling thread. */
    private val sameThreadExecutor = Executor { it.run() }

    @Test
    fun `blank paths are ignored`() {
        val service = SoundPlayerService(sameThreadExecutor)
        try {
            assertDoesNotThrow { service.play(null, 50) }
            assertDoesNotThrow { service.play("", 50) }
            assertDoesNotThrow { service.play("   ", 50) }
        } finally {
            service.dispose()
        }
    }

    @Test
    fun `missing file does not propagate exceptions`() {
        val service = SoundPlayerService(sameThreadExecutor)
        try {
            assertDoesNotThrow { service.play("/definitely/not/here.wav", 80) }
        } finally {
            service.dispose()
        }
    }

    @Test
    fun `dispose is safe to call`() {
        val service = SoundPlayerService(sameThreadExecutor)
        assertDoesNotThrow { service.dispose() }
    }
}
