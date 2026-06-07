package com.soundbuild.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.soundbuild.model.SoundEvent

/**
 * Persistent, application-level settings for the plugin.
 *
 * Implemented as a light service with [PersistentStateComponent]; the platform
 * serializes [State] to `CustomSoundAlertSettings.xml` in the IDE config
 * directory, so all preferences survive restarts.
 *
 * The nested [State] is a plain data class with default values — that is all
 * the XML serializer needs, and it makes the state trivially copyable for
 * tests.
 */
@Service(Service.Level.APP)
@State(
    name = "CustomSoundAlertSettings",
    storages = [Storage("CustomSoundAlertSettings.xml")],
)
class SoundSettingsState : PersistentStateComponent<SoundSettingsState.State> {

    data class State(
        var enabled: Boolean = true,
        var volume: Int = 80,
        // Per-event on/off. By default only failures sound (knowing something
        // broke is useful); success sounds are opt-in to avoid noise on every
        // green build. Users can toggle any of these on the settings screen.
        var buildSuccessEnabled: Boolean = false,
        var buildFailureEnabled: Boolean = true,
        var testSuccessEnabled: Boolean = false,
        var testFailureEnabled: Boolean = true,
        var buildSuccessPath: String = "",
        var buildFailurePath: String = "",
        var testSuccessPath: String = "",
        var testFailurePath: String = "",
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    // --- Convenience accessors (stable property refs for UI binding) -------

    var enabled: Boolean
        get() = state.enabled
        set(value) {
            state.enabled = value
        }

    var volume: Int
        get() = state.volume
        set(value) {
            state.volume = value.coerceIn(0, 100)
        }

    /** Whether sounds are enabled for [event] (independent of the master switch). */
    fun isEnabled(event: SoundEvent): Boolean = when (event) {
        SoundEvent.BUILD_SUCCESS -> state.buildSuccessEnabled
        SoundEvent.BUILD_FAILURE -> state.buildFailureEnabled
        SoundEvent.TEST_SUCCESS -> state.testSuccessEnabled
        SoundEvent.TEST_FAILURE -> state.testFailureEnabled
    }

    fun setEnabled(event: SoundEvent, value: Boolean) {
        when (event) {
            SoundEvent.BUILD_SUCCESS -> state.buildSuccessEnabled = value
            SoundEvent.BUILD_FAILURE -> state.buildFailureEnabled = value
            SoundEvent.TEST_SUCCESS -> state.testSuccessEnabled = value
            SoundEvent.TEST_FAILURE -> state.testFailureEnabled = value
        }
    }

    fun pathFor(event: SoundEvent): String = when (event) {
        SoundEvent.BUILD_SUCCESS -> state.buildSuccessPath
        SoundEvent.BUILD_FAILURE -> state.buildFailurePath
        SoundEvent.TEST_SUCCESS -> state.testSuccessPath
        SoundEvent.TEST_FAILURE -> state.testFailurePath
    }

    fun setPath(event: SoundEvent, path: String) {
        val trimmed = path.trim()
        when (event) {
            SoundEvent.BUILD_SUCCESS -> state.buildSuccessPath = trimmed
            SoundEvent.BUILD_FAILURE -> state.buildFailurePath = trimmed
            SoundEvent.TEST_SUCCESS -> state.testSuccessPath = trimmed
            SoundEvent.TEST_FAILURE -> state.testFailurePath = trimmed
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(): SoundSettingsState = service()
    }
}
