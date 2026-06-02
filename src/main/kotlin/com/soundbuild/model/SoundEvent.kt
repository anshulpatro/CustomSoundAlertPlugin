package com.soundbuild.model

/**
 * The set of IDE events that the plugin can react to with a sound.
 *
 * Each constant carries the human readable label used on the settings screen,
 * and optionally a [defaultResource] — a classpath path to a sound bundled with
 * the plugin that is played when the user has not configured their own file for
 * this event. All four events ship with a built-in default sound (a success
 * clip for the success events, the configured clip for failures).
 */
enum class SoundEvent(
    val displayLabel: String,
    val defaultResource: String? = null,
) {
    BUILD_SUCCESS("Build success", "/sounds/success.mp3"),
    BUILD_FAILURE("Build failure", "/sounds/failure.mp3"),
    TEST_SUCCESS("Test success", "/sounds/success.mp3"),
    TEST_FAILURE("Test failure", "/sounds/failure.mp3"),
}
