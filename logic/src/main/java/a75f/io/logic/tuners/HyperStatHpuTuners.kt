package a75f.io.logic.tuners

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.logic.bo.building.definitions.Units

/**
 * Created by Manjunath K on 09-09-2022.
 */

class HyperStatHpuTuners {
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

        fun addHyperStatModuleTuners(
            hayStack: CCUHsApi, siteRef: String, equipRef: String, equipDis: String, tz: String,
            roomRef: String, floorRef: String
        ) {
            HyperstatCpuTuners.addHyperstatModuleTuners(hayStack, siteRef, equipRef, equipDis, tz,roomRef,floorRef)
            addHpuSpecificTuners(hayStack, siteRef, equipRef, equipDis, tz,roomRef,floorRef,true)
        }

        // Tuners for Hpu specific
        //        auxHeating1Activate = 3(F)                              auxHeating2Activate =4(F)
        private fun addHpuSpecificTuners(
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

        }
    }
}

