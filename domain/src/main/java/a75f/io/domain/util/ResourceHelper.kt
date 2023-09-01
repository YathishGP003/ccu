package a75f.io.domain.util

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.google.gson.JsonParseException
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.ModelDirectiveFactory
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.configuration.ObjectMapperConfig
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
        val objectMapper = ObjectMapperConfig().objectMapper()
        val modelDirectiveFactory = ModelDirectiveFactory(objectMapper)
        return modelDirectiveFactory.fromJson(modelData)
    }
}