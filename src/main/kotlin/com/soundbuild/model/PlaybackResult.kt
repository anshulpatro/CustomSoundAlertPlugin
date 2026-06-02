package com.soundbuild.model

/**
 * The reason a sound could not be played. Used purely for diagnostics/logging —
 * playback failures are never surfaced to the user as dialogs.
 */
enum class PlaybackError {
    /** No file path is configured for the event. */
    EMPTY_PATH,

    /** The configured path does not point at an existing file. */
    FILE_NOT_FOUND,

    /** The file extension is not one of the supported audio formats. */
    UNSUPPORTED_FORMAT,

    /** No audio output line is available (e.g. a headless / CI environment). */
    NO_AUDIO_DEVICE,

    /** The file exists but could not be decoded (corrupted / truncated). */
    DECODE_FAILED,
}

/**
 * Outcome of an attempt to play a sound. Modelled as a sealed type so callers
 * can react exhaustively without resorting to exceptions for control flow.
 */
sealed interface PlaybackResult {

    /** The sound played to completion. */
    object Success : PlaybackResult

    /** The sound could not be played; [error] explains why. */
    data class Failure(
        val error: PlaybackError,
        val message: String,
        val cause: Throwable? = null,
    ) : PlaybackResult
}
