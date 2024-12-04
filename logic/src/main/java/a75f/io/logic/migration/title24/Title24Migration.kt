package a75f.io.logic.migration.title24

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import java.util.function.Consumer

class Title24Migration {
    companion object {

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