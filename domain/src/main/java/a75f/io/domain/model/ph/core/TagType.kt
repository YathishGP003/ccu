package a75f.io.domain.model.ph.core

import java.util.*

enum class TagType {
    BOOL,
    COORD,
    DATE,
    DATETIME,
    DICT,
    LIST,
    MARKER,
    NA,
    NUMBER,
    REF,
    SCALAR, // Str, Number, or Bool
    STR,
    TIME,
    URI;

    fun nameMatches(other: String) = name.lowercase(Locale.getDefault()) == other.lowercase(Locale.getDefault())

    companion object {
        fun isValid(tagType: String): Boolean {
            try {
                valueOf(tagType.uppercase(Locale.getDefault()))
            } catch (ex: Exception) {
                return false
            }
            return true
        }

        fun names() = values().map { it.name.lowercase(Locale.getDefault()) }
    }
}
