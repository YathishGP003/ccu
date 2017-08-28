package a75f.io.logic.jobs;

import android.support.annotation.NonNull;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import org.javolution.io.Struct;

import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

import a75f.io.bo.serial.CcuToCmOverUsbCcuHeartbeatMessage_t;
import a75f.io.bo.serial.MessageType;
import a75f.io.logic.SerialBLL;

/**
 * Created by Yinten on 8/24/2017.
 */

public class HeartBeatJob extends Job {

    public static final String TAG = "HeartBeatJob";
    public static final short HEARTBEAT_INTERVAL = 1;  // minutes
    private static final short HEARTBEAT_MULTIPLIER = 5;

    @Override
    @NonNull
    protected Result onRunJob(Params params) {
        Log.i(TAG, "On Run Job");
        Log.i(TAG, "Sending Heartbeat");
        SerialBLL.getInstance().sendSerialStruct(getHeartBeat((short) 0));
        HeartBeatJob.scheduleJob();

        return Result.SUCCESS;
    }

    public static void scheduleJob() {
        Log.i(TAG, "Job Scheduled: " + HeartBeatJob.TAG);
        new JobRequest.Builder(HeartBeatJob.TAG).setPersisted(true).setExecutionWindow(50000, 70000)
                .build()
                .schedule();
    }


    private CcuToCmOverUsbCcuHeartbeatMessage_t getHeartBeat(short temperatureOffset) {
        CcuToCmOverUsbCcuHeartbeatMessage_t heartbeatMessage_t = new CcuToCmOverUsbCcuHeartbeatMessage_t();
        heartbeatMessage_t.interval.set(HEARTBEAT_INTERVAL);
        heartbeatMessage_t.messageType.set(MessageType.CCU_HEARTBEAT_UPDATE);
        heartbeatMessage_t.multiplier.set(HEARTBEAT_MULTIPLIER);
        heartbeatMessage_t.temperatureOffset.set((byte) temperatureOffset);
        return heartbeatMessage_t;
    }


}

