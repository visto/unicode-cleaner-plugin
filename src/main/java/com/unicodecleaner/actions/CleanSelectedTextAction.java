package com.unicodecleaner.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.unicodecleaner.settings.UnicodeCleanerSettings;
import com.unicodecleaner.utils.UnicodeDetector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Action to clean Unicode characters in selected text.
 */
public class CleanSelectedTextAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            Messages.showMessageDialog(
                project,
                "No editor is currently active.",
                "Unicode Cleaner",
                Messages.getWarningIcon()
            );
            return;
        }

        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();

        if (selectedText == null || selectedText.isEmpty()) {
            Messages.showMessageDialog(
                project,
                "No text is currently selected.",
                "Unicode Cleaner",
                Messages.getWarningIcon()
            );
            return;
        }

        UnicodeCleanerSettings settings = UnicodeCleanerSettings.getInstance();
        UnicodeDetector detector = new UnicodeDetector();

        // Detect issues in selected text
        List<UnicodeDetector.UnicodeIssue> issues = detector.detectIssues(
            selectedText, 
            settings.getEnabledCategories()
        );

        if (issues.isEmpty()) {
            Messages.showMessageDialog(
                project,
                "No Unicode issues found in the selected text.",
                "Unicode Cleaner",
                Messages.getInformationIcon()
            );
            return;
        }

        // Show confirmation dialog
        int result = Messages.showYesNoDialog(
            project,
            String.format(
                "Found %d Unicode issue(s) in the selected text.\n\n" +
                "Do you want to clean them?",
                issues.size()
            ),
            "Unicode Cleaner",
            Messages.getQuestionIcon()
        );

        if (result == Messages.YES) {
            cleanSelectedText(project, editor, selectionModel, selectedText, detector, settings);
        }
    }

    private void cleanSelectedText(@NotNull Project project,
                                  @NotNull Editor editor,
                                  @NotNull SelectionModel selectionModel,
                                  @NotNull String selectedText,
                                  @NotNull UnicodeDetector detector,
                                  @NotNull UnicodeCleanerSettings settings) {

        String cleanedText = detector.cleanText(selectedText, settings.getEnabledCategories());
        int removedChars = selectedText.length() - cleanedText.length();

        WriteCommandAction.runWriteCommandAction(project, () -> {
            Document document = editor.getDocument();
            int startOffset = selectionModel.getSelectionStart();
            int endOffset = selectionModel.getSelectionEnd();
            
            document.replaceString(startOffset, endOffset, cleanedText);
            
            // Update selection to the new text
            selectionModel.setSelection(startOffset, startOffset + cleanedText.length());
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
        // Enable action only when editor and selection are available
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        boolean enabled = project != null && editor != null && 
                         editor.getSelectionModel().hasSelection();
        
        e.getPresentation().setEnabledAndVisible(enabled);
    }
}
