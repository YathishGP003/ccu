package a75f.io.api.haystack.util

import a75f.io.api.haystack.CCUHsApi

data class ReadAllResponse(val points : String?, val levelData: MutableList<LevelData>)

data class LevelData(val pointId: String, val levelArray: ArrayList<String?>?)

fun retrieveLevelValues(id: String): ArrayList<String?> {
    val arrayList = ArrayList<String?>()
    val listOfMap = CCUHsApi.getInstance().readPoint(id)
    listOfMap.forEachIndexed { index, hashMap ->
        run {
            if (hashMap["val"] != null) {
                arrayList.add("${index + 1}: ${hashMap["val"].toString()}")
            }
        }
    }
    return arrayList
}