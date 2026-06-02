package com.soundbuild.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

/**
 * Registers the settings page under **Settings → Tools → SoundBuild**.
 *
 * Lifecycle of the UI is delegated to [SoundSettingsComponent]; this class only
 * wires it into the platform's [Configurable] contract. Registered via
 * `<applicationConfigurable>` in plugin.xml.
 */
class SoundSettingsConfigurable : Configurable {

    private var component: SoundSettingsComponent? = null

    override fun getDisplayName(): String = "SoundBuild"

    override fun createComponent(): JComponent {
        val created = SoundSettingsComponent()
        component = created
        return created.panel
    }

    override fun getPreferredFocusedComponent(): JComponent? =
        component?.preferredFocusedComponent

    override fun isModified(): Boolean = component?.isModified() ?: false

    override fun apply() {
        component?.apply()
    }

    override fun reset() {
        component?.reset()
    }

    override fun disposeUIResources() {
        component = null
    }
}
