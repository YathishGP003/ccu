package a75f.io.domain.modeldef

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.util.*

class Tag(
    val name: String,
    val type: TagType = TagType.MARKER,
    val value: Any? = null
) {

    fun implements(tagDef: TagDef): Boolean {
        return tagDef.kind == type
    }

    override fun toString(): String {
        return if (value == null) "$name [$type]" else "$name:$value [$type]"
    }

    /**
     * 2 Tags are considered equal if:
     * 1) both are markers and have the same name
     * 2) both are of the same non-marker type and have equal non-null values
     */
    override fun equals(other: Any?): Boolean {
        return if (other is Tag) {
            (type == other.type && type == TagType.MARKER && name == other.name) || (type == other.type && value != null && value == other.value)
        } else false
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        value?.let { result = 31 * result + it.hashCode() }
        return result
    }
}

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

    class Adapter {
        @ToJson
        fun toJson(type: TagType): String {
            return type.name.lowercase()
        }

        @FromJson
        fun fromJson(type: String): TagType? {
            return values().find { it.name.lowercase() == type }
        }
    }
}
