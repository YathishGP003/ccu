package a75f.io.logic.bo.building.ss4pfcu

class FourPipeFanCoilUnitConfigurationUtil {
    companion object {
        fun isAnyRelayEnabledAssociatedToCooling(configuration: FourPipeFanCoilUnitConfiguration): Boolean {
            return when {
                (configuration.enableRelay6) -> true
                else -> false
            }
        }

        fun isAnyRelayEnabledAssociatedToHeating(configuration: FourPipeFanCoilUnitConfiguration): Boolean {
            return when {
                (configuration.enableRelay4) -> true
                else -> false
            }
        }
    }
}