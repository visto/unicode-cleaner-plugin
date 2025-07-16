package com.unicodecleaner.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.unicodecleaner.settings.UnicodeCleanerSettings;
import com.unicodecleaner.utils.UnicodeDetector;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Action to clean Unicode characters in selected files from project view.
 */
public class CleanSelectedFilesAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        VirtualFile[] selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (selectedFiles == null || selectedFiles.length == 0) {
            Messages.showMessageDialog(
                project,
                "No files are selected.",
                "Unicode Cleaner",
                Messages.getWarningIcon()
            );
            return;
        }

        UnicodeCleanerSettings settings = UnicodeCleanerSettings.getInstance();
        
        // Filter files that should be processed
        List<VirtualFile> filesToProcess = Arrays.stream(selectedFiles)
            .filter(file -> !file.isDirectory() && settings.shouldCheckFileType(file.getName()))
            .toList();

        if (filesToProcess.isEmpty()) {
            Messages.showMessageDialog(
                project,
                "No files match the configured file types for Unicode cleaning.",
                "Unicode Cleaner",
                Messages.getInformationIcon()
            );
            return;
        }

        // Show confirmation dialog
        int result = Messages.showYesNoDialog(
            project,
            String.format(
                "This will clean Unicode characters in %d selected file(s).\n\n" +
                "Do you want to continue?",
                filesToProcess.size()
            ),
            "Clean Selected Files",
            Messages.getQuestionIcon()
        );

        if (result != Messages.YES) {
            return;
        }

        // Run the cleaning operation in background
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Cleaning Selected Files", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                cleanFiles(project, filesToProcess, settings, indicator);
            }
        });
    }

    private void cleanFiles(@NotNull Project project,
                           @NotNull List<VirtualFile> files,
                           @NotNull UnicodeCleanerSettings settings,
                           @NotNull ProgressIndicator indicator) {

        indicator.setIndeterminate(false);
        indicator.setText("Cleaning selected files...");

        UnicodeDetector detector = new UnicodeDetector();
        PsiManager psiManager = PsiManager.getInstance(project);
        int processedFiles = 0;
        int cleanedFiles = 0;
        int totalIssuesFixed = 0;

        for (VirtualFile vFile : files) {
            if (indicator.isCanceled()) return;

            indicator.setFraction((double) processedFiles / files.size());
            indicator.setText2("Processing: " + vFile.getName());

            try {
                PsiFile psiFile = psiManager.findFile(vFile);
                if (psiFile != null) {
                    String originalText = psiFile.getText();

                    List<UnicodeDetector.UnicodeIssue> issues = detector.detectIssues(
                        originalText,
                        settings.getEnabledCategories()
                    );

                    if (!issues.isEmpty()) {
                        String cleanedText = detector.cleanText(
                            originalText,
                            settings.getEnabledCategories()
                        );

                        if (!originalText.equals(cleanedText)) {
                            // Apply changes in EDT
                            WriteCommandAction.runWriteCommandAction(project, () -> {
                                try {
                                    com.intellij.openapi.editor.Document document =
                                        com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(psiFile);
                                    if (document != null) {
                                        document.setText(cleanedText);
                                        com.intellij.psi.PsiDocumentManager.getInstance(project).commitDocument(document);
                                    }
                                } catch (Exception ex) {
                                    System.err.println("Failed to clean file: " + vFile.getName() + " - " + ex.getMessage());
                                }
                            });

                            cleanedFiles++;
                            totalIssuesFixed += issues.size();
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("Error processing file: " + vFile.getName() + " - " + ex.getMessage());
            }

            processedFiles++;
        }

        // Show results
        final int finalCleanedFiles = cleanedFiles;
        final int finalTotalIssues = totalIssuesFixed;
        final int finalProcessedFiles = processedFiles;

        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            Messages.showMessageDialog(
                project,
                String.format(
                    "Selected files cleaning completed!\n\n" +
                    "Files processed: %d\n" +
                    "Files cleaned: %d\n" +
                    "Total issues fixed: %d",
                    finalProcessedFiles,
                    finalCleanedFiles,
                    finalTotalIssues
                ),
                "Unicode Cleaner",
                Messages.getInformationIcon()
            );
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable action only when project and files are selected
        Project project = e.getProject();
        VirtualFile[] selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        
        boolean enabled = project != null && selectedFiles != null && selectedFiles.length > 0;
        e.getPresentation().setEnabledAndVisible(enabled);
    }
}
