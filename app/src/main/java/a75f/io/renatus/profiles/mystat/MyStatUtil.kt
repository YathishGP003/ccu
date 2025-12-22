package a75f.io.renatus.profiles.mystat

import a75f.io.renatus.util.Option
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.common.point.Constraint
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint

/**
 * Created by Manjunath K on 16-01-2025.
 */


const val MYSTAT: String = "MyStat"
const val CPU: String = "Conventional Package Unit"
const val PIPE2: String = "2 Pipe FCU"
const val PIPE4: String = "4 Pipe FCU"
const val HPU: String = "Heat Pump Unit"

var minMaxVoltage = List(11) { Option(it, it.toString()) }
var lowMediumHighPercent = List(101) { Option(it, it.toString()) }
val testSignalVoltage = (1..100).map {
    Option((it / 10.0).toInt(), (it / 10.0).toString())
}
var damperOpeningRate = (10..100 step 10).toList().map { Option(it, it.toString()) }
fun getPointByDomainName(
    modelDefinition: SeventyFiveFProfileDirective,
    domainName: String
): SeventyFiveFProfilePointDef? {
    return modelDefinition.points.find { (it.domainName.contentEquals(domainName)) }
}

fun getAllowedValues(domainName: String, model: SeventyFiveFProfileDirective): List<Option> {
    val pointDef = getPointByDomainName(model, domainName) ?: return emptyList()
    return if (pointDef.valueConstraint.constraintType == Constraint.ConstraintType.MULTI_STATE) {
        val constraint = pointDef.valueConstraint as MultiStateConstraint
        val enums = mutableListOf<Option>()
        constraint.allowedValues.forEach {
            enums.add(Option(it.index, it.value, it.dis))
        }
        enums
    } else {
        emptyList()
    }
}

