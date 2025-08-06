package a75f.io.logic.bo.building.statprofiles.util

import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.HyperStatSplitEquip
import a75f.io.domain.equips.hyperstat.CpuV2Equip
import a75f.io.domain.equips.hyperstat.HpuV2Equip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.equips.hyperstat.Pipe2V2Equip
import a75f.io.domain.equips.mystat.MyStatCpuEquip
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.equips.mystat.MyStatHpuEquip
import a75f.io.domain.equips.mystat.MyStatPipe2Equip
import a75f.io.domain.equips.unitVentilator.Pipe4UVEquip
import a75f.io.logic.tuners.TunerUtil

/**
 * Created by Manjunath K on 22-04-2025.
 */

fun fetchBaseTuners(tunerObj: BaseStatTuners, equipRef: String) {
    /**
     * Consider that
     * proportionalGain = proportionalKFactor
     * integralGain = integralKFactor
     * proportionalSpread = temperatureProportionalRange
     * integralMaxTimeout = temperatureIntegralTime
     */

    tunerObj.proportionalGain = TunerUtil.getProportionalGain(equipRef)
    tunerObj.integralGain = TunerUtil.getIntegralGain(equipRef)
    tunerObj.proportionalSpread = TunerUtil.getProportionalSpread(equipRef)
    tunerObj.integralMaxTimeout = TunerUtil.getIntegralTimeout(equipRef).toInt()
    tunerObj.humidityHysteresis =
        getTuner(DomainName.standaloneHumidityHysteresis, equipRef).toInt()
    tunerObj.relayActivationHysteresis =
        getTuner(DomainName.standaloneRelayActivationHysteresis, equipRef).toInt()
    tunerObj.analogFanSpeedMultiplier =
        getTuner(DomainName.standaloneAnalogFanSpeedMultiplier, equipRef)
}

fun fetchHyperStatTuners(equip: HyperStatEquip): BaseStatTuners {

    // These are generic tuners
    val tuners = HyperStatProfileTuners()
    fetchBaseTuners(tuners, equip.equipRef)
    // These are specific tuners
    when (equip) {

        is CpuV2Equip -> {
            tuners.minFanRuntimePostConditioning =
                getTuner(DomainName.minFanRuntimePostConditioning, equip.equipRef).toInt()
        }

        is HpuV2Equip -> {
            tuners.auxHeating1Activate = getTuner(DomainName.auxHeating1Activate, equip.equipRef)
            tuners.auxHeating2Activate = getTuner(DomainName.auxHeating2Activate, equip.equipRef)
        }

        is Pipe2V2Equip -> {
            tuners.auxHeating1Activate = getTuner(DomainName.auxHeating1Activate, equip.equipRef)
            tuners.auxHeating2Activate = getTuner(DomainName.auxHeating2Activate, equip.equipRef)
            tuners.heatingThreshold =
                getTuner(DomainName.hyperstatPipe2FancoilHeatingThreshold, equip.equipRef)
            tuners.coolingThreshold =
                getTuner(DomainName.hyperstatPipe2FancoilCoolingThreshold, equip.equipRef)
            tuners.waterValveSamplingOnTime =
                getTuner(DomainName.waterValveSamplingOnTime, equip.equipRef).toInt()
            tuners.waterValveSamplingWaitTime =
                getTuner(DomainName.waterValveSamplingWaitTime, equip.equipRef).toInt()
            tuners.waterValveSamplingDuringLoopDeadbandOnTime =
                getTuner(DomainName.waterValveSamplingLoopDeadbandOnTime, equip.equipRef).toInt()
            tuners.waterValveSamplingDuringLoopDeadbandWaitTime =
                getTuner(DomainName.waterValveSamplingLoopDeadbandWaitTime, equip.equipRef).toInt()
        }
    }
    return tuners
}

fun fetchMyStatTuners(equip: MyStatEquip): BaseStatTuners {

    // These are generic tuners
    val tuners = MyStatTuners()
    fetchBaseTuners(tuners, equip.equipRef)

    when (equip) {

        is MyStatCpuEquip -> {
            tuners.minFanRuntimePostConditioning =
                getTuner(DomainName.minFanRuntimePostConditioning, equip.equipRef).toInt()
        }

        is MyStatHpuEquip -> {
            tuners.auxHeating1Activate =
                getTuner(DomainName.mystatAuxHeating1Activate, equip.equipRef)
        }

        is MyStatPipe2Equip -> {
            tuners.auxHeating1Activate =
                getTuner(DomainName.mystatAuxHeating1Activate, equip.equipRef)
            tuners.heatingThreshold =
                getTuner(DomainName.mystatPipe2FancoilHeatingThreshold, equip.equipRef)
            tuners.coolingThreshold =
                getTuner(DomainName.mystatPipe2FancoilCoolingThreshold, equip.equipRef)
            tuners.waterValveSamplingOnTime =
                getTuner(DomainName.mystatWaterValveSamplingOnTime, equip.equipRef).toInt()
            tuners.waterValveSamplingWaitTime =
                getTuner(DomainName.mystatWaterValveSamplingWaitTime, equip.equipRef).toInt()
            tuners.waterValveSamplingDuringLoopDeadbandOnTime = getTuner(
                DomainName.mystatWaterValveSamplingLoopDeadbandOnTime, equip.equipRef
            ).toInt()
            tuners.waterValveSamplingDuringLoopDeadbandWaitTime = getTuner(
                DomainName.mystatWaterValveSamplingLoopDeadbandWaitTime, equip.equipRef
            ).toInt()
        }
    }
    return tuners
}

fun getSplitTuners(equip: HyperStatSplitEquip): BaseStatTuners {
    val hsTuners = HyperStatProfileTuners()
    fetchBaseTuners(hsTuners, equip.equipRef)
    hsTuners.minFanRuntimePostConditioning =
        getTuner(DomainName.minFanRuntimePostConditioning, equip.equipRef).toInt()
    return hsTuners
}

fun pipe4UvTuners(equip: Pipe4UVEquip): UvTuners {
    val tuners = UvTuners()
    fetchBaseTuners(tuners, equip.equipRef)
    tuners.minFanRuntimePostConditioning = getTuner(DomainName.minFanRuntimePostConditioning, equip.equipRef).toInt()
    tuners.saTemperingSetpoint = getTuner(DomainName.saTemperingSetpoint, equip.equipRef)
    tuners.saTemperingIntegralKFactor = getTuner(DomainName.saTemperingIntegralKFactor, equip.equipRef)
    tuners.saTemperingTemperatureIntegralTime = getTuner(DomainName.saTemperingTemperatureIntegralTime, equip.equipRef).toInt()
    tuners.saTemperingProportionalKFactor = getTuner(DomainName.saTemperingProportionalKFactor, equip.equipRef)
    tuners.saTemperingTemperatureProportionalRange = getTuner(DomainName.saTemperingTemperatureProportionalRange, equip.equipRef)
    tuners.economizingToMainCoolingLoopMap = getTuner(DomainName.standaloneEconomizingToMainCoolingLoopMap, equip.equipRef)
    tuners.faceBypassDamperActivationHysteresis = getTuner(DomainName.faceBypassDamperRelayActivationHysteresis, equip.equipRef)
    return tuners
}

fun getTuner(domainName: String, equipRef: String): Double {
    return TunerUtil.readTunerValByQuery("domainName ==\"$domainName\"", equipRef)
}