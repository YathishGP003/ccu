package a75f.io.logic.migration.modbus

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.logger.CcuLog
import a75f.io.logic.L

        private val MASIBUS_DI16_MODEL = Pair("Masibus", "DI16")
        private val MASIBUS_AHU16_DI_MODULE_MODEL = Pair("Masibus", "aHU16DIModule")
        private val F75_CSUSA_HQCP_MODEL = Pair("f75", "csusaHQCP")
        private val F75_WENDYS_POWELL_MODEL = Pair("F75", "wendysPowellMAU")

        private const val DEFAULT_ENUM_PAIR = "Off=0,On=1"
        private const val TRIP_ENUM_PAIR = "Normal=0,Trip=1"
        private const val FILTER_ENUM_PAIR = "Normal=0,Clogged=1"
        private const val DAMPER_ENUM_PAIR = "Close=0,Open=1"
        private const val MANUAL_AUTO_ENUM_PAIR = "Manual=0,Auto=1"
        private const val DI16_ALARM_NORMAL_ENUM_PAIR = "Alarm=0,Normal=1"
        private const val CSUSA_ALARM_NORMAL_ENUM_PAIR = "Normal=0,Alarm=1"

        private val enumMappings = mapOf(
                MASIBUS_AHU16_DI_MODULE_MODEL.second to { shortDis: String ->
                    when (shortDis) {
                        "VFD-1 & VFD-2 ON/OFF Status", "AHU Run Status", "CSU-1 Run Status", "CSU-2 Run Status"  -> DEFAULT_ENUM_PAIR
                        "AHU Filter Status", "CSU-1 Filter Status", "CSU-2 Filter Status" -> FILTER_ENUM_PAIR
                        "VFD-1 & VFD-2 Trip Status" -> TRIP_ENUM_PAIR
                        "Fire Damper Status" -> DAMPER_ENUM_PAIR
                        "Ex Fan A/M Status" -> MANUAL_AUTO_ENUM_PAIR
                        else -> DEFAULT_ENUM_PAIR
                    }
                },
                MASIBUS_DI16_MODEL.second to {shortDis: String ->
                    when (shortDis) {
                        "AHU Auto Manual Status" -> MANUAL_AUTO_ENUM_PAIR
                        "AHU Filter Status" -> FILTER_ENUM_PAIR
                        "AHU Run Status" -> DEFAULT_ENUM_PAIR
                        "AHU Fire Damper Status" -> DAMPER_ENUM_PAIR
                        "Fire Alarm Status" -> DI16_ALARM_NORMAL_ENUM_PAIR
                        else -> DEFAULT_ENUM_PAIR
                    }
                },
                F75_CSUSA_HQCP_MODEL.second to { shortDis: String ->
                    when (shortDis) {
                        "CT Fan Sts", "CT Fan SS" -> DEFAULT_ENUM_PAIR
                        "CT Fan Fault", "CT Alarm" -> CSUSA_ALARM_NORMAL_ENUM_PAIR
                        else -> DEFAULT_ENUM_PAIR
                    }
                },
                F75_WENDYS_POWELL_MODEL.second to { shortDis: String ->
                    when (shortDis) {
                        "Loop Action" -> DEFAULT_ENUM_PAIR
                        else -> DEFAULT_ENUM_PAIR
                    }
                }
        )

        fun correctEnumsForCorruptModbusPoints(hayStack: CCUHsApi) {
            CcuLog.d(L.TAG_CCU_MODBUS, "Performing correction of enums for Modbus points if required")
            val statusQuery = "((status and not ota and not message and zone and his) or (occupancy and (mode or state))) and enum and modbus"
            val modelList = listOf(MASIBUS_DI16_MODEL, MASIBUS_AHU16_DI_MODULE_MODEL, F75_CSUSA_HQCP_MODEL, F75_WENDYS_POWELL_MODEL)

            if(hayStack.readEntity(statusQuery).isNotEmpty()) {
                modelList.forEach { vendorModelPair ->
                    val modbusEquip = hayStack.readAllEntities("equip and modbus and model == \"${vendorModelPair.second}\" and vendor == \"${vendorModelPair.first}\"")
                    modbusEquip.forEach { modbusEquipMap ->
                        val statusPoints = hayStack.readAllEntities("$statusQuery and equipRef == \"${modbusEquipMap["id"].toString()}\"")
                        statusPoints.forEach { pointMap ->
                            val point = Point.Builder().setHashMap(pointMap).build()
                            if(!point.enums.isNullOrEmpty() && (point.enums.contains("rfdead") || point.enums.contains("demandresponseoccupied"))) {
                                point.enums = getEnumsByShortDis(vendorModelPair.second, point.shortDis)
                                hayStack.updatePoint(point, point.id)
                            }
                        }
                    }
                }
            }
            CcuLog.d(L.TAG_CCU_MODBUS, "Correction of enums for Modbus points is done")
        }

        private fun getEnumsByShortDis(modelName: String, shortDis: String): String {
            return enumMappings[modelName]?.invoke(shortDis) ?: DEFAULT_ENUM_PAIR
        }