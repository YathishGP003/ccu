package a75f.io.api.haystack

/**
 * Created by Manjunath K on 26-10-2021.
 */
class Queries {
    companion object{
        const val OUT = "output"
        const val IN = "input"
        const val ANALOG1_OUT = "analog1 and output"
        const val ANALOG2_OUT = "analog2 and output"
        const val ANALOG3_OUT = "analog3 and output"
        const val ANALOG4_OUT = "analog4 and output"
        const val ANALOG1_IN = "analog1 and input"
        const val ANALOG2_IN = "analog2 and input"
        const val EQUIP_AND_TUNER = "equip and tuner"
        const val SENSE_EQUIP = "equip and sense"
        const val PI_LOOP_EQUIP = "equip and pid"
        const val BUILDING_OCCUPANCY = Tags.BUILDING + " and " +Tags.OCCUPANCY
        const val ZONE_COOLING_USER_LIMIT_MAX = "schedulable and zone and cooling and user and limit and max and " +
                "roomRef == "
        const val ZONE_COOLING_USER_LIMIT_MIN = "schedulable and zone and cooling and user and limit and min and " +
                "roomRef == "
        const val ZONE_HEATING_USER_LIMIT_MAX = "schedulable and zone and heating and user and limit and max and " +
                "roomRef == "
        const val ZONE_HEATING_USER_LIMIT_MIN = "schedulable and zone and heating and user and limit and min and " +
                "roomRef == "
        const val ZONE_COOLING_DEADBAND = "schedulable and zone and cooling and deadband and roomRef == "
        const val ZONE_HEATING_DEADBAND = "schedulable and zone and heating and deadband and roomRef == "
        const val ZONE_UNOCCUPIED_ZONE_SETBACK = "schedulable and zone and unoccupied and setback and roomRef == "


    }

}