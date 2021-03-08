package a75f.io.logic.bo.building.hvac;

/**
 * Enum maps to all the fan stages used in Fan coil equipments.
 *
 *         <item>  Off </item>
 *         <item>  Auto </item>
 *         <item>  Fan Low Current Occupied Period </item>
 *         <item>  Fan Low Occupied Period</item>
 *         <item>  Fan Low All Times</item>
 *         <item>  Fan Medium Current Occupied Period </item>
 *         <item>  Fan Medium Occupied Period</item>
 *         <item>  Fan Medium All Times</item>
 *         <item>  Fan High Current Occupied Period </item>
 *         <item>  Fan High Occupied Period</item>
 *         <item>  Fan High All Times</item>
 */
public enum SSEFanStage {
    OFF,
    AUTO,
    LOW_CUR_OCC,
    LOW_OCC,
    LOW_ALL_TIME,
    MEDIUM_CUR_OCC,
    MEDIUM_OCC,
    MEDIUM_ALL_TIME,
    HIGH_CUR_OCC,
    HIGH_OCC,
    HIGH_ALL_TIME,
}
