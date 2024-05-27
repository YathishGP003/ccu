package a75f.io.logic.bo.building.hyperstatsplit.common

import a75f.io.logic.Globals
import android.content.Context
import android.content.SharedPreferences

/**
 * Created for HyperStat by Manjunath K on 13-08-2021.
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */
class FanModeCacheStorage {

      private var sharedPreferences: SharedPreferences = Globals.getInstance()
          .applicationContext.getSharedPreferences("HyperstatSplitFanMode",Context.MODE_PRIVATE)

    fun saveFanModeInCache(equipId: String, value: Int){
        sharedPreferences.edit().putInt(equipId,value).apply()
    }

    fun getFanModeFromCache(equipId: String): Int{ return sharedPreferences.getInt(equipId,0) }

    fun removeFanModeFromCache(equipId: String){  sharedPreferences.edit().remove(equipId).apply() }

}

