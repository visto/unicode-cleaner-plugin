package com.unicodecleaner.inspections;

import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.LeafPsiElement;

import com.unicodecleaner.settings.UnicodeCleanerSettings;
import com.unicodecleaner.utils.UnicodeDetector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * IntelliJ inspection that detects problematic Unicode characters in text elements.
 */
public class UnicodeCharacterInspection extends LocalInspectionTool {

    @Override
    public @NotNull String getShortName() {
        return "UnicodeCharacterIssues";
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Unicode Character Issues";
    }

    @Override
    public @NotNull String getGroupDisplayName() {
        return "Text Quality";
    }

    @Override
    public @NotNull String[] getGroupPath() {
        return new String[]{"General", "Text Quality"};
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public @NotNull com.intellij.codeHighlighting.HighlightDisplayLevel getDefaultLevel() {
        return com.intellij.codeHighlighting.HighlightDisplayLevel.WARNING;
    }

    @Override
    public @Nullable String getStaticDescription() {
        return "Detects problematic Unicode characters that may trigger AI detection systems " +
                "or cause compatibility issues. Suggests ASCII replacements.";
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        UnicodeCleanerSettings settings = UnicodeCleanerSettings.getInstance();

        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                super.visitElement(element);

                // Check if this element type should be inspected
                if (!shouldInspectElement(element, settings)) {
                    return;
                }

                String text = element.getText();
                if (text == null || text.isEmpty()) {
                    return;
                }

                List<UnicodeDetector.UnicodeIssue> issues = new UnicodeDetector()
                        .detectIssues(text, settings.getEnabledCategories());

                for (UnicodeDetector.UnicodeIssue issue : issues) {
                    registerProblem(holder, element, issue);
                }
            }
        };
    }

    private boolean shouldInspectElement(PsiElement element, UnicodeCleanerSettings settings) {
        // Check file extension
        String fileName = element.getContainingFile().getName();
        if (!settings.shouldCheckFileType(fileName)) {
            return false;
        }

        // Check element types
        return element instanceof PsiPlainText ||
                element instanceof PsiComment ||
                element instanceof LeafPsiElement;
    }

    private void registerProblem(@NotNull ProblemsHolder holder,
                                 @NotNull PsiElement element,
                                 @NotNull UnicodeDetector.UnicodeIssue issue) {

        UnicodeDetector.CharacterInfo charInfo = issue.characterInfo;

        String description = String.format(
                "Problematic Unicode character: %s (%s) - %s",
                charInfo.unicode,
                charInfo.category.getDisplayName(),
                charInfo.description
        );

        TextRange range = new TextRange(
                issue.startOffset,
                issue.endOffset
        );

        // Create quick fixes
        List<LocalQuickFix> fixes = new ArrayList<>();

        // Single character fix
        fixes.add(new UnicodeQuickFix(charInfo));

        // File-wide fix
        fixes.add(new CleanFileQuickFix());

        // Category-wide fix
        fixes.add(new CleanCategoryQuickFix(charInfo.category));

        holder.registerProblem(
                element,
                description,
                ProblemHighlightType.WARNING,
                range,
                fixes.toArray(new LocalQuickFix[0])
        );
    }

    /**
     * Quick fix to replace a single Unicode character with its ASCII equivalent.
     */
    private static class UnicodeQuickFix implements LocalQuickFix {
        private final UnicodeDetector.CharacterInfo characterInfo;

        public UnicodeQuickFix(UnicodeDetector.CharacterInfo characterInfo) {
            this.characterInfo = characterInfo;
        }

        @Override
        public @NotNull String getName() {
            if (characterInfo.replacement.isEmpty()) {
                return "Remove " + characterInfo.unicode;
            } else {
                return String.format("Replace with \"%s\"", characterInfo.replacement);
            }
        }

        @Override
        public @NotNull String getFamilyName() {
            return "Unicode Character Fixes";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) {
                return;
            }

            String text = element.getText();
            if (text == null) {
                return;
            }

            String newText = text.replace(
                    String.valueOf(characterInfo.character),
                    characterInfo.replacement
            );

            replacePsiElementText(element, newText);
        }
    }

    /**
     * Quick fix to clean all Unicode issues in the current file.
     */
    private static class CleanFileQuickFix implements LocalQuickFix {
        @Override
        public @NotNull String getName() {
            return "Clean all Unicode issues in file";
        }

        @Override
        public @NotNull String getFamilyName() {
            return "Unicode Character Fixes";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) {
                return;
            }

            PsiFile file = element.getContainingFile();
            String originalText = file.getText();

            UnicodeDetector detector = new UnicodeDetector();
            String cleanedText = detector.cleanText(originalText);

            if (!originalText.equals(cleanedText)) {
                replaceFileContent(file, cleanedText);
            }
        }
    }

    /**
     * Quick fix to clean all Unicode issues of a specific category in the file.
     */
    private static class CleanCategoryQuickFix implements LocalQuickFix {
        private final UnicodeDetector.CharacterCategory category;

        public CleanCategoryQuickFix(UnicodeDetector.CharacterCategory category) {
            this.category = category;
        }

        @Override
        public @NotNull String getName() {
            return "Clean all " + category.getDisplayName() + " in file";
        }

        @Override
        public @NotNull String getFamilyName() {
            return "Unicode Character Fixes";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) {
                return;
            }

            PsiFile file = element.getContainingFile();
            String originalText = file.getText();

            UnicodeDetector detector = new UnicodeDetector();
            String cleanedText = detector.cleanText(originalText,
                    java.util.Set.of(category));

            if (!originalText.equals(cleanedText)) {
                replaceFileContent(file, cleanedText);
            }
        }
    }

    /**
     * Helper method to replace PSI element text safely.
     */
    private static void replacePsiElementText(@NotNull PsiElement element, @NotNull String newText) {
        Project project = element.getProject();

        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                // Handle plain text elements via document
                Document document = getDocumentForElement(element);
                if (document != null) {
                    TextRange range = element.getTextRange();
                    document.replaceString(range.getStartOffset(), range.getEndOffset(), newText);
                }
            } catch (Exception e) {
                // Log error
                System.err.println("Failed to replace element text: " + e.getMessage());
            }
        });
    }

    /**
     * Helper method to replace entire file content safely.
     */
    private static void replaceFileContent(@NotNull PsiFile file, @NotNull String newContent) {
        Project project = file.getProject();

        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                Document document = PsiDocumentManager.getInstance(project).getDocument(file);
                if (document != null) {
                    document.setText(newContent);
                    PsiDocumentManager.getInstance(project).commitDocument(document);
                }
            } catch (Exception e) {
                // Fallback: recreate file content
                try {
                    PsiFileFactory factory = PsiFileFactory.getInstance(project);
                    PsiFile newFile = factory.createFileFromText(
                            file.getName(),
                            file.getFileType(),
                            newContent
                    );
                    file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
                } catch (Exception ex) {
                    // Log error or show notification
                    System.err.println("Failed to replace file content: " + ex.getMessage());
                }
            }
        });
    }

    /**
     * Get document for PSI element.
     */
    private static @Nullable Document getDocumentForElement(@NotNull PsiElement element) {
        PsiFile file = element.getContainingFile();
        if (file == null) {
            return null;
        }

        return PsiDocumentManager.getInstance(element.getProject()).getDocument(file);
    }

    /**
     * Escape string literal content for Java/Kotlin code.
     */
    private static @NotNull String escapeStringLiteral(@NotNull String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}