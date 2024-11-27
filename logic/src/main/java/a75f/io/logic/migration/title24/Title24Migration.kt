package a75f.io.logic.migration.title24

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.CpuAnalogOutAssociation
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuEconAnalogOutAssociation
import a75f.io.logic.tuners.TunerConstants
import java.util.function.Consumer

class Title24Migration {
    companion object {

        private fun prepareAndCreateRecirculateConfigPoint(
            haystack: CCUHsApi,
            equip: Equip,
            display: String,
            analogTag: String
        ) {
            val analogInAssociation = Point.Builder()
                .setDisplayName(equip.displayName + display)
                .setSiteRef(equip.siteRef)
                .setEquipRef(equip.id)
                .setRoomRef(equip.roomRef)
                .setFloorRef(equip.floorRef)
                .setTz(equip.tz)
                .setGroup(equip.group.toString())
                .setUnit("V")
                .setHisInterpolate("cov")
                .addMarker("zone").addMarker("config").addMarker("recirculate")
                .addMarker("writable").addMarker("his").addMarker("sp").addMarker(analogTag)
                .build()
            val analogInAssociationId = haystack.addPoint(analogInAssociation)
            haystack.writeDefaultValById(analogInAssociationId, 4.0)
        }

        private fun createEconomizerConifgPoint(
            haystack: CCUHsApi,
            equip: Equip,
            display: String,
            analogTag: String
        ) {
            val analogInAssociation = Point.Builder()
                .setDisplayName(equip.displayName + display)
                .setSiteRef(equip.siteRef)
                .setEquipRef(equip.id)
                .setRoomRef(equip.roomRef)
                .setFloorRef(equip.floorRef)
                .setTz(equip.tz)
                .setGroup(equip.group.toString())
                .setUnit("V")
                .setHisInterpolate("cov")
                .addMarker("zone").addMarker("config").addMarker("economizer")
                .addMarker("writable").addMarker("his").addMarker("sp").addMarker(analogTag)
                .build()
            val analogInAssociationId = haystack.addPoint(analogInAssociation)
            haystack.writeDefaultValById(analogInAssociationId, 7.0)
        }

        fun doTitle24HsPointMigration(haystack: CCUHsApi) {
            val equips = haystack.readAllEntities("equip and hyperstat")
            equips.forEach(Consumer { equip: HashMap<Any?, Any?>? ->
                val hsEquip: Equip =
                    Equip.Builder().setHashMap(equip).build()

                // Config: analogOutAtRecirculate point
                val aoutEnabled: Double =
                    haystack.readDefaultVal("enabled and analog1 and  output and equipRef == \"" + hsEquip.id + "\"")
                val aoutMapping: Double =
                    haystack.readDefaultVal("output and association and analog1 and equipRef == \"" + hsEquip.id + "\"")
                val aout2Enabled: Double =
                    haystack.readDefaultVal("enabled and analog2 and  output and equipRef == \"" + hsEquip.id + "\"")
                val aout2Mapping: Double =
                    haystack.readDefaultVal("output and association and analog2 and equipRef == \"" + hsEquip.id + "\"")
                val aout3Enabled: Double =
                    haystack.readDefaultVal("enabled and analog3 and  output and equipRef == \"" + hsEquip.id + "\"")
                val aout3Mapping: Double =
                    haystack.readDefaultVal("output and association and analog3 and equipRef == \"" + hsEquip.id + "\"")
                if (aoutEnabled == 1.0 && aoutMapping == CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal.toDouble()) {
                    prepareAndCreateRecirculateConfigPoint(
                        haystack,
                        hsEquip,
                        "fanOutRecirculateAnalog1",
                        "analog1"
                    )
                }
                if (aout2Enabled == 1.0 && aout2Mapping == CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal.toDouble()) {
                    prepareAndCreateRecirculateConfigPoint(
                        haystack,
                        hsEquip,
                        "fanOutRecirculateAnalog2",
                        "analog2"
                    )
                }
                if (aout3Enabled == 1.0 && aout3Mapping == CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal.toDouble()) {
                    prepareAndCreateRecirculateConfigPoint(
                        haystack,
                        hsEquip,
                        "fanOutRecirculateAnalog3",
                        "analog3"
                    )
                }
                val minFanRuntimePostConditioningread: HashMap<Any, Any> =
                    CCUHsApi.getInstance()
                        .readEntity("postconditioning and tuner and fan and cur and min and zone and equipRef == \"" + hsEquip.id + "\"")
                if (minFanRuntimePostConditioningread.isEmpty()) {
                    // Tuner: Add MinFanRuntimePostConditioning tuner
                    val minFanRuntimePostConditioning: Point =
                        Point.Builder()
                            .setDisplayName(hsEquip.displayName + "-standalone" + "MinFanRuntimePostConditioning")
                            .setSiteRef(hsEquip.siteRef)
                            .setEquipRef(hsEquip.id)
                            .setRoomRef(hsEquip.roomRef)
                            .setFloorRef(hsEquip.floorRef)
                            .setTz(hsEquip.tz)
                            .setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                            .setMinVal(0.toString()).setMaxVal(60.toString())
                            .setIncrementVal(1.toString())
                            .setUnit("m")
                            .setHisInterpolate("cov")
                            .addMarker("tuner").addMarker("sp").addMarker("writable")
                            .addMarker("min")
                            .addMarker("zone")
                            .addMarker("his").addMarker("default").addMarker("fan").addMarker("cur")
                            .addMarker("runtime").addMarker("postconditioning")
                            .build()
                    val minFanRuntimePostConditioningId: String =
                        haystack.addPoint(minFanRuntimePostConditioning)
                    haystack.writeDefaultTunerValById(minFanRuntimePostConditioningId, 5.0)
                }
            })
        }

        private fun universalInPointsMigration(equip: Equip, ccuHsApi: CCUHsApi) {
            val universalInPoints = ccuHsApi.readAllEntities(
                "association and " +
                        "(universal1 or universal2 or universal3 or universal4 or universal5 or universal6 or universal7 or universal8) and cpu and output equipRef == \"" + equip.id + "\""
            )
            for (universalInPoint: HashMap<Any, Any> in universalInPoints) {
                if (universalInPoint["enum"].toString().contains("mixedAirTemperature")) {
                    val enumUpdatedUniversalInPoint = Point.Builder().setHashMap(universalInPoint)
                        .setEnums(
                            ("currentTx(0-10amps),currentTx(0-20amps),currentTx(0-50amps),currentTx(0-100amps),currentTx(0-150amps)," +
                                    "supplyAir,mixedAir,outsideAir,filter(NC),filter(NO),condensate(NC),condensate(NO),pressure(0-1 inH2o)," +
                                    "pressure(0-2 inH2o),generic(0-10V),generic(1-100kOhm),pressure(0-10 inH2o),genericFault(NC),genericFault(NO)")
                        ).build()
                    CCUHsApi.getInstance()
                        .updatePoint(enumUpdatedUniversalInPoint, enumUpdatedUniversalInPoint.id)
                }
            }
        }

        fun removeDuplicateTitle24Points(haystack: CCUHsApi){

            fun deleteThesePoints(haystack: CCUHsApi, pointsList: ArrayList<HashMap<Any, Any>>) {
                for (pointMap: HashMap<Any, Any> in pointsList) {
                    CcuLog.i(L.TAG_CCU_MIGRATION_UTIL,"Deleting Title24 redundant point: " + pointMap["dis"] + " id: " + pointMap["id"])
                    haystack.deleteEntity(pointMap["id"].toString())
                }
            }
            fun deleteThisPoint(haystack: CCUHsApi, pointMap: HashMap<Any, Any>) {
                if (pointMap.isNotEmpty()) {
                    CcuLog.i(L.TAG_CCU_MIGRATION_UTIL,"Deleting Title24 redundant point: " + pointMap["dis"] + " id: " + pointMap["id"])
                    haystack.deleteEntity(pointMap["id"].toString())
                }
            }

            val equips = haystack.readAllEntities("equip and hyperstatsplit")

            equips.forEach(Consumer { equip: HashMap<Any, Any> ->
                val equipId = equip["id"].toString()

                val fanOutRecirculateAnalogPoints = haystack.readAllEntities("not domainName and recirculate and" +
                        " config and (analog1 or analog2 or analog3) and equipRef == \"" + equipId + "\"")
                deleteThesePoints(haystack, fanOutRecirculateAnalogPoints)

                val economizerAnalogPoints = haystack.readAllEntities("not domainName and economizer and" +
                        " config and (analog1 or analog2 or analog3) and equipRef == \"" + equipId + "\"")
                deleteThesePoints(haystack, economizerAnalogPoints)

                val minFanRuntimePostConditioningTuner = haystack.readEntity("not domainName and postconditioning and tuner and" +
                        " fan and cur and min and runtime and zone and equipRef == \"" + equipId + "\"")

                deleteThisPoint(haystack, minFanRuntimePostConditioningTuner)
                val prePurgeRunTimeread = haystack.readEntity("not domainName and prePurge and tuner and min and cpu and standalone and oao and runtime and cur and equipRef == \"$equipId\"")
                deleteThisPoint(haystack, prePurgeRunTimeread)
                val prePurgeOccupiedTimeRead = haystack.readEntity("not domainName and prePurge and offset and time and cur and zone and occupied and cpu and standalone and oao and equipRef == \"$equipId\"")
                deleteThisPoint(haystack, prePurgeOccupiedTimeRead)
                val prePurgeFanSpeedTunerRead = haystack.readEntity("not domainName and prePurge and oao and fan and speed and cur and cpu and standalone and zone and default and equipRef == \"$equipId\"")
                deleteThisPoint(haystack, prePurgeFanSpeedTunerRead)
                val prePurgeEnabledRead = haystack.readEntity("not domainName and prePurge and userIntent and writable and standalone and cpu and cur and standalone and zone and enabled and equipRef == \"$equipId\"")
                deleteThisPoint(haystack, prePurgeEnabledRead)
                val outsideDamperMinOpenDuringRecirc = haystack.readEntity("not domainName and config and outside and oao and damper and min and open and recirc and equipRef ==\"$equipId\"")
                deleteThisPoint(haystack, outsideDamperMinOpenDuringRecirc)
                val outsideDamperMinOpenDuringConditioning = haystack.readEntity("not domainName and config and outside and oao and damper and min and open and conditioning and equipRef ==\"$equipId\"")
                deleteThisPoint(haystack, outsideDamperMinOpenDuringConditioning)
                val outsideDamperMinOpenDuringFanLow = haystack.readEntity("not domainName and config and outside and oao and damper and min and open and fan and low and equipRef ==\"$equipId\"")
                deleteThisPoint(haystack, outsideDamperMinOpenDuringFanLow)
                val outsideDamperMinOpenDuringFanMedium = haystack.readEntity("not domainName and config and outside and oao and damper and min and open and fan and medium and equipRef ==\"$equipId\"")
                deleteThisPoint(haystack, outsideDamperMinOpenDuringFanMedium)
                val outsideDamperMinOpenDuringFanHigh = haystack.readEntity("not domainName and config and outside and oao and damper and min and open and fan and high and equipRef ==\"$equipId\"")
                deleteThisPoint(haystack, outsideDamperMinOpenDuringFanHigh)
            })
        }
    }
}