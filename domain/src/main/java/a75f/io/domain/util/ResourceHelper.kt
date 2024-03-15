package a75f.io.domain.util

import a75f.io.logger.CcuLog
import android.content.Context
import androidx.annotation.Nullable
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.ModelDirectiveFactory
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.common.ConstraintDeserializer
import io.seventyfivef.domainmodeler.common.ModelDirectiveDeserializer
import io.seventyfivef.domainmodeler.common.PointConfigurationDeserializer
import io.seventyfivef.domainmodeler.common.point.Constraint
import io.seventyfivef.domainmodeler.common.point.PointConfiguration
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

object ResourceHelper {
    @Nullable
    fun loadString(name: String?): String? {
        return loadString(ResourceHelper::class.java.classLoader, name)
    }

    @Nullable
    fun loadString(@Nullable loader: ClassLoader?, name: String?): String? {
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
        //val objectMapper = ObjectMapperConfig().objectMapper()
        val modelDirectiveFactory = ModelDirectiveFactory(getObjectMapper())
        return modelDirectiveFactory.fromJson(modelData!!) as SeventyFiveFProfileDirective
    }

    fun loadDeviceModelDefinition(fileName: String) : SeventyFiveFDeviceDirective {
        @Nullable val modelData: String? = loadString(fileName)

        val modelDirectiveFactory = ModelDirectiveFactory(getObjectMapper())
        return modelDirectiveFactory.fromJson(modelData!!) as SeventyFiveFDeviceDirective
    }

    fun getModelVersion(fileName: String): JSONObject {
        @Nullable val version: String? = loadString(fileName)
        if (version != null){
            return JSONObject(version)
        }
        return JSONObject()
    }

    fun loadModelDefinition(fileName : String): ModelDirective? {
        @Nullable val modelData: String? = loadString(fileName)
        if (modelData.isNullOrEmpty())
            return null
        //val objectMapper = ObjectMapperConfig().objectMapper()
        val modelDirectiveFactory = ModelDirectiveFactory(getObjectMapper())
        return modelDirectiveFactory.fromJson(modelData)
    }

    fun loadModel(fileName : String) : ModelDirective {
        CcuLog.i("CCU_DM", "loadModel $fileName")
        @Nullable val modelData: String? = loadString(fileName)
        //val objectMapper = ObjectMapperConfig().objectMapper()
        CcuLog.i("CCU_DM", "loadModel data $modelData")
        val modelDirectiveFactory = ModelDirectiveFactory(getObjectMapper())
        return modelDirectiveFactory.fromJson(modelData!!)
    }
    fun loadModel(fileName : String, context : Context) : ModelDirective {
        CcuLog.i("CCU_DM", "loadModel $fileName $context")
        val inputStream = context.assets.open(fileName)
        CcuLog.i("CCU_DM", "loadModel stream $inputStream")
        @Nullable val modelData: String? = loadString(inputStream)
        CcuLog.printLongMessage("CCU_DM", "loadModel modelString $modelData")
        val model = JSONObject(modelData)
        val versionData = model.getJSONObject("version")
        CcuLog.printLongMessage("CCU_DM", "Model Version $versionData")
        val modelDirectiveFactory = ModelDirectiveFactory(getObjectMapper())
        return modelDirectiveFactory.fromJson(modelData!!)
    }

    fun getObjectMapper() : ObjectMapper {
        return jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModules(
                SimpleModule()
                    .addDeserializer(ModelDirective::class.java, ModelDirectiveDeserializer())
                    .addDeserializer(Constraint::class.java, ConstraintDeserializer())
                    .addDeserializer(PointConfiguration::class.java, PointConfigurationDeserializer())
            )
    }
}