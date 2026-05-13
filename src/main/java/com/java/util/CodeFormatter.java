package com.java.util;

public class CodeFormatter {
    private CodeFormatter() {}

    public static String formatGeneratedCode(String code, String language) {
        if (code == null) return "";

        String normalized = code
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\\t", "    ")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();

        if (language == null) return normalized;

        String lang = language.toLowerCase();
        if ("python".equals(lang) || "py".equals(lang)) {
            return normalized;
        }
        if (!"java".equals(lang) && !"cpp".equals(lang) && !"c++".equals(lang)) {
            return normalized;
        }

        String compact = normalized.replaceAll("\\s+", " ");
        if (normalized.lines().count() > 3 && normalized.length() != compact.length()) {
            return normalized;
        }

        String expanded = compact
                .replace("{", "{\n")
                .replace("}", "\n}\n")
                .replace(";", ";\n")
                .replaceAll("\\n\\s*\\n+", "\n");

        return indentBraceLanguage(expanded);
    }

    private static String indentBraceLanguage(String code) {
        StringBuilder out = new StringBuilder();
        int indent = 0;

        for (String rawLine : code.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("}")) {
                indent = Math.max(0, indent - 1);
            }

            out.append("    ".repeat(indent)).append(line).append("\n");

            if (line.endsWith("{")) {
                indent++;
            }
        }

        return out.toString().trim();
    }
}
