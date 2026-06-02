package com.soundbuild.listeners

import com.intellij.openapi.diagnostic.logger
import com.intellij.task.ProjectTaskListener
import com.intellij.task.ProjectTaskManager
import com.soundbuild.model.SoundEvent
import com.soundbuild.services.SoundNotifier

/**
 * Detects build completion via the modern [ProjectTaskListener] API.
 *
 * [ProjectTaskListener] is the supported, non-deprecated way to observe the
 * outcome of "Build", "Rebuild" and "Make" actions, including Gradle-delegated
 * builds in Android Studio and IntelliJ IDEA.
 *
 * Registered on the project message bus through `<projectListeners>` in
 * plugin.xml.
 */
class BuildEventListener : ProjectTaskListener {

    private val log = logger<BuildEventListener>()

    override fun finished(result: ProjectTaskManager.Result) {
        val hasErrors = result.hasErrors()
        log.info("Build finished: hasErrors=$hasErrors, isAborted=${result.isAborted}")

        // Check errors first: a failed build can also report as aborted, and a
        // build with errors is unambiguously a failure regardless.
        when {
            hasErrors -> SoundNotifier.play(SoundEvent.BUILD_FAILURE)
            result.isAborted -> Unit // genuinely cancelled — neither success nor failure
            else -> SoundNotifier.play(SoundEvent.BUILD_SUCCESS)
        }
    }
}
