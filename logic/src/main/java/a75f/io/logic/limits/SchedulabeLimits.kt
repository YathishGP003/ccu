package a75f.io.logic.limits

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.Units
import a75f.io.logic.tuners.TunerConstants

class SchedulabeLimits {


    companion object {
        fun addSchedulableLimits(isBuilding: Boolean, roomRef: String?, zoneDis: String?) {
            val hayStack = CCUHsApi.getInstance()
            val schedulablePoint: HashMap<Any, Any> = hayStack.readEntity("schedulable and default")
            if (schedulablePoint.size > 0 && isBuilding) {
                CcuLog.d(L.TAG_CCU_SYSTEM, "Schedulable points are already present")
                return
            }
            val siteMap = hayStack.readEntity(Tags.SITE)
            val siteRef: String = siteMap[Tags.ID].toString()
            val tz = siteMap["tz"].toString()
            var equipDis = ""
            val tuner: HashMap<Any, Any> = hayStack.readEntity("equip and tuner")
            var ref =""
            if (isBuilding) {
                 ref = tuner["id"].toString()
                equipDis = "BuildingSchedulable"
            } else{
                if (roomRef != null) {
                    ref = roomRef
                }
                if (zoneDis != null) {
                    val room = CCUHsApi.getInstance().readMapById(roomRef)
                    val floorRef = room["floorRef"].toString()
                    val floor = CCUHsApi.getInstance().readMapById(floorRef)
                    val siteMap = CCUHsApi.getInstance().readEntity(Tags.SITE)
                    val siteDis = siteMap["dis"].toString()
                    equipDis = siteDis+"-"+ floor["dis"].toString()+"-"+zoneDis
                }
            }


            if (isBuilding) {
                val buildingLimitMax: Point.Builder = Point.Builder()

                        .setDisplayName("$equipDis-buildingLimitMax")
                        .setSiteRef(siteRef)
                        .setHisInterpolate("cov")
                        .addMarker("writable").addMarker("his")
                        .addMarker("system").addMarker("building").addMarker("limit").addMarker("max")
                        .addMarker("sp").addMarker("cur")
                        .setMinVal("32").setMaxVal("140").setIncrementVal("1")
                        .setUnit("\u00B0F")
                        .setTz(tz)
                addTagsBasedOnCondition(isBuilding, buildingLimitMax, ref)
                val buildingLimitMaxId = hayStack.addPoint(buildingLimitMax.build())
                hayStack.writePointForCcuUser(buildingLimitMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 110.0, 0)
                hayStack.writeHisValById(buildingLimitMaxId, 110.0)
                val buildingLimitMin: Point.Builder = Point.Builder()
                        .setDisplayName("$equipDis-buildingLimitMin")
                        .setSiteRef(siteRef)
                        .setHisInterpolate("cov").addMarker("cur")
                        .addMarker("writable").addMarker("his")
                        .addMarker("system").addMarker("building").addMarker("limit").addMarker("min").addMarker("sp")
                        .setMinVal("32").setMaxVal("140").setIncrementVal("1")
                        .setUnit("\u00B0F")
                        .setTz(tz)
                addTagsBasedOnCondition(isBuilding, buildingLimitMin, ref)
                val buildingLimitMinId = hayStack.addPoint(buildingLimitMin.build())
                hayStack.writePointForCcuUser(buildingLimitMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 50.0, 0)
                hayStack.writeHisValById(buildingLimitMinId, 50.0)


                val buildingToZoneDifferential = Point.Builder()
                    .setDisplayName("$equipDis-buildingToZoneDifferential")
                    .setSiteRef(siteRef)
                    .setHisInterpolate("cov")
                    .addMarker("default").addMarker("writable").addMarker("his").addMarker("cur")
                    .addMarker("system").addMarker("building").addMarker("zone")
                    .addMarker("differential")
                    .addMarker("sp")
                    .setMinVal("0").setMaxVal("20").setIncrementVal("1")
                    .setTz(tz).setUnit(Units.FAHRENHEIT)
                addTagsBasedOnCondition(isBuilding, buildingToZoneDifferential, ref)
                val buildingToZoneDifferentialId =
                    hayStack.addPoint(buildingToZoneDifferential.build())
                hayStack.writePointForCcuUser(
                    buildingToZoneDifferentialId,
                    TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,
                    TunerConstants.BUILDING_TO_ZONE_DIFFERENTIAL,
                    0
                )
                hayStack.writeHisValById(
                    buildingToZoneDifferentialId,
                    TunerConstants.BUILDING_TO_ZONE_DIFFERENTIAL
                )
            }
            val coolingUserLimitMin: Point.Builder = Point.Builder()
                    .setDisplayName("$equipDis-coolingUserLimitMin")
                    .setSiteRef(siteRef)
                    .setHisInterpolate("cov")
                    .addMarker("writable").addMarker("his").setMinVal("70").setMaxVal("77")
                    .setIncrementVal("1").addMarker("schedulable").addMarker("cur")
                    .addMarker("cooling").addMarker("user").addMarker("limit").addMarker("min").addMarker("sp")
                    .setMinVal("50").setMaxVal("100").setIncrementVal("1")
                    .setUnit("\u00B0F")
                    .setTz(tz)
            addTagsBasedOnCondition(isBuilding, coolingUserLimitMin, ref)
            val coolingUserLimitMinId = hayStack.addPoint(coolingUserLimitMin.build())
            hayStack.writePointForCcuUser(coolingUserLimitMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_COOLING_USERLIMIT_MIN, 0)
            hayStack.writeHisValById(coolingUserLimitMinId, TunerConstants.ZONE_COOLING_USERLIMIT_MIN)
            if(!isBuilding){
                val buildingPoint: HashMap<Any, Any> = hayStack.readEntity("schedulable and cooling and user " +
                        "and limit and min and default")
                val pointVal =  HSUtil.getPriorityLevelVal(buildingPoint["id"].toString(),16)
                if (pointVal > 0)
                    hayStack.writePointForCcuUser(coolingUserLimitMinId, TunerConstants.SYSTEM_BUILDING_VAL_LEVEL,
                            pointVal, 0)
            }

            val coolingUserLimitMax: Point.Builder = Point.Builder()
                    .setDisplayName("$equipDis-coolingUserLimitMax")
                    .setSiteRef(siteRef)
                    .setHisInterpolate("cov")
                    .addMarker("schedulable").addMarker("writable").addMarker("his")
                    .addMarker("cooling").addMarker("user").addMarker("limit").addMarker("max").addMarker("sp")
                    .setMinVal("50").setMaxVal("100").setIncrementVal("1").addMarker("cur")
                    .setUnit("\u00B0F")
                    .setTz(tz)
            addTagsBasedOnCondition(isBuilding, coolingUserLimitMax, ref)
            val coolingUserLimitMaxId = hayStack.addPoint(coolingUserLimitMax.build())
            hayStack.writePointForCcuUser(coolingUserLimitMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_COOLING_USERLIMIT_MAX, 0)
            hayStack.writeHisValById(coolingUserLimitMaxId, TunerConstants.ZONE_COOLING_USERLIMIT_MAX)
            if(!isBuilding){
                val buildingPoint: HashMap<Any, Any> = hayStack.readEntity("schedulable and cooling and user " +
                        "and limit and max and default")
                val pointVal = HSUtil.getPriorityLevelVal(buildingPoint["id"].toString(), 16)
                if (pointVal > 0)
                    hayStack.writePointForCcuUser(coolingUserLimitMaxId, TunerConstants.SYSTEM_BUILDING_VAL_LEVEL,
                            pointVal, 0)
            }


            val heatingUserLimitMin: Point.Builder = Point.Builder()
                    .setDisplayName("$equipDis-heatingUserLimitMin")
                    .setSiteRef(siteRef)
                    .setHisInterpolate("cov")
                    .addMarker("schedulable").addMarker("writable").addMarker("his").addMarker("cur")
                    .addMarker("heating").addMarker("user").addMarker("limit").addMarker("min").addMarker("sp")
                    .setMinVal("50").setMaxVal("100").setIncrementVal("1")
                    .setUnit("\u00B0F")
                    .setTz(tz)
            addTagsBasedOnCondition(isBuilding, heatingUserLimitMin, ref)
            val heatingUserLimitMinId = hayStack.addPoint(heatingUserLimitMin.build())
            hayStack.writePointForCcuUser(heatingUserLimitMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_HEATING_USERLIMIT_MIN, 0)
            hayStack.writeHisValById(heatingUserLimitMinId, TunerConstants.ZONE_HEATING_USERLIMIT_MIN)
            if(!isBuilding){
                val buildingPoint: HashMap<Any, Any> = hayStack.readEntity("schedulable and heating and user " +
                        "and limit and min and default")
                val pointVal = HSUtil.getPriorityLevelVal(buildingPoint["id"].toString(), 16)
                if (pointVal > 0)
                    hayStack.writePointForCcuUser(heatingUserLimitMinId, TunerConstants.SYSTEM_BUILDING_VAL_LEVEL,
                        pointVal, 0)
            }

            val heatingUserLimitMax: Point.Builder = Point.Builder()
                    .setDisplayName("$equipDis-heatingUserLimitMax")
                    .setSiteRef(siteRef)
                    .setHisInterpolate("cov")
                    .addMarker("schedulable").addMarker("writable").addMarker("his").addMarker("cur")
                    .addMarker("heating").addMarker("user").addMarker("limit").addMarker("max").addMarker("sp")
                    .setMinVal("50").setMaxVal("100").setIncrementVal("1")
                    .setUnit("\u00B0F")
                    .setTz(tz)
            addTagsBasedOnCondition(isBuilding, heatingUserLimitMax, ref)
            val heatingUserLimitMaxId = hayStack.addPoint(heatingUserLimitMax.build())
            hayStack.writePointForCcuUser(heatingUserLimitMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_HEATING_USERLIMIT_MAX, 0)
            hayStack.writeHisValById(heatingUserLimitMaxId, TunerConstants.ZONE_HEATING_USERLIMIT_MAX)
            if(!isBuilding){
                val buildingPoint: HashMap<Any, Any> = hayStack.readEntity("schedulable and heating and user " +
                        "and limit and max and default")
                val pointVal = HSUtil.getPriorityLevelVal(buildingPoint["id"].toString(), 16)
                if (pointVal > 0)
                   hayStack.writePointForCcuUser(heatingUserLimitMaxId, TunerConstants.SYSTEM_BUILDING_VAL_LEVEL,
                        pointVal , 0)
            }

            val coolingDb = Point.Builder()
                    .setDisplayName("$equipDis-coolingDeadband")
                    .setSiteRef(siteRef)
                    .setHisInterpolate("cov")
                    .addMarker("writable")
                    .addMarker("his")
                    .addMarker("cooling").addMarker("deadband").addMarker("base")
                    .addMarker("sp").addMarker("schedulable").addMarker("cur")
                    .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5")
                    .setUnit("\u00B0F")
                    .setTz(tz)
            addTagsBasedOnCondition(isBuilding, coolingDb, ref)
            val coolingDbId = hayStack.addPoint(coolingDb.build())
            hayStack.writePointForCcuUser(coolingDbId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.VAV_COOLING_DB, 0)
            hayStack.writeHisValById(coolingDbId, TunerConstants.VAV_COOLING_DB)
            if(!isBuilding){
                val buildingPoint: HashMap<Any, Any> = hayStack.readEntity("schedulable and cooling and deadband " +
                        "and default")
                val pointVal = HSUtil.getPriorityLevelVal(buildingPoint["id"].toString(), 16)
                if (pointVal > 0)
                    hayStack.writePointForCcuUser(coolingDbId, TunerConstants.SYSTEM_BUILDING_VAL_LEVEL,
                            pointVal , 0)
            }

            val heatingDb = Point.Builder()
                    .setDisplayName("$equipDis-heatingDeadband")
                    .setSiteRef(siteRef)
                    .setHisInterpolate("cov")
                    .addMarker("writable")
                    .addMarker("his")
                    .addMarker("heating").addMarker("deadband").addMarker("base")
                    .addMarker("sp").addMarker("schedulable").addMarker("cur")
                    .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5")
                    .setUnit("\u00B0F")
                    .setTz(tz)
            addTagsBasedOnCondition(isBuilding, heatingDb, ref)
            val heatingDbId = hayStack.addPoint(heatingDb.build())
            hayStack.writePointForCcuUser(heatingDbId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.VAV_COOLING_DB, 0)
            hayStack.writeHisValById(heatingDbId, TunerConstants.VAV_COOLING_DB)
            if(!isBuilding){
                val buildingPoint: HashMap<Any, Any> = hayStack.readEntity("schedulable and heating and deadband " +
                        "and default")
                val pointVal = HSUtil.getPriorityLevelVal(buildingPoint["id"].toString(), 16)
                if (pointVal > 0)
                    hayStack.writePointForCcuUser(heatingDbId, TunerConstants.SYSTEM_BUILDING_VAL_LEVEL,
                            pointVal , 0)
            }

            val unoccupiedZoneSetback = Point.Builder()
                    .setDisplayName("$equipDis-unoccupiedZoneSetback")
                    .setSiteRef(siteRef)
                    .setHisInterpolate("cov")
                    .addMarker("schedulable").addMarker("writable").addMarker("his").addMarker("cur")
                    .addMarker("unoccupied").addMarker("setback").addMarker("sp")
                    .setMinVal("0").setMaxVal("20").setIncrementVal("1")
                    .setUnit("\u00B0F")
                    .setTz(tz)
            addTagsBasedOnCondition(isBuilding, unoccupiedZoneSetback, ref)
            val unoccupiedZoneSetbackId = hayStack.addPoint(unoccupiedZoneSetback.build())
            hayStack.writePointForCcuUser(unoccupiedZoneSetbackId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.ZONE_UNOCCUPIED_SETBACK, 0)
            hayStack.writeHisValById(unoccupiedZoneSetbackId, TunerConstants.ZONE_UNOCCUPIED_SETBACK)
            if(!isBuilding){
                val buildingPoint: HashMap<Any, Any> = hayStack.readEntity("schedulable and unoccupied and zone and setback " +
                        "and default")
                val pointVal = HSUtil.getPriorityLevelVal(buildingPoint["id"].toString(), 16)
                if (pointVal > 0)
                    hayStack.writePointForCcuUser(unoccupiedZoneSetbackId, TunerConstants.SYSTEM_BUILDING_VAL_LEVEL,
                            pointVal , 0)
            }
        }

        private fun addTagsBasedOnCondition(isBuilding: Boolean, point: Point.Builder, reference: String) {
            if (isBuilding) {
                point.addMarker("default").setEquipRef(reference)
            } else {
                point.addMarker("zone").setRoomRef(reference)
            }
        }
    }
}