package com.unicodecleaner.utils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Core Unicode character detection and replacement logic.
 * Identifies problematic Unicode characters that trigger AI detection systems.
 */
public class UnicodeDetector {
    
    public enum CharacterCategory {
        HIDDEN_CONTROL("Hidden/Control Characters"),
        SPACE("Space Characters"),
        QUOTES("Quotes & Apostrophes"),
        DASHES("Dashes"),
        PUNCTUATION("Punctuation"),
        FULL_WIDTH("Full-Width Characters"),
        VARIATION("Variation Selectors");
        
        private final String displayName;
        
        CharacterCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public static class CharacterInfo {
        public final char character;
        public final String unicode;
        public final String replacement;
        public final CharacterCategory category;
        public final String description;
        
        public CharacterInfo(char character, String replacement, CharacterCategory category, String description) {
            this.character = character;
            this.unicode = String.format("U+%04X", (int) character);
            this.replacement = replacement;
            this.category = category;
            this.description = description;
        }
    }
    
    public static class UnicodeIssue {
        public final int startOffset;
        public final int endOffset;
        public final CharacterInfo characterInfo;
        public final String context;
        
        public UnicodeIssue(int startOffset, int endOffset, CharacterInfo characterInfo, String context) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.characterInfo = characterInfo;
            this.context = context;
        }
    }
    
    // Comprehensive mapping of problematic Unicode characters
    private static final Map<Character, CharacterInfo> PROBLEMATIC_CHARS = new HashMap<>();
    
    static {
        // Hidden/Control Characters
        addChar('\u00AD', "", CharacterCategory.HIDDEN_CONTROL, "Soft hyphen");
        addChar('\u180E', "", CharacterCategory.HIDDEN_CONTROL, "Mongolian vowel separator");
        addChar('\u200B', "", CharacterCategory.HIDDEN_CONTROL, "Zero width space");
        addChar('\u200C', "", CharacterCategory.HIDDEN_CONTROL, "Zero width non-joiner");
        addChar('\u200D', "", CharacterCategory.HIDDEN_CONTROL, "Zero width joiner");
        addChar('\u200E', "", CharacterCategory.HIDDEN_CONTROL, "Left-to-right mark");
        addChar('\u200F', "", CharacterCategory.HIDDEN_CONTROL, "Right-to-left mark");
        addChar('\u202A', "", CharacterCategory.HIDDEN_CONTROL, "Left-to-right embedding");
        addChar('\u202B', "", CharacterCategory.HIDDEN_CONTROL, "Right-to-left embedding");
        addChar('\u202C', "", CharacterCategory.HIDDEN_CONTROL, "Pop directional formatting");
        addChar('\u202D', "", CharacterCategory.HIDDEN_CONTROL, "Left-to-right override");
        addChar('\u202E', "", CharacterCategory.HIDDEN_CONTROL, "Right-to-left override");
        addChar('\u2060', "", CharacterCategory.HIDDEN_CONTROL, "Word joiner");
        addChar('\u2061', "", CharacterCategory.HIDDEN_CONTROL, "Function application");
        addChar('\u2062', "", CharacterCategory.HIDDEN_CONTROL, "Invisible times");
        addChar('\u2063', "", CharacterCategory.HIDDEN_CONTROL, "Invisible separator");
        addChar('\u2064', "", CharacterCategory.HIDDEN_CONTROL, "Invisible plus");
        addChar('\u206A', "", CharacterCategory.HIDDEN_CONTROL, "Inhibit symmetric swapping");
        addChar('\u206B', "", CharacterCategory.HIDDEN_CONTROL, "Activate symmetric swapping");
        addChar('\u206C', "", CharacterCategory.HIDDEN_CONTROL, "Inhibit Arabic form shaping");
        addChar('\u206D', "", CharacterCategory.HIDDEN_CONTROL, "Activate Arabic form shaping");
        addChar('\u206E', "", CharacterCategory.HIDDEN_CONTROL, "National digit shapes");
        addChar('\u206F', "", CharacterCategory.HIDDEN_CONTROL, "Nominal digit shapes");
        addChar('\uFEFF', "", CharacterCategory.HIDDEN_CONTROL, "Zero width no-break space (BOM)");
        
        // Variation selectors
        for (int i = 0xFE00; i <= 0xFE0F; i++) {
            addChar((char) i, "", CharacterCategory.VARIATION, "Variation selector");
        }
        
        // Space characters
        addChar('\u00A0', " ", CharacterCategory.SPACE, "Non-breaking space");
        addChar('\u1680', " ", CharacterCategory.SPACE, "Ogham space mark");
        addChar('\u2000', " ", CharacterCategory.SPACE, "En quad");
        addChar('\u2001', " ", CharacterCategory.SPACE, "Em quad");
        addChar('\u2002', " ", CharacterCategory.SPACE, "En space");
        addChar('\u2003', " ", CharacterCategory.SPACE, "Em space");
        addChar('\u2004', " ", CharacterCategory.SPACE, "Three-per-em space");
        addChar('\u2005', " ", CharacterCategory.SPACE, "Four-per-em space");
        addChar('\u2006', " ", CharacterCategory.SPACE, "Six-per-em space");
        addChar('\u2007', " ", CharacterCategory.SPACE, "Figure space");
        addChar('\u2008', " ", CharacterCategory.SPACE, "Punctuation space");
        addChar('\u2009', " ", CharacterCategory.SPACE, "Thin space");
        addChar('\u200A', " ", CharacterCategory.SPACE, "Hair space");
        addChar('\u202F', " ", CharacterCategory.SPACE, "Narrow no-break space");
        addChar('\u205F', " ", CharacterCategory.SPACE, "Medium mathematical space");
        addChar('\u3000', " ", CharacterCategory.SPACE, "Ideographic space");
        
        // Dashes
        addChar('\u2012', "-", CharacterCategory.DASHES, "Figure dash");
        addChar('\u2013', "-", CharacterCategory.DASHES, "En dash");
        addChar('\u2014', "-", CharacterCategory.DASHES, "Em dash");
        addChar('\u2015', "-", CharacterCategory.DASHES, "Horizontal bar");
        addChar('\u2212', "-", CharacterCategory.DASHES, "Minus sign");
        
        // Quotes and apostrophes
        addChar('\u2018', "'", CharacterCategory.QUOTES, "Left single quotation mark");
        addChar('\u2019', "'", CharacterCategory.QUOTES, "Right single quotation mark");
        addChar('\u201A', "'", CharacterCategory.QUOTES, "Single low-9 quotation mark");
        addChar('\u201B', "'", CharacterCategory.QUOTES, "Single high-reversed-9 quotation mark");
        addChar('\u201C', "\"", CharacterCategory.QUOTES, "Left double quotation mark");
        addChar('\u201D', "\"", CharacterCategory.QUOTES, "Right double quotation mark");
        addChar('\u201E', "\"", CharacterCategory.QUOTES, "Double low-9 quotation mark");
        addChar('\u201F', "\"", CharacterCategory.QUOTES, "Double high-reversed-9 quotation mark");
        addChar('\u2032', "'", CharacterCategory.QUOTES, "Prime");
        addChar('\u2033', "\"", CharacterCategory.QUOTES, "Double prime");
        addChar('\u2034', "'''", CharacterCategory.QUOTES, "Triple prime");
        addChar('\u2035', "'", CharacterCategory.QUOTES, "Reversed prime");
        addChar('\u2036', "\"", CharacterCategory.QUOTES, "Reversed double prime");
        addChar('\u00AB', "\"", CharacterCategory.QUOTES, "Left-pointing double angle quotation mark");
        addChar('\u00BB', "\"", CharacterCategory.QUOTES, "Right-pointing double angle quotation mark");
        
        // Punctuation
        addChar('\u2026', "...", CharacterCategory.PUNCTUATION, "Horizontal ellipsis");
        addChar('\u2022', "*", CharacterCategory.PUNCTUATION, "Bullet");
        addChar('\u00B7', "*", CharacterCategory.PUNCTUATION, "Middle dot");
        
        // Full-width characters
        for (int i = 0xFF01; i <= 0xFF5E; i++) {
            char fullWidth = (char) i;
            char ascii = (char) (i - 0xFF01 + 0x21);
            String description = "Full-width " + getCharacterName(ascii);
            addChar(fullWidth, String.valueOf(ascii), CharacterCategory.FULL_WIDTH, description);
        }
    }
    
    private static void addChar(char character, String replacement, CharacterCategory category, String description) {
        PROBLEMATIC_CHARS.put(character, new CharacterInfo(character, replacement, category, description));
    }
    
    private static String getCharacterName(char c) {
        switch (c) {
            case '!': return "exclamation mark";
            case '"': return "quotation mark";
            case '#': return "number sign";
            case '$': return "dollar sign";
            case '%': return "percent sign";
            case '&': return "ampersand";
            case '\'': return "apostrophe";
            case '(': return "left parenthesis";
            case ')': return "right parenthesis";
            case '*': return "asterisk";
            case '+': return "plus sign";
            case ',': return "comma";
            case '-': return "hyphen-minus";
            case '.': return "full stop";
            case '/': return "solidus";
            case ':': return "colon";
            case ';': return "semicolon";
            case '<': return "less-than sign";
            case '=': return "equals sign";
            case '>': return "greater-than sign";
            case '?': return "question mark";
            case '@': return "commercial at";
            case '[': return "left square bracket";
            case '\\': return "reverse solidus";
            case ']': return "right square bracket";
            case '^': return "circumflex accent";
            case '_': return "low line";
            case '`': return "grave accent";
            case '{': return "left curly bracket";
            case '|': return "vertical line";
            case '}': return "right curly bracket";
            case '~': return "tilde";
            default: return "character";
        }
    }
    
    /**
     * Detects all Unicode issues in the given text.
     * 
     * @param text The text to analyze
     * @return List of detected Unicode issues
     */
    public List<UnicodeIssue> detectIssues(String text) {
        List<UnicodeIssue> issues = new ArrayList<>();
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            CharacterInfo info = PROBLEMATIC_CHARS.get(c);
            
            if (info != null) {
                String context = getContext(text, i, 20);
                issues.add(new UnicodeIssue(i, i + 1, info, context));
            }
        }
        
        return issues;
    }
    
    /**
     * Detects issues for specific character categories.
     */
    public List<UnicodeIssue> detectIssues(String text, Set<CharacterCategory> enabledCategories) {
        List<UnicodeIssue> allIssues = detectIssues(text);
        return allIssues.stream()
                .filter(issue -> enabledCategories.contains(issue.characterInfo.category))
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }
    
    /**
     * Cleans all Unicode issues in the text.
     */
    public String cleanText(String text) {
        StringBuilder cleaned = new StringBuilder(text);
        
        // Process from end to beginning to maintain correct indices
        List<UnicodeIssue> issues = detectIssues(text);
        issues.sort((a, b) -> Integer.compare(b.startOffset, a.startOffset));
        
        for (UnicodeIssue issue : issues) {
            cleaned.replace(issue.startOffset, issue.endOffset, issue.characterInfo.replacement);
        }
        
        // Normalize multiple spaces (but preserve newlines)
        String result = cleaned.toString();
        result = result.replaceAll("[ \\t]+", " ");
        
        // Clean up excessive blank lines
        result = result.replaceAll("\\n{3,}", "\n\n");
        
        return result.trim();
    }
    
    /**
     * Cleans text for specific character categories.
     */
    public String cleanText(String text, Set<CharacterCategory> enabledCategories) {
        StringBuilder cleaned = new StringBuilder(text);
        
        List<UnicodeIssue> issues = detectIssues(text, enabledCategories);
        issues.sort((a, b) -> Integer.compare(b.startOffset, a.startOffset));
        
        for (UnicodeIssue issue : issues) {
            cleaned.replace(issue.startOffset, issue.endOffset, issue.characterInfo.replacement);
        }
        
        return cleaned.toString();
    }
    
    /**
     * Gets character information for a specific character.
     */
    public static CharacterInfo getCharacterInfo(char character) {
        return PROBLEMATIC_CHARS.get(character);
    }
    
    /**
     * Checks if a character is problematic.
     */
    public static boolean isProblematicCharacter(char character) {
        return PROBLEMATIC_CHARS.containsKey(character);
    }
    
    /**
     * Gets all supported character categories.
     */
    public static Set<CharacterCategory> getAllCategories() {
        return EnumSet.allOf(CharacterCategory.class);
    }
    
    private String getContext(String text, int position, int maxLength) {
        int start = Math.max(0, position - maxLength / 2);
        int end = Math.min(text.length(), position + maxLength / 2);
        return text.substring(start, end);
    }
}
