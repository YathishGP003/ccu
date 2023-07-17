package a75f.io.logic.bo.building.sscpu
class SmartStatAssociationUtil {
    companion object {
        fun isAnyRelayEnabledAssociatedToCooling(configuration: ConventionalUnitConfiguration): Boolean {
            return when {
                (configuration.enableRelay1) -> true
                (configuration.enableRelay2) -> true
                else -> false
            }
        }

        fun isAnyRelayEnabledAssociatedToHeating(configuration: ConventionalUnitConfiguration): Boolean {
            return when {
                (configuration.enableRelay4) -> true
                (configuration.enableRelay5) -> true
                else -> false
            }
        }
    }
}