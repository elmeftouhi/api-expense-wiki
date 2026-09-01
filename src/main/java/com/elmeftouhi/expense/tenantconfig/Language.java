package com.elmeftouhi.expense.tenantconfig;

public enum Language {
    EN("English"),
    FR("Français"),
    ES("Español"),
    DE("Deutsch"),
    AR("العربية"),
    ZH("中文"),
    JA("日本語"),
    PT("Português"),
    RU("Русский"),
    IT("Italiano");

    private final String displayName;

    Language(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

