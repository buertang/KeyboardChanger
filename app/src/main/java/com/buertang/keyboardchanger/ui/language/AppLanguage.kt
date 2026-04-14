package com.buertang.keyboardchanger.ui.language

enum class AppLanguage(
    val storageValue: String,
    val localeTag: String
) {
    SIMPLIFIED_CHINESE("zh", "zh"),
    TRADITIONAL_CHINESE("zh-TW", "zh-TW"),
    ENGLISH("en", "en");

    companion object {
        fun fromStorageValue(value: String?): AppLanguage {
            return entries.firstOrNull { it.storageValue == value } ?: SIMPLIFIED_CHINESE
        }
    }
}

