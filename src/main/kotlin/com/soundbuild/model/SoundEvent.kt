package com.soundbuild.model

/**
 * The set of IDE events that the plugin can react to with a sound.
 *
 * Each constant carries the human readable label used on the settings screen,
 * and optionally a [defaultResource] — a classpath path to a sound bundled with
 * the plugin that is played when the user has not configured their own file for
 * this event. Failure events ship with a built-in default; success events do
 * not (silent unless the user picks a file).
 */
enum class SoundEvent(
    val displayLabel: String,
    val defaultResource: String? = null,
) {
    BUILD_SUCCESS("Build success"),
    BUILD_FAILURE("Build failure", "/sounds/mario_death_song.mp3"),
    TEST_SUCCESS("Test success"),
    TEST_FAILURE("Test failure", "/sounds/mario_death_song.mp3"),
}
