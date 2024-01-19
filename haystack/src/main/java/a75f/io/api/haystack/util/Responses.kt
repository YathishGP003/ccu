package a75f.io.api.haystack.util

import a75f.io.api.haystack.CCUHsApi

data class ReadAllResponse(val points : String?, val levelData: MutableList<LevelData>)

data class BaseResponse<T>(val data: T? = null, val error: String? = null)

data class LevelData(val pointId: String, val levelArray: ArrayList<String?>?)


fun retrieveLevelValues(id: String): MutableList<LevelData> {
    val arrayList = ArrayList<String?>()
    val listOfMap =  CCUHsApi.getInstance().readPoint(id)
    for(i in listOfMap){
        if(i["val"] == null){
            arrayList.add(null)
        }else{
            arrayList.add("val : ${i["val"].toString()}")
        }
    }
    val mutableList = mutableListOf<LevelData>()
    mutableList.add(LevelData(id, arrayList))
    return mutableList
}