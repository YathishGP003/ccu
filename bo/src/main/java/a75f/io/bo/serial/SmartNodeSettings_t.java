package a75f.io.bo.serial;

import javolution.io.Struct;
import javolution.io.Union;

/**
 * Created by ryanmattison on 7/25/17.
 */

public class SmartNodeSettings_t extends Struct {


    public final Unsigned8 maxUserTem = new Unsigned8(); /* default 80 - in deg F */
    public final Unsigned8 minUserTemp = new Unsigned8(); /* default 60 - in deg F */
    public final Unsigned8 maxDamperOpen = new Unsigned8(); /* default 100 - in percent */
    public final Unsigned8 minDamperOpen = new Unsigned8(); /* default 40 - in percent */
    public final Unsigned8 temperatureOffset = new Unsigned8(); /* default 0 - in 1/10 deg F. This is added to the measured temp */
    public final Unsigned8 forwardMotorBacklash = new Unsigned8(); /* default 5 - backlash to be added when motor changes direction from
reverse to forward - in % damper opening */
    public final Unsigned8 reverseMotorBacklash = new Unsigned8(); /* default 5 - backlash to be added when motor changes direction from
forward to reverse - in % damper opening */
    public final Unsigned8 proportionalConstant = new Unsigned8(); /* default 50 - k constants for Proportional in 1/100 */
    public final Unsigned8 integralConstant = new Unsigned8(); /* default 50 - k constant for Integral in 1/100 */
    public final Unsigned8 proportionalTemperatureRange = new Unsigned8(); /* default 15 - temp range in 1/10 deg that the proportional
control will apply */
    public final Unsigned8 integrationTime = new Unsigned8(); /* default 30 - time in minutes the integration takes to max out */
    public final Unsigned8 airflowHeatingTemperature = new Unsigned8(); /* default 105 - airflow temperature in deg F above which we consider
unit is in heating mode for failsafe mode */
    public final Unsigned8 airflowCoolingTemperature = new Unsigned8(); /* default 60 - airflow temperature in deg F below which we consider
unit is in cooling mode for failsafe mode */
    public final SmartNodeLedBitmap_t ledBitmap = inner(new SmartNodeLedBitmap_t()); /* Determines which LEDs are enabled */
    public final SmartNodeProfileBitmap_t profileBitmap = inner(new SmartNodeProfileBitmap_t()); /* Determines which profiles are enabled */
    public final Unsigned8 lightingIntensityForOccupantDetected = new Unsigned8(); /* Lighting intensity (%) to use when occupants are
detected */
    public final Unsigned8 minLightingControlOverrideTimeInMinutes = new Unsigned8(); /* Minimum time that a lighting control override will
stay in effect */
    public final Unsigned8 defaultOutsideAirOptimizationDamperPosition = new Unsigned8(); /* Percentage to open OAO damper if connection to
CCU is lost */
    public final Enum8<DamperActuator_t> outsideAirOptimizationDamperActuatorType = new Enum8<>(DamperActuator_t.values()); /* Type of actuator used for the OAO damper
*/
    public final Enum8<DamperActuator_t> returnAirDamperActuatorType = new Enum8<>(DamperActuator_t.values());


    public final UTF8String roomName = new UTF8String(SerialConsts.ROOM_NAME_MAX_LENGTH);

    public final SmartNodeSettings_t_Extras smartNodeSettingsMerge = inner(new SmartNodeSettings_t_Extras());

    public class SmartNodeSettings_t_Extras extends Union
    {

        public final Unsigned8 mergeInnerUnsignedInt = new Unsigned8();
        public final SmartNodeMergeInner mergeInner = inner(new SmartNodeMergeInner());

        public class SmartNodeMergeInner extends Struct
        {
            public final BitField showCentigrade = new BitField(1);
            public final BitField displayHold = new BitField(1);
            public final BitField militaryTime = new BitField(1);
            public final BitField enableOccupationDetection = new BitField(1);
        }

    }

}
