package a75f.io.domain.model.common.point

class PointState(
    val index: Int,
    val value: String
) : Comparable<PointState> {
    init {
        require(index >= 0)
        //Validation.isNotBlank(this.value, "PointState value")
    }
    override fun compareTo(other: PointState): Int {
        return this.index - other.index
    }

    override fun equals(other: Any?): Boolean {
        if (other is PointState)
            return this.index == other.index && this.value == other.value
        return false
    }

    override fun toString(): String {
        return "$index: $value"
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
