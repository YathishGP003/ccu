package a75f.io.logic.bo.building.mystat.profiles.util

import a75f.io.logic.Globals
import android.content.Context
import android.content.SharedPreferences

/**
 * Created by Manjunath K on 20-01-2025.
 */


class MyStatFanModeCacheStorage {

    private var sharedPreferences: SharedPreferences =
        Globals.getInstance().applicationContext.getSharedPreferences(
            "MyStatFanMode",
            Context.MODE_PRIVATE
        )

    fun saveFanModeInCache(equipId: String, value: Int) {
        sharedPreferences.edit().putInt(equipId, value).apply()
    }

    fun getFanModeFromCache(equipId: String): Int {
        return sharedPreferences.getInt(equipId, 0)
    }

    fun removeFanModeFromCache(equipId: String) {
        sharedPreferences.edit().remove(equipId).apply()
    }

}