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
        return sharedPreferences.getInt(BACKFILL_TIME_SP_SELECTED, BACKFILL_DEFAULT_TIME_SP)
    }

    enum class BackFillDuration(val displayName: String) {
        NONE("None"),
        ONE_HOUR("1 Hr"),
        TWO_HOURS("2 Hrs"),
        THREE_HOURS("3 Hrs"),
        SIX_HOURS("6 Hrs"),
        TWELVE_HOURS("12 Hrs"),
        TWENTY_FOUR_HOURS("24 Hrs"),
        FORTY_EIGHT_HOURS("48 Hrs"),
        SEVENTY_TWO_HOURS("72 Hrs");

        companion object {
            val displayNames: Array<String?>
                get() {
                    val values: Array<BackFillDuration> =
                        values()
                    val displayNames = arrayOfNulls<String>(values.size)
                    for (i in values.indices) {
                        displayNames[i] = values[i].displayName
                    }
                    return displayNames
                }

            fun toIntArray(): IntArray {
                val intValues =
                    IntArray(values().size)
                for (i in values().indices) {
                    val stringValue: String =
                        values()[i].displayName
                    if (stringValue == "None") {
                        intValues[i] = 0
                    } else {
                        intValues[i] = stringValue.split(" ").toTypedArray()[0].toInt()
                    }
                }
                return intValues
            }

            fun getIndex(array: IntArray, selectedValue: Int, defaultVal: Int): Int {
                for (i in array.indices) {
                    if (array[i] == selectedValue) {
                        return i
                    }
                }
                return defaultVal
            }
        }
    }

    companion object {
        const val BACKFILL_CACHE = "backfill_cache"
        const val BACKFILL_TIME_DURATION = "backFillTimeDuration"
        const val BACKFILL_TIME_SP_SELECTED = "backFillTimeSpSelected"
        const val BACKFILL_DEFAULT_TIME = 24
        const val BACKFILL_DEFAULT_TIME_SP = 6
    }

}