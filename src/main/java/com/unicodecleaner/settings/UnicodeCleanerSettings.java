package com.unicodecleaner.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.unicodecleaner.utils.UnicodeDetector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Persistent settings for the Unicode Cleaner plugin.
 */
@State(
    name = "UnicodeCleanerSettings",
    storages = @Storage("unicodeCleanerSettings.xml")
)
public class UnicodeCleanerSettings implements PersistentStateComponent<UnicodeCleanerSettings.State> {
    
    public static class State {
        // Character category settings
        public boolean enableHiddenControl = true;
        public boolean enableSpaces = true;
        public boolean enableQuotes = true;
        public boolean enableDashes = true;
        public boolean enablePunctuation = true;
        public boolean enableFullWidth = true;
        public boolean enableVariation = true;
        
        // File type settings
        public Set<String> enabledExtensions = new HashSet<>(Arrays.asList(
            "txt", "md", "rst", "java", "js", "ts", "py", "cpp", "c", "h",
            "xml", "json", "yaml", "yml", "properties", "html", "css"
        ));
        
        // Git integration settings
        public boolean enablePreCommitCheck = false;
        public boolean blockCommitsWithIssues = false;
        public boolean autoFixBeforeCommit = false;
        public boolean showCommitSummary = true;
        
        // Performance settings
        public int maxFileSizeKB = 10240; // 10MB
        public boolean enableRealTimeDetection = true;
        
        // UI settings
        public boolean showTooltips = true;
        public boolean highlightIssues = true;
        public String highlightColor = "FFFF00"; // Yellow
    }
    
    private State state = new State();
    
    public static UnicodeCleanerSettings getInstance() {
        return ApplicationManager.getApplication().getService(UnicodeCleanerSettings.class);
    }
    
    @Override
    public @Nullable State getState() {
        return state;
    }
    
    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, this.state);
    }
    
    // Character category getters and setters
    public boolean isHiddenControlEnabled() {
        return state.enableHiddenControl;
    }
    
    public void setHiddenControlEnabled(boolean enabled) {
        state.enableHiddenControl = enabled;
    }
    
    public boolean isSpacesEnabled() {
        return state.enableSpaces;
    }
    
    public void setSpacesEnabled(boolean enabled) {
        state.enableSpaces = enabled;
    }
    
    public boolean isQuotesEnabled() {
        return state.enableQuotes;
    }
    
    public void setQuotesEnabled(boolean enabled) {
        state.enableQuotes = enabled;
    }
    
    public boolean isDashesEnabled() {
        return state.enableDashes;
    }
    
    public void setDashesEnabled(boolean enabled) {
        state.enableDashes = enabled;
    }
    
    public boolean isPunctuationEnabled() {
        return state.enablePunctuation;
    }
    
    public void setPunctuationEnabled(boolean enabled) {
        state.enablePunctuation = enabled;
    }
    
    public boolean isFullWidthEnabled() {
        return state.enableFullWidth;
    }
    
    public void setFullWidthEnabled(boolean enabled) {
        state.enableFullWidth = enabled;
    }
    
    public boolean isVariationEnabled() {
        return state.enableVariation;
    }
    
    public void setVariationEnabled(boolean enabled) {
        state.enableVariation = enabled;
    }
    
    public Set<UnicodeDetector.CharacterCategory> getEnabledCategories() {
        Set<UnicodeDetector.CharacterCategory> categories = new HashSet<>();
        
        if (state.enableHiddenControl) {
            categories.add(UnicodeDetector.CharacterCategory.HIDDEN_CONTROL);
        }
        if (state.enableSpaces) {
            categories.add(UnicodeDetector.CharacterCategory.SPACE);
        }
        if (state.enableQuotes) {
            categories.add(UnicodeDetector.CharacterCategory.QUOTES);
        }
        if (state.enableDashes) {
            categories.add(UnicodeDetector.CharacterCategory.DASHES);
        }
        if (state.enablePunctuation) {
            categories.add(UnicodeDetector.CharacterCategory.PUNCTUATION);
        }
        if (state.enableFullWidth) {
            categories.add(UnicodeDetector.CharacterCategory.FULL_WIDTH);
        }
        if (state.enableVariation) {
            categories.add(UnicodeDetector.CharacterCategory.VARIATION);
        }
        
        return categories;
    }
    
    // File type settings
    public Set<String> getEnabledExtensions() {
        return new HashSet<>(state.enabledExtensions);
    }
    
    public void setEnabledExtensions(Set<String> extensions) {
        state.enabledExtensions = new HashSet<>(extensions);
    }
    
    public boolean shouldCheckFileType(String fileName) {
        if (fileName == null) return false;
        
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0) return false;
        
        String extension = fileName.substring(lastDot + 1).toLowerCase();
        return state.enabledExtensions.contains(extension);
    }
    
    public void addFileExtension(String extension) {
        state.enabledExtensions.add(extension.toLowerCase());
    }
    
    public void removeFileExtension(String extension) {
        state.enabledExtensions.remove(extension.toLowerCase());
    }
    
    // Git integration settings
    public boolean isPreCommitCheckEnabled() {
        return state.enablePreCommitCheck;
    }
    
    public void setPreCommitCheckEnabled(boolean enabled) {
        state.enablePreCommitCheck = enabled;
    }
    
    public boolean shouldBlockCommitsWithIssues() {
        return state.blockCommitsWithIssues;
    }
    
    public void setBlockCommitsWithIssues(boolean block) {
        state.blockCommitsWithIssues = block;
    }
    
    public boolean shouldAutoFixBeforeCommit() {
        return state.autoFixBeforeCommit;
    }
    
    public void setAutoFixBeforeCommit(boolean autoFix) {
        state.autoFixBeforeCommit = autoFix;
    }
    
    public boolean shouldShowCommitSummary() {
        return state.showCommitSummary;
    }
    
    public void setShowCommitSummary(boolean show) {
        state.showCommitSummary = show;
    }
    
    // Performance settings
    public int getMaxFileSizeKB() {
        return state.maxFileSizeKB;
    }
    
    public void setMaxFileSizeKB(int sizeKB) {
        state.maxFileSizeKB = sizeKB;
    }
    
    public boolean isRealTimeDetectionEnabled() {
        return state.enableRealTimeDetection;
    }
    
    public void setRealTimeDetectionEnabled(boolean enabled) {
        state.enableRealTimeDetection = enabled;
    }
    
    // UI settings
    public boolean shouldShowTooltips() {
        return state.showTooltips;
    }
    
    public void setShowTooltips(boolean show) {
        state.showTooltips = show;
    }
    
    public boolean shouldHighlightIssues() {
        return state.highlightIssues;
    }
    
    public void setHighlightIssues(boolean highlight) {
        state.highlightIssues = highlight;
    }
    
    public String getHighlightColor() {
        return state.highlightColor;
    }
    
    public void setHighlightColor(String color) {
        state.highlightColor = color;
    }
    
    // Utility methods
    public boolean shouldInspectFile(String fileName, long fileSizeBytes) {
        if (!shouldCheckFileType(fileName)) {
            return false;
        }
        
        long fileSizeKB = fileSizeBytes / 1024;
        return fileSizeKB <= state.maxFileSizeKB;
    }
    
    public void resetToDefaults() {
        state = new State();
    }
    
    public Map<String, Object> getSettingsMap() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("enableHiddenControl", state.enableHiddenControl);
        settings.put("enableSpaces", state.enableSpaces);
        settings.put("enableQuotes", state.enableQuotes);
        settings.put("enableDashes", state.enableDashes);
        settings.put("enablePunctuation", state.enablePunctuation);
        settings.put("enableFullWidth", state.enableFullWidth);
        settings.put("enableVariation", state.enableVariation);
        settings.put("enabledExtensions", state.enabledExtensions);
        settings.put("enablePreCommitCheck", state.enablePreCommitCheck);
        settings.put("blockCommitsWithIssues", state.blockCommitsWithIssues);
        settings.put("autoFixBeforeCommit", state.autoFixBeforeCommit);
        settings.put("maxFileSizeKB", state.maxFileSizeKB);
        settings.put("enableRealTimeDetection", state.enableRealTimeDetection);
        return settings;
    }
}
