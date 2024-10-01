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

    public final Enum8<InputSensorType_t> inputSensor1 = new Enum8<>(InputSensorType_t.values()); //Analog 1 or TH1 or Native Sensor
    public final Enum8<InputSensorType_t> inputSensor2 = new Enum8<>(InputSensorType_t.values()); //Analog2 if enabled
    public final Signed16 setpointSensorOffset = new Signed16();
    public final Signed16 genericPiProportionalRange = new Signed16();
    public final Unsigned8 turnOnRelay1 = new Unsigned8();
    public final Unsigned8 turnOnRelay2 = new Unsigned8();
    public final Unsigned8 turnOffRelay1 = new Unsigned8();
    public final Unsigned8 turnOffRelay2 = new Unsigned8();
    public final Unsigned8 expectedZeroErrorAtMidpoint = new Unsigned8(1);
    public final Unsigned8 invertControlLoopOutput = new Unsigned8(1);
    public final Unsigned8 useAnalogIn2ForDynamicSetpoint = new Unsigned8(1);
    public final Unsigned8 relay1Enable = new Unsigned8(1);
    public final Unsigned8 relay2Enable = new Unsigned8(1);
    public final Unsigned8 runPILoopOnNode = new Unsigned8(1);
    public final Unsigned8 minVolt = new Unsigned8();
    public final Unsigned8 maxVolt = new Unsigned8();
    public final Signed16 minEngVal = new Signed16();
    public final Signed16 maxEngVal = new Signed16();
    public final RelayBitmap_t relayBitmap = inner(new RelayBitmap_t()); // This is used to set the type of relay used ex : NO or NC.
    public final Unsigned8 maxDischargeAirTemperature = new Unsigned8(); // 70-120 default of 90

    public class RelayBitmap_t extends Struct
    {
        public final Unsigned8 relay1 = new Unsigned8(1); // This bit is based on relay type( NO: 1 or NC: 0)
        public final Unsigned8 relay2 = new Unsigned8(1); //This bit is based on relay type( NO: 1 or NC: 0)
        public final Unsigned8 relay3 = new Unsigned8(1); //This bit is based on relay type( NO: 1 or NC: 0)
        public final Unsigned8 relay4 = new Unsigned8(1); //This bit is based on relay type( NO: 1 or NC: 0)
    }

}
