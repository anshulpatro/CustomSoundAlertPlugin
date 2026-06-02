package com.soundbuild.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.soundbuild.settings.SoundSettingsState

/**
 * Tools-menu toggle that mirrors the "Enable Custom Sound Alert" setting, so the
 * master switch can be flipped without opening the settings dialog. Shows a
 * check mark when sound notifications are enabled.
 */
class ToggleSoundAlertsAction : ToggleAction() {

    override fun isSelected(e: AnActionEvent): Boolean =
        SoundSettingsState.getInstance().enabled

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        SoundSettingsState.getInstance().enabled = state
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
