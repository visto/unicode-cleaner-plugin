package com.unicodecleaner.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import com.unicodecleaner.settings.UnicodeCleanerSettings;
import com.unicodecleaner.utils.UnicodeDetector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Action to clean Unicode characters in the current file.
 */
public class CleanCurrentFileAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (editor == null || psiFile == null) {
            Messages.showWarningDialog(
                    project,
                    "No file is currently open in the editor.",
                    "Unicode Cleaner"
            );
            return;
        }

        Document document = editor.getDocument();
        String originalText = document.getText();

        // Check if file should be processed
        UnicodeCleanerSettings settings = UnicodeCleanerSettings.getInstance();
        if (!settings.shouldInspectFile(psiFile.getName(), originalText.length())) {
            Messages.showMessageDialog(
                    project,
                    "This file type is not configured for Unicode cleaning.\n" +
                            "Check your settings to enable this file type.",
                    "Unicode Cleaner",
                    Messages.getInformationIcon()
            );
            return;
        }

        // Detect issues
        UnicodeDetector detector = new UnicodeDetector();
        List<UnicodeDetector.UnicodeIssue> issues = detector.detectIssues(
                originalText,
                settings.getEnabledCategories()
        );

        if (issues.isEmpty()) {
            Messages.showMessageDialog(
                    project,
                    "No Unicode issues found in this file.",
                    "Unicode Cleaner",
                    Messages.getInformationIcon()
            );
            return;
        }

        // Show confirmation dialog
        int result = Messages.showYesNoDialog(
                project,
                String.format(
                        "Found %d Unicode issue(s) in this file.\n\n" +
                                "Do you want to clean them?",
                        issues.size()
                ),
                "Unicode Cleaner",
                Messages.getQuestionIcon()
        );

        if (result == Messages.YES) {
            cleanFile(project, document, originalText, detector, settings);
        }
    }

    private void cleanFile(@NotNull Project project,
                           @NotNull Document document,
                           @NotNull String originalText,
                           @NotNull UnicodeDetector detector,
                           @NotNull UnicodeCleanerSettings settings) {

        String cleanedText = detector.cleanText(originalText, settings.getEnabledCategories());
        int removedChars = originalText.length() - cleanedText.length();

        WriteCommandAction.runWriteCommandAction(project, () -> {
            document.setText(cleanedText);
        });

        // Show success message AFTER the write action completes
        Messages.showMessageDialog(
                project,
                String.format(
                        "Unicode cleaning completed!\n\n" +
                                "Characters removed/replaced: %d",
                        removedChars
                ),
                "Unicode Cleaner",
                Messages.getInformationIcon()
        );
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable action only when editor and file are available
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        boolean enabled = project != null && editor != null && psiFile != null;
        e.getPresentation().setEnabledAndVisible(enabled);
    }
}