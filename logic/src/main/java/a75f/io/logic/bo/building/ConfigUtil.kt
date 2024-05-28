package a75f.io.logic.bo.building

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.haystack.device.SmartNode

class ConfigUtil  {
    companion object {
        const val TAG = "Config Util";
        fun addConfigPoints(profileName: String, siteRef: String, roomRef: String, floorRef: String,
                            equipRef: String, tz: String, nodeAddress: String, equipDis: String,tag : String,
                            autoawayVal: Double, autoForceVal : Double) {
            CcuLog.d(TAG, "AutoForceOcccupied and Autoaway add Points")

            val hs = CCUHsApi.getInstance()
            if(!isAutoForceOccupiedAlreadyExist(equipRef)) {
                val autoforceoccupiedPoint = Point.Builder()
                    .setDisplayName("$equipDis-autoForceOccupiedEnabled")
                    .setEquipRef(equipRef)
                    .setSiteRef(siteRef)
                    .setRoomRef(roomRef)
                    .setFloorRef(floorRef)
                    .addMarker("config").addMarker(profileName).addMarker("writable")
                    .addMarker("zone")
                    .addMarker("forced").addMarker("occupied").addMarker("auto")
                    .addMarker("his").addMarker("enabled").setHisInterpolate("cov")
                    .setGroup(nodeAddress)
                    .setEnums("off,on")
                    .setTz(tz)
                if (tag != "") {
                    autoforceoccupiedPoint.addMarker(tag)
                    if (tag.contains("fcu")) {
                        autoforceoccupiedPoint.addMarker("standalone")
                    }
                }
                val autoforceoccupied = autoforceoccupiedPoint.build()
                val autoforceoccupiedId = hs.addPoint(autoforceoccupied)
                hs.writeDefaultValById(autoforceoccupiedId, autoForceVal)
                hs.writeHisValById(autoforceoccupiedId, autoForceVal)
            } else {
                CcuLog.d(TAG, "AutoForce Occcupied point is already exist")
            }

            if(!isAutoAwayAlreadyExist(equipRef)) {
                val autoawayPoint = Point.Builder()
                    .setDisplayName("$equipDis-autoawayEnabled")
                    .setEquipRef(equipRef)
                    .setSiteRef(siteRef)
                    .setRoomRef(roomRef)
                    .setFloorRef(floorRef)
                    .addMarker("config").addMarker(profileName).addMarker("writable")
                    .addMarker("zone").addMarker("away").addMarker("auto")
                    .setHisInterpolate("cov").addMarker("his").addMarker("enabled")
                    .setGroup(nodeAddress)
                    .setEnums("off,on")
                    .setTz(tz)
                if (tag != "") {
                    autoawayPoint.addMarker(tag)
                    if (tag.contains("fcu")) {
                        autoawayPoint.addMarker("standalone")
                    }
                }
                val autoaway = autoawayPoint.build()
                val autoawayId = hs.addPoint(autoaway)
                hs.writeDefaultValById(autoawayId, autoawayVal)
                hs.writeHisValById(autoawayId, autoawayVal)
            }else{
                CcuLog.d(TAG, "Auto away point is already exist")
            }

        }
        fun addOccupancyPointsSN(device: SmartNode, profileName: String, siteRef: String, roomRef: String, floorRef: String,
                               equipRef: String, tz: String, nodeAddress: String, equipDis: String){
            val hs = CCUHsApi.getInstance();
            if(!isOccupancyDetectionAlreadyExist(equipRef)) {
                val occupancyDetection = Point.Builder()
                    .setDisplayName("$equipDis-occupancyDetection")
                    .setEquipRef(equipRef)
                    .setSiteRef(siteRef)
                    .setRoomRef(roomRef)
                    .setFloorRef(floorRef).setHisInterpolate("cov")
                    .addMarker("occupancy").addMarker("detection").addMarker(profileName)
                    .addMarker("his").addMarker("zone")
                    .setGroup(nodeAddress)
                    .setEnums("false,true")
                    .setTz(tz)
                    .build()
                val occupancyDetectionId = hs.addPoint(occupancyDetection)
                hs.writeHisValById(occupancyDetectionId, 0.0)
            }else{
                CcuLog.d(TAG, "OccupancyDetection already exist")
            }
        }

        private fun isAutoForceOccupiedAlreadyExist(equipRef: String): Boolean {
            return CCUHsApi.getInstance().readEntity("config and auto and forced and occupied and equipRef == \"$equipRef\"").isNotEmpty()
        }
        private fun isAutoAwayAlreadyExist(equipRef: String): Boolean {
            return CCUHsApi.getInstance().readEntity("config and auto and away and enabled and equipRef  == \"$equipRef\"").isNotEmpty()
        }
        private fun isOccupancyDetectionAlreadyExist(equipRef: String): Boolean {
            return CCUHsApi.getInstance().readEntity("occupancy and detection and equipRef  == \"$equipRef\"").isNotEmpty()
        }

    }
}