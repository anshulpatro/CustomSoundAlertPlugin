package com.soundbuild.listeners

import com.intellij.task.ProjectTaskListener
import com.intellij.task.ProjectTaskManager
import com.soundbuild.model.SoundEvent
import com.soundbuild.services.SoundNotifier

/**
 * Detects build completion via the modern [ProjectTaskListener] API.
 *
 * [ProjectTaskListener] is the supported, non-deprecated way to observe the
 * outcome of "Build", "Rebuild" and "Make" actions. It is fired for both the
 * built-in JPS builder and Gradle/Maven delegated builds, so a single listener
 * covers IntelliJ IDEA and Android Studio alike.
 *
 * Registered on the project message bus through `<projectListeners>` in
 * plugin.xml.
 */
class BuildEventListener : ProjectTaskListener {

    override fun finished(result: ProjectTaskManager.Result) {
        // A cancelled build is neither a success nor a failure.
        if (result.isAborted) return

        val event = if (result.hasErrors()) SoundEvent.BUILD_FAILURE else SoundEvent.BUILD_SUCCESS
        SoundNotifier.play(event)
    }
}
