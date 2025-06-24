package a75f.io.logic.util


import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

object SequenceApplyPrefHandler {

    private const val KEY_HANDLING_MESSAGES = "handling_messages_map"
    private const val PREF_NAME = "a75f.io.renatus_preferences"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isMessageBeingHandled(context: Context, messageId: String, seqId: String): Boolean {
        val json = getPrefs(context).getString(KEY_HANDLING_MESSAGES, null) ?: return false
        val map = JSONObject(json)

        return map.optString(messageId) == seqId
    }

    fun markMessageAsHandling(context: Context, messageId: String, seqId: String) {
        val prefs = getPrefs(context)
        val json = prefs.getString(KEY_HANDLING_MESSAGES, null)
        val map = if (json != null) JSONObject(json) else JSONObject()

        map.put(messageId, seqId)

        prefs.edit().putString(KEY_HANDLING_MESSAGES, map.toString()).apply()
    }

    fun removeMessageFromHandling(context: Context, messageId: String) {
        val prefs = getPrefs(context)
        val json = prefs.getString(KEY_HANDLING_MESSAGES, null) ?: return
        val map = JSONObject(json)

        map.remove(messageId)

        prefs.edit().putString(KEY_HANDLING_MESSAGES, map.toString()).apply()
    }

    fun clearHandledMessages(context: Context) {
        getPrefs(context).edit().remove(KEY_HANDLING_MESSAGES).apply()
    }
}
