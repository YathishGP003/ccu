package a75f.io.api.haystack.util

import a75f.io.api.haystack.CCUTagsDb
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mockito.*
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import java.util.concurrent.ConcurrentHashMap

/**
 * This Kotlin unit test uses Mockito for mocking and google's Truth for assertions.
 * The normal preference is to use Mockk (i.e "mock for kotlin") -- however that can have
 * issues mocking complex Java classes such as CCUTagsDB.  In this case, it had trouble with the
 * ConcurrentHashMaps that needed mocking.  So I just fell back to Mockito.
 *
 * It's a single test that does a lot, but there is nothing fancy in this unit test -- just
 * comparing data before and after a method call.
 *
 * @author tcase@75f.io
 * Created on 2/16/21.
 */
class MigrateToStrTest {

   @Test
   fun `migrate db from haystack to silo`() {

      // The data comes in as tagsMap, updateIdMap & idMap in the CCUTagsDb object
      val tagsMapSample = initialTagsMap()
      val idMapSample = initialIdMap()
      val updateIdMapSample = emptyIdsForUpdateMap()

      val mockCCUTagsDb = mock(CCUTagsDb::class.java)
      `when`(mockCCUTagsDb.getTagsMap()).thenReturn(tagsMapSample)
      `when`(mockCCUTagsDb.getIdMap()).thenReturn(idMapSample)
      `when`(mockCCUTagsDb.getUpdateIdMap()).thenReturn(updateIdMapSample)

      // run the migrate function.
      migrateTagsDb(mockCCUTagsDb)

      // check that the data -- each map above -- has changed as exp

      // data points have been properly migrated
      assertThat(tagsMapSample).isEqualTo(expectedTagsMap())

      // luid to guid map has not changed.
      assertThat(idMapSample).isEqualTo(initialIdMap())

      // id's that have changed are put into updateIdMap in proper format
      assertThat(updateIdMapSample).isEqualTo(expectedIdsForUpdatedMap())

      // Also, check that .saveTags() called on mock.
      verify(mockCCUTagsDb, atLeastOnce()).saveTags()
   }

   private fun initialTagsMap(): ConcurrentHashMap<String, HDict> {

      // build map from our entities test set
      val tagsMap = ConcurrentHashMap<String, HDict>()
      localIds.forEachIndexed { index, id -> tagsMap[id] = entitiesBeforeMigration[index].hDict }
      return tagsMap;
   }

   private fun expectedTagsMap(): ConcurrentHashMap<String, HDict> {

      // build map of entities as we expect in the result
      val tagsMap = ConcurrentHashMap<String, HDict>()
      localIds.forEachIndexed { index, id -> tagsMap[id] = entitiesAfterMigration[index].hDict }
      return tagsMap;
   }

   // our map of luid to guid
   private fun initialIdMap(): ConcurrentHashMap<String, String> {

      val map = mutableMapOf<String, String>()
      localIds
         .forEachIndexed { index, key -> map.put("@$key", globalIds[index]) }
      return ConcurrentHashMap(map)
   }

   // ids to update starts out empty
   private fun emptyIdsForUpdateMap() = ConcurrentHashMap<String, String>()

   // update map after migration.
   private fun expectedIdsForUpdatedMap(): ConcurrentHashMap<String, String> {

      return ConcurrentHashMap(expectedUpdateIdMap)
   }
}

// TEST DATA


private val localIds = listOf<String>( "l0", "l1", "l2", "l3", "l4", "l5", "l6", "l7", "l8", "l9" )
private val globalIds = listOf<String>( "@g0", "@g1", "@g2", "@g3", "@g4", "@g5", "@g6", "@g7", "@g8", "@g9" )


// Test entities.  Built using the defaults in the helper Entity data class, below.
val basicNumEnt = Entity(id = localIds[0])

// Entity with "his" and "string".  Expect only "string" to change.
// We have not *yet* put in a general fix for this.  So, expect no change to this.
val hisStringEnt = Entity(
   id = localIds[1],
   kind = "string",
   isHis = true,
   dis = "Tony215f-DiagEquip-someStatus",
   additionalNumKeyValues = null )

// We want to change "string" to "Str"  (currently, same as above)
val validstringEnt = Entity(
   id = localIds[2],
   kind = "string",
   isHis = false,
   dis = "Tony215f-DiagEquip-someString",
   additionalNumKeyValues = null )

// Same
val validStringEnt = Entity(
   id = localIds[3],
   kind = "String",
   isHis = false,
   dis = "Tony215f-DiagEquip-someStringUppercase",
   additionalNumKeyValues = null )

// Entity with "enabled" marker tag.  Should not change.
val enabledTagEnt = Entity(
   id = localIds[4],
   isHis = false,
   tags = listOf("enabled", "sp", "setting"),
   dis = "Tony215f-Equip-enabledTag",
   additionalNumKeyValues = null )

// Entity with "enabled" Bool tag.  Should change to "portEnabled"
val enabledBoolEnt = Entity(
   id = localIds[5],
   tags = listOf("sp", "setting"),
   additionalBoolKeyValues = mapOf("enabled" to false),
   dis = "Tony215f-Equip-enabledBool",
   additionalNumKeyValues = null )

// App Version point (remove his)
val appVersionEnt = Entity(id = localIds[6],
   isHis = true, kind = "string",
   tags = listOf("version", "app", "writable", "diag"),
   dis = "Tony215f-Equip-enabledTag",
   additionalNumKeyValues = null )

// scheduleStatus in DualDuct (remove his)
val hisStringScheduleStatusEnt = Entity(
   id = localIds[7],
   kind = "string",
   isHis = true,
   tags = listOf("dualDuct", "scheduleStatus"),
   dis = "Tony215f-DualDuct-scheduleStatus",
   additionalNumKeyValues = null )

// Any other point in DualDuct of kind:string (change to kind:Number)
val dualDuctHisStringEnt = Entity(
   id = localIds[8],
   kind = "string",
   isHis = true,
   tags = listOf("dualDuct"),
   dis = "Tony215f-DualDuct-miscNum",
   additionalNumKeyValues = null )

// Other DualDuct points (no change)
val normalDualDuctEnt = Entity(
   id = localIds[9],
   kind = "Number",
   isHis = true,
   tags = listOf("dualDuct"),
   dis = "Tony215f-DualDuct-miscNumGood",
   additionalNumKeyValues = null )


// Expect indexes 1, 3, 5, 6, 7 & 8 to be added.
val expectedUpdateIdMap =
   mapOf("@l1" to "@g1", "@l2" to "@g2", "@l3" to "@g3", "@l5" to "@g5", "@l6" to "@g6", "@l7" to "@g7", "@l8" to "@g8")

// Resulting entities
val hisStringEntMigrated = hisStringEnt.copy(kind = "Str")
val validstringEntMigrated = validstringEnt.copy(kind = "Str")
val validStringEntMigrated = validStringEnt.copy(kind = "Str")
val enabledBoolEntMigrated = enabledBoolEnt.copy(additionalBoolKeyValues = mapOf("portEnabled" to false))
val appVersionEntMigrated = appVersionEnt.copy(kind = "Str", isHis = false)
val hisStringScheduleStatusEntMigrated =  hisStringScheduleStatusEnt.copy(kind = "Str", isHis = false)
val dualDuctHisStringEntMigrated = dualDuctHisStringEnt.copy(kind = "Number")

val entitiesBeforeMigration = listOf(
   basicNumEnt,
   hisStringEnt,
   validstringEnt,
   validStringEnt,
   enabledTagEnt,
   enabledBoolEnt,
   appVersionEnt,
   hisStringScheduleStatusEnt,
   dualDuctHisStringEnt,
   normalDualDuctEnt
)

val entitiesAfterMigration = listOf(
   basicNumEnt,                  // No migration
   hisStringEntMigrated,
   validstringEntMigrated,
   validStringEntMigrated,
   enabledTagEnt,                // No migration
   enabledBoolEntMigrated,
   appVersionEntMigrated,
   hisStringScheduleStatusEntMigrated,
   dualDuctHisStringEntMigrated,
   normalDualDuctEnt             // No migration
)


data class Entity(
   val id: String,
   val siteRef: String = "@site-ref",
   val floorRef: String? = "@floor-ref",
   val roomRef: String? = "@room-ref",
   val equipRef: String? = "@equip-ref",
   val kind: String = "Number",
   val isPoint: Boolean = true,
   val isHis: Boolean = true,
   val tags: List<String> = listOf("igain", "tuner", "equipProfile", "writable"),
   val dis: String = "Tony215f-DualDuct-1000-integralKFactor",
   val additionalNumKeyValues: Map<String,Double>? = mapOf("maxVal" to 1.0, "minVal" to 0.1, "incrementVal" to 0.1),
   val additionalBoolKeyValues: Map<String, Boolean>? = null
) {

   val hDict: HDict
     get() {
        val builder = HDictBuilder()
           .add("id", "@$id")
           .add("siteRef", siteRef)
           .add("kind", kind)
           .add("dis", dis)

        with (builder) {
           floorRef?.let { add("floorRef", it) }
           roomRef?.let { add("roomRef", it) }
           equipRef?.let { add("equipRef", it) }
           additionalNumKeyValues?.let {
              it.forEach { pair -> add(pair.key, pair.value) }
           }
           additionalBoolKeyValues?.let {
              it.forEach { pair -> add(pair.key, pair.value) }
           }
           if (isPoint) add("point")
           if (isHis) add("his")
           tags.forEach { add(it) }
        }
        return builder.toDict()
     }
}
