package a75f.io.renatus.compose

import com.google.gson.JsonParseException
import org.json.JSONArray

/**
 * Created by Manjunath K on 14-07-2023.
 */

fun getModelListFromJson(response: String): List<ModelMetaData> {
    val modelList = mutableListOf<ModelMetaData>()
    try {
        val data = JSONArray(response)
        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)
            val versionObj = item.getJSONObject(VERSION)
            modelList.add(
                ModelMetaData(
                    item.getString(ID),
                    item.getString(NAME),
                    item.getString(DESCRIPTION),
                    arrayToList(item.getJSONArray(TAG_NAMES)),
                    "V${versionObj.getString(MAJOR)}.${versionObj.getString(MINOR)}.${versionObj.getString(PATCH)}"
                )
            )
        }
    } catch (e: JsonParseException) {
        e.printStackTrace()
    }
    return modelList
}

fun arrayToList(arr: JSONArray): List<String> {
    val tagsList = mutableListOf<String>()
    for (i in 0 until arr.length()) {
        tagsList.add(arr.getString(i))
    }
    return tagsList
}