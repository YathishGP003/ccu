package a75f.io.device;

/**
 * Created by samjithsadasivan on 9/19/18.
 */

public class DeviceConstants
{
    public static final class IntentActions {
        public static final String LSERIAL_MESSAGE = "a75f.io.device.LSERIAL_MESSAGE";
        public static final String ACTIVITY_MESSAGE = "a75f.io.device.ACTIVITY_MESSAGE";
        public static final String PUBNUB_MESSAGE = "a75f.io.device.PUBNUB_MESSAGE";
    }

    public static final  String TAG                  = "CCU_DEVICE";
    public static final  short  HEARTBEAT_INTERVAL   = 1;  // minutes
    public static final short  HEARTBEAT_MULTIPLIER = 5;
    public static final int    SIMULATION_SLEEP_TIME = 100;
    
}
