package a75f.io.domain.model

import a75f.io.domain.model.ph.core.TagType

data class TagDef(
    val name: String,
    val kind: TagType,
    val defaultValue: Any? = null,
    val valueEnum: Set<String> = emptySet()
) {
    init {
        if (valueEnum.isNotEmpty() && defaultValue != null) {
            require(defaultValue in valueEnum) {
                "Tag values for $name must be one of $valueEnum"
            }
        }
    }

    override fun toString(): String {
        return if (defaultValue == null) "$name [$kind]" else "$name:$defaultValue [$kind]"
    }

    override fun equals(other: Any?): Boolean {
        return if (other is TagDef) {
            (kind == other.kind && kind == TagType.MARKER && name == other.name) ||
                (kind == other.kind && kind != TagType.MARKER && defaultValue == other.defaultValue)
        } else false
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        defaultValue?.let { result = 31 * result + it.hashCode() }
        return result
    }
}
