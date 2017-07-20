package a75f.io.dal;

import com.kinvey.android.store.DataStore;
import com.kinvey.java.store.StoreType;

import java.io.IOException;

import a75f.io.bo.Schedule;
import a75f.io.util.Globals;

/**
 * Created by rmatt on 7/19/2017.
 */

public class ScheduleCloudSync {

    public static boolean saveSchedule(Schedule schedule) throws IOException {
        DataStore<Schedule> scheduleDataStore = DataStore.collection(Schedule.class.getSimpleName(), Schedule.class, StoreType.NETWORK, Globals.getInstance().getKinveyClient());
        scheduleDataStore.sync(schedule);
        return true;
    }

}
