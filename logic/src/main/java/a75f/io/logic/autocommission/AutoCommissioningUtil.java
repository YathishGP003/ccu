package a75f.io.logic.autocommission;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;

public class AutoCommissioningUtil {
    public static final String SCHEDULEDSTOPDATETIME = "scheduledStopDatetime";

    private static final int NOT_STARTED_TIMER = 60000;

    public static void setAutoCommissionState(AutoCommissioningState autoCommissioningState){
        String autoCommissioningPointId =  CCUHsApi.getInstance().readId("point and diag and auto and commissioning");
        CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(autoCommissioningPointId), HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make((double) autoCommissioningState.ordinal()), HNum.make(0));
    }

    public static AutoCommissioningState getAutoCommissionState(){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        Double autoCommissioningPoint = hayStack.readPointPriorityValByQuery("point and diag and auto and commissioning");
        return autoCommissioningPoint == null ? AutoCommissioningState.NOT_STARTED : AutoCommissioningState.values()[autoCommissioningPoint.intValue()];
    }

    public static boolean isAutoCommissioningStarted(){
        return getAutoCommissionState() == AutoCommissioningState.STARTED;
    }

    public static void handleAutoCommissioningState(long scheduledStopDatetimeInMillis) {

        long stopDateTimeInMillis = scheduledStopDatetimeInMillis - System.currentTimeMillis();
        CcuLog.d(L.TAG_CCU_AUTO_COMMISSIONING,
                "current time in millis : " + new Date(System.currentTimeMillis()) + "" +
                        " stop times in millis : " + new Date(scheduledStopDatetimeInMillis) +
                        " auto-cx test duration in millis : " + stopDateTimeInMillis);
        CCUHsApi instance  = CCUHsApi.getInstance();
        /*
        * if stopDateTimeInMillis is greater than 0 we are scheduling the test using TimerTask
        * stopDateTimeInMillis is stopDateTime(we set in sharedPreference) - current time in millis
        * */
        if (stopDateTimeInMillis > 0) {
            TimerTask timertask = new TimerTask() {
                @Override
                public void run() {
                   if(isAutoCommissioningStarted()){
                       new Handler(Looper.getMainLooper()).post(new Runnable() {
                           @Override
                           public void run() {
                               CcuLog.d(L.TAG_CCU_AUTO_COMMISSIONING, "Auto-commissioning timer completed");
                               Toast.makeText(Globals.getInstance().getApplicationContext(),
                                       "Auto-commissioning Test completed.",Toast.LENGTH_SHORT).show();
                           }
                       });
                       String autoCommissioningPointId = CCUHsApi.getInstance().readId("point and diag and auto and commissioning");
                       instance.pointWriteForCcuUser(HRef.copy(autoCommissioningPointId),
                               HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make((double) AutoCommissioningState.COMPLETED.ordinal()), HNum.make(0));
                       instance.writeHisValById(autoCommissioningPointId, (double) AutoCommissioningState.COMPLETED.ordinal());
                       setNotStarted(instance, autoCommissioningPointId);
                   }
                }
            };
            Timer timer = new Timer();
            timer.schedule(timertask, stopDateTimeInMillis);
        }else {
            if (isAutoCommissioningStarted()) {
                //This section deals with exceptional situations may be when the CCU is offline or not opened during the auto-commissioning process
                CcuLog.d(L.TAG_CCU_AUTO_COMMISSIONING, "Setting auto-commissioning to COMPLETED state: " + new Date(System.currentTimeMillis()));
                String autoCommissioningPointId = CCUHsApi.getInstance().readId("point and diag and auto and commissioning");
                instance.pointWriteForCcuUser(HRef.copy(autoCommissioningPointId),
                        HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make((double) AutoCommissioningState.COMPLETED.ordinal()), HNum.make(0));
                instance.writeHisValById(autoCommissioningPointId, (double) AutoCommissioningState.COMPLETED.ordinal());
                setNotStarted(instance, autoCommissioningPointId);
            }
        }
    }

    private static void setNotStarted(CCUHsApi instance, String autoCommissioningPointId){
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

}
