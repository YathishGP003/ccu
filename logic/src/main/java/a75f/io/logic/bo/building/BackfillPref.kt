package a75f.io.logic.bo.building

import a75f.io.logic.Globals
import android.content.Context
import android.content.SharedPreferences

class BackfillPref {

    private var sharedPreferences: SharedPreferences =
        Globals.getInstance().applicationContext.getSharedPreferences(
            BACKFILL_CACHE,
            Context.MODE_PRIVATE
        )

    fun saveBackfillConfig (
        backfillTimeDuration : Int,
        backfillTImeSpSelected : Int
    ) {
        sharedPreferences.edit().putInt(BACKFILL_TIME_DURATION, backfillTimeDuration).apply()
        sharedPreferences.edit().putInt(BACKFILL_TIME_SP_SELECTED, backfillTImeSpSelected).apply()
    }

    fun getBackFillTimeDuration () : Int {
        return sharedPreferences.getInt(BACKFILL_TIME_DURATION, BACKFILL_DEFAULT_TIME)
    }

    fun getBackFillTimeSPSelected () : Int {
        return sharedPreferences.getInt(BACKFILL_TIME_SP_SELECTED, BACKFILL_SP_DEFAULT_SELECTION)
    }

    companion object {
        const val BACKFILL_CACHE = "backfill_cache"
        const val BACKFILL_TIME_DURATION = "backFillTimeDuration"
        const val BACKFILL_TIME_SP_SELECTED = "backFillTimeSpSelected"
        const val BACKFILL_DEFAULT_TIME = 24
        const val BACKFILL_SP_DEFAULT_SELECTION = 6
    }
}