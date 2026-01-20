package com.google.jetstream.presentation.theme

enum class ThemeOption(val id: String, val label: String) {
    Ocean("ocean", "Ocean"),
    Sunset("sunset", "Sunset"),
    Forest("forest", "Forest");

    companion object {
        fun fromId(id: String?): ThemeOption {
            return values().firstOrNull { it.id == id } ?: Ocean
        }
    }
}
