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
 * Determine if given floor contains zone equips belonging to CCU other than this one.
 * This method blocks, calling the server to check for other equip/CCUs on this site.
 */
fun isFloorUsedByOtherCcuAsync(floorId: String): Boolean {

   val ccu = getCcu() ?: throw IllegalStateException("No CCU.  Cannot check one of its floors.")
   val thisAhuRef = ccu.get("ahuRef")
   val thisGatewayRef = ccu.get("gatewayRef")

   // Check all equips for the site from remote for a match with this CCU
   val equips = getZoneEquipsForSiteAsync()
   equips.forEach { equip ->
      CcuLog.i("CCU_HS", "Checking equip ${equip.displayName} for match of same floor, remote ccu, with floorRef ${equip.floorRef}, ahuRef ${equip.ahuRef}, and gatewayRef ${equip.gatewayRef}")

      val sameFloor = equip.floorRef == floorId
      val ahuRefsDifferent = equip.ahuRef != null && equip.ahuRef != thisAhuRef
      val gatewayRefsDifferent = equip.gatewayRef != null && equip.gatewayRef != thisGatewayRef

      // same floor but different ahuRef (other ccu)
      if (sameFloor && (ahuRefsDifferent || gatewayRefsDifferent)) {
         CcuLog.d("CCU_HS", "This floor has an equip belonging to a different CCU.");
         return true
      }
   }

   return false
}


// todo: test without a network connection.  Should throw an exception.
fun getZoneEquipsForSiteAsync(): List<Equip> {

   val hClient = HClient(CCUHsApi.getInstance().hsUrl, HayStackConstants.USER, HayStackConstants.PASS)
   val siteUID = CCUHsApi.getInstance().siteIdRef.toString()

   val hDictForCall = HDictBuilder().add("filter", "equip and zone and siteRef == $siteUID").toDict()
   val equipsHGrid = hClient.call("read", HGridBuilder.dictToGrid(hDictForCall))
      ?: throw java.lang.IllegalStateException("Unexpected null calling remote hClient in getZoneEquipsForSite")

   val hGridList: List<HashMap<*, *>> = CCUHsApi.getInstance().HGridToList(equipsHGrid)

   return hGridList.map {
      Equip.Builder().setHashMap(it).build()
   }
}
