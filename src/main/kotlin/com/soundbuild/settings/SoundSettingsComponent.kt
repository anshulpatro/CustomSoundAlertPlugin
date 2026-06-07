package com.soundbuild.settings

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.soundbuild.model.SoundEvent
import com.soundbuild.ui.SoundRow
import javax.swing.JComponent

/**
 * Builds and owns the Swing UI for the settings screen and mediates between the
 * widgets and [SoundSettingsState].
 *
 * The Kotlin UI DSL is used purely for layout; modified/apply/reset is handled
 * explicitly against the live components, which keeps the data flow obvious and
 * avoids coupling to binding-API specifics.
 */
class SoundSettingsComponent {

    private val settings get() = SoundSettingsState.getInstance()

    private val enabledCheckBox = JBCheckBox("Enable Custom Sound Alert")
    private val volumeSpinner = JBIntSpinner(80, 0, 100)

    private val rows: Map<SoundEvent, SoundRow> =
        SoundEvent.entries.associateWith { event ->
            SoundRow(event) { volumeSpinner.number }
        }

    val panel: DialogPanel = buildPanel()

    val preferredFocusedComponent: JComponent get() = enabledCheckBox

    private fun buildPanel(): DialogPanel = panel {
        row {
            cell(enabledCheckBox)
                .comment("Master switch for all sound notifications.")
        }

        group("Sounds") {
            row {
                label("Untick an event to silence just that one. Leave a path blank to use the built-in sound.")
            }
            for (event in SoundEvent.entries) {
                val soundRow = rows.getValue(event)
                row {
                    cell(soundRow.enabledCheckBox)
                    cell(soundRow.pathField)
                        .align(AlignX.FILL)
                        .resizableColumn()
                    cell(soundRow.testButton)
                }
            }
        }

        group("Playback") {
            row("Volume:") {
                cell(volumeSpinner)
                label("%")
            }
        }
    }

    fun isModified(): Boolean {
        val s = settings
        if (enabledCheckBox.isSelected != s.enabled) return true
        if (volumeSpinner.number != s.volume) return true
        return SoundEvent.entries.any { event ->
            val row = rows.getValue(event)
            row.path != s.pathFor(event) || row.soundEnabled != s.isEnabled(event)
        }
    }

    fun apply() {
        val s = settings
        s.enabled = enabledCheckBox.isSelected
        s.volume = volumeSpinner.number
        SoundEvent.entries.forEach { event ->
            val row = rows.getValue(event)
            s.setEnabled(event, row.soundEnabled)
            s.setPath(event, row.path)
        }
    }

    fun reset() {
        val s = settings
        enabledCheckBox.isSelected = s.enabled
        volumeSpinner.number = s.volume
        SoundEvent.entries.forEach { event ->
            val row = rows.getValue(event)
            row.soundEnabled = s.isEnabled(event)
            row.path = s.pathFor(event)
        }
    }
}
