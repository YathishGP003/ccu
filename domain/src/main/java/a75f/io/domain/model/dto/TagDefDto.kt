package a75f.io.domain.model.dto

import a75f.io.domain.model.TagDef
import a75f.io.domain.model.ph.core.TagType

data class TagDefDto(
    val id: String?,
    val name: String,
    val kind: String,
    val description: String?,
    val ph4Native: Boolean?,
    val valueEnum: Set<String> = emptySet()
) {
    fun toTagDef(defaultValue: Any? = null): TagDef {
        return TagDef(
            name = name,
            kind = TagType.valueOf(kind.uppercase()),
            valueEnum = valueEnum,
            defaultValue = defaultValue
        )
    }
}
