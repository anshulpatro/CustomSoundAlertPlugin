package com.soundbuild.model

/**
 * The set of IDE events that the plugin can react to with a sound.
 *
 * Each constant carries the human readable label used on the settings screen,
 * keeping the UI and the rest of the code in sync from a single source of
 * truth.
 */
enum class SoundEvent(val displayLabel: String) {
    BUILD_SUCCESS("Build success"),
    BUILD_FAILURE("Build failure"),
    TEST_SUCCESS("Test success"),
    TEST_FAILURE("Test failure"),
}
