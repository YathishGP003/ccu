package a75f.io.domain.modeldef

var i = 0
var nameS = "name "

// TODO: This client must be bundled with a Domain Model SDK that JVM consumers can use to read models
class ModelDef(
    val id: String,
    val name: String,
    val tagNames: Set<String>,
    val tags: Set<TagDef>,
    val points: List<PointDef>,
    val equips: List<ModelDef>,
    val namespace: String? = null,
    val manufacturerName: String? = null,
) {

    override fun toString(): String {
        return "$name $tagNames"
    }

    /*fun allPoints(): Set<PointDef> {
        return (points + equips.map { it.allPoints() }.flatten()).toSet()
    }

    fun allPointIds(): Set<String> {
        return (points.map { it.id } + equips.map { it.allPointIds() }.flatten()).toSet()
    }*/
}

class ModelDefDto(
    val id: String,
    val name: String,
    val tagNames: Set<String>,
    val tags: List<TagDefDto>,
    val points: List<ModelPointDto>,
    val equips: List<ModelEquipDto>,
    val namespace: String,
    private val manufacturerName: String? = null,
) {
    fun toModelDef(): ModelDef {
        return ModelDef(
            id = id,
            name = name,
            tagNames = tagNames.filterNot { it in EQUIP_DEFAULT_TAG_NAMES }.toSet(),
            tags = tags.filterNot { it.name in EQUIP_DEFAULT_TAG_NAMES }.map(TagDefDto::toTagDef).toSet(),
            points = points.map(ModelPointDto::toPointDef),
            equips = equips.map(ModelEquipDto::toModelDef),
            namespace = namespace,
            manufacturerName = manufacturerName
        )
    }
}

class ModelVersion(val id: String, val version: String? = null)

class PointDef(
    val id: String?,
    val name: String,
    val tagNames: Set<String>,
    val rootTagNames: Set<String>?,
    val tags: Set<TagDef>,
    val pointType: PointType?,
    val valueConstraint: Constraint?,
    val presentationData: Map<String, Any>?,
    val defaultUnit: String?,
) {

    init {
        if (valueConstraint is NumericConstraint) {
            require(defaultUnit != null) {
                "Point with a numeric value constraint must have a default unit"
            }
        }
    }

    /**
     * Any 2 ModelPointDefs are considered equal if they have the same pointDefId & tags
     */
    override fun equals(other: Any?): Boolean {
        return other is PointDef && id == other.id && tags == other.tags
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + tags.hashCode()
        return result
    }

    override fun toString(): String {
        return "$name $tags"
    }
}

class ModelPointDto(
    private val pointId: String,
    private val point: PointDefDto,
    private val isGpc36: Boolean,
    private val tagNames: Set<String>,
    val tags: List<TagDefDto> = emptyList(),
    // Model-Level Point Overrides
    val name: String?,
    private val description: String?,
    private val pointType: PointType?,
    private val valueConstraint: Constraint?,
    private val presentationData: Map<String, Any>?,
    private val defaultUnit: String?,
    private val tagValues: Map<String, Any>?,
) {
    fun toPointDef(): PointDef {
        val constraint = valueConstraint ?: point.getValueConstraint()

        val defaultTagNames = when (constraint) {
            is NumericConstraint -> NUMERIC_POINT_TAG_NAMES
            is MultiStateConstraint -> NUMERIC_POINT_TAG_NAMES
            else -> POINT_DEFAULT_TAG_NAMES
        }

        return PointDef(
            id = pointId,
            name = name ?: point.name,
            tagNames = (tagNames + point.tagNames).filterNot { it in defaultTagNames }.toSortedSet(),
            rootTagNames = point.tagNames - defaultTagNames,
            tags = (tags + point.tags).filterNot { it.name in defaultTagNames }.map { it.toTagDef(tagValues?.get(it.name)) }.toSortedSet { t1, t2 -> t1.name.compareTo(t2.name) },
            pointType = pointType ?: point.pointType,
            valueConstraint = constraint,
            presentationData = presentationData ?: point.presentationData,
            defaultUnit = defaultUnit ?: point.defaultUnit
        )
    }
}

class PointDefDto(
    val id: String,
    val name: String,
    val description: String,
    val pointType: PointType,
    val tagNames: Set<String>,
    val tags: List<TagDefDto>,
    val defaultUnit: String?,
    val is75F: Boolean,
    val valueConstraintType: Constraint.ConstraintType,
    val allowedValues: List<PointState>?,
    val minValue: Double?,
    val maxValue: Double?,
    val presentationData: Map<String, Any>?
) {
    fun getValueConstraint(): Constraint {
        return if (minValue != null && maxValue !== null) {
            NumericConstraint(minValue, maxValue)
        } else if (!allowedValues.isNullOrEmpty()) {
            MultiStateConstraint(allowedValues)
        } else NoConstraint()
    }

    fun toPointDef(): PointDef {
        val constraint = getValueConstraint()
        val defaultTagNames = when (constraint) {
            is NumericConstraint -> NUMERIC_POINT_TAG_NAMES
            is MultiStateConstraint -> MULTI_DEFAULT_TAG_NAMES
            else -> POINT_DEFAULT_TAG_NAMES
        }

        return PointDef(
            id = id,
            name = name,
            tagNames = tagNames.filterNot { it in defaultTagNames }.toSet(),
            rootTagNames = tagNames.filterNot { it in defaultTagNames }.toSet(),
            tags = tags.filterNot { it.name in defaultTagNames }.map { it.toTagDef() }.toSet(),
            pointType = pointType,
            valueConstraint = constraint,
            presentationData = presentationData,
            defaultUnit = defaultUnit
        )
    }
}

class ModelEquipDto(
    val equipId: String,
    val equip: ModelDefDto,
    val name: String?,
    private val tagNames: Set<String>,
    val points: List<ModelPointDto>,
    val tags: List<TagDefDto>?,
    val tagValues: Map<String, Any>?
) {
    fun toModelDef(): ModelDef {
        return ModelDef(
            id = equipId,
            name = name ?: equip.name,
            tagNames = (tagNames + equip.tagNames).filterNot { it in EQUIP_DEFAULT_TAG_NAMES }.toSet(),
            tags = ((tags ?: emptyList()) + equip.tags)
                .filterNot { it.name in EQUIP_DEFAULT_TAG_NAMES }
                .map { it.toTagDef(tagValues?.get(it.name)) }.toSet(),
            points = points.map(ModelPointDto::toPointDef),
            equips = equip.equips.map(ModelEquipDto::toModelDef),
            namespace = equip.namespace
        )
    }
}

data class TagDefDto(
    val id: String,
    val name: String,
    val kind: String,
    val description: String,
    val ph4Native: Boolean,
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

data class TagDef(
    val name: String,
    val kind: TagType,
    val defaultValue: Any? = null,
    val valueEnum: Set<String> = emptySet()
) {
    constructor(tag: Tag) : this(tag.name, tag.type, tag.value)

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

class PointState(
    val index: Int,
    val value: String
) : Comparable<PointState> {
    override fun compareTo(other: PointState): Int {
        return this.index - other.index
    }
}

abstract class Constraint {
    abstract val constraintType: ConstraintType
    enum class ConstraintType {
        NONE,
        MULTI_STATE,
        NUMERIC,
    }
}

class MultiStateConstraint(
    val allowedValues: List<PointState>,
) : Constraint() {
    override val constraintType = ConstraintType.MULTI_STATE
    constructor(allowedValues: String) : this(
        allowedValues = allowedValues.split(",").mapIndexed { index, s -> PointState(index, s) }
    )
}

data class NumericConstraint(
    val minValue: Double,
    val maxValue: Double,
) : Constraint() {
    override val constraintType = ConstraintType.NUMERIC
}

class NoConstraint : Constraint() {
    override val constraintType = ConstraintType.NONE
}

enum class PointType(val displayName: String) {
    NUMBER("Number"),
    STR("Str"),
    BOOL("Bool"),
}

val EQUIP_DEFAULT_TAG_NAMES = setOf(Tags.EQUIP, Tags.ZONE, Tags.SOURCE_MODEL, Tags.ROOM_REF, Tags.FLOOR_REF, Tags.SPACE_REF, Tags.SITE_REF, Tags.EQUIP_REF)
val POINT_DEFAULT_TAG_NAMES = setOf(Tags.ZONE, Tags.POINT, Tags.KIND, Tags.HIS, Tags.WRITABLE, Tags.CUR, Tags.TIMEZONE, Tags.EQUIP_REF, Tags.SPACE_REF, Tags.ROOM_REF, Tags.FLOOR_REF, Tags.SOURCE_POINT)
val NUMERIC_POINT_TAG_NAMES = POINT_DEFAULT_TAG_NAMES + setOf(Tags.UNIT, Tags.MIN_VALUE, Tags.MAX_VALUE, Tags.INCREMENT_VALUE)
val MULTI_DEFAULT_TAG_NAMES = POINT_DEFAULT_TAG_NAMES + Tags.ENUMERATIONS