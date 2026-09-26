package dev.aleian.pockethid.gamepad

enum class StickResponseCurve(val displayName: String) {
    LINEAR("Linear"),
    PRECISION("Precision"),
    AGGRESSIVE("Aggressive");

    companion object {
        fun fromString(value: String): StickResponseCurve {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: LINEAR
        }
    }
}
