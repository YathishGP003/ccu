package a75f.io.domain.model.common.point

import a75f.io.domain.model.common.Version

enum class ComparisonType {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_OR_EQUAL_TO,
    LESS_THAN_OR_EQUAL_TO
}

/**
 * Defines the profile configuration logic for creating a point.
 */
abstract class PointConfiguration {
    abstract val configurationType: ConfigType
    enum class ConfigType {
        BASE, // A base point is always created for a profile
        DEPENDENT, // A dependent point is created if the point it depends on has a certain value
        ASSOCIATION, // Associates another point with one of the points specified in its list of allowedValues. Can only be used on enum-type points.
        ASSOCIATED, // An associated point is created based on the criteria specified on an ASSOCIATION-type point
        DYNAMIC_SENSOR // A dynamic point is created based on a certain reading of the point
    }
}

class PointVal (val index : Int, val value: String)
class BaseConfiguration : PointConfiguration(){
    override val configurationType = ConfigType.BASE
}

class AssociatedConfiguration : PointConfiguration(){
    override val configurationType = ConfigType.ASSOCIATED
}
class AssociationConfiguration (val domainName : String, val value : PointVal, val comparisonType : String) : PointConfiguration(){
    override val configurationType = ConfigType.ASSOCIATION
}

class DynamicSensorConfiguration (val sensorType : String, val value : Double, val comparisonType : String) : PointConfiguration(){
    override val configurationType = ConfigType.DYNAMIC_SENSOR
}

class DependentConfiguration (val domainName : String, val value : PointVal) : PointConfiguration(){
    override val configurationType = ConfigType.DEPENDENT
}
class ModelAssociation(val modelId: String, val version: Version)
