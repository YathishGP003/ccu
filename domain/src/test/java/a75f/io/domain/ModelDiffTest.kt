package a75f.io.domain

import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.migration.DiffFinder
import a75f.io.domain.migration.DiffManger
import io.seventyfivef.domainmodeler.client.ModelDirective
import org.junit.Before
import org.junit.Test

/**
 * Created by Manjunath K on 16-06-2023.
 */

class ModelDiffTest {
    private lateinit var dmModel1: ModelDirective
    private lateinit var dmModel2: ModelDirective
    @Before
    fun setUp() {
        dmModel1 = ResourceHelper.loadProfileModelDefinition("DiffBuilder_TestModel.json")
        dmModel2 = ResourceHelper.loadProfileModelDefinition("newmodels/DiffBuilder_TestModelNew.json")

    }


    @Test
    fun validate() {
        val diffFinder = DiffFinder()
        val diff = diffFinder.calculateDiff(dmModel1, dmModel2)
        val entityConfiguration = EntityConfiguration()
        diffFinder.findEquipUpdate(dmModel1.domainName, diff, entityConfiguration)
        diffFinder.findPointUpdate(diff, entityConfiguration)


        println("\nTo be tobeAdded\n")
        entityConfiguration.tobeAdded.forEach {
            println(it.domainName)
        }
        println("\nTo be tobeUpdated\n")
        entityConfiguration.tobeUpdated.forEach {
            println(it.domainName)
        }

        println("\nTo be tobeDeleted\n")
        entityConfiguration.tobeDeleted.forEach {
            println(it.domainName)
        }
    }

    private fun getTestProfileConfig(node: Int): ProfileConfiguration {
        val profile = HyperStat2pfcuTestConfiguration(node, "HS", 0, "", "")
        profile.autoForcedOccupied.enabled = true
        profile.autoAway.enabled = true
        return profile
    }

    @Test
    fun getVersionDetailsTest(){
        val version = ResourceHelper.getModelVersion(DiffManger.NEW_VERSION)
        println(version.toString())
        version!!.keys().forEach {
            //println(it)

            print(version.getJSONObject(it as String).getInt("patch"))
            print(version.getJSONObject(it as String).getInt("major"))
            print(version.getJSONObject(it as String).getInt("minor"))
        }
        println()
    }
}