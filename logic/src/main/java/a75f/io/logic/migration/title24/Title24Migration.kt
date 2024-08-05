package a75f.io.logic.migration.title24

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.CpuAnalogOutAssociation
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuEconAnalogOutAssociation
import a75f.io.logic.tuners.SystemTuners
import a75f.io.logic.tuners.TunerConstants
import java.util.function.Consumer

class Title24Migration {
    companion object {
        fun doTitle24OaoPointsMigration(haystack: CCUHsApi) {
            val oaoEquips = haystack.readAllEntities("equip and oao and not hyperstatsplit")
            for (oaoEquipMap: HashMap<Any, Any> in oaoEquips) {
                val oaoEquip = Equip.Builder().setHashMap(oaoEquipMap).build()
                val outsideDamperMinOpen =
                    haystack.readEntity("point and config and outside and damper and min and open and not purge and not enhanced and equipRef == \"" + oaoEquip.id + "\"")
                val outsideDamperMinOpenVal =
                    haystack.readPointPriorityVal(outsideDamperMinOpen["id"].toString())
                val siteMap = haystack.read(Tags.SITE)
                val siteRef = siteMap[Tags.ID] as String?
                val siteDis = siteMap["dis"] as String?
                val equipDis = siteDis + "-OAO-" + oaoEquip.group
                val tz = siteMap["tz"].toString()

                // delete outsideDamperMinOpen point. Add min open points for recirc/conditioning/fan low/fan med/fan high
                // set each of these to the old value of outsideDamperMinOpen
                if (outsideDamperMinOpen.isNotEmpty()) {
                    haystack.deleteEntity(outsideDamperMinOpen["id"].toString())
                    val outsideDamperMinOpenDuringRecirc = Point.Builder().setDisplayName(
                        SystemTuners.getDisplayNameFromVariation(
                            "$equipDis-outsideDamperMinOpenDuringRecirc"
                        )
                    )
                        .setEquipRef(oaoEquip.id)
                        .setSiteRef(siteRef)
                        .addMarker("config").addMarker("oao").addMarker("writable")
                        .addMarker("outside").addMarker("damper").addMarker("min").addMarker("open")
                        .addMarker("recirc").addMarker("sp")
                        .setGroup(oaoEquip.group.toString())
                        .setUnit("%").setTz(tz).build()
                    val outsideDamperMinOpenDuringRecircId =
                        haystack.addPoint(outsideDamperMinOpenDuringRecirc)
                    haystack.writeDefaultValById(
                        outsideDamperMinOpenDuringRecircId,
                        outsideDamperMinOpenVal
                    )
                    val outsideDamperMinOpenDuringConditioning = Point.Builder().setDisplayName(
                        SystemTuners.getDisplayNameFromVariation(
                            "$equipDis-outsideDamperMinOpenDuringConditioning"
                        )
                    )
                        .setEquipRef(oaoEquip.id)
                        .setSiteRef(siteRef)
                        .addMarker("config").addMarker("oao").addMarker("writable")
                        .addMarker("outside").addMarker("damper").addMarker("min").addMarker("open")
                        .addMarker("conditioning").addMarker("sp")
                        .setGroup(oaoEquip.group.toString())
                        .setUnit("%").setTz(tz).build()
                    val outsideDamperMinOpenDuringConditioningId =
                        haystack.addPoint(outsideDamperMinOpenDuringConditioning)
                    haystack.writeDefaultValById(
                        outsideDamperMinOpenDuringConditioningId,
                        outsideDamperMinOpenVal
                    )
                    val outsideDamperMinOpenDuringFanLow = Point.Builder().setDisplayName(
                        SystemTuners.getDisplayNameFromVariation(
                            "$equipDis-outsideDamperMinOpenDuringFanLow"
                        )
                    )
                        .setEquipRef(oaoEquip.id)
                        .setSiteRef(siteRef)
                        .addMarker("config").addMarker("oao").addMarker("writable")
                        .addMarker("outside").addMarker("damper").addMarker("min").addMarker("open")
                        .addMarker("fan").addMarker("low").addMarker("sp")
                        .setGroup(oaoEquip.group.toString())
                        .setUnit("%").setTz(tz).build()
                    val outsideDamperMinOpenDuringFanLowId =
                        haystack.addPoint(outsideDamperMinOpenDuringFanLow)
                    haystack.writeDefaultValById(
                        outsideDamperMinOpenDuringFanLowId,
                        outsideDamperMinOpenVal
                    )
                    val outsideDamperMinOpenDuringFanMedium = Point.Builder().setDisplayName(
                        SystemTuners.getDisplayNameFromVariation(
                            "$equipDis-outsideDamperMinOpenDuringFanMedium"
                        )
                    )
                        .setEquipRef(oaoEquip.id)
                        .setSiteRef(siteRef)
                        .addMarker("config").addMarker("oao").addMarker("writable")
                        .addMarker("outside").addMarker("damper").addMarker("min").addMarker("open")
                        .addMarker("fan").addMarker("medium").addMarker("sp")
                        .setGroup(oaoEquip.group.toString())
                        .setUnit("%").setTz(tz).build()
                    val outsideDamperMinOpenDuringFanMediumId =
                        haystack.addPoint(outsideDamperMinOpenDuringFanMedium)
                    haystack.writeDefaultValById(
                        outsideDamperMinOpenDuringFanMediumId,
                        outsideDamperMinOpenVal
                    )
                    val outsideDamperMinOpenDuringFanHigh = Point.Builder().setDisplayName(
                        SystemTuners.getDisplayNameFromVariation(
                            "$equipDis-outsideDamperMinOpenDuringFanHigh"
                        )
                    )
                        .setEquipRef(oaoEquip.id)
                        .setSiteRef(siteRef)
                        .addMarker("config").addMarker("oao").addMarker("writable")
                        .addMarker("outside").addMarker("damper").addMarker("min").addMarker("open")
                        .addMarker("fan").addMarker("high").addMarker("sp")
                        .setGroup(oaoEquip.group.toString())
                        .setUnit("%").setTz(tz).build()
                    val outsideDamperMinOpenDuringFanHighId =
                        haystack.addPoint(outsideDamperMinOpenDuringFanHigh)
                    haystack.writeDefaultValById(
                        outsideDamperMinOpenDuringFanHighId,
                        outsideDamperMinOpenVal
                    )
                }
            }
        }

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

        fun doTitle24HssPointsMigration(haystack: CCUHsApi) {
            val equips = haystack.readAllEntities("equip and hyperstatsplit")
            equips.forEach(Consumer { equip: HashMap<Any?, Any?>? ->
                val hssEquip: Equip =
                    Equip.Builder().setHashMap(equip).build()

                // Config: fanOutRecirculateAnalog1/2/3 points
                val aoutEnabled: Double =
                    haystack.readDefaultVal("enabled and analog1 and  output and equipRef == \"" + hssEquip.id + "\"")
                val aoutMapping: Double =
                    haystack.readDefaultVal("output and association and analog1 and equipRef == \"" + hssEquip.id + "\"")
                val aout2Enabled: Double =
                    haystack.readDefaultVal("enabled and analog2 and  output and equipRef == \"" + hssEquip.id + "\"")
                val aout2Mapping: Double =
                    haystack.readDefaultVal("output and association and analog2 and equipRef == \"" + hssEquip.id + "\"")
                val aout3Enabled: Double =
                    haystack.readDefaultVal("enabled and analog3 and  output and equipRef == \"" + hssEquip.id + "\"")
                val aout3Mapping: Double =
                    haystack.readDefaultVal("output and association and analog3 and equipRef == \"" + hssEquip.id + "\"")
                if (aoutEnabled == 1.0 && aoutMapping == CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal.toDouble()) {
                    prepareAndCreateRecirculateConfigPoint(
                        haystack,
                        hssEquip,
                        "fanOutRecirculateAnalog1",
                        "analog1"
                    )
                    createEconomizerConifgPoint(
                        haystack,
                        hssEquip,
                        "analog1DuringEconomizer",
                        "analog1"
                    )
                }
                if (aout2Enabled == 1.0 && aout2Mapping == CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal.toDouble()) {
                    prepareAndCreateRecirculateConfigPoint(
                        haystack,
                        hssEquip,
                        "fanOutRecirculateAnalog2",
                        "analog2"
                    )
                    createEconomizerConifgPoint(
                        haystack,
                        hssEquip,
                        "analog2DuringEconomizer",
                        "analog2"
                    )
                }
                if (aout3Enabled == 1.0 && aout3Mapping == CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal.toDouble()) {
                    prepareAndCreateRecirculateConfigPoint(
                        haystack,
                        hssEquip,
                        "fanOutRecirculateAnalog3",
                        "analog3"
                    )
                    createEconomizerConifgPoint(
                        haystack,
                        hssEquip,
                        "analog3DuringEconomizer",
                        "analog3"
                    )
                }
                val minFanRuntimePostConditioningread: HashMap<Any, Any> =
                    CCUHsApi.getInstance()
                        .readEntity("postconditioning and tuner and fan and cur and min and zone and equipRef == \"" + hssEquip.id + "\"")
                if (minFanRuntimePostConditioningread.isEmpty()) {
                    // Tuner: Add MinFanRuntimePostConditioning tuner
                    val minFanRuntimePostConditioningTuner: Point =
                        Point.Builder()
                            .setDisplayName(hssEquip.displayName + "-standalone" + "MinFanRuntimePostConditioning")
                            .setSiteRef(hssEquip.siteRef)
                            .setEquipRef(hssEquip.id)
                            .setRoomRef(hssEquip.roomRef)
                            .setFloorRef(hssEquip.floorRef)
                            .setTz(hssEquip.tz)
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
                        CCUHsApi.getInstance().addPoint(minFanRuntimePostConditioningTuner)
                    CCUHsApi.getInstance().writeDefaultTunerValById(minFanRuntimePostConditioningId, 5.0)
                }

                // User Story 25461: Title 24: Pre-Purge sequence on HS Split
                // Tuner: PrePurgeRunTime
                val prePurgeRunTimeread: HashMap<Any, Any> = CCUHsApi.getInstance()
                    .readEntity("prePurge and tuner and min and cpu and standalone and oao and runtime and cur and equipRef == \"" + hssEquip.id + "\"")
                if (prePurgeRunTimeread.isEmpty()) {
                    val prePurgeRunTime: Point = Point.Builder()
                        .setDisplayName(hssEquip.displayName + "-standalone" + "PrePurgeRunTime")
                        .setSiteRef(hssEquip.siteRef)
                        .setEquipRef(hssEquip.id)
                        .setRoomRef(hssEquip.roomRef)
                        .setFloorRef(hssEquip.floorRef)
                        .setTz(hssEquip.tz)
                        .setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                        .setMinVal(0.toString()).setMaxVal(360.toString())
                        .setIncrementVal(1.toString())
                        .setUnit("m")
                        .setHisInterpolate("cov")
                        .addMarker("tuner").addMarker("sp").addMarker("writable").addMarker("min")
                        .addMarker("his").addMarker("default")
                        .addMarker("prePurge").addMarker("cur").addMarker("runtime")
                        .addMarker("zone")
                        .addMarker("default").addMarker("cpu").addMarker("standalone")
                        .addMarker("oao")
                        .build()
                    val prePurgeRunTimeId: String = CCUHsApi.getInstance().addPoint(prePurgeRunTime)
                    CCUHsApi.getInstance().writeDefaultTunerValById(prePurgeRunTimeId, 120.0)
                }


                // Tuner: PrePurgeOccupiedTimeOffset
                val prePurgeOccupiedTimeRead: HashMap<Any, Any> =
                    CCUHsApi.getInstance()
                        .readEntity("prePurge and offset and time and cur and zone and occupied and cpu and standalone and oao and equipRef == \"" + hssEquip.id + "\"")
                if (prePurgeOccupiedTimeRead.isEmpty()) {
                    val prePurgeOccupiedTime: Point =
                        Point.Builder()
                            .setDisplayName(hssEquip.displayName + "-standalone" + "PrePurgeOccupiedTimeOffset")
                            .setSiteRef(hssEquip.siteRef)
                            .setEquipRef(hssEquip.id)
                            .setRoomRef(hssEquip.roomRef)
                            .setFloorRef(hssEquip.floorRef)
                            .setTz(hssEquip.tz)
                            .setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                            .setMinVal(0.toString()).setMaxVal(360.toString())
                            .setIncrementVal(1.toString())
                            .setUnit("m")
                            .setHisInterpolate("cov")
                            .addMarker("tuner").addMarker("sp").addMarker("writable")
                            .addMarker("min")
                            .addMarker("his").addMarker("default")
                            .addMarker("prePurge").addMarker("offset").addMarker("cur")
                            .addMarker("time")
                            .addMarker("zone").addMarker("occupied").addMarker("cpu")
                            .addMarker("standalone").addMarker("oao")
                            .build()
                    val prePurgeOccupiedTimeId: String =
                        CCUHsApi.getInstance().addPoint(prePurgeOccupiedTime)
                    CCUHsApi.getInstance().writeDefaultTunerValById(prePurgeOccupiedTimeId, 180.0)
                }

                // Tuner: PrePurgeFanSpeedTuner
                val prePurgeFanSpeedTunerRead: HashMap<Any, Any> =
                    CCUHsApi.getInstance()
                        .readEntity("prePurge and oao and fan and speed and cur and cpu and standalone and zone and default and equipRef == \"" + hssEquip.id + "\"")
                if (prePurgeFanSpeedTunerRead.isEmpty()) {
                    val prePurgeFanSpeedTuner: Point =
                        Point.Builder()
                            .setDisplayName(hssEquip.displayName + "-standalone" + "PrePurgeFanSpeedTuner")
                            .setSiteRef(hssEquip.siteRef)
                            .setEquipRef(hssEquip.id)
                            .setRoomRef(hssEquip.roomRef)
                            .setFloorRef(hssEquip.floorRef)
                            .setTz(hssEquip.tz)
                            .setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                            .setMinVal(0.toString()).setMaxVal(100.toString())
                            .setIncrementVal(1.toString())
                            .setUnit("%")
                            .setHisInterpolate("cov")
                            .addMarker("tuner").addMarker("sp").addMarker("writable")
                            .addMarker("min")
                            .addMarker("his").addMarker("default")
                            .addMarker("prePurge").addMarker("oao").addMarker("fan")
                            .addMarker("cur")
                            .addMarker("speed").addMarker("cpu").addMarker("standalone")
                            .addMarker("zone")
                            .build()
                    val prePurgeFanSpeedTunerId: String =
                        CCUHsApi.getInstance().addPoint(prePurgeFanSpeedTuner)
                    CCUHsApi.getInstance().writeDefaultTunerValById(prePurgeFanSpeedTunerId, 50.0)
                }

                // Config: PrePurgeEnabled
                val prePurgeEnabledRead: HashMap<Any, Any> = CCUHsApi.getInstance()
                    .readEntity("prePurge and userIntent and writable and standalone and cpu and cur and standalone and zone and enabled and equipRef == \"" + hssEquip.id + "\"")
                if (prePurgeEnabledRead.isEmpty()) {
                    val prePurgeEnabled: Point = Point.Builder()
                        .setDisplayName(hssEquip.displayName + "-prePurgeEnabled")
                        .setSiteRef(hssEquip.siteRef)
                        .setEquipRef(hssEquip.id)
                        .setRoomRef(hssEquip.roomRef)
                        .setFloorRef(hssEquip.floorRef)
                        .setTz(hssEquip.tz)
                        .setGroup(hssEquip.group.toString())
                        .setHisInterpolate(Tags.COV)
                        .setEnums("off,on")
                        .addMarker("prePurge").addMarker("userIntent").addMarker("writable")
                        .addMarker("standalone").addMarker("cpu").addMarker("cur").addMarker("his")
                        .addMarker("zone").addMarker("enabled")
                        .build()
                    val prePurgeEnabledId: String = haystack.addPoint(prePurgeEnabled)
                    haystack.writeDefaultValById(prePurgeEnabledId, 0.0)
                }

                // User Story 25096: Title 24: Addition of Generic Fault options to Universal Inputs migration
                universalInPointsMigration(hssEquip, haystack)
                val outsideDamperMinOpen: HashMap<Any, Any> =
                    haystack.readEntity("config and outside and damper and min and open and not enhanced and not purge and equipRef ==\"" + hssEquip.id + "\"")
                if (outsideDamperMinOpen.isNotEmpty()) {
                    val outsideDamperMinOpenVal: Double =
                        haystack.readPointPriorityVal(outsideDamperMinOpen["id"].toString())
                    haystack.deleteEntity(outsideDamperMinOpen["id"].toString())
                    val outsideDamperMinOpenDuringRecirc: Point =
                        Point.Builder()
                            .setDisplayName(hssEquip.displayName + "-outsideDamperMinOpenDuringRecirc")
                            .setSiteRef(hssEquip.siteRef)
                            .setEquipRef(hssEquip.id)
                            .setRoomRef(hssEquip.roomRef)
                            .setFloorRef(hssEquip.floorRef)
                            .setTz(hssEquip.tz)
                            .setGroup(hssEquip.group.toString())
                            .setUnit("%")
                            .addMarker(Tags.CPU)
                            .addMarker(Tags.STANDALONE)
                            .setHisInterpolate("cov")
                            .addMarker("config").addMarker("oao").addMarker("writable")
                            .addMarker("outside")
                            .addMarker("damper").addMarker("min").addMarker("open")
                            .addMarker("recirc")
                            .addMarker("sp").addMarker("cpu").addMarker("his")
                            .build()
                    val outsideDamperMinOpenDuringRecircId: String =
                        haystack.addPoint(outsideDamperMinOpenDuringRecirc)
                    haystack.writeDefaultValById(
                        outsideDamperMinOpenDuringRecircId,
                        outsideDamperMinOpenVal
                    )
                    val outsideDamperMinOpenDuringConditioning: Point =
                        Point.Builder()
                            .setDisplayName(hssEquip.displayName + "-outsideDamperMinOpenDuringConditioning")
                            .setSiteRef(hssEquip.siteRef)
                            .setEquipRef(hssEquip.id)
                            .setRoomRef(hssEquip.roomRef)
                            .setFloorRef(hssEquip.floorRef)
                            .setTz(hssEquip.tz)
                            .setGroup(hssEquip.group.toString())
                            .setUnit("%")
                            .addMarker(Tags.CPU)
                            .addMarker(Tags.STANDALONE)
                            .setHisInterpolate("cov")
                            .addMarker("config").addMarker("oao").addMarker("writable")
                            .addMarker("outside")
                            .addMarker("damper").addMarker("min").addMarker("open")
                            .addMarker("conditioning").addMarker("sp").addMarker("cpu")
                            .addMarker("his")
                            .build()
                    val outsideDamperMinOpenDuringConditioningId: String =
                        haystack.addPoint(outsideDamperMinOpenDuringConditioning)
                    haystack.writeDefaultValById(
                        outsideDamperMinOpenDuringConditioningId,
                        outsideDamperMinOpenVal
                    )
                    val outsideDamperMinOpenDuringFanLow: Point =
                        Point.Builder()
                            .setDisplayName(hssEquip.displayName + "-outsideDamperMinOpenDuringFanLow")
                            .setSiteRef(hssEquip.siteRef)
                            .setEquipRef(hssEquip.id)
                            .setRoomRef(hssEquip.roomRef)
                            .setFloorRef(hssEquip.floorRef)
                            .setTz(hssEquip.tz)
                            .setGroup(hssEquip.group.toString())
                            .setUnit("%")
                            .addMarker(Tags.CPU)
                            .addMarker(Tags.STANDALONE)
                            .setHisInterpolate("cov")
                            .addMarker("config").addMarker("oao").addMarker("writable")
                            .addMarker("outside")
                            .addMarker("damper").addMarker("min").addMarker("open").addMarker("fan")
                            .addMarker("low").addMarker("sp").addMarker("cpu").addMarker("his")
                            .build()
                    val outsideDamperMinOpenDuringFanLowId: String =
                        haystack.addPoint(outsideDamperMinOpenDuringFanLow)
                    haystack.writeDefaultValById(
                        outsideDamperMinOpenDuringFanLowId,
                        outsideDamperMinOpenVal
                    )
                    val outsideDamperMinOpenDuringFanMedium: Point =
                        Point.Builder()
                            .setDisplayName(hssEquip.displayName + "-outsideDamperMinOpenDuringFanMedium")
                            .setSiteRef(hssEquip.siteRef)
                            .setEquipRef(hssEquip.id)
                            .setRoomRef(hssEquip.roomRef)
                            .setFloorRef(hssEquip.floorRef)
                            .setTz(hssEquip.tz)
                            .setGroup(hssEquip.group.toString())
                            .setUnit("%")
                            .addMarker(Tags.CPU)
                            .addMarker(Tags.STANDALONE)
                            .setHisInterpolate("cov")
                            .addMarker("config").addMarker("oao").addMarker("writable")
                            .addMarker("outside")
                            .addMarker("damper").addMarker("min").addMarker("open").addMarker("fan")
                            .addMarker("medium").addMarker("sp").addMarker("cpu").addMarker("his")
                            .build()
                    val outsideDamperMinOpenDuringFanMediumId: String =
                        haystack.addPoint(outsideDamperMinOpenDuringFanMedium)
                    haystack.writeDefaultValById(
                        outsideDamperMinOpenDuringFanMediumId,
                        outsideDamperMinOpenVal
                    )
                    val outsideDamperMinOpenDuringFanHigh: Point =
                        Point.Builder()
                            .setDisplayName(hssEquip.displayName + "-outsideDamperMinOpenDuringFanHigh")
                            .setSiteRef(hssEquip.siteRef)
                            .setEquipRef(hssEquip.id)
                            .setRoomRef(hssEquip.roomRef)
                            .setFloorRef(hssEquip.floorRef)
                            .setTz(hssEquip.tz)
                            .setGroup(hssEquip.group.toString())
                            .setUnit("%")
                            .addMarker(Tags.CPU)
                            .addMarker(Tags.STANDALONE)
                            .setHisInterpolate("cov")
                            .addMarker("config").addMarker("oao").addMarker("writable")
                            .addMarker("outside")
                            .addMarker("damper").addMarker("min").addMarker("open").addMarker("fan")
                            .addMarker("high").addMarker("sp").addMarker("cpu").addMarker("his")
                            .build()
                    val outsideDamperMinOpenDuringFanHighId: String =
                        haystack.addPoint(outsideDamperMinOpenDuringFanHigh)
                    haystack.writeDefaultValById(
                        outsideDamperMinOpenDuringFanHighId,
                        outsideDamperMinOpenVal
                    )
                }
            })
        }
    }
}