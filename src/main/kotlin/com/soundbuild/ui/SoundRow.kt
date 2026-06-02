package com.soundbuild.ui

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.LocalFileSystem
import com.soundbuild.model.SoundEvent
import com.soundbuild.services.SoundPlayerService
import com.soundbuild.util.AudioSupport
import javax.swing.JButton

/**
 * Reusable widget for a single sound setting: a path field with a Browse button
 * plus a Test button. Encapsulating it here keeps the settings panel
 * declarative and lets each event row behave identically.
 *
 * @param volumeProvider supplies the *current* (possibly unsaved) volume so the
 *        Test button previews exactly what the user is configuring.
 */
class SoundRow(
    @Suppress("unused") private val event: SoundEvent,
    private val volumeProvider: () -> Int,
) {

    val pathField: TextFieldWithBrowseButton = TextFieldWithBrowseButton().apply {
        textField.toolTipText = "Path to a .wav or .mp3 file"
    }

    val testButton: JButton = JButton("Test")

    init {
        // Drive the browse button ourselves so we can apply a format filter and
        // pre-select the currently configured file.
        pathField.addActionListener { chooseFile() }
        testButton.addActionListener {
            SoundPlayerService.getInstance().play(path.ifEmpty { null }, volumeProvider())
        }
    }

    /** Trimmed file path currently shown in the field. */
    var path: String
        get() = pathField.text.trim()
        set(value) {
            pathField.text = value
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
