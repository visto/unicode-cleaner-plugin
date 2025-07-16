package com.unicodecleaner.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

/**
 * Action to show all Unicode issues in the project.
 */
public class ShowUnicodeIssuesAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // For now, show a simple dialog
        // TODO: Implement a proper tool window to show Unicode issues
        Messages.showMessageDialog(
            project,
            "Unicode Issues view is not yet implemented.\n\n" +
            "Use 'Clean Unicode Characters in File' or 'Clean Unicode Characters in Project' " +
            "to clean Unicode issues directly.",
            "Unicode Issues",
            Messages.getInformationIcon()
        );
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable action only when project is available
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
}
