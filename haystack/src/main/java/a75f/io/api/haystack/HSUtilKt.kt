package a75f.io.api.haystack

import a75f.io.logger.CcuLog
import org.projecthaystack.*
import org.projecthaystack.client.HClient
import java.util.*

/**
 * @author tcase@75f.io
 * Created on 5/19/21.
 */

/**
 * Get this CCU -- the CCU object this instance of the app runs on.
 * Ccu is historically represented as a HashMap<*,*> like many haystack entities.
 */
fun getCcu(): HashMap<*,*>? {
   return CCUHsApi.getInstance().read("device and ccu")
}

/**
 * Determine if given floor contains zone equips belonging to an application (CCU or Site Manager) other than this one.
 * This method blocks, calling the server to check for other equip/CCUs on this site.
 */
fun isFloorUsedByOtherApplicationAsync(floorId: String): Boolean {

   val ccu = getCcu() ?: throw IllegalStateException("No CCU.  Cannot check one of its floors.")
   val thisAhuRef = ccu.get("ahuRef")
   val thisGatewayRef = ccu.get("gatewayRef")

   // Check if floor is created by SiteManager
   val floors = getFloorsForSiteAsync()
   floors.forEach { floor ->
      CcuLog.i("CCU_HS", "Checking floor ${floor.displayName} for match of same floor, remote ccu, with floorRef ${floor.id}")

      val sameFloor = floor.id == floorId
      val createdBySiteManager = floor.createdByApplication != null && floor.createdByApplication.equals(SITE_MANAGER_APPLICATION)
      if (sameFloor && createdBySiteManager) {
         CcuLog.d("CCU_HS", "This floor is created by Site Manager.");
         return true
      }
   }

   // Check all equips for the site from remote for a match with this CCU
   val equips = getZoneEquipsForSiteAsync()
   equips.forEach { equip ->
      CcuLog.i("CCU_HS", "Checking equip ${equip.displayName} for match of same floor, remote ccu, with floorRef ${equip.floorRef}, ahuRef ${equip.ahuRef}, and gatewayRef ${equip.gatewayRef}")

      val sameFloor = equip.floorRef == floorId
      val ahuRefsDifferent = equip.ahuRef != null && equip.ahuRef != thisAhuRef
      val gatewayRefsDifferent = equip.gatewayRef != null && equip.gatewayRef != thisGatewayRef
      val createdBySiteManager = equip.createByApplication != null && equip.createByApplication.equals(SITE_MANAGER_APPLICATION)

      if (sameFloor) {
         // same floor but different ahuRef (other ccu)
         if(ahuRefsDifferent || gatewayRefsDifferent) {
            CcuLog.d("CCU_HS", "This floor has an equip belonging to a different CCU.");
            return true
         }
         else if(createdBySiteManager) {
            CcuLog.d("CCU_HS", "This floor has an equip created by Site Manager.");
            return true
         }
      }
   }

   val zones = getZonesForSiteAsync()
   zones.forEach { zone ->
      CcuLog.i("CCU_HS", "Checking zone ${zone.displayName} for match of same floor, remote ccu, with floorRef ${zone.floorRef}")

      val sameFloor = zone.floorRef == floorId
      val createdBySiteManager = zone.createdByApplication != null && zone.createdByApplication.equals(SITE_MANAGER_APPLICATION)
      if (sameFloor && createdBySiteManager) {
         CcuLog.d("CCU_HS", "This floor has a zone created by Site Manager.");
         return true
      }
   }

   return false
}


// todo: test without a network connection.  Should throw an exception.
fun getZoneEquipsForSiteAsync(): List<Equip> {
   val hGridList: List<HashMap<*, *>> = readByFilter("equip and zone and siteRef")

   return hGridList.map {
      Equip.Builder().setHashMap(it).build()
   }
}

fun getZonesForSiteAsync(): List<Zone> {
   val hGridList: List<HashMap<*, *>> = readByFilter("room and siteRef")

   return hGridList.map {
      Zone.Builder().setHashMap(it).build()
   }
}

fun getFloorsForSiteAsync(): List<Floor> {
   val hGridList: List<HashMap<*, *>> = readByFilter("floor and siteRef")

   return hGridList.map {
      Floor.Builder().setHashMap(it).build()
   }
}

private fun readByFilter(filter: String): List<HashMap<*, *>> {
   val hClient = HClient(CCUHsApi.getInstance().hsUrl, HayStackConstants.USER, HayStackConstants.PASS)
   val siteUID = CCUHsApi.getInstance().siteIdRef.toString()

   val hDictForCall = HDictBuilder().add("filter", "$filter == $siteUID").toDict()
   val hGrids = hClient.call("read", HGridBuilder.dictToGrid(hDictForCall))
           ?: throw java.lang.IllegalStateException("Unexpected null calling remote hClient in readByFilter")

   return CCUHsApi.getInstance().HGridToList(hGrids)
}

const val SITE_MANAGER_APPLICATION: String = "SITE_MANAGER"