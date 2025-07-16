package com.unicodecleaner.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.unicodecleaner.utils.UnicodeDetector;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Settings panel for Unicode Cleaner plugin.
 */
public class UnicodeCleanerConfigurable implements Configurable {

    private JPanel mainPanel;
    private Map<UnicodeDetector.CharacterCategory, JCheckBox> categoryCheckboxes;
    private JTextField extensionsField;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Unicode Cleaner";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mainPanel = new JPanel(new BorderLayout());
        
        // Create the settings panel
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 10, 0);
        gbc.anchor = GridBagConstraints.WEST;
        settingsPanel.add(new JLabel("<html><h3>Character Categories to Detect</h3></html>"), gbc);
        
        // Character category checkboxes
        categoryCheckboxes = new HashMap<>();
        int row = 1;
        
        for (UnicodeDetector.CharacterCategory category : UnicodeDetector.CharacterCategory.values()) {
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.gridwidth = 2;
            gbc.insets = new Insets(2, 20, 2, 0);
            
            JCheckBox checkbox = new JCheckBox(category.getDisplayName());
            checkbox.setToolTipText("Enable detection for " + category.getDisplayName().toLowerCase());
            categoryCheckboxes.put(category, checkbox);
            settingsPanel.add(checkbox, gbc);
        }
        
        // File extensions section
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 0, 10, 0);
        settingsPanel.add(new JLabel("<html><h3>File Extensions</h3></html>"), gbc);
        
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(2, 20, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;
        settingsPanel.add(new JLabel("Extensions:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(2, 0, 2, 0);
        extensionsField = new JTextField();
        extensionsField.setToolTipText("Comma-separated list of file extensions (e.g., txt,md,java,js)");
        settingsPanel.add(extensionsField, gbc);
        
        // Add some vertical glue to push everything to the top
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        settingsPanel.add(Box.createVerticalGlue(), gbc);
        
        mainPanel.add(settingsPanel, BorderLayout.CENTER);
        
        // Load current settings
        reset();
        
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        UnicodeCleanerSettings settings = UnicodeCleanerSettings.getInstance();
        
        // Check if any category checkbox state has changed
        if (categoryCheckboxes.get(UnicodeDetector.CharacterCategory.HIDDEN_CONTROL).isSelected() != settings.isHiddenControlEnabled()) return true;
        if (categoryCheckboxes.get(UnicodeDetector.CharacterCategory.SPACE).isSelected() != settings.isSpacesEnabled()) return true;
        if (categoryCheckboxes.get(UnicodeDetector.CharacterCategory.QUOTES).isSelected() != settings.isQuotesEnabled()) return true;
        if (categoryCheckboxes.get(UnicodeDetector.CharacterCategory.DASHES).isSelected() != settings.isDashesEnabled()) return true;
        if (categoryCheckboxes.get(UnicodeDetector.CharacterCategory.PUNCTUATION).isSelected() != settings.isPunctuationEnabled()) return true;
        if (categoryCheckboxes.get(UnicodeDetector.CharacterCategory.FULL_WIDTH).isSelected() != settings.isFullWidthEnabled()) return true;
        if (categoryCheckboxes.get(UnicodeDetector.CharacterCategory.VARIATION).isSelected() != settings.isVariationEnabled()) return true;
        
        // Check extensions field
        String currentExtensions = String.join(",", settings.getEnabledExtensions());
        String fieldExtensions = extensionsField.getText().trim();
        if (!currentExtensions.equals(fieldExtensions)) return true;
        
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
        UnicodeCleanerSettings settings = UnicodeCleanerSettings.getInstance();
        
        // Apply category settings
        settings.setHiddenControlEnabled(categoryCheckboxes.get(UnicodeDetector.CharacterCategory.HIDDEN_CONTROL).isSelected());
        settings.setSpacesEnabled(categoryCheckboxes.get(UnicodeDetector.CharacterCategory.SPACE).isSelected());
        settings.setQuotesEnabled(categoryCheckboxes.get(UnicodeDetector.CharacterCategory.QUOTES).isSelected());
        settings.setDashesEnabled(categoryCheckboxes.get(UnicodeDetector.CharacterCategory.DASHES).isSelected());
        settings.setPunctuationEnabled(categoryCheckboxes.get(UnicodeDetector.CharacterCategory.PUNCTUATION).isSelected());
        settings.setFullWidthEnabled(categoryCheckboxes.get(UnicodeDetector.CharacterCategory.FULL_WIDTH).isSelected());
        settings.setVariationEnabled(categoryCheckboxes.get(UnicodeDetector.CharacterCategory.VARIATION).isSelected());
        
        // Apply extensions
        String extensionsText = extensionsField.getText().trim();
        if (!extensionsText.isEmpty()) {
            java.util.Set<String> extensions = new java.util.HashSet<>();
            for (String ext : extensionsText.split(",")) {
                String trimmed = ext.trim().toLowerCase();
                if (!trimmed.isEmpty()) {
                    extensions.add(trimmed);
                }
            }
            settings.setEnabledExtensions(extensions);
        }
    }

    @Override
    public void reset() {
        UnicodeCleanerSettings settings = UnicodeCleanerSettings.getInstance();
        
        // Load category settings
        categoryCheckboxes.get(UnicodeDetector.CharacterCategory.HIDDEN_CONTROL).setSelected(settings.isHiddenControlEnabled());
        categoryCheckboxes.get(UnicodeDetector.CharacterCategory.SPACE).setSelected(settings.isSpacesEnabled());
        categoryCheckboxes.get(UnicodeDetector.CharacterCategory.QUOTES).setSelected(settings.isQuotesEnabled());
        categoryCheckboxes.get(UnicodeDetector.CharacterCategory.DASHES).setSelected(settings.isDashesEnabled());
        categoryCheckboxes.get(UnicodeDetector.CharacterCategory.PUNCTUATION).setSelected(settings.isPunctuationEnabled());
        categoryCheckboxes.get(UnicodeDetector.CharacterCategory.FULL_WIDTH).setSelected(settings.isFullWidthEnabled());
        categoryCheckboxes.get(UnicodeDetector.CharacterCategory.VARIATION).setSelected(settings.isVariationEnabled());
        
        // Load extensions
        String extensions = String.join(",", settings.getEnabledExtensions());
        extensionsField.setText(extensions);
    }
}
