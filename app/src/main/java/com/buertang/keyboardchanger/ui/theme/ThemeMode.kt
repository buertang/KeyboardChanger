package com.buertang.keyboardchanger.ui.theme

enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorageValue(value: String?): ThemeMode {
            return values().firstOrNull { it.storageValue == value } ?: SYSTEM
        }
    }
}
