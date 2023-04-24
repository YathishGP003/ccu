package a75f.io.messaging.handler;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.util.TimeUtil;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.autocommission.AutoCommissioningState;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.messaging.MessageHandler;

public class AutoCommissioningStateHandler  implements MessageHandler {

    public static final String CMD = "autoCommissioningMode";
    public static final String SCHEDULEDSTOPDATETIME = "scheduledStopDatetime";
    private static final String STOP_DATE_TIME = "autoCXStopTime";
    private static final String STATE = "autoCXState";
    private static final String CCU_ID = "id";

    /**
     * Handles the auto-commissioning state based on the remote command sent.
     */
    public void handleAutoCommissioning(JsonObject msgObject) {
        String scheduledStopDatetime = msgObject.get(STOP_DATE_TIME).getAsString();
        int state = msgObject.get(STATE).getAsInt();
        long scheduledStopDatetimeInMillis = TimeUtil.getDateTimeInMillis(scheduledStopDatetime);
        long stopDateTimeInMillis = scheduledStopDatetimeInMillis - System.currentTimeMillis();

        String autoCommissioningPointId = CCUHsApi.getInstance().readId("point and diag and auto and commissioning");
        CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(autoCommissioningPointId),
                HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make((double) state), HNum.make(0));
        Log.d(L.TAG_CCU_AUTO_COMMISSIONING, "Auto-commissioning received properties - state : "+state+", stopDateTime : "+scheduledStopDatetime);

        if(checkAutoCommissioning(state, scheduledStopDatetimeInMillis, stopDateTimeInMillis)) {
            PreferenceUtil.setScheduledStopDatetime(AutoCommissioningStateHandler.SCHEDULEDSTOPDATETIME, scheduledStopDatetimeInMillis);
            handleAutoCommissioningState(scheduledStopDatetimeInMillis);
        }
    }

    public void handleAutoCommissioningState(long scheduledStopDatetimeInMillis) {

        long stopDateTimeInMillis = scheduledStopDatetimeInMillis - System.currentTimeMillis();
        Log.d(L.TAG_CCU_AUTO_COMMISSIONING, " currentTimeMillis : " + new Date(System.currentTimeMillis()) + ", stopTimesInMillis : " + new Date(scheduledStopDatetimeInMillis) + ", (stopTimesInMillis - current time) in millis : " + stopDateTimeInMillis);

        if (stopDateTimeInMillis > 0) {
            TimerTask timertask = new TimerTask() {
                @Override
                public void run() {
                    Log.d(L.TAG_CCU_AUTO_COMMISSIONING, "Auto-commissioning timer completed");
                    String autoCommissioningPointId = CCUHsApi.getInstance().readId("point and diag and auto and commissioning");
                    CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(autoCommissioningPointId), HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make((double) AutoCommissioningState.COMPLETED.ordinal()), HNum.make(0));
                    CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(autoCommissioningPointId), HayStackConstants.WEB_APP_WRITE_LEVEL, HNum.make((double) AutoCommissioningState.COMPLETED.ordinal()), HNum.make(1));  //clearing the upper level value
                }
            };
            Timer timer = new Timer();
            timer.schedule(timertask, stopDateTimeInMillis);
        }else {
            Log.d(AutoCommissioningStateHandler.class.toString(), "Setting auto-commissioning to COMPLETED state");
            String autoCommissioningPointId = CCUHsApi.getInstance().readId("point and diag and auto and commissioning");
            CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(autoCommissioningPointId), HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make((double) AutoCommissioningState.COMPLETED.ordinal()), HNum.make(0));
            CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(autoCommissioningPointId), HayStackConstants.WEB_APP_WRITE_LEVEL, HNum.make((double) AutoCommissioningState.COMPLETED.ordinal()), HNum.make(1)); //clearing the upper level value
        }
    }

    private boolean checkAutoCommissioning(int state, long scheduledStopDatetimeInMillis, long stopDateTimeInMillis) {
        return scheduledStopDatetimeInMillis > 0 && stopDateTimeInMillis > 0 &&
                state == AutoCommissioningState.STARTED.ordinal();
    }

    @NonNull
    @Override
    public List<String> getCommand() {
        return Collections.singletonList(CMD);
    }

    @Override
    public void handleMessage(JsonObject msgObject, @NonNull Context context) {
        Log.d(L.TAG_CCU_AUTO_COMMISSIONING, "auto-commissioning msgObject : "+msgObject.toString());
        if(!msgObject.has(CCU_ID)){
            Log.d(L.TAG_CCU_AUTO_COMMISSIONING, "ccuId property not found in msg object");
            return;
        }

        if(!msgObject.has(STATE)){
            Log.d(L.TAG_CCU_AUTO_COMMISSIONING, "state property not found in msg object");
            return;
        }

        if(msgObject.get(STATE).getAsInt() != 1){
            String autoCommissioningPointId = CCUHsApi.getInstance().readId("point and diag and auto and commissioning");
            CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(autoCommissioningPointId),
                    HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make((double) msgObject.get(STATE).getAsInt()), HNum.make(0));
            Toast.makeText(Globals.getInstance().getApplicationContext(),
                    "Auto-commissioning Test Stopped.",Toast.LENGTH_SHORT).show();
            Log.d(L.TAG_CCU_AUTO_COMMISSIONING, "Auto-commissioning Test Stopped: State: "+msgObject.get(STATE).getAsInt());
            return;
        }

        if(!msgObject.has(STOP_DATE_TIME)){
            Log.d(L.TAG_CCU_AUTO_COMMISSIONING, "stopDateTime property not found in msg object");
            return;
        }

        String ccuID = CCUHsApi.getInstance().readId("ccu and device");
        String msgCCUid = msgObject.get(CCU_ID).getAsString();
        if(msgCCUid != null && msgCCUid.equals(ccuID)) {
            Toast.makeText(Globals.getInstance().getApplicationContext(),
                    "Auto-commissioning command received.",Toast.LENGTH_SHORT).show();
            handleAutoCommissioning(msgObject);
        }else{
            Log.d(L.TAG_CCU_AUTO_COMMISSIONING, "CCU ID is not matching- received ccuId: "+msgCCUid+" actual ccuId: "+ccuID);
        }
    }
}
