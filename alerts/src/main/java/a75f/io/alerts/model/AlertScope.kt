package a75f.io.alerts.model

import a75f.io.logger.CcuLog
import org.joda.time.DateTime

/**
 * AlertScore and associated classes are DTOs defined in the remote alerts service.  They are how
 * the server communicated deep muting for alert defs.
 *
 * @author tcase@75f.io
 * Created on 4/22/21.
 */

data class AlertScope(
   val disableDateTimeFrom: DateTime?,
   val disableDateTimeThru: DateTime?,
   val devices: List<AlertScopeDevice>
) {

   @JvmOverloads
   fun isMuted(ccuId: String? = null, equipId: String? = null): Boolean {

      if (isWhollyMuted()) return true

      if (isCcuMuted(ccuId)) return true

      val scopedEquip = findEquip(equipId, ccuId)
      scopedEquip?.let {
         return isMutedForTimes(it.disableDateTimeFrom, it.disableDateTimeThru)
      }
      return false
   }

   fun deepMuteEndTimeString(ccuId: String? = null, equipId: String? = null): String {

      return when {
         isWhollyMuted() -> {
            disableDateTimeThru?.toString() ?: "indefinite"
         }
         isCcuMuted(ccuId) -> {
            findCcu(ccuId)?.disableDateTimeThru?.toString() ?: "indefinite"
         }
         isMuted(ccuId, equipId) -> {
            findEquip(ccuId, equipId)?.disableDateTimeThru?.toString() ?: "indefinite"
         }
         else -> "unknown"
      }
   }

   private fun findCcu(ccuId: String?): AlertScopeDevice? {
      if (ccuId == null) return null
      return devices.find { it.deviceId == ccuId }
   }


   private fun findEquip(equipId: String?, ccuId: String?): AlertScopeEquip? {
      if (ccuId == null || equipId == null) return null

      val scopedDevice = devices.find { it.deviceId == ccuId }
      return scopedDevice?.equips?.find { it.equipId == equipId }
   }

   private fun isCcuMuted(ccuId: String?): Boolean {

      findCcu(ccuId)?.let { device ->
         return isMutedForTimes(device.disableDateTimeFrom, device.disableDateTimeThru)
      }
      return false
   }

   private fun isWhollyMuted() = isMutedForTimes(disableDateTimeFrom, disableDateTimeThru)
}

fun isMutedForTimes(start: DateTime?, end: DateTime?): Boolean {
   return when {
      start == null -> false
      end == null -> start.isBeforeNow
      else -> {
         val range = start.rangeTo(end)
         range.contains(DateTime.now())
      }
   }
}

data class AlertScopeDevice(
   val deviceId: String,
   val disableDateTimeFrom: DateTime?,
   val disableDateTimeThru: DateTime?,
   val equips: List<AlertScopeEquip>
)

data class AlertScopeEquip(
   val equipId: String,
   val disableDateTimeFrom: DateTime,
   val disableDateTimeThru: DateTime,
)

