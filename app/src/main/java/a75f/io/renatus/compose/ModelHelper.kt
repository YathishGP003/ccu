package a75f.io.renatus.compose

import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.modbusbox.ModbusParser
import android.content.Context
import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonParseException
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Created by Manjunath K on 14-07-2023.
 */

fun getModelListFromJson(response: String): List<ModelMetaData> {
    val modelList = mutableListOf<ModelMetaData>()
    try {
        val data = JSONArray(response)
        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)
            modelList.add(
                ModelMetaData(
                    item.getString(ID),
                    item.getString(NAME),
                    item.getString(DESCRIPTION),
                    arrayToList(item.getJSONArray(TAG_NAMES))
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


fun testModel(context: Context): EquipmentDevice? {

    try {
        var data = readFileFromAssets(context,"downloaded.json")
        var device = ModbusParser().parseModbusDataFromString(data)
        if(device != null){
            Log.i("domain", "text: null")
            return device
        }
    }catch (e: Exception){
        e.printStackTrace()
    }
    return null
}

fun readFileFromAssets(context: Context, fileName: String): String {
    try {
        val inputStream = context.assets.open(fileName)
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        return String(buffer)
    } catch (e: IOException) {
        e.printStackTrace()
        return ""
    }
}