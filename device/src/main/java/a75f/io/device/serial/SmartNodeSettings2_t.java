package a75f.io.device.serial;

import org.javolution.io.Struct;

public class SmartNodeSettings2_t extends Struct {

    public final Enum8<ProfileMap_t> profileMap2 = new Enum8<>(ProfileMap_t.values()); // Refer to ProfileMap_t
    public final Unsigned16 kFactor = new Unsigned16(); ///< 1/100th of the value. Default - 200(2.00)
    public final Unsigned16 minCFMCooling = new Unsigned16(); ///< Ranges from 50-4000. Increments of 5. Default - 50
    public final Unsigned16 maxCFMCooling = new Unsigned16(); ///< Ranges from 50-4000. Increments of 5. Default - 250
    public final Unsigned16 minCFMReheating = new Unsigned16(); ///< Ranges from 50-4000. Increments of 5. Default - 50
    public final Unsigned16 maxCFMReheating = new Unsigned16(); ///< Ranges from 50-4000. Increments of 5. Default - 250
    public final Enum8<DamperShape_t> damperShape = new Enum8<>(DamperShape_t.values()); ///< Refer to DamperShape_t. Default - Round
    public final Enum8<CondensateSensor_t> condensateSensor = new Enum8<>(CondensateSensor_t.values()); ///< Refer to CondensateSensor_t. Default - Normally Open
    public final Unsigned8 damperSize = new Unsigned8(); ///< Value in Inches. Will be converted to Feet in Fail-safe. Nick: to confirm
    public final Unsigned16 airflowCFMProportionalRange = new Unsigned16(); ///< 0-1500. Increments of 10. Default - 200
    public final Unsigned8 airflowCFMProportionalKFactor = new Unsigned8(); ///< 1/100th of the value. Default - 50
    public final Unsigned8 airflowCFMIntegralTime = new Unsigned8(); ///< Ranges from 1-60. Default - 30mins
    public final Unsigned8 airflowCFMIntegralKFactor = new Unsigned8(); ///< 1/100th of the value. Default - 50
    public final Unsigned8 enableCFM = new Unsigned8(1); ///< . Default - False(Disabled)

}
