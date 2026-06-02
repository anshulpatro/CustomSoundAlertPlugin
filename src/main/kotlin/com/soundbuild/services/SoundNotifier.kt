package com.soundbuild.services

import com.intellij.openapi.diagnostic.logger
import com.soundbuild.model.SoundEvent
import com.soundbuild.settings.SoundSettingsState

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
     * Plays the sound configured for [event], honouring the global enable flag
     * and volume. Does nothing when the plugin is disabled or no sound is set.
     */
    fun play(event: SoundEvent) {
        val settings = SoundSettingsState.getInstance()
        val path = settings.pathFor(event)
        log.info("SoundNotifier.play(event=$event): enabled=${settings.enabled}, path='$path', volume=${settings.volume}")
        if (!settings.enabled) return
        SoundPlayerService.getInstance().play(path, settings.volume)
    }
}
