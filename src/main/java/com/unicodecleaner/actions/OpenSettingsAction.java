package com.unicodecleaner.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Action to open Unicode Cleaner settings.
 */
public class OpenSettingsAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // Open the settings dialog for Unicode Cleaner
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "Unicode Cleaner");
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable action only when project is available
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
}
