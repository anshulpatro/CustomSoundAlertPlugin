package com.soundbuild.services

import com.intellij.openapi.diagnostic.logger
import com.soundbuild.model.SoundEvent
import com.soundbuild.settings.SoundSettingsState
import java.util.concurrent.ConcurrentHashMap

/**
 * Bridges IDE events to sound playback.
 *
 * Listeners depend only on this tiny façade rather than on the settings store
 * and the player directly, which keeps event-detection code free of any
 * knowledge about how sounds are configured or produced (dependency inversion).
 */
object SoundNotifier {

    private val log = logger<SoundNotifier>()

    /**
     * Minimum gap between two plays of the *same* event. Because a single build
     * can be observed by more than one listener (e.g. [com.soundbuild.listeners.BuildEventListener]
     * and [com.soundbuild.listeners.GradleBuildListener]), this coalesces those
     * near-simultaneous duplicates into one sound. Distinct builds are seconds
     * apart, so this never suppresses a genuinely separate result.
     */
    private const val DEBOUNCE_MS = 1000L
    private val lastPlayedAt = ConcurrentHashMap<SoundEvent, Long>()

    /**
     * Plays the sound configured for [event], honouring the global enable flag
     * and volume. Does nothing when the plugin is disabled or no sound is set.
     */
    fun play(event: SoundEvent) {
        val now = System.currentTimeMillis()
        val previous = lastPlayedAt.put(event, now)
        if (previous != null && now - previous < DEBOUNCE_MS) {
            log.info("Debounced duplicate $event (${now - previous} ms since last)")
            return
        }

        val settings = SoundSettingsState.getInstance()
        val path = settings.pathFor(event)
        log.info("SoundNotifier.play(event=$event): enabled=${settings.enabled}, path='$path', volume=${settings.volume}")
        if (!settings.enabled) return
        SoundPlayerService.getInstance().play(path, settings.volume)
    }
}
