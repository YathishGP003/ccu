package a75f.io.logic.bo.building.hyperstat.actions

import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.common.BasicSettings
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.HyperStatPipe2Equip


/**
 * Created by Manjunath K on 11-07-2022.
 */

interface RelayActions {

    fun doCoolingStage1( port: Port,
                         coolingLoopOutput: Int,
                         relayActivationHysteresis: Int,
                         relayStages: HashMap<String, Int>)

    fun doCoolingStage2( port: Port,
                         coolingLoopOutput: Int,
                         relayActivationHysteresis: Int,
                         divider: Int,
                         relayStages: HashMap<String, Int>)

    fun doCoolingStage3( port: Port,
                         coolingLoopOutput: Int,
                         relayActivationHysteresis: Int,
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


    fun doFanLowSpeed(logicalPointId: String,
                      mediumLogicalPoint : String?,
                      highLogicalPoint : String?,
                      fanMode: StandaloneFanStage,
                      fanLoopOutput: Int,
                      relayActivationHysteresis: Int,
                      relayStages: HashMap<String, Int>,
                      divider: Int)

    fun doFanMediumSpeed( logicalPointId: String,
                          superLogicalPoint : String?,
                          fanMode: StandaloneFanStage,
                          fanLoopOutput: Int,
                          relayActivationHysteresis: Int,
                          divider: Int,
                          relayStages: HashMap<String, Int>)

    fun doFanHighSpeed( logicalPointId: String,
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
    fun doRelayWaterValveOperation(
        equip: HyperStatPipe2Equip,
        port: Port,
        basicSettings: BasicSettings,
        loopOutput: Int,
        relayActivationHysteresis: Int,
        relayStages: HashMap<String, Int>
    )
    fun doCompressorStage1( port: Port,
                            compressorLoopOutput: Int,
                         relayActivationHysteresis: Int,
                         relayStages: HashMap<String, Int>,
                            zoneMode: ZoneState)
    fun doCompressorStage2(
                            port: Port,
                            compressorLoopOutput: Int,
                            relayActivationHysteresis: Int,
                            divider: Int,
                            relayStages: HashMap<String, Int>,
                            zoneMode: ZoneState)

    fun doCompressorStage3( port: Port,
                            compressorLoopOutput: Int,
                         relayActivationHysteresis: Int,
                         relayStages: HashMap<String, Int>,
                            zoneMode: ZoneState)
}