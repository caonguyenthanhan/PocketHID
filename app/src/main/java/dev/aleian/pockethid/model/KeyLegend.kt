package dev.aleian.pockethid.model

/**
 * Standard key legend model supporting primary and secondary (shifted) characters.
 * Used for hardware-style keycap rendering where the shifted symbol sits above the primary symbol.
 */
data class KeyLegend(
    val primary: String,
    val shifted: String? = null,
    val keyCode: Byte,
    val forceShift: Boolean = false
) {
    /**
     * Resolves the active label to display based on Shift and Caps Lock states.
     * For alphabetic keys (no shifted symbol), handles lowercase/uppercase toggle.
     * For symbol/number keys, returns primary or shifted symbol depending on Shift state.
     */
    fun resolveActiveLabel(isShiftActive: Boolean, isCapsLockActive: Boolean): String {
        return if (shifted != null) {
            if (isShiftActive) shifted else primary
        } else {
            val isUpper = (isCapsLockActive && !isShiftActive) || (!isCapsLockActive && isShiftActive)
            if (isUpper) primary.uppercase() else primary.lowercase()
        }
    }
}

/**
 * Canonical US Keyboard key mappings for standard number row and symbol keys.
 */
object KeyLegends {
    // Row 1: Number Row
    val GRAVE = KeyLegend("`", "~", HidConstants.KEY_GRAVE)
    val NUM_1 = KeyLegend("1", "!", HidConstants.KEY_1)
    val NUM_2 = KeyLegend("2", "@", HidConstants.KEY_2)
    val NUM_3 = KeyLegend("3", "#", HidConstants.KEY_3)
    val NUM_4 = KeyLegend("4", "$", HidConstants.KEY_4)
    val NUM_5 = KeyLegend("5", "%", HidConstants.KEY_5)
    val NUM_6 = KeyLegend("6", "^", HidConstants.KEY_6)
    val NUM_7 = KeyLegend("7", "&", HidConstants.KEY_7)
    val NUM_8 = KeyLegend("8", "*", HidConstants.KEY_8)
    val NUM_9 = KeyLegend("9", "(", HidConstants.KEY_9)
    val NUM_0 = KeyLegend("0", ")", HidConstants.KEY_0)
    val MINUS = KeyLegend("-", "_", HidConstants.KEY_MINUS)
    val EQUAL = KeyLegend("=", "+", HidConstants.KEY_EQUAL)

    val NUMBER_ROW: List<KeyLegend> = listOf(
        GRAVE, NUM_1, NUM_2, NUM_3, NUM_4, NUM_5,
        NUM_6, NUM_7, NUM_8, NUM_9, NUM_0, MINUS, EQUAL
    )

    // Row 2: Q W E R T Y U I O P [ ] \
    val Q = KeyLegend("q", null, HidConstants.KEY_Q)
    val W = KeyLegend("w", null, HidConstants.KEY_W)
    val E = KeyLegend("e", null, HidConstants.KEY_E)
    val R = KeyLegend("r", null, HidConstants.KEY_R)
    val T = KeyLegend("t", null, HidConstants.KEY_T)
    val Y = KeyLegend("y", null, HidConstants.KEY_Y)
    val U = KeyLegend("u", null, HidConstants.KEY_U)
    val I = KeyLegend("i", null, HidConstants.KEY_I)
    val O = KeyLegend("o", null, HidConstants.KEY_O)
    val P = KeyLegend("p", null, HidConstants.KEY_P)
    val LBRACKET = KeyLegend("[", "{", HidConstants.KEY_LEFTBRACE)
    val RBRACKET = KeyLegend("]", "}", HidConstants.KEY_RIGHTBRACE)
    val BACKSLASH = KeyLegend("\\", "|", HidConstants.KEY_BACKSLASH)

    val ROW_2: List<KeyLegend> = listOf(
        Q, W, E, R, T, Y, U, I, O, P, LBRACKET, RBRACKET, BACKSLASH
    )

    // Row 3: A S D F G H J K L ; '
    val A = KeyLegend("a", null, HidConstants.KEY_A)
    val S = KeyLegend("s", null, HidConstants.KEY_S)
    val D = KeyLegend("d", null, HidConstants.KEY_D)
    val F = KeyLegend("f", null, HidConstants.KEY_F)
    val G = KeyLegend("g", null, HidConstants.KEY_G)
    val H = KeyLegend("h", null, HidConstants.KEY_H)
    val J = KeyLegend("j", null, HidConstants.KEY_J)
    val K = KeyLegend("k", null, HidConstants.KEY_K)
    val L = KeyLegend("l", null, HidConstants.KEY_L)
    val SEMICOLON = KeyLegend(";", ":", HidConstants.KEY_SEMICOLON)
    val APOSTROPHE = KeyLegend("'", "\"", HidConstants.KEY_APOSTROPHE)

    val ROW_3: List<KeyLegend> = listOf(
        A, S, D, F, G, H, J, K, L, SEMICOLON, APOSTROPHE
    )

    // Row 4: Z X C V B N M , . /
    val Z = KeyLegend("z", null, HidConstants.KEY_Z)
    val X = KeyLegend("x", null, HidConstants.KEY_X)
    val C = KeyLegend("c", null, HidConstants.KEY_C)
    val V = KeyLegend("v", null, HidConstants.KEY_V)
    val B = KeyLegend("b", null, HidConstants.KEY_B)
    val N = KeyLegend("n", null, HidConstants.KEY_N)
    val M = KeyLegend("m", null, HidConstants.KEY_M)
    val COMMA = KeyLegend(",", "<", HidConstants.KEY_COMMA)
    val DOT = KeyLegend(".", ">", HidConstants.KEY_DOT)
    val SLASH = KeyLegend("/", "?", HidConstants.KEY_SLASH)

    val ROW_4: List<KeyLegend> = listOf(
        Z, X, C, V, B, N, M, COMMA, DOT, SLASH
    )
}
