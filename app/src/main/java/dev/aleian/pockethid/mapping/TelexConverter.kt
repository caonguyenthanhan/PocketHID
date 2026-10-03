package dev.aleian.pockethid.mapping

/**
 * Converts Vietnamese Unicode strings into Telex keystroke sequences.
 * This allows PocketHID to send Vietnamese text to a PC by generating the
 * raw ASCII keystrokes that a standard physical keyboard would produce.
 * 
 * HOST REQUIREMENT: The PC must be running a Vietnamese IME (e.g., Unikey)
 * configured in TELEX mode.
 */
object TelexConverter {

    private val telexMap = mapOf(
        'ă' to "aw", 'â' to "aa", 'ê' to "ee", 'ô' to "oo", 'ơ' to "ow", 'ư' to "uw", 'đ' to "dd",
        'Ă' to "Aw", 'Â' to "Aa", 'Ê' to "Ee", 'Ô' to "Oo", 'Ơ' to "Ow", 'Ư' to "Uw", 'Đ' to "Dd",

        'á' to "as", 'à' to "af", 'ả' to "ar", 'ã' to "ax", 'ạ' to "aj",
        'Á' to "As", 'À' to "Af", 'Ả' to "Ar", 'Ã' to "Ax", 'Ạ' to "Aj",

        'ắ' to "aws", 'ằ' to "awf", 'ẳ' to "awr", 'ẵ' to "awx", 'ặ' to "awj",
        'Ắ' to "Aws", 'Ằ' to "Awf", 'Ẳ' to "Awr", 'Ẵ' to "Awx", 'Ặ' to "Awj",

        'ấ' to "aas", 'ầ' to "aaf", 'ẩ' to "aar", 'ẫ' to "aax", 'ậ' to "aaj",
        'Ấ' to "Aas", 'Ầ' to "Aaf", 'Ẩ' to "Aar", 'Ẫ' to "Aax", 'Ậ' to "Aaj",

        'é' to "es", 'è' to "ef", 'ẻ' to "er", 'ẽ' to "ex", 'ẹ' to "ej",
        'É' to "Es", 'È' to "Ef", 'Ẻ' to "Er", 'Ẽ' to "Ex", 'Ẹ' to "Ej",

        'ế' to "ees", 'ề' to "eef", 'ể' to "eer", 'ễ' to "eex", 'ệ' to "eej",
        'Ế' to "Ees", 'Ề' to "Eef", 'Ể' to "Eer", 'Ễ' to "Eex", 'Ệ' to "Eej",

        'í' to "is", 'ì' to "if", 'ỉ' to "ir", 'ĩ' to "ix", 'ị' to "ij",
        'Í' to "Is", 'Ì' to "If", 'Ỉ' to "Ir", 'Ĩ' to "Ix", 'Ị' to "Ij",

        'ó' to "os", 'ò' to "of", 'ỏ' to "or", 'õ' to "ox", 'ọ' to "oj",
        'Ó' to "Os", 'Ò' to "Of", 'Ỏ' to "Or", 'Õ' to "Ox", 'Ọ' to "Oj",

        'ố' to "oos", 'ồ' to "oof", 'ổ' to "oor", 'ỗ' to "oox", 'ộ' to "ooj",
        'Ố' to "Oos", 'Ồ' to "Oof", 'Ổ' to "Oor", 'Ỗ' to "Oox", 'Ộ' to "Ooj",

        'ớ' to "ows", 'ờ' to "owf", 'ở' to "owr", 'ỡ' to "owx", 'ợ' to "owj",
        'Ớ' to "Ows", 'Ờ' to "Owf", 'Ở' to "Owr", 'Ỡ' to "Owx", 'Ợ' to "Owj",

        'ú' to "us", 'ù' to "uf", 'ủ' to "ur", 'ũ' to "ux", 'ụ' to "uj",
        'Ú' to "Us", 'Ù' to "Uf", 'Ủ' to "Ur", 'Ũ' to "Ux", 'Ụ' to "Uj",

        'ứ' to "uws", 'ừ' to "uwf", 'ử' to "uwr", 'ữ' to "uwx", 'ự' to "uwj",
        'Ứ' to "Uws", 'Ừ' to "Uwf", 'Ử' to "Uwr", 'Ữ' to "Uwx", 'Ự' to "Uwj",

        'ý' to "ys", 'ỳ' to "yf", 'ỷ' to "yr", 'ỹ' to "yx", 'ỵ' to "yj",
        'Ý' to "Ys", 'Ỳ' to "Yf", 'Ỷ' to "Yr", 'Ỹ' to "Yx", 'Ỵ' to "Yj"
    )

    fun convert(unicodeText: String): String {
        val builder = java.lang.StringBuilder(unicodeText.length * 2)
        for (char in unicodeText) {
            val telex = telexMap[char]
            if (telex != null) {
                builder.append(telex)
            } else {
                builder.append(char)
            }
        }
        return builder.toString()
    }
}
