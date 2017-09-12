package a75f.io.logic.jobs;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import a75f.io.bo.serial.CcuToCmOverUsbCcuHeartbeatMessage_t;
import a75f.io.bo.serial.MessageType;
import a75f.io.logic.SerialBLL;
import a75f.io.logic.cache.Globals;

/**
 * Created by Yinten isOn 8/24/2017.
 */
//0x11 0x00
public class HeartBeatJob
{
	
	public static final  String TAG                  = "HeartBeatJob";
	public static final  short  HEARTBEAT_INTERVAL   = 1;  // minutes
	private static final short  HEARTBEAT_MULTIPLIER = 5;
	private static final short  HEART_BEAT_TOLERANCE = 10;
	
	
	public static void scheduleJob()
	{
		// This task runs every minute.
		Globals.getInstance().getScheduledThreadPool().scheduleAtFixedRate(new Runnable()
		{
			public void run()
			{
				doJob();
			}
		}, 0, 1, TimeUnit.MINUTES);
	}
	
	
	//This task should run every minute.
	public static void doJob()
	{
		if (SerialBLL.getInstance().isConnected())
		{
			SerialBLL.getInstance().sendSerialStruct(getHeartBeat((short) 0));
		}
		else
		{
			Log.d(TAG, "Serial is not connected, rescheduling heartbeat");
		}
	}
	
	
	private static CcuToCmOverUsbCcuHeartbeatMessage_t getHeartBeat(short temperatureOffset)
	{
		CcuToCmOverUsbCcuHeartbeatMessage_t heartbeatMessage_t =
				new CcuToCmOverUsbCcuHeartbeatMessage_t();
		heartbeatMessage_t.interval.set(HEARTBEAT_INTERVAL);
		heartbeatMessage_t.messageType.set(MessageType.CCU_HEARTBEAT_UPDATE);
		heartbeatMessage_t.multiplier.set(HEARTBEAT_MULTIPLIER);
		heartbeatMessage_t.temperatureOffset.set((byte) temperatureOffset);
		return heartbeatMessage_t;
	}
}

