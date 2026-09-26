package dev.aleian.pockethid.action

/**
 * Target operating system of the connected host computer.
 * Used by [ActionResolver] to translate semantic actions into OS-specific HID keycodes.
 */
enum class HostOs(val displayName: String) {
    WINDOWS("Windows"),
    MACOS("macOS"),
    LINUX("Linux");

    companion object {
        fun fromString(value: String): HostOs {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: WINDOWS
        }
    }
}
