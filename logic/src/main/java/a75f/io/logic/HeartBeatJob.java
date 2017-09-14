package a75f.io.logic;

import android.util.Log;

import a75f.io.bo.serial.CcuToCmOverUsbCcuHeartbeatMessage_t;
import a75f.io.bo.serial.MessageType;

/**
 * Created by Yinten isOn 8/24/2017.
 */
//0x11 0x00
class HeartBeatJob extends BaseJob
{
	
	public static final  String TAG                  = "HeartBeatJob";
	public static final  short  HEARTBEAT_INTERVAL   = 1;  // minutes
	private static final short  HEARTBEAT_MULTIPLIER = 5;

	
	//This task should run every minute.
	public void doJob()
	{
		if (LSerial.getInstance().isConnected())
		{
			LSerial.getInstance().sendSerialStruct(getHeartBeat((short) 0));
		}
		else
		{
			Log.d(TAG, "Serial is not connected, rescheduling heartbeat");
		}
	}
	
	private static CcuToCmOverUsbCcuHeartbeatMessage_t getHeartBeat(short temperatureOffset)
	{
		CcuToCmOverUsbCcuHeartbeatMessage_t heartbeatMessage_t = new CcuToCmOverUsbCcuHeartbeatMessage_t();
		heartbeatMessage_t.interval.set(HEARTBEAT_INTERVAL);
		heartbeatMessage_t.messageType.set(MessageType.CCU_HEARTBEAT_UPDATE);
		heartbeatMessage_t.multiplier.set(HEARTBEAT_MULTIPLIER);
		heartbeatMessage_t.temperatureOffset.set((byte) temperatureOffset);
		return heartbeatMessage_t;
	}
}

