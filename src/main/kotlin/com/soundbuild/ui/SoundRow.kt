package com.soundbuild.ui

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBCheckBox
import com.soundbuild.model.SoundEvent
import com.soundbuild.services.SoundPlayerService
import com.soundbuild.util.AudioSupport
import javax.swing.JButton

/**
 * Reusable widget for a single sound setting:
 * `[✓ <event>]  [path field] [Browse]  [Test]`.
 *
 * The leading checkbox is the per-event on/off switch. When unchecked, the
 * event is silent (not even the bundled default plays) and the path field /
 * Test button are disabled. When checked, the configured file plays, or — if
 * the field is blank — the bundled default for the event.
 *
 * @param volumeProvider supplies the *current* (possibly unsaved) volume so the
 *        Test button previews exactly what the user is configuring.
 */
class SoundRow(
    private val event: SoundEvent,
    private val volumeProvider: () -> Int,
) {

    val enabledCheckBox: JBCheckBox = JBCheckBox(event.displayLabel)

    val pathField: TextFieldWithBrowseButton = TextFieldWithBrowseButton().apply {
        textField.toolTipText = buildString {
            append("Path to a .wav or .mp3 file")
            if (event.defaultResource != null) append(" — leave blank to use the built-in default")
        }
    }

    val testButton: JButton = JButton("Test")

    init {
        pathField.addActionListener { chooseFile() }
        testButton.addActionListener {
            val player = SoundPlayerService.getInstance()
            val chosen = path.ifEmpty { null }
            when {
                chosen != null -> player.play(chosen, volumeProvider())
                event.defaultResource != null -> player.playResource(event.defaultResource, volumeProvider())
            }
        }
        enabledCheckBox.addActionListener { syncEnabledState() }
        syncEnabledState()
    }

    /** Per-event on/off. */
    var soundEnabled: Boolean
        get() = enabledCheckBox.isSelected
        set(value) {
            enabledCheckBox.isSelected = value
            syncEnabledState()
        }

    /** Trimmed file path currently shown in the field. */
    var path: String
        get() = pathField.text.trim()
        set(value) {
            pathField.text = value
        }

    private fun syncEnabledState() {
        val on = enabledCheckBox.isSelected
        pathField.isEnabled = on
        testButton.isEnabled = on
    }

    private fun chooseFile() {
        val descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
            .withTitle("Select Sound File")
            .withDescription("Choose a WAV or MP3 audio file")
            .withFileFilter { vf -> AudioSupport.isSupportedExtension(vf.name) }

        val toSelect = path.takeIf { it.isNotEmpty() }
            ?.let { LocalFileSystem.getInstance().findFileByPath(it) }

        FileChooser.chooseFile(descriptor, null, toSelect)?.let { chosen ->
            pathField.text = chosen.path
        }
    }
}
