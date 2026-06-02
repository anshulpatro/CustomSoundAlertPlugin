package com.soundbuild.util

import com.soundbuild.model.PlaybackError
import com.soundbuild.model.PlaybackResult
import com.soundbuild.model.SoundEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/** Pure-logic unit tests for the audio engine helpers. */
class AudioSupportTest {

    @Test
    fun `recognises supported extensions case-insensitively`() {
        assertTrue(AudioSupport.isSupportedExtension("alert.wav"))
        assertTrue(AudioSupport.isSupportedExtension("ALERT.WAV"))
        assertTrue(AudioSupport.isSupportedExtension("/some/path/done.mp3"))
        assertFalse(AudioSupport.isSupportedExtension("song.ogg"))
        assertFalse(AudioSupport.isSupportedExtension("no-extension"))
    }

    @Test
    fun `volume maps to decibels logarithmically`() {
        assertEquals(0.0f, AudioSupport.volumeToDecibels(100), 0.001f)
        assertEquals(-6.0206f, AudioSupport.volumeToDecibels(50), 0.01f)
        assertEquals(Float.NEGATIVE_INFINITY, AudioSupport.volumeToDecibels(0))
    }

    @Test
    fun `volume is clamped to the 0-100 range`() {
        assertEquals(0.0f, AudioSupport.volumeToDecibels(150), 0.001f)
        assertEquals(Float.NEGATIVE_INFINITY, AudioSupport.volumeToDecibels(-10))
    }

    @Test
    fun `gain is clamped to the line range`() {
        assertEquals(-80.0f, AudioSupport.clampGain(Float.NEGATIVE_INFINITY, -80.0f, 6.0f))
        assertEquals(6.0f, AudioSupport.clampGain(20.0f, -80.0f, 6.0f))
        assertEquals(-3.0f, AudioSupport.clampGain(-3.0f, -80.0f, 6.0f))
    }

    @Test
    fun `validate rejects blank paths`() {
        assertEquals(PlaybackError.EMPTY_PATH, AudioSupport.validate(null))
        assertEquals(PlaybackError.EMPTY_PATH, AudioSupport.validate("   "))
    }

    @Test
    fun `validate rejects missing files`() {
        assertEquals(
            PlaybackError.FILE_NOT_FOUND,
            AudioSupport.validate("/this/does/not/exist.wav"),
        )
    }

    @Test
    fun `validate rejects unsupported formats`(@TempDir dir: Path) {
        val file = File(dir.toFile(), "note.txt").apply { writeText("not audio") }
        assertEquals(PlaybackError.UNSUPPORTED_FORMAT, AudioSupport.validate(file.absolutePath))
    }

    @Test
    fun `validate accepts an existing supported file`(@TempDir dir: Path) {
        val file = File(dir.toFile(), "alert.wav").apply { writeBytes(byteArrayOf(0)) }
        assertNull(AudioSupport.validate(file.absolutePath))
    }

    @Test
    fun `play fails gracefully for missing files`() {
        val result = AudioSupport.play("/no/such/file.wav", 50)
        val failure = assertInstanceOf(PlaybackResult.Failure::class.java, result)
        assertEquals(PlaybackError.FILE_NOT_FOUND, failure.error)
    }

    @Test
    fun `play fails gracefully for corrupted audio`(@TempDir dir: Path) {
        // A .wav file containing garbage must not blow up the caller.
        val file = File(dir.toFile(), "broken.wav").apply { writeText("definitely not a wav") }
        val result = AudioSupport.play(file.absolutePath, 50)
        assertInstanceOf(PlaybackResult.Failure::class.java, result)
    }

    @Test
    fun `playResource fails gracefully for a missing bundled resource`() {
        val result = AudioSupport.playResource("/sounds/does-not-exist.mp3", 50)
        val failure = assertInstanceOf(PlaybackResult.Failure::class.java, result)
        assertEquals(PlaybackError.DECODE_FAILED, failure.error)
    }

    @Test
    fun `playResource rejects unsupported bundled formats`() {
        val result = AudioSupport.playResource("/sounds/note.txt", 50)
        val failure = assertInstanceOf(PlaybackResult.Failure::class.java, result)
        assertEquals(PlaybackError.UNSUPPORTED_FORMAT, failure.error)
    }

    @Test
    fun `every declared default sound is bundled on the classpath`() {
        val defaults = SoundEvent.entries.mapNotNull { it.defaultResource }.distinct()
        assertTrue(defaults.isNotEmpty(), "expected at least one bundled default")
        defaults.forEach { resource ->
            assertTrue(AudioSupport.isSupportedExtension(resource), "unsupported default: $resource")
            val stream = AudioSupport.javaClass.getResourceAsStream(resource)
            assertNotNull(stream, "bundled sound missing from classpath: $resource")
            stream?.close()
        }
    }
}
