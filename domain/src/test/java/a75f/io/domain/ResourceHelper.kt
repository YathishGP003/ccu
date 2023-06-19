package a75f.io.domain

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import io.seventyfivef.domainmodeler.client.ModelDirectiveFactory
import io.seventyfivef.domainmodeler.client.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.configuration.ObjectMapperConfig
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

    fun loadProfileModelDefinition(fileName : String) : SeventyFiveFProfileDirective {
        @Nullable val modelData: String? = loadString(fileName)

        /*val moshiInstance = Moshi.Builder()
            .add(NullToEmptyStringAdapter)
            .add(
                PolymorphicJsonAdapterFactory.of(Constraint::class.java, Constraint::constraintType.name)
                    .withSubtype(NoConstraint::class.java, Constraint.ConstraintType.NONE.name)
                    .withSubtype(NumericConstraint::class.java, Constraint.ConstraintType.NUMERIC.name)
                    .withSubtype(MultiStateConstraint::class.java, Constraint.ConstraintType.MULTI_STATE.name))
            .add(
                PolymorphicJsonAdapterFactory.of(PointConfiguration::class.java, PointConfiguration::configType.name)
                    .withSubtype(BaseConfiguration::class.java, PointConfiguration.ConfigType.BASE.name)
                    .withSubtype(AssociatedConfiguration::class.java, PointConfiguration.ConfigType.ASSOCIATED.name)
                    .withSubtype(AssociationConfiguration::class.java, PointConfiguration.ConfigType.ASSOCIATION.name)
                    .withSubtype(DependentConfiguration::class.java, PointConfiguration.ConfigType.DEPENDENT.name)
                    .withSubtype(DynamicSensorConfiguration::class.java, PointConfiguration.ConfigType.DYNAMIC_SENSOR.name))
            .add(KotlinJsonAdapterFactory())
            .build()
        val jsonAdapter: JsonAdapter<SeventyFiveFProfileDirective> = moshiInstance.adapter(SeventyFiveFProfileDirective::class.java)

        return modelData?.let {jsonAdapter.fromJson(modelData)}!!*/

        val objectMapper = ObjectMapperConfig().objectMapper()
        val modelDirectiveFactory = ModelDirectiveFactory(objectMapper)
        return modelDirectiveFactory.fromJson(modelData!!) as SeventyFiveFProfileDirective
    }
}