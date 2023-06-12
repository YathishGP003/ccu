package a75f.io.domain

import a75f.io.domain.model.ModelDef
import a75f.io.domain.model.common.point.AssociatedConfiguration
import a75f.io.domain.model.common.point.AssociationConfiguration
import a75f.io.domain.model.common.point.BaseConfiguration
import a75f.io.domain.model.common.point.Constraint
import a75f.io.domain.model.common.point.DependentConfiguration
import a75f.io.domain.model.common.point.DynamicSensorConfiguration
import a75f.io.domain.model.common.point.MultiStateConstraint
import a75f.io.domain.model.common.point.NoConstraint
import a75f.io.domain.model.common.point.NumericConstraint
import a75f.io.domain.model.common.point.PointConfiguration
import android.view.Display.Mode
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

object ResourceHelper {
    @Nullable
    fun loadString(@NonNull name: String?): String? {
        return loadString(ResourceHelper::class.java.classLoader, name)
    }

    @Nullable
    fun loadString(@Nullable loader: ClassLoader?, @NonNull name: String?): String? {
        if (loader == null) {
            return null
        }
        try {
            loader.getResourceAsStream(name).use { inputStream -> return loadString(inputStream) }
        } catch (e: IOException) {
            return null
        } catch (e: NullPointerException) {
            return null
        }
    }

    @Nullable
    fun loadString(@Nullable inputStream: InputStream?): String? {
        if (inputStream == null) {
            return null
        }
        try {
            ByteArrayOutputStream().use { result ->
                val buffer = ByteArray(4096)
                var length: Int
                while (inputStream.read(buffer).also { length = it } > 0) {
                    result.write(buffer, 0, length)
                }
                return result.toString("UTF-8")
            }
        } catch (e: IOException) {
            return null
        }
    }

    fun loadModelDefinition(fileName : String) : ModelDef{
        @Nullable val modelData: String? = ResourceHelper.loadString(fileName)

        val moshiInstance = Moshi.Builder()
            .add(
                PolymorphicJsonAdapterFactory.of(Constraint::class.java, Constraint::constraintType.name)
                    .withSubtype(NoConstraint::class.java, Constraint.ConstraintType.NONE.name)
                    .withSubtype(NumericConstraint::class.java, Constraint.ConstraintType.NUMERIC.name)
                    .withSubtype(MultiStateConstraint::class.java, Constraint.ConstraintType.MULTI_STATE.name))
            .add(
                PolymorphicJsonAdapterFactory.of(PointConfiguration::class.java, PointConfiguration::configurationType.name)
                    .withSubtype(BaseConfiguration::class.java, PointConfiguration.ConfigType.BASE.name)
                    .withSubtype(AssociatedConfiguration::class.java, PointConfiguration.ConfigType.ASSOCIATED.name)
                    .withSubtype(AssociationConfiguration::class.java, PointConfiguration.ConfigType.ASSOCIATION.name)
                    .withSubtype(DependentConfiguration::class.java, PointConfiguration.ConfigType.DEPENDENT.name)
                    .withSubtype(DynamicSensorConfiguration::class.java, PointConfiguration.ConfigType.DYNAMIC_SENSOR.name))
            .add(KotlinJsonAdapterFactory())
            .build()
        val jsonAdapter: JsonAdapter<ModelDef> = moshiInstance.adapter(ModelDef::class.java)

        return modelData?.let {jsonAdapter.fromJson(modelData)}!!
    }
}