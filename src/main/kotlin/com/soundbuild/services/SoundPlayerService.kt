package com.soundbuild.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.serviceContainer.NonInjectable
import com.intellij.util.concurrency.AppExecutorUtil
import com.soundbuild.model.PlaybackResult
import com.soundbuild.util.AudioSupport
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException

/**
 * Application-level singleton that plays sounds off the UI thread.
 *
 * Registered as a light service via the [Service] annotation (see the absence
 * of any `<applicationService>` entry in plugin.xml — that is intentional).
 *
 * Responsibilities are deliberately narrow: thread management and lifecycle.
 * The actual decoding/playback lives in [AudioSupport], which keeps this class
 * trivially correct and the audio logic independently testable.
 *
 * Thread-safety: all playback is funnelled through a single-threaded bounded
 * executor, so sounds never overlap and there is no shared mutable state.
 */
@Service(Service.Level.APP)
class SoundPlayerService @TestOnly @NonInjectable internal constructor(
    private val executor: Executor,
) : Disposable {

    private val log = logger<SoundPlayerService>()

    /**
     * Constructor used by the platform's service container. The
     * [@NonInjectable][NonInjectable] annotation on the primary constructor
     * tells the container to instantiate the service through this one.
     */
    @Suppress("unused")
    constructor() : this(
        AppExecutorUtil.createBoundedApplicationPoolExecutor("CustomSoundAlert.SoundPlayer", 1),
    )

    /**
     * Schedules a user-chosen file [path] to be played asynchronously at the
     * given volume. Returns immediately; the UI thread is never blocked. A
     * blank/`null` path is a no-op.
     */
    fun play(path: String?, volumePercent: Int) {
        if (path.isNullOrBlank()) {
            log.info("No sound configured for this event; skipping playback")
            return
        }
        log.info("Scheduling playback (file): '$path' at volume $volumePercent")
        submit(path) { AudioSupport.play(path, volumePercent) }
    }

    /**
     * Schedules a sound bundled in the plugin jar (classpath [resourcePath]) to
     * be played asynchronously — used for the built-in default sounds.
     */
    fun playResource(resourcePath: String, volumePercent: Int) {
        log.info("Scheduling playback (bundled): '$resourcePath' at volume $volumePercent")
        submit(resourcePath) { AudioSupport.playResource(resourcePath, volumePercent) }
    }

    private fun submit(label: String, task: () -> PlaybackResult) {
        try {
            executor.execute {
                when (val result = task()) {
                    is PlaybackResult.Success ->
                        log.info("Played sound: $label")
                    is PlaybackResult.Failure ->
                        log.warn("Sound playback failed [${result.error}] for '$label': ${result.message}", result.cause)
                }
            }
        } catch (e: RejectedExecutionException) {
            // Happens only if the IDE is shutting down — safe to ignore.
            log.debug("Sound player is shutting down; dropping playback request", e)
        }
    }

    override fun dispose() {
        (executor as? ExecutorService)?.shutdownNow()
    }

    companion object {
        @JvmStatic
        fun getInstance(): SoundPlayerService = service()
    }
}
