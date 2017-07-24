package a75f.io.dal;

import com.kinvey.android.store.DataStore;
import com.kinvey.android.sync.KinveyPullResponse;
import com.kinvey.android.sync.KinveyPushResponse;
import com.kinvey.android.sync.KinveySyncCallback;
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

        /*scheduleDataStore.sync(new KinveySyncCallback<Schedule>() {
            @Override
            public void onSuccess(KinveyPushResponse kinveyPushResponse, KinveyPullResponse<Schedule> kinveyPullResponse) {

            }

            @Override
            public void onPullStarted() {

            }

            @Override
            public void onPushStarted() {

            }

            @Override
            public void onPullSuccess(KinveyPullResponse<Schedule> kinveyPullResponse) {

            }

            @Override
            public void onPushSuccess(KinveyPushResponse kinveyPushResponse) {

            }

            @Override
            public void onFailure(Throwable throwable) {

            }
        });*/
        return true;
    }

}
