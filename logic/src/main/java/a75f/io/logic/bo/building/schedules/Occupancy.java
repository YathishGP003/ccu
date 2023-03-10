package a75f.io.logic.bo.building.schedules;

/**
 * Created by samjithsadasivan on 3/26/19.
 */
public enum Occupancy
{
    UNOCCUPIED,
    OCCUPIED,
    PRECONDITIONING,
    FORCEDOCCUPIED,
    VACATION,
    OCCUPANCYSENSING,
    AUTOFORCEOCCUPIED,
    AUTOAWAY,
    EMERGENCY_CONDITIONING,
    KEYCARD_AUTOAWAY,
    WINDOW_OPEN,
    NO_CONDITIONING,
    NONE;

    public static String getEnumStringDefinition() {
        return "unoccupied,occupied,preconditioning,forcedoccupied,vacation,occupancysensing,autoforcedoccupied,autoaway," +
                "emergencyconditioning,keycardautoaway,windowopen,noconditioning";
    }
}
