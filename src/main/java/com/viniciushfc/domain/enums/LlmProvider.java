package com.viniciushfc.domain.enums;

public enum LlmProvider {
    GEMINI("Gemini", Integer.MAX_VALUE),
    GROQ("Groq", 18_000),
    CLAUDE("Claude", Integer.MAX_VALUE),
    LOCAL("IA local", 12_000);

    public final String label;
    public final int maxJobChars;

    LlmProvider(String label, int maxJobChars) {
        this.label = label;
        this.maxJobChars = maxJobChars;
    }
}
