package a75f.io.sanity.cases

import a75f.io.alerts.AlertManager
import a75f.io.alerts.CM_RESET
import a75f.io.api.haystack.CCUHsApi
import a75f.io.sanity.framework.SanityCase
import a75f.io.sanity.framework.SanityResultSeverity
import com.google.gson.Gson
import com.google.gson.JsonObject

class DeviceEntityCheck : SanityCase {
    var failure = false
    override fun getName(): String = "DeviceEntityCheck"

    override fun execute(): Boolean {
        // Following query should return only 1 CCU point and the ahuRef should not be null or empty
        // for the sanity test to pass.
        failure = false
        val ccuList = CCUHsApi.getInstance().readAllHDictByQuery("ccu")

        if (ccuList.size == 1 && ccuList[0].has("ahuRef") && ccuList[0].has("gatewayRef")) {
            failure = false
            return true
        } else {
            // No duplicates found
            failure = true
            return false
        }
    }

    override fun report(): String {
        if(failure) {
            return "Duplicate CCUs found in device entities."
        } else {
            return "No duplicate CCUs found in device entities."
        }
    }

    override fun correct(): Boolean {
        // Placeholder for correction logic
        return true
    }

    override fun getSeverity(): SanityResultSeverity {
        return SanityResultSeverity.HIGH
    }

    override fun getDescription(): String {
        if(failure) {
            return "Duplicate CCUs in device entities"
        } else {
            return "No duplicate CCUs found in device entities"
        }
    }

}