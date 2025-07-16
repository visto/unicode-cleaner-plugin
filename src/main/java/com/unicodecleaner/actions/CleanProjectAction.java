package com.unicodecleaner.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.unicodecleaner.settings.UnicodeCleanerSettings;
import com.unicodecleaner.utils.UnicodeDetector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Action to clean Unicode characters in all project files.
 */
public class CleanProjectAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        UnicodeCleanerSettings settings = UnicodeCleanerSettings.getInstance();
        
        // Show confirmation dialog
        int result = Messages.showYesNoDialog(
            project,
            "This will scan and clean Unicode characters in all project files.\n\n" +
            "This operation may take some time for large projects.\n" +
            "Do you want to continue?",
            "Clean Project Unicode Characters",
            Messages.getQuestionIcon()
        );

        if (result != Messages.YES) {
            return;
        }

        // Run the cleaning operation in background
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Cleaning Unicode Characters", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                cleanProjectFiles(project, settings, indicator);
            }
        });
    }

    private void cleanProjectFiles(@NotNull Project project, 
                                  @NotNull UnicodeCleanerSettings settings,
                                  @NotNull ProgressIndicator indicator) {
        
        indicator.setIndeterminate(false);
        indicator.setText("Scanning project files...");
        
        // Get all files in the project
        Collection<VirtualFile> allFiles = FileBasedIndex.getInstance()
            .getContainingFiles(FileTypeIndex.NAME, 
                com.intellij.openapi.fileTypes.PlainTextFileType.INSTANCE, 
                GlobalSearchScope.projectScope(project));
        
        // Add other file types
        List<VirtualFile> filesToProcess = new ArrayList<>();
        PsiManager psiManager = PsiManager.getInstance(project);
        
        // Collect files that should be processed
        for (VirtualFile vFile : allFiles) {
            if (indicator.isCanceled()) return;
            
            if (settings.shouldCheckFileType(vFile.getName())) {
                filesToProcess.add(vFile);
            }
        }
        
        if (filesToProcess.isEmpty()) {
            Messages.showMessageDialog(
                project,
                "No files found that match the configured file types.",
                "Unicode Cleaner",
                Messages.getInformationIcon()
            );
            return;
        }
        
        indicator.setText("Processing " + filesToProcess.size() + " files...");
        
        UnicodeDetector detector = new UnicodeDetector();
        int processedFiles = 0;
        int cleanedFiles = 0;
        int totalIssuesFixed = 0;
        
        for (VirtualFile vFile : filesToProcess) {
            if (indicator.isCanceled()) return;
            
            indicator.setFraction((double) processedFiles / filesToProcess.size());
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
                    "Project Unicode cleaning completed!\n\n" +
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
        // Enable action only when project is available
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
}
