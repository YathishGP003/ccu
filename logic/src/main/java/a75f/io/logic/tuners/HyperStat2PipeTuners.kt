package a75f.io.logic.tuners

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.logic.bo.building.definitions.Units

/**
 * Created by Manjunath K on 09-09-2022.
 */

class HyperStat2PipeTuners {
    companion object{

        private fun pushTunerPointToHaystack(hayStack: CCUHsApi, point: Point, defaultValue: Double) {
            val pointId: String = hayStack.addPoint(point)
            hayStack.writePointForCcuUser(
                pointId,
                TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,
                defaultValue,
                0
            )
            hayStack.writeHisValById(pointId, defaultValue)
        }



        fun addPipe2BuildingTuner(
            hayStack: CCUHsApi, siteRef: String, equipRef: String, equipDis: String, tz: String
        ){
            addPipe2SpecificTuners(hayStack,siteRef,equipRef,equipDis,tz,null,null,false)
        }

        fun addHyperstatModuleTuners(
            hayStack: CCUHsApi, siteRef: String, equipRef: String, equipDis: String, tz: String,
            roomRef: String, floorRef: String
        ) {
            HyperstatCpuTuners.addHyperstatModuleTuners(hayStack, siteRef, equipRef, equipDis, tz,roomRef,floorRef)
            addPipe2SpecificTuners(hayStack, siteRef, equipRef, equipDis, tz,roomRef,floorRef,true)
        }

        // Tuners for 2pipe specific
        //        auxHeating1Activate = 3(F)                              auxHeating2Activate =4(F)
        //        2-pipe FanCoil Heating Threshold(°F) = 85	              2-pipe FanCoil Cooling Threshold(°F) = 65
        //        waterValveSamplingOnTime=2min                           waterValveSamplingWaitTime = 58min
        //        waterValveSamplingDuringLoopDeadbandOnTime=2min         waterValveSamplingDuringLoopDeadbandWaitTime=5min
        //
        private fun addPipe2SpecificTuners(
            hayStack: CCUHsApi, siteRef: String, equipRef: String, equipDis: String, tz: String,
            roomRef: String?, floorRef: String?, isModuleTuner: Boolean){

            val auxHeating1Activate = Point.Builder()
                .setDisplayName("$equipDis-auxHeating1Activate")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)

                .setHisInterpolate("cov").addMarker("tuner").addMarker("base")
                .addMarker("standalone").addMarker("writable").addMarker("his")
                .addMarker("heating").addMarker("aux").addMarker("stage1")
                .setMinVal("0").setMaxVal("10").setIncrementVal("1")
                .setTunerGroup(TunerConstants.HYPERSTAT_TUNER_GROUP)
                .setUnit(Units.FAHRENHEIT)
                .setTz(tz)
            if(isModuleTuner) {
                auxHeating1Activate.setRoomRef(roomRef)
                auxHeating1Activate.setFloorRef(floorRef)
            }
            pushTunerPointToHaystack(hayStack, auxHeating1Activate.build(), 3.0)

            val auxHeating2Activate = Point.Builder()
                .setDisplayName("$equipDis-auxHeating2Activate")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .setHisInterpolate("cov").addMarker("tuner").addMarker("base")
                .addMarker("standalone").addMarker("writable").addMarker("his")
                .addMarker("heating").addMarker("aux").addMarker("stage2")
                .setMinVal("0").setMaxVal("10").setIncrementVal("1")
                .setTunerGroup(TunerConstants.HYPERSTAT_TUNER_GROUP)
                .setUnit(Units.FAHRENHEIT)
                .setTz(tz)

            if(isModuleTuner) {
                auxHeating2Activate.setRoomRef(roomRef)
                auxHeating2Activate.setFloorRef(floorRef)
            }
            pushTunerPointToHaystack(hayStack, auxHeating2Activate.build(), 4.0)

            val sa2PfcHeatingThreshold = Point.Builder()
                .setDisplayName("$equipDis-2PipeFancoilHeatingThreshold")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .setHisInterpolate("cov")
                .addMarker("tuner").addMarker("base").addMarker("writable")
                .addMarker("his").addMarker("standalone").addMarker("heating").addMarker("threshold")
                .addMarker("pipe2").addMarker("fcu").addMarker("sp")
                .setMinVal("80").setMaxVal("150").setIncrementVal("1")
                .setTunerGroup(TunerConstants.HYPERSTAT_TUNER_GROUP)
                .setUnit(Units.FAHRENHEIT)
                .setTz(tz)

            if(isModuleTuner) {
                sa2PfcHeatingThreshold.setRoomRef(roomRef)
                sa2PfcHeatingThreshold.setFloorRef(floorRef)
            }

            pushTunerPointToHaystack(hayStack, sa2PfcHeatingThreshold.build(), 85.0)

            val sa2PfcCoolingThreshold = Point.Builder()
                .setDisplayName("$equipDis-2PipeFancoilCoolingThreshold")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .setHisInterpolate("cov")
                .addMarker("tuner").addMarker("base").addMarker("writable")
                .addMarker("his").addMarker("standalone").addMarker("cooling").addMarker("threshold")
                .addMarker("pipe2").addMarker("fcu").addMarker("sp")
                .setMinVal("35").setMaxVal("70").setIncrementVal("1")
                .setTunerGroup(TunerConstants.HYPERSTAT_TUNER_GROUP)
                .setUnit(Units.FAHRENHEIT)
                .setTz(tz)

            if(isModuleTuner) {
                sa2PfcCoolingThreshold.setRoomRef(roomRef)
                sa2PfcCoolingThreshold.setFloorRef(floorRef)
            }

            pushTunerPointToHaystack(hayStack, sa2PfcCoolingThreshold.build(), 65.0)
            val waterValveSamplingOnTime = Point.Builder()
                .setDisplayName("$equipDis-waterValveSamplingOnTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .setHisInterpolate("cov").addMarker("tuner").addMarker("base")
                .addMarker("writable").addMarker("his").addMarker("sp")
                .addMarker("water").addMarker("valve").addMarker("samplingrate").addMarker("on").addMarker("time")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1")
                .setTunerGroup(TunerConstants.HYPERSTAT_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
            if(isModuleTuner) {
                waterValveSamplingOnTime.setRoomRef(roomRef)
                waterValveSamplingOnTime.setFloorRef(floorRef)
            }
            pushTunerPointToHaystack(hayStack, waterValveSamplingOnTime.build(), 2.0)

            val waterValveSamplingWaitTime = Point.Builder()
                .setDisplayName("$equipDis-waterValveSamplingWaitTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .setHisInterpolate("cov").addMarker("tuner").addMarker("base")
                .addMarker("writable").addMarker("his").addMarker("sp")
                .addMarker("water").addMarker("valve").addMarker("samplingrate").addMarker("wait").addMarker("time")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1")
                .setTunerGroup(TunerConstants.HYPERSTAT_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
            if(isModuleTuner) {
                waterValveSamplingWaitTime.setRoomRef(roomRef)
                waterValveSamplingWaitTime.setFloorRef(floorRef)
            }
            pushTunerPointToHaystack(hayStack, waterValveSamplingWaitTime.build(), 58.0)


            val waterValveSamplingDuringLoopDeadbandOnTime = Point.Builder()
                .setDisplayName("$equipDis-waterValveSamplingDuringLoopDeadbandOnTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .setHisInterpolate("cov").addMarker("tuner").addMarker("base")
                .addMarker("writable").addMarker("his").addMarker("sp")
                .addMarker("water").addMarker("valve").addMarker("samplingrate").addMarker("loop")
                .addMarker("deadband").addMarker("on").addMarker("time")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1")
                .setTunerGroup(TunerConstants.HYPERSTAT_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
            if(isModuleTuner) {
                waterValveSamplingDuringLoopDeadbandOnTime.setRoomRef(roomRef)
                waterValveSamplingDuringLoopDeadbandOnTime.setFloorRef(floorRef)
            }
            pushTunerPointToHaystack(hayStack, waterValveSamplingDuringLoopDeadbandOnTime.build(), 2.0)

            val waterValveSamplingDuringLoopDeadbandWaitTime = Point.Builder()
                .setDisplayName("$equipDis-waterValveSamplingDuringLoopDeadbandWaitTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .setHisInterpolate("cov").addMarker("tuner").addMarker("base")
                .addMarker("writable").addMarker("his").addMarker("sp")
                .addMarker("water").addMarker("valve").addMarker("samplingrate").addMarker("loop")
                .addMarker("deadband").addMarker("wait").addMarker("time")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1")
                .setTunerGroup(TunerConstants.HYPERSTAT_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
            if(isModuleTuner) {
                waterValveSamplingDuringLoopDeadbandWaitTime.setRoomRef(roomRef)
                waterValveSamplingDuringLoopDeadbandWaitTime.setFloorRef(floorRef)
            }
            pushTunerPointToHaystack(hayStack, waterValveSamplingDuringLoopDeadbandWaitTime.build(), 5.0)

        }
    }
}

