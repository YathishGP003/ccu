package a75f.io.logic.pubnub;

import a75f.io.api.haystack.Schedule;

public interface BuildingScheduleListener {
    void refreshScreen(Schedule updatedSchedule);
}
