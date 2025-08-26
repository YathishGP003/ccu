package a75f.io.sanity.cases

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.util.hayStack
import a75f.io.logger.CcuLog
import a75f.io.sanity.framework.SANITTY_TAG
import a75f.io.sanity.framework.SanityCase


class NamedScheduleValidation : SanityCase {
    var issueFound = false
    override fun getName(): String = "NamedScheduleValidation"

    override fun execute(): Boolean {
        // Placeholder for actual implementation
        // Step 1: Read all points with room query
        var roomList = CCUHsApi.getInstance().readAllHDictByQuery("room")
        // Step 2: Check if the point has a valid named schedule
        for (roomDict in roomList) {
            val scheduleRef = roomDict.get("scheduleRef").toString()
            // Check if the room has a scheduleRef
            val scheduleRefPt = CCUHsApi.getInstance().readMapById(scheduleRef)
            val scheduleType = CCUHsApi.getInstance().readPointPriorityValByQuery(
                "scheduleType and roomRef == \"${roomDict.get("id")}\""
            )
            // If the scheduleType is zone, then skip validation since it will be taken care of by ScheduleRefValidation
            if(scheduleType == null || scheduleType == ScheduleType.ZONE.ordinal.toDouble()) continue
            // Check if the room has a named schedule
            // Step 3: Check if the scheduleRef exists in the database and is valid
            if (scheduleRefPt.isEmpty()) {
                val remotePoint = hayStack.readRemotePoint("id == $scheduleRef")
                if(remotePoint == null) {
                    CcuLog.e(SANITTY_TAG, "Remote name schedule not found in backend")
                    issueFound = true
                    return false
                }
                // Step 4: If the scheduleRef is invalid, log an error and fetch a named schedule from back end
                CcuLog.e(SANITTY_TAG, "Invalid named schedule for room: ${roomDict.get("id")}, creating new named schedule")
                hayStack.tagsDb.addHDict(scheduleRef, remotePoint);
                issueFound = true
                return false
            }
        }

        issueFound = false
        return true
    }

    override fun report(): String {
        return "Named schedule validation passed"
    }

    override fun correct(): Boolean {
        // Placeholder for correction logic
        return false
    }

    override fun getDescription(): String {
        if(issueFound) {
            return "Found and fixed corrupt named schedules."
        }
        return "No corrupt named schedules found."
    }
}