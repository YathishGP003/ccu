package a75f.io.logic.autocommission;

import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.logic.L;

public class AutoCommissioningUtil {
    public static final String SCHEDULEDSTOPDATETIME = "scheduledStopDatetime";

    public static void setAutoCommissionState(AutoCommissioningState autoCommissioningState){
        String autoCommissioningPointId =  CCUHsApi.getInstance().readId("point and diag and auto and commissioning");
        CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(autoCommissioningPointId), HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make((double) autoCommissioningState.ordinal()), HNum.make(0));
    }

    public static AutoCommissioningState getAutoCommissionState(){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        return AutoCommissioningState.values()[hayStack.readPointPriorityValByQuery("point and diag and auto and commissioning").intValue()];
    }

    public static boolean isAutoCommissioningStarted(){
        return getAutoCommissionState() == AutoCommissioningState.STARTED;
    }

    public static void handleAutoCommissioningState(long scheduledStopDatetimeInMillis) {

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
            Log.d(AutoCommissioningUtil.class.toString(), "Setting auto-commissioning to COMPLETED state");
            String autoCommissioningPointId = CCUHsApi.getInstance().readId("point and diag and auto and commissioning");
            CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(autoCommissioningPointId), HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make((double) AutoCommissioningState.COMPLETED.ordinal()), HNum.make(0));
            CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(autoCommissioningPointId), HayStackConstants.WEB_APP_WRITE_LEVEL, HNum.make((double) AutoCommissioningState.COMPLETED.ordinal()), HNum.make(1)); //clearing the upper level value
        }
    }
}
