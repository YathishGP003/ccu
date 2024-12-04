package a75f.io.messaging.handler;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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
import a75f.io.domain.api.Domain;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.autocommission.AutoCommissioningState;
import a75f.io.logic.autocommission.AutoCommissioningUtil;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.messaging.MessageHandler;

public class AutoCommissioningStateHandler  implements MessageHandler {

    public static final String CMD = "autoCommissioningMode";
    public static final String SCHEDULEDSTOPDATETIME = "scheduledStopDatetime";
    private static final String STOP_DATE_TIME = "autoCXStopTime";
    private static final String STATE = "autoCXState";
    private static final String CCU_ID = "id";
    private static final int NOT_STARTED_TIMER = 60000;
    Timer timer;
    String autoCommissioningPointId = null;
    /**
     * Handles the auto-commissioning state based on the remote command sent.
     */

      public void startAutoCX(long scheduleStopTime) {
        long scheduledStopDatetimeInMillis = scheduleStopTime - System.currentTimeMillis();
        CcuLog.d(L.TAG_CCU_AUTO_COMMISSIONING,
                "current time in millis : " + new Date(System.currentTimeMillis()) +
                        " stop times in millis : " + new Date(scheduleStopTime) +
                        " auto-cx test duration in millis : " + scheduledStopDatetimeInMillis);

        if (scheduledStopDatetimeInMillis > 0) {
            TimerTask timertask = new TimerTask() {
                @Override
                public void run() {

                    new Handler(Looper.getMainLooper()).post(() -> {
                        CcuLog.d(L.TAG_CCU_AUTO_COMMISSIONING, "Auto-commissioning test completed "+new Date(System.currentTimeMillis()));
                        Toast.makeText(Globals.getInstance().getApplicationContext(),
                                "Auto-commissioning test completed.",Toast.LENGTH_SHORT).show();
                    });
                    CCUHsApi instance  = CCUHsApi.getInstance();
                    instance.pointWriteForCcuUser(HRef.copy(autoCommissioningPointId),
                            HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make((double) AutoCommissioningState.COMPLETED.ordinal()), HNum.make(0));
                    instance.writeHisValById(autoCommissioningPointId, (double) AutoCommissioningState.COMPLETED.ordinal());
                    setNotStarted(instance, autoCommissioningPointId);
                }
            };
            timer = new Timer();
            timer.schedule(timertask, scheduledStopDatetimeInMillis);
            PreferenceUtil.setScheduledStopDatetime(AutoCommissioningStateHandler.SCHEDULEDSTOPDATETIME, scheduledStopDatetimeInMillis);
            CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(autoCommissioningPointId),
                    HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make((double) AutoCommissioningState.STARTED.ordinal()), HNum.make(0));
            CCUHsApi.getInstance().writeHisValById(autoCommissioningPointId, (double) AutoCommissioningState.STARTED.ordinal());
            CcuLog.d(L.TAG_CCU_AUTO_COMMISSIONING, "Auto-commissioning test started " + new Date(System.currentTimeMillis()));
            Toast.makeText(Globals.getInstance().getApplicationContext(),
                    "Auto-commissioning test started.", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(Globals.getInstance().getApplicationContext(), "Auto-commissioning can't start.",Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    @Override
    public List<String> getCommand() {
        return Collections.singletonList(CMD);
    }

    @Override
    public void handleMessage(JsonObject msgObject, @NonNull Context context) {
        CcuLog.d(L.TAG_CCU_AUTO_COMMISSIONING, "auto-commissioning msgObject : "+msgObject.toString());

        if(!msgObject.has(CCU_ID)){
            CcuLog.d(L.TAG_CCU_AUTO_COMMISSIONING, "ccuId property not found in msg object");
            return;
        }

        if(!msgObject.has(STATE)){
            CcuLog.d(L.TAG_CCU_AUTO_COMMISSIONING, "state property not found in msg object");
            return;
        }


        String ccuID = Domain.ccuDevice.getId();
        String msgCCUid = msgObject.get(CCU_ID).getAsString();
        autoCommissioningPointId = AutoCommissioningUtil.getAutoCommissioningPointId();
        if(msgCCUid != null && msgCCUid.equals(ccuID)) {

            switch (msgObject.get(STATE).getAsInt()) {
                case 1:
                    if(!msgObject.has(STOP_DATE_TIME)){
                        CcuLog.d(L.TAG_CCU_AUTO_COMMISSIONING, "stopDateTime property not found in msg object");
                        return;
                    }
                    startAutoCX(TimeUtil.getDateTimeInMillis(msgObject.get(STOP_DATE_TIME).getAsString()));
                    return;
                case 2:
                    handleAutoCxState(msgObject);
                    CcuLog.d(L.TAG_CCU_AUTO_COMMISSIONING, "Auto-commissioning test completed " + new Date(System.currentTimeMillis()));
                    Toast.makeText(Globals.getInstance().getApplicationContext(),
                            "Auto-commissioning test completed.", Toast.LENGTH_SHORT).show();
                    return;
                case 3:
                    handleAutoCxState(msgObject);
                    CcuLog.d(L.TAG_CCU_AUTO_COMMISSIONING, "Auto-commissioning test aborted " + new Date(System.currentTimeMillis()));
                    Toast.makeText(Globals.getInstance().getApplicationContext(),
                            "Auto-commissioning test aborted.", Toast.LENGTH_SHORT).show();
                    return;
                default:
                    CcuLog.d(L.TAG_CCU_AUTO_COMMISSIONING, "wrong command for auto-commissioning " + new Date(System.currentTimeMillis()));
                    Toast.makeText(Globals.getInstance().getApplicationContext(),
                            "wrong command received.", Toast.LENGTH_SHORT).show();
            }
        }else{
            CcuLog.d(L.TAG_CCU_AUTO_COMMISSIONING, "CCU ID is not matching- received ccuId: "+msgCCUid+" actual ccuId: "+ccuID);
        }
    }

    public void handleAutoCxState(JsonObject msgObject){
        CCUHsApi instance  = CCUHsApi.getInstance();
        if(AutoCommissioningUtil.isAutoCommissioningStarted()){
            instance.pointWriteForCcuUser(HRef.copy(autoCommissioningPointId),
                    HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make((double) msgObject.get(STATE).getAsInt()), HNum.make(0));
            instance.writeHisValById(autoCommissioningPointId, (double) msgObject.get(STATE).getAsInt());
            timer.cancel();
            setNotStarted(instance, autoCommissioningPointId);
        }else{
            setNotStarted(instance, autoCommissioningPointId);
        }
    }

    private void setNotStarted(CCUHsApi instance, String autoCommissioningPointId){
        new Timer().schedule(new TimerTask(){
            @Override
            public void run() {
                instance.pointWriteForCcuUser(HRef.copy(autoCommissioningPointId),
                        HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make((double) AutoCommissioningState.NOT_STARTED.ordinal()), HNum.make(0));
                instance.writeHisValById(autoCommissioningPointId, (double) AutoCommissioningState.NOT_STARTED.ordinal());
                CcuLog.d(L.TAG_CCU_AUTO_COMMISSIONING, "auto-cx state reverted to state 0");
            }
        }, NOT_STARTED_TIMER);
    }

    @Override
    public boolean ignoreMessage(@NonNull JsonObject jsonObject, @NonNull Context context) {
        return false;
    }
}
