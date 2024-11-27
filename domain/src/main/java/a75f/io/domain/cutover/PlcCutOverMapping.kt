package a75f.io.domain.cutover

object PlcCutOverMapping {
    val entries = linkedMapOf(
        "analog1InputSensor" to "analog1InputType",
        "th1InputSensor" to "thermistor1InputType",
        "nativeInputSensor" to "nativeSensorType",

        "controlLoopInversion" to "invertControlLoopoutput",
        "setpointSensorOffset" to "setpointSensorOffset",
        "expectZeroErrorAtMidpoint" to "expectZeroErrorAtMidpoint",
        "useAnalogIn2ForSetpoint" to "useAnalogIn2ForSetpoint",
        "analog2InputSensor" to "analog2InputType",
        "relay1ConfigEnabled" to "relay1OutputEnable",
        "relay1OffThreshold" to "relay1OffThreshold",
        "relay1OnThreshold" to "relay1OnThreshold",
        "relay1Cmd" to "relay1Cmd",
        "relay2Cmd" to "relay2Cmd",
        "relay2ConfigEnabled" to "relay2OutputEnable",
        "relay2OnThreshold" to "relay2OnThreshold",
        "relay2OffThreshold" to "relay2OffThreshold",

        "heartBeat" to "heartBeat",
        "pidProportionalRange" to "pidProportionalRange",
        "proportionalKFactor" to "proportionalKFactor",
        "pidIntegralTime" to "pidIntegralTime",
        "integralKFactor" to "integralKFactor",


        "scheduleType" to "scheduleType",
        "equipStatusMessage" to "equipStatusMessage",

        "analog1AtMinOutput" to "analog1MinOutput",
        "analog1AtMaxOutput" to "analog1MaxOutput",
        "processVariable" to "processVariable",
        "controlVariable" to "controlVariable",
        "pidTargetValue" to "pidTargetValue",
        "dynamicTargetValue" to "dynamicTargetValue",

        "otaStatus" to "otaStatus",

    )

}