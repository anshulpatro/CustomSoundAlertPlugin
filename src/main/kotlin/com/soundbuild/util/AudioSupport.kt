package com.soundbuild.util

import com.soundbuild.model.PlaybackError
import com.soundbuild.model.PlaybackResult
import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
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
 * Two sources are supported:
 *  - [play] — a user-chosen file on disk.
 *  - [playResource] — a sound bundled inside the plugin jar (the built-in
 *    defaults), read from the classpath.
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
     * Decodes and plays a file from disk, synchronously on the calling thread.
     * Never throws — every failure becomes a [PlaybackResult.Failure].
     */
    fun play(path: String?, volumePercent: Int): PlaybackResult {
        validate(path)?.let { return PlaybackResult.Failure(it, describe(it, path)) }
        val file = File(path!!)
        val extension = extensionOf(file.name)
        return playFrom(file.name, extension, volumePercent) { openBaseStream(file, extension) }
    }

    /**
     * Decodes and plays a sound bundled in the plugin jar (classpath
     * [resourcePath], e.g. `/sounds/default-error.mp3`), synchronously on the
     * calling thread. Never throws.
     */
    fun playResource(resourcePath: String, volumePercent: Int): PlaybackResult {
        val extension = extensionOf(resourcePath)
        if (!isSupportedExtension(resourcePath)) {
            return PlaybackResult.Failure(
                PlaybackError.UNSUPPORTED_FORMAT,
                "Unsupported bundled sound: $resourcePath",
            )
        }
        return playFrom(resourcePath, extension, volumePercent) {
            // Read the whole clip into memory and decode from a ByteArrayInputStream.
            // Bundled sounds are tiny, and this gives unlimited mark/reset support,
            // which the MP3 reader needs (a plain BufferedInputStream can fail its
            // probe with "Resetting to invalid mark").
            val bytes = (AudioSupport.javaClass.getResourceAsStream(resourcePath)
                ?: throw IOException("Bundled sound not found on classpath: $resourcePath"))
                .use { it.readBytes() }
            openBaseStream(ByteArrayInputStream(bytes), extension)
        }
    }

    /**
     * Core playback loop shared by [play] and [playResource]. [openBase] is
     * invoked inside the try-block so that I/O errors while opening are mapped
     * to the same [PlaybackResult.Failure] values as errors during playback.
     */
    private fun playFrom(
        sourceName: String,
        extension: String,
        volumePercent: Int,
        openBase: () -> AudioInputStream,
    ): PlaybackResult {
        var stream: AudioInputStream? = null
        var line: SourceDataLine? = null
        return try {
            stream = toSignedPcm(openBase(), extension)
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
                "Unsupported or corrupted audio: $sourceName",
                e,
            )
        } catch (e: IOException) {
            PlaybackResult.Failure(PlaybackError.DECODE_FAILED, "Failed to read audio: $sourceName", e)
        } catch (e: RuntimeException) {
            // mp3spi/JLayer can raise unchecked exceptions on malformed input.
            PlaybackResult.Failure(PlaybackError.DECODE_FAILED, "Unexpected error playing $sourceName", e)
        } finally {
            runCatching { line?.stop() }
            runCatching { line?.close() }
            runCatching { stream?.close() }
        }
    }

    private fun openBaseStream(file: File, extension: String): AudioInputStream =
        if (extension == "mp3") MpegAudioFileReader().getAudioInputStream(file)
        else AudioSystem.getAudioInputStream(file)

    private fun openBaseStream(input: InputStream, extension: String): AudioInputStream {
        // The MP3/WAV readers probe the stream and need mark/reset support.
        val markable = if (input.markSupported()) input else BufferedInputStream(input)
        return if (extension == "mp3") MpegAudioFileReader().getAudioInputStream(markable)
        else AudioSystem.getAudioInputStream(markable)
    }

    /** Converts a decoded base stream to signed 16-bit PCM ready for a line. */
    private fun toSignedPcm(baseStream: AudioInputStream, extension: String): AudioInputStream {
        val base = baseStream.format
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
