package a75f.io.logic.preconfig

import a75f.io.logger.CcuLog
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

object PreconfigurationManager {

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    }

    fun transitionTo(state: PreconfigurationState) {
        if (::prefs.isInitialized.not()) {
            throw IllegalStateException("PreconfigurationManager is not initialized. Call init(context) first.")
        }
        val editor = prefs.edit()
        editor.putString("preconfiguration_state", state.toStringValue())
        editor.apply()
        onStateChange(state)
    }

    fun getState(): PreconfigurationState {
        if (::prefs.isInitialized.not()) {
            throw IllegalStateException("PreconfigurationManager is not initialized. Call init(context) first.")
        }
        val stateString = prefs.getString("preconfiguration_state", PreconfigurationState.NotConfigured.toStringValue())
        return PreconfigurationState.fromString(stateString ?: PreconfigurationState.NotConfigured.toStringValue())
    }

    private fun onStateChange(state: PreconfigurationState) {
        CcuLog.i("PreconfigurationManager", "State changed to: $state")
    }
}