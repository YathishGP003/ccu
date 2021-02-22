@file:JvmName("MigrateToStr")

package a75f.io.api.haystack.util

import a75f.io.api.haystack.CCUTagsDb
import a75f.io.logger.CcuLog
import org.projecthaystack.HBool
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HMarker
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Function.  (Can be called statically in Java with `MigrateToStr.migrateTagsDb(..)`)
 *
 * For given ccu tags database, swap in "Str" for any kind: "string" values in the tags Map.
 * Save the data and update the "updateIdMap" so values are synced with the server.
 *
 * This code in intended to run one time for any updating CCU as of Jan 2021.
 *
 * @author Tony Case
 * Created on 1/15/21.
 */
fun migrateTagsDb(tagsDb: CCUTagsDb) {

   val tagsMap = tagsDb.getTagsMap()
   val updateIdMap = tagsDb.getUpdateIdMap()
   val localToGlobalIdMapping = tagsDb.getIdMap()

   CcuLog.i("CCU_HS", "tags map migration" )
   printTagsMap(tagsMap)

   // we are migrating the TagsMap entries in place inside the hashmap.
   tagsMap.entries

      // for all entries, take only those with kind: string (ingore case)
      .filter { it.value.get("kind", false)?.toString()?.toLowerCase(Locale.US).equals("string", true) }

      // update the HDict for that key in place, with new kind value
      .forEach {
         tagsMap[it.key] = it.value.mutate().swap("kind", "Str")
         addIdToUpdateMap(it.key, updateIdMap, localToGlobalIdMapping)
      }

   // Again.  This time to change Tag enabled (Boolean) to portEnabled.  Don't change enabled marker tag.
   tagsMap.entries
      // for all entries, take only those with an enabled tag that is not a marker tag.
      .filter { it.value.get("enabled", false) !is HMarker? }
      // update the HDict, changing enabled to portEnabled, and it type to HBool
      .forEach {
         tagsMap[it.key] = it.value.mutate().swapKeyChangeValueToBool("enabled", "portEnabled")
         addIdToUpdateMap(it.key, updateIdMap, localToGlobalIdMapping)
      }

   // This time remove his tag from appVersion
   tagsMap.entries
      .filter { it.value.get("diag",false) != null
             && it.value.get("app",false) != null
             && it.value.get("version",false) != null
             && it.value.get("his",false) != null }
      .forEach {
         val hBuilder = it.value.mutate()
         hBuilder.remove("his")
         tagsMap[it.key] = hBuilder.toDict()
         addIdToUpdateMap(it.key, updateIdMap, localToGlobalIdMapping)
      }

   // This time remove his tag from scheduleStatus tag in dual duct
   tagsMap.entries
      .filter { it.value.get("scheduleStatus",false) != null
         && it.value.get("dualDuct",false) != null
         && it.value.get("his",false) != null }
      .forEach {
         val hBuilder = it.value.mutate()
         hBuilder.remove("his")
         tagsMap[it.key] = hBuilder.toDict()
         addIdToUpdateMap(it.key, updateIdMap, localToGlobalIdMapping)
      }

   // replace kind:Str from all other his tags in dualDuct with kind:Number
   tagsMap.entries
      .filter { it.value.get("dualDuct",false) != null
         && it.value.get("his",false) != null
         && (it.value.get("kind", false)?.toString()?.equals("Str") ?: false)
      }
      .forEach {
         tagsMap[it.key] = it.value.mutate().swap("kind", "Number")
         addIdToUpdateMap(it.key, updateIdMap, localToGlobalIdMapping)
      }

   CcuLog.i("CCU_HS", "tags map migration complete" )
   printTagsMap(tagsMap)

   // save changes locally
   tagsDb.saveTags()
}

// Left in case its handy for debugging.  Remove whenever
private fun printTagsMap(tagsMap: ConcurrentHashMap<String, HDict>) {
   tagsMap.toMap()
      .forEach { entry ->
         CcuLog.i("CCU_HS", "${entry.key}: ${entry.value.toString()}" ) }
}

private fun addIdToUpdateMap(
   entryKey: String,
   updateIdMap: ConcurrentHashMap<String, String>,
   localToGlobalIdMapping: ConcurrentHashMap<String, String>,
) {
   // At the point the id switches from non-@ to @ prefix.
   val id = "@$entryKey"

   // put entry in updateIdMap so that change is synced to server.
   val globalId = localToGlobalIdMapping[id]
   if (globalId == null) {
      CcuLog.e("CCU_HS", "Migrate to Str fail; global id not found for $id")
   } else {
      updateIdMap[id] = globalId
   }
}

// extension function which transforms an HDict to an HDictBuilder.
private fun HDict.mutate() = HDictBuilder().add(this)
// extension function which inserts a new or updated value into an HDictBuilder, then converts back to HDict.
private fun HDictBuilder.swap(name: String, value: String) = add(name, value).toDict()

// swap the given key to a new name and swap its type to HBool (from String)
private fun HDictBuilder.swapKeyChangeValueToBool(name: String, newName: String): HDict {

   // expect this to be "true" or "false"
   val valTrue = get(name).toString() == "true"
   remove(name)
   add(newName, HBool.make( valTrue ))
   return this.toDict()
}

