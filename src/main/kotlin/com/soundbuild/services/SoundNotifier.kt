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
        if (!settings.enabled) {
            log.info("SoundNotifier.play(event=$event): plugin disabled; skipping")
            return
        }
        if (!settings.isEnabled(event)) {
            log.info("SoundNotifier.play(event=$event): event disabled; skipping")
            return
        }

        val path = settings.pathFor(event)
        val player = SoundPlayerService.getInstance()
        val volume = settings.volume

        if (path.isNotBlank()) {
            // User-chosen file takes precedence.
            log.info("SoundNotifier.play(event=$event): user file='$path', volume=$volume")
            player.play(path, volume)
            return
        }

        // No user file: fall back to the bundled default sound, if this event
        // has one (failure events do; success events don't).
        val default = event.defaultResource
        if (default != null) {
            log.info("SoundNotifier.play(event=$event): built-in default '$default', volume=$volume")
            player.playResource(default, volume)
        } else {
            log.info("SoundNotifier.play(event=$event): no sound configured and no default")
        }
    }
}
