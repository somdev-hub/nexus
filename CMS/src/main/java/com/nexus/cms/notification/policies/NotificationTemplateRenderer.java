package com.nexus.cms.notification.policies;

import java.util.Map;

/**
 * Renders a notification template.
 * <p>
 * A template is plain text with {@code {placeholder}} markers. A placeholder
 * is replaced by the matching value; one that has no value is removed, so a
 * missing fact never leaves a stray {@code {token}} in front of a user.
 */
public final class NotificationTemplateRenderer {

    private NotificationTemplateRenderer() {
    }

    /**
     * Render a template against a set of values.
     *
     * @param template the template, may be null
     * @param values   the values available to the template
     * @return the rendered text, never null
     */
    public static String render(String template, Map<String, Object> values) {
        if (template == null || template.isBlank()) {
            return "";
        }
        if (values == null || values.isEmpty()) {
            return template.replaceAll("\\{[^}]*}", "");
        }

        StringBuilder rendered = new StringBuilder(template.length() + 64);
        int index = 0;
        while (index < template.length()) {
            int open = template.indexOf('{', index);
            if (open < 0) {
                rendered.append(template, index, template.length());
                break;
            }
            int close = template.indexOf('}', open);
            if (close < 0) {
                // Unterminated placeholder — treat the rest as literal text.
                rendered.append(template, index, template.length());
                break;
            }
            rendered.append(template, index, open);
            String key = template.substring(open + 1, close).trim();
            Object value = values.get(key);
            if (value != null) {
                rendered.append(value);
            }
            index = close + 1;
        }
        return rendered.toString().replaceAll("\\s{2,}", " ").trim();
    }
}
