package a75f.io.domain

import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.EquipBuilder
import a75f.io.domain.model.ModelDef
import a75f.io.domain.model.common.point.*
import androidx.annotation.Nullable
import androidx.dynamicanimation.animation.DynamicAnimation
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Test

class ModelTest {
    @Test
    fun modelParseTest() {
        @Nullable val modelData: String? = ResourceHelper.loadString("testModel_05_30_2023.json")
        //modelData?.let {  File("parsedData.json").writeText(it)}

        val moshiInstance = Moshi.Builder()
                        .add(PolymorphicJsonAdapterFactory.of(Constraint::class.java, Constraint::constraintType.name)
                                .withSubtype(NoConstraint::class.java, Constraint.ConstraintType.NONE.name)
                                .withSubtype(NumericConstraint::class.java, Constraint.ConstraintType.NUMERIC.name)
                                .withSubtype(MultiStateConstraint::class.java, Constraint.ConstraintType.MULTI_STATE.name))
                        .add(PolymorphicJsonAdapterFactory.of(PointConfiguration::class.java, PointConfiguration::configurationType.name)
                            .withSubtype(BaseConfiguration::class.java, PointConfiguration.ConfigType.BASE.name)
                            .withSubtype(AssociatedConfiguration::class.java, PointConfiguration.ConfigType.ASSOCIATED.name)
                            .withSubtype(AssociationConfiguration::class.java, PointConfiguration.ConfigType.ASSOCIATION.name)
                            .withSubtype(DependentConfiguration::class.java, PointConfiguration.ConfigType.DEPENDENT.name)
                            .withSubtype(DynamicSensorConfiguration::class.java, PointConfiguration.ConfigType.DYNAMIC_SENSOR.name))
                        //.add(TagType.Adapter())
                        .add(KotlinJsonAdapterFactory())
                        .build()
        val jsonAdapter: JsonAdapter<ModelDef> = moshiInstance.adapter(ModelDef::class.java)

        val dmModel = modelData?.let {jsonAdapter.fromJson(modelData)}
        //dmModel?.points?.forEach { println(it.toPointDef()) }
        //println(dmModel?.points?.size)
        //println(dmModel)
        dmModel?.let {
            val entityMapper = EntityMapper(dmModel)
            println("Base points")
            val basePoints = entityMapper.getBasePoints()
            basePoints.forEach { println(it) }
            assert(basePoints.size == 2)

            println("Associated points")
            val associatedPoints = entityMapper.getAssociatedPoints()
            associatedPoints.forEach { println(it) }
            assert(associatedPoints.size == 1)

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
            enabledAssociations.forEach { println(it.domainName) }
            assert(enabledAssociations.size == 1)

            val enabledDependencies = entityMapper.getEnabledDependencies(getTestProfileConfig())
            println("Enabled Dependencies")
            enabledDependencies.forEach { println(it.domainName) }
            assert(enabledDependencies.size == 1)
        }


    }

    private fun getTestProfileConfig() : ProfileConfiguration {
        val profile = HyperStat2pfcuTestConfiguration(1000,"HS",0)

        profile.autoForcedOccupied.enabled = true
        profile.autoAway.enabled = true

        return profile
    }
}