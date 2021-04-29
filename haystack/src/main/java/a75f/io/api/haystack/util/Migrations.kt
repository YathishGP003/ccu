@file:JvmName("Migrations")

package a75f.io.api.haystack.util

import a75f.io.api.haystack.CCUTagsDb
import a75f.io.api.haystack.HisItem
import a75f.io.logger.CcuLog
import com.google.gson.Gson
import io.objectbox.Box
import org.projecthaystack.*
import java.lang.IndexOutOfBoundsException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap

/**
 * This function modifies the contents of the CCUTabsDb object in place, replacing all
 * LUID's with GUIDs.
 */
fun migrateGuidsToLuids(dbStrings: DbStrings, gson: Gson): DbStrings {

   CcuLog.d("CCU_HS", "before migration")
   dbStrings.printData("before")

   val idMapSB = StringBuilder(dbStrings.idMapStr)
   val updateIdMapSB = StringBuilder(dbStrings.updateIdMapStr)
   val removeIdMapSB = StringBuilder(dbStrings.removeIdMapStr)
   val writeArrayaSB = StringBuilder(dbStrings.waStr)
   val tagsSB = StringBuilder(dbStrings.tagsStr)

   // construct an idMap here to make it convenient to get guids and luids.
   val idMap = gson.fromJson<ConcurrentHashMap<String, String>>(dbStrings.idMapStr, ConcurrentHashMap::class.java)

   idMap
      .forEach { (luidAt, guidAt) ->
         val luid = luidAt.dropAtSign()
         val guid = guidAt.dropAtSign()
         idMapSB.replaceAll(luid, guid)
         updateIdMapSB.replaceAll(luid, guid)
         removeIdMapSB.replaceAll(luid, guid)
         writeArrayaSB.replaceAll(luid, guid)
         tagsSB.replaceAll(luid, guid)
      }

   val result = DbStrings(
      idMapStr = idMapSB.toString(),
      removeIdMapStr = removeIdMapSB.toString(),
      updateIdMapStr = updateIdMapSB.toString(),
      tagsStr = tagsSB.toString(),
      waStr = writeArrayaSB.toString(),
      migrationMap = idMap
   )

   result.printData("after")

   return result
}

fun ConcurrentHashMap<String, String>.replaceKeyIfPresent(oldKey: String, newKey: String) {
   get(oldKey)?.let {
      put(newKey, it)
      remove(oldKey)
   }
}


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

   CcuLog.i("CCU_HS", "tags map migration")
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
      .filter { it.value.get("diag", false) != null
             && it.value.get("app", false) != null
             && it.value.get("version", false) != null
             && it.value.get("his", false) != null }
      .forEach {
         val hBuilder = it.value.mutate()
         hBuilder.remove("his")
         tagsMap[it.key] = hBuilder.toDict()
         addIdToUpdateMap(it.key, updateIdMap, localToGlobalIdMapping)
      }

   // This time remove his tag from scheduleStatus tag in dual duct
   tagsMap.entries
      .filter { it.value.get("scheduleStatus", false) != null
         && it.value.get("dualDuct", false) != null
         && it.value.get("his", false) != null }
      .forEach {
         val hBuilder = it.value.mutate()
         hBuilder.remove("his")
         tagsMap[it.key] = hBuilder.toDict()
         addIdToUpdateMap(it.key, updateIdMap, localToGlobalIdMapping)
      }

   // replace kind:Str from all other his tags in dualDuct with kind:Number
   tagsMap.entries
      .filter { it.value.get("dualDuct", false) != null
         && it.value.get("his", false) != null
         && (it.value.get("kind", false)?.toString()?.equals("Str") ?: false)
      }
      .forEach {
         tagsMap[it.key] = it.value.mutate().swap("kind", "Number")
         addIdToUpdateMap(it.key, updateIdMap, localToGlobalIdMapping)
      }

   CcuLog.i("CCU_HS", "tags map migration complete")
   printTagsMap(tagsMap)

   // save changes locally
   tagsDb.saveTags()
}

// Left in case its handy for debugging.  Remove whenever
private fun printTagsMap(tagsMap: ConcurrentHashMap<String, HDict>) {
   tagsMap.toMap()
      .forEach { entry ->
         CcuLog.i("CCU_HS", "${entry.key}: ${entry.value.toString()}") }
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

fun String.dropAtSign() = dropWhile { it == '@' }

// extension function which transforms an HDict to an HDictBuilder.
private fun HDict.mutate() = HDictBuilder().add(this)
// extension function which inserts a new or updated value into an HDictBuilder, then converts back to HDict.
private fun HDictBuilder.swap(name: String, value: String) = add(name, value).toDict()

// swap the given key to a new name and swap its type to HBool (from String)
private fun HDictBuilder.swapKeyChangeValueToBool(name: String, newName: String): HDict {

   // expect this to be "true" or "false"
   val valTrue = get(name).toString() == "true"
   remove(name)
   add(newName, HBool.make(valTrue))
   return this.toDict()
}

fun StringBuilder.replaceAll(oldStr: String, newStr: String): StringBuilder {

      var occurrenceIndex: Int = indexOf(oldStr, 0)
      // FAST PATH: no match
      if (occurrenceIndex < 0) return this

      val oldStrLength = oldStr.length
      val searchStep = oldStrLength.coerceAtLeast(1)

      do {
         try {
            replace(occurrenceIndex, occurrenceIndex+oldStrLength, newStr)
            if (occurrenceIndex >= length) break
            occurrenceIndex = indexOf(oldStr, occurrenceIndex + searchStep)
         } catch (ex: IndexOutOfBoundsException) {
            println("IndexOutOfBoundsException: ${ex.localizedMessage}")
            println("occurrenceIndex = $occurrenceIndex, oldStrLength = $oldStrLength, newStr = $newStr")
            println("this string: ${this.toString()}")
            throw ex
         }
      }
      while (occurrenceIndex > 0)
   return this
}

data class DbStrings (
   val idMapStr: String,
   val removeIdMapStr: String,
   val updateIdMapStr: String,
   val tagsStr: String,
   val waStr: String,
   val migrationMap: Map<String, String>? = null
) {
   fun printData(msg: String = "") {
      CcuLog.d("CCU_HS", "$msg  id map $idMapStr.")
      CcuLog.d("CCU_HS", "$msg  tags map $tagsStr")
      CcuLog.d("CCU_HS", "$msg  remove Ids $removeIdMapStr")
      CcuLog.d("CCU_HS", "$msg  update Ids $updateIdMapStr")
      CcuLog.d("CCU_HS", "$msg  write arrays map $waStr")
   }
}

fun migrateHisData(ccuTagsDb: CCUTagsDb, idMap: Map<String, String>?) {

   CcuLog.d("CCU", "migrate his data");

   idMap?.forEach { (luid, guid) ->
      val href = HRef.make(luid.dropAtSign())

      // Get all the items for update.  (Ideally, sync & prune will have occurred to keep this DB from being in a large state.)
      val hisItems: List<HisItem> = ccuTagsDb.getHisItemsForMigration(href, 0, 99)
      CcuLog.d("CCU", "got ${hisItems.size} hisItems for $href" + (if (hisItems.size > 0) "including ${hisItems[0]}" else ""))

      // for all items, switch the rec field from old (luid to new (guild)
      hisItems.forEach { it.rec = guid }

      // update them
      ccuTagsDb.updateHisItems(hisItems)
   }
}

