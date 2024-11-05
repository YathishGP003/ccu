package a75f.io.renatus.profiles.plc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PlcProfileViewState {

    var analogIn1InputSensor by mutableStateOf (0.0)
    var targetValue by mutableStateOf (0.0)
    var th1InInputSensor by mutableStateOf (0.0)
    var expectedErrorRange by mutableStateOf (0.0)
    var nativeSensorInput by mutableStateOf (0.0)


    var zeroErrorAtMidpoint by mutableStateOf (false)
    var invertControlLoopOp by mutableStateOf (false)
    var useAnalogIn2DynamicSp by mutableStateOf (false)

    var analogIn2InputSensor by mutableStateOf (0.0)
    var setpointSensorOffset by mutableStateOf (0.0)
    var analogOut1AtMinOp by mutableStateOf (0.0)
    var analogOut1AtMaxOp by mutableStateOf (0.0)

    var relay1 by mutableStateOf (false)
    var relay2 by mutableStateOf (false)
    
    var turnOnRelay1 by mutableStateOf (0.0)
    var turnOnRelay2 by mutableStateOf (0.0)
    var turnOffRelay1 by mutableStateOf (0.0)
    var turnOffRelay2 by mutableStateOf (0.0)
}