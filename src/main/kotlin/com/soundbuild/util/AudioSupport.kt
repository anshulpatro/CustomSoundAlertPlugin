package com.soundbuild.util

import com.soundbuild.model.PlaybackError
import com.soundbuild.model.PlaybackResult
import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader
import java.io.File
import java.io.IOException
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.SourceDataLine
import javax.sound.sampled.UnsupportedAudioFileException
import kotlin.math.log10

/**
 * Stateless audio engine helpers.
 *
 * Everything here is pure or fully self-contained (no IDE/platform
 * dependencies), which keeps the actual decoding/playback logic unit-testable
 * and keeps [com.soundbuild.services.SoundPlayerService] focused on threading
 * and lifecycle concerns only (single responsibility).
 *
 * Format strategy:
 *  - WAV/AIFF/AU are decoded by the JDK's built-in providers.
 *  - MP3 is decoded by explicitly instantiating the mp3spi providers. We do
 *    NOT rely on [AudioSystem]'s `ServiceLoader` discovery because, inside an
 *    IDE plugin, the providers live on the plugin class loader and are not
 *    visible to the system-class-loader based discovery that `AudioSystem`
 *    uses.
 */
object AudioSupport {

    /** File extensions we are willing to attempt to play. */
    val SUPPORTED_EXTENSIONS: Set<String> = setOf("wav", "mp3")

    private const val BUFFER_SIZE = 8 * 1024

    /** Lower-cased file extension (without the dot), or "" when there is none. */
    fun extensionOf(nameOrPath: String): String =
        nameOrPath.substringAfterLast('.', "").lowercase()

    fun isSupportedExtension(nameOrPath: String): Boolean =
        extensionOf(nameOrPath) in SUPPORTED_EXTENSIONS

    /**
     * Maps a 0..100 volume percentage to a gain in decibels.
     *
     * 100% -> 0 dB (no attenuation), and the perceived loudness scales
     * logarithmically. 0% is treated as silence ([Float.NEGATIVE_INFINITY]),
     * which callers clamp to the line's minimum gain.
     */
    fun volumeToDecibels(volumePercent: Int): Float {
        val v = volumePercent.coerceIn(0, 100)
        if (v == 0) return Float.NEGATIVE_INFINITY
        return (20.0 * log10(v / 100.0)).toFloat()
    }

    /** Clamps a gain value into the [min, max] range advertised by a line. */
    fun clampGain(decibels: Float, min: Float, max: Float): Float =
        decibels.coerceIn(min, max)

    /**
     * Validates a configured path without touching the audio subsystem.
     *
     * @return the relevant [PlaybackError] or `null` when the path looks
     *         playable.
     */
    fun validate(path: String?): PlaybackError? {
        if (path.isNullOrBlank()) return PlaybackError.EMPTY_PATH
        val file = File(path)
        if (!file.exists() || !file.isFile) return PlaybackError.FILE_NOT_FOUND
        if (!isSupportedExtension(file.name)) return PlaybackError.UNSUPPORTED_FORMAT
        return null
    }

    /**
     * Decodes and plays the given file synchronously on the calling thread.
     *
     * This method never throws: every failure mode is converted into a
     * [PlaybackResult.Failure] so the caller can simply log it.
     */
    fun play(path: String?, volumePercent: Int): PlaybackResult {
        validate(path)?.let { return PlaybackResult.Failure(it, describe(it, path)) }

        val file = File(path!!)
        val extension = extensionOf(file.name)

        var stream: AudioInputStream? = null
        var line: SourceDataLine? = null
        return try {
            stream = openPcmStream(file, extension)
            val format = stream.format
            line = AudioSystem.getSourceDataLine(format)
            line.open(format)
            applyGain(line, volumePercent)
            line.start()

            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = stream.read(buffer, 0, buffer.size)
                if (read < 0) break
                line.write(buffer, 0, read)
            }
            line.drain()
            PlaybackResult.Success
        } catch (e: LineUnavailableException) {
            PlaybackResult.Failure(PlaybackError.NO_AUDIO_DEVICE, "No audio output device available", e)
        } catch (e: UnsupportedAudioFileException) {
            PlaybackResult.Failure(
                PlaybackError.UNSUPPORTED_FORMAT,
                "Unsupported or corrupted audio file: ${file.name}",
                e,
            )
        } catch (e: IOException) {
            PlaybackResult.Failure(PlaybackError.DECODE_FAILED, "Failed to read audio file: ${file.name}", e)
        } catch (e: RuntimeException) {
            // mp3spi/JLayer can raise unchecked exceptions on malformed input.
            PlaybackResult.Failure(PlaybackError.DECODE_FAILED, "Unexpected error playing ${file.name}", e)
        } finally {
            runCatching { line?.stop() }
            runCatching { line?.close() }
            runCatching { stream?.close() }
        }
    }

    /**
     * Opens an [AudioInputStream] of signed 16-bit PCM data ready to be written
     * to a [SourceDataLine], decoding MP3 on the way when necessary.
     */
    private fun openPcmStream(file: File, extension: String): AudioInputStream {
        val baseStream: AudioInputStream = if (extension == "mp3") {
            MpegAudioFileReader().getAudioInputStream(file)
        } else {
            AudioSystem.getAudioInputStream(file)
        }

        val base = baseStream.format
        // WAV is usually already signed PCM and can be streamed as-is.
        if (extension != "mp3" && base.encoding == AudioFormat.Encoding.PCM_SIGNED) {
            return baseStream
        }

        val target = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            base.sampleRate,
            16,
            base.channels,
            base.channels * 2,
            base.sampleRate,
            false,
        )
        return if (extension == "mp3") {
            MpegFormatConversionProvider().getAudioInputStream(target, baseStream)
        } else {
            AudioSystem.getAudioInputStream(target, baseStream)
        }
    }

    private fun applyGain(line: SourceDataLine, volumePercent: Int) {
        if (!line.isControlSupported(FloatControl.Type.MASTER_GAIN)) return
        val control = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        control.value = clampGain(volumeToDecibels(volumePercent), control.minimum, control.maximum)
    }

    private fun describe(error: PlaybackError, path: String?): String = when (error) {
        PlaybackError.EMPTY_PATH -> "No sound file configured"
        PlaybackError.FILE_NOT_FOUND -> "Sound file not found: $path"
        PlaybackError.UNSUPPORTED_FORMAT -> "Unsupported sound format: $path"
        PlaybackError.NO_AUDIO_DEVICE -> "No audio output device available"
        PlaybackError.DECODE_FAILED -> "Could not decode sound: $path"
    }
}
