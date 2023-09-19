package a75f.io.logic.diag.otastatus

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Tags
import a75f.io.logic.diag.otastatus.OtaStatusDiagPoint.Companion.addOTAStatusPoint

/**
 * Created by Manjunath K on 28-02-2023.
 */

class OtaStatusMigration {


    companion object{


        fun migrateOtaStatusPoint(){

            val hsApi = CCUHsApi.getInstance()
            val siteMap = CCUHsApi.getInstance().read(Tags.SITE)
            val ccu: HashMap<*, *> = CCUHsApi.getInstance().readEntity("device and ccu")
            val systemEquip: HashMap<*, *> = CCUHsApi.getInstance().readEntity("system and equip and not modbus")

            migrateOtaStatusForCCU(siteMap,ccu)
            migrateOtaStatusForCM(siteMap, systemEquip[Tags.ID].toString())
            migrateOtaStatusForModules(hsApi)

        }

        private fun migrateOtaStatusForCCU(siteMap: HashMap<*,*>, ccu: HashMap<*,*>) {
            if (siteMap.isNotEmpty()) {
                addOTAStatusPoint(
                    siteMap["dis"].toString() + "-CCU",
                    ccu["equipRef"].toString(),
                    ccu["siteRef"].toString(),
                    siteMap[Tags.TZ].toString(),
                    CCUHsApi.getInstance()
                )
            }
        }
        private fun migrateOtaStatusForCM(site: HashMap<*,*>, systemEquipRef: String){
            if (site.isNotEmpty()) {
                addOTAStatusPoint(
                    site[Tags.DIS].toString() + "-CM",
                    systemEquipRef,
                    site[Tags.ID].toString(),
                    site[Tags.TZ].toString(),
                    CCUHsApi.getInstance()
                )
            }
        }

        private fun migrateOtaStatusForModules(hsApi: CCUHsApi){

            // there are some devices with equip but do not have actual smart device so following query will fetch equips that are contains any smart device
            // For OTN and modbus we won't support OTA so we are not creating any point for them

           val allZoneEquipments =  hsApi.readAllEntities("equip and zone and (smartnode or hyperstat  or helionode or smartstat)")

            allZoneEquipments.forEach { equipMap ->

                val equip =  Equip.Builder().setHashMap(equipMap).build()
                var nodeName = Tags.HN
                if (equip.markers.contains(Tags.HYPERSTAT)){
                    nodeName = Tags.HS
                } else if(equip.markers.contains(Tags.SMART_NODE)){
                    nodeName = Tags.SN
                } else if(equip.markers.contains(Tags.SMART_STAT)){
                    nodeName = Tags.SS
                }
                addOTAStatusPoint(
                    nodeName + "-" + equip.group.toInt(),
                    equip.id,
                    equip.siteRef,
                    equip.roomRef,
                    equip.floorRef,
                    equip.group.toInt(),
                    equip.tz,
                    hsApi
                )

            }

        }

    }
}