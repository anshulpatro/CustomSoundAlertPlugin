package com.soundbuild.listeners

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.soundbuild.model.SoundEvent
import com.soundbuild.services.SoundNotifier

/**
 * Detects **Gradle** build and project-sync outcomes via the external-system
 * task notifications.
 *
 * This is what Android Studio (and IntelliJ IDEA with Gradle) actually uses to
 * run builds and import/sync the project — those operations do NOT go through
 * the JPS-oriented [com.intellij.task.ProjectTaskListener], which is why a
 * Gradle build/sync result would otherwise go unnoticed.
 *
 * We implement [ExternalSystemTaskNotificationListener] directly (rather than
 * the deprecated `…ListenerAdapter`, whose constructor is scheduled for
 * removal). Only success/failure carry logic; the remaining members — which are
 * `abstract` on older platforms and `default` on newer ones — are no-ops.
 *
 * Mapping (both Gradle build tasks and project syncs):
 *  - `EXECUTE_TASK` / `RESOLVE_PROJECT` success -> build success
 *  - `EXECUTE_TASK` / `RESOLVE_PROJECT` failure -> build failure
 *
 * Every successful Gradle build/sync sounds. [SoundNotifier] debounces, so a
 * single action that triggers near-simultaneous events plays once, and it never
 * double-plays with [BuildEventListener].
 *
 * Registered via the `com.intellij.externalSystemTaskNotificationListener`
 * extension point.
 */
class GradleBuildListener : ExternalSystemTaskNotificationListener {

    private val log = logger<GradleBuildListener>()

    override fun onSuccess(id: ExternalSystemTaskId) {
        log.info("Gradle task success: type=${id.type}")
        if (id.type == ExternalSystemTaskType.EXECUTE_TASK ||
            id.type == ExternalSystemTaskType.RESOLVE_PROJECT
        ) {
            SoundNotifier.play(SoundEvent.BUILD_SUCCESS)
        }
    }

    override fun onFailure(id: ExternalSystemTaskId, e: Exception) {
        log.info("Gradle task failure: type=${id.type}")
        if (id.type == ExternalSystemTaskType.EXECUTE_TASK ||
            id.type == ExternalSystemTaskType.RESOLVE_PROJECT
        ) {
            SoundNotifier.play(SoundEvent.BUILD_FAILURE)
        }
    }

    // --- Remaining listener callbacks: intentionally no-ops -----------------
    override fun onStatusChange(event: ExternalSystemTaskNotificationEvent) = Unit
    override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) = Unit
    override fun onEnd(id: ExternalSystemTaskId) = Unit
    override fun beforeCancel(id: ExternalSystemTaskId) = Unit
    override fun onCancel(id: ExternalSystemTaskId) = Unit
}
