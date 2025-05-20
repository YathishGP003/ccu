package a75f.io.domain.logic

import io.seventyfivef.domainmodeler.common.point.ComparisonType
import io.seventyfivef.domainmodeler.common.point.Operator

/**
 * Created by Manjunath K on 13-09-2024.
 */

// Function to apply the logical operators between two Boolean values
fun applyOperator(left: Boolean, operator: Operator, right: Boolean): Boolean {
    return when (operator) {
        Operator.AND -> left && right
        Operator.OR -> left || right
        Operator.NONE -> true
    }
}

fun applyLogicalOperations(booleanList: List<Boolean>, operators: List<Operator>): Boolean {

    if (booleanList.size == 1)
        return booleanList[0]

    if (booleanList.size != operators.size) {
        return false
    }

    // Start with the first Boolean value
    var result = booleanList[0]

    // Apply each operator from left to right
    for (i in 1 until booleanList.size) {
        result = applyOperator(result, operators[i], booleanList[i])
    }

    return result
}

fun evaluateConfiguration(comparisonType: ComparisonType, leftVal : Int, rightVal: Int) :  Boolean {
    return when(comparisonType) {
        ComparisonType.EQUALS -> leftVal == rightVal
        ComparisonType.NOT_EQUALS -> leftVal != rightVal
        ComparisonType.GREATER_THAN -> leftVal > rightVal
        ComparisonType.LESS_THAN -> leftVal < rightVal
        ComparisonType.GREATER_THAN_OR_EQUAL_TO -> leftVal >= rightVal
        ComparisonType.LESS_THAN_OR_EQUAL_TO -> leftVal <= rightVal
    }
}


