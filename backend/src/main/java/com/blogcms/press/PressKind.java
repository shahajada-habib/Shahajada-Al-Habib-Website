package com.blogcms.press;

import java.util.List;

/**
 * The kinds of work that show up in the press section. Stored as slugs so the
 * visible label can come from the message bundle and follow the language
 * toggle; the display order here is the order the filter chips appear in.
 */
public final class PressKind {

    public static final String FEATURE = "feature";
    public static final String DEFAULT = FEATURE;

    public static final List<String> ALL = List.of(
            FEATURE,
            "report",
            "literature",
            "column",
            "interview",
            "travel",
            "photography");

    private PressKind() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return DEFAULT;
        }
        String normalized = value.trim().toLowerCase();
        return ALL.contains(normalized) ? normalized : DEFAULT;
    }
}
