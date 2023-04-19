package a75f.io.domain

import a75f.io.domain.modeldef.*
import androidx.annotation.Nullable
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Test
import java.io.File

class ModelTest {
    @Test
    fun modelParseTest() {
        @Nullable val modelData: String? = ResourceHelper.loadString("hyperstat2pfcu.json")
        modelData?.let {  File("parsedData.json").writeText(it)}

        val moshiInstance = Moshi.Builder()
                        .add(PolymorphicJsonAdapterFactory.of(Constraint::class.java, Constraint::constraintType.name)
                                .withSubtype(NoConstraint::class.java, Constraint.ConstraintType.NONE.name)
                                .withSubtype(NumericConstraint::class.java, Constraint.ConstraintType.NUMERIC.name)
                                .withSubtype(MultiStateConstraint::class.java, Constraint.ConstraintType.MULTI_STATE.name))
                        .add(TagType.Adapter())
                        .add(KotlinJsonAdapterFactory())
                        .build()
        val jsonAdapter: JsonAdapter<ModelDefDto> = moshiInstance.adapter(ModelDefDto::class.java)

        val dmModel = modelData?.let {jsonAdapter.fromJson(modelData)}
        dmModel?.points?.forEach { println(it.toPointDef()) }
        //println(dmModel?.points?.size)
        //println(dmModel)
        dmModel?.let {
            val equipBuilder = DMEquipBuilder()
            equipBuilder.buildEquipAndPoints(getMyProfileConfig(), dmModel)
        }


    }

    private fun getMyProfileConfig() : ProfileConfiguration {
        val profile = HyperStat2fcuProfileConfiguration(1000,"HS",0)


        profile.autoForcedOccupied.enabled = true
        profile.autoAway.enabled = true

        return profile
    }
}