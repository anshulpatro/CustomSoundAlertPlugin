package com.soundbuild.listeners

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.diagnostic.logger
import com.soundbuild.model.SoundEvent
import com.soundbuild.services.SoundNotifier

/**
 * Detects test-run completion via the SMTRunner framework, which backs every
 * modern test runner in the platform (JUnit, TestNG, Kotlin, Gradle test tasks,
 * Android instrumented/unit tests, ...).
 *
 * We extend [SMTRunnerEventsAdapter] (the no-op base implementation) and only
 * override the single callback we care about, so we stay forward-compatible if
 * new callbacks are added to the listener interface.
 *
 * Registered on the project message bus through `<projectListeners>` in
 * plugin.xml against the `SMTRunnerEventsListener` topic.
 */
class TestEventListener : SMTRunnerEventsAdapter() {

    private val log = logger<TestEventListener>()

    override fun onTestingFinished(testsRoot: SMTestProxy.SMRootTestProxy) {
        // Only consider actual leaf tests; ignore the synthetic root/suite
        // nodes and bail out when a run produced no tests at all.
        val leaves = testsRoot.allTests.filter { it.isLeaf }
        val anyFailed = leaves.any { it.isDefect }
        log.info("Testing finished: leafTests=${leaves.size}, anyFailed=$anyFailed")

        if (leaves.isEmpty()) return

        val event = if (anyFailed) SoundEvent.TEST_FAILURE else SoundEvent.TEST_SUCCESS
        SoundNotifier.play(event)
    }
}
