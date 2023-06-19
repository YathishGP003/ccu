package a75f.io.domain

import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.EntityMapper
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.SeventyFiveFProfileDirective
import org.junit.Before
import org.junit.Test

class EntityMapperTest {

    private lateinit var dmModel: ModelDirective

    @Before
    fun setUp() {
        dmModel = ResourceHelper.loadProfileModelDefinition("EntityMapper_TestModel.json")

    }

    @Test
    fun modelParseTest() {

        dmModel?.let {
            val entityMapper = EntityMapper(dmModel as SeventyFiveFProfileDirective)
            println("Base points")
            val basePoints = entityMapper.getBasePoints()
            basePoints.forEach { println(it) }
            assert(basePoints.size == 2)

            println("Associated points")
            val associatedPoints = entityMapper.getAssociatedPoints()
            associatedPoints.forEach { println(it) }
            assert(associatedPoints.size == 2)

            println("Association points")
            val associationPoints = entityMapper.getAssociationPoints()
            associationPoints.forEach { println(it) }
            assert(associationPoints.size == 1)

            println("Dependent points")
            val dependentPoints = entityMapper.getDependentPoints()
            dependentPoints.forEach { println(it) }
            assert(dependentPoints.size == 1)

            println("Dynamic Sensor points")
            val sensorPoints = entityMapper.getDynamicSensorPoints()
            sensorPoints.forEach { println(it) }
            assert(sensorPoints.size == 1)

            val enabledAssociations = entityMapper.getEnabledAssociations(getTestProfileConfig())
            println("Enabled Associations")
            enabledAssociations.forEach { println(it) }
            assert(enabledAssociations.size == 1)

            val enabledDependencies = entityMapper.getEnabledDependencies(getTestProfileConfig())
            println("Enabled Dependencies")
            enabledDependencies.forEach { println(it) }
            assert(enabledDependencies.size == 1)
        }

    }

    private fun getTestProfileConfig() : ProfileConfiguration {
        val profile = HyperStat2pfcuTestConfiguration(1000,"HS",0, "","")

        profile.autoForcedOccupied.enabled = true
        profile.autoAway.enabled = true

        return profile
    }
}