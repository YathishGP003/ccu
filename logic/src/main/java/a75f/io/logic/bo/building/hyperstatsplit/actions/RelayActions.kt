package a75f.io.logic.bo.building.hyperstatsplit.actions

import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hvac.StandaloneFanStage

/**
 * Created by Nick P on 07-24-2023.
 */
interface RelayActions {

    fun doCoolingStage1( port: Port,
                         coolingLoopOutput: Int,
                         relayActivationHysteresis: Int,
                         divider: Int,
                         relayStages: HashMap<String, Int>)

    fun doCoolingStage2( port: Port,
                         coolingLoopOutput: Int,
                         relayActivationHysteresis: Int,
                         divider: Int,
                         relayStages: HashMap<String, Int>)

    fun doCoolingStage3( port: Port,
                         coolingLoopOutput: Int,
                         relayActivationHysteresis: Int,
                         divider: Int,
                         relayStages: HashMap<String, Int>)

    fun doHeatingStage1(  port: Port,
                          heatingLoopOutput: Int,
                          relayActivationHysteresis: Int,
                          relayStages: HashMap<String, Int>)

    fun doHeatingStage2( port: Port,
                         heatingLoopOutput: Int,
                         relayActivationHysteresis: Int,
                         divider: Int,
                         relayStages: HashMap<String, Int>)

    fun doHeatingStage3( port: Port,
                         heatingLoopOutput: Int,
                         relayActivationHysteresis: Int,
                         relayStages: HashMap<String, Int>)


    fun doFanLowSpeed(port: Port,
                      logicalPointId: String,
                      mediumLogicalPoint : String?,
                      highLogicalPoint : String?,
                      fanMode: StandaloneFanStage,
                      fanLoopOutput: Int,
                      relayActivationHysteresis: Int,
                      relayStages: HashMap<String, Int>,
                      divider: Int)

    fun doFanMediumSpeed(port: Port,
                         logicalPointId: String,
                         superLogicalPoint : String?,
                         fanMode: StandaloneFanStage,
                         fanLoopOutput: Int,
                         relayActivationHysteresis: Int,
                         divider: Int,
                         relayStages: HashMap<String, Int>)

    fun doFanHighSpeed(port: Port,
                       logicalPointId: String,
                       fanMode: StandaloneFanStage,
                       fanLoopOutput: Int,
                       relayActivationHysteresis: Int,
                       relayStages: HashMap<String, Int>)

    fun doFanEnabled( currentState: ZoneState,
                      whichPort: Port,
                      fanLoopOutput : Int)

    fun doOccupiedEnabled( relayPort: Port)

    fun doHumidifierOperation(  relayPort: Port,
                                humidityHysteresis: Int,
                                targetMinInsideHumidity: Double)

    fun doDeHumidifierOperation( relayPort: Port,
                                 humidityHysteresis: Int,
                                 targetMaxInsideHumidity: Double)

    fun doExhaustFanStage1( relayPort: Port,
                            outsideAirFinalLoopOutput: Int,
                            exhaustFanStage1Threshold: Int,
                            exhaustFanHysteresis: Int)


    fun doExhaustFanStage2( relayPort: Port,
                            outsideAirFinalLoopOutput: Int,
                            exhaustFanStage1Threshold: Int,
                            exhaustFanHysteresis: Int)

}