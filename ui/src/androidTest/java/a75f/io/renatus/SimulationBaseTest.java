package a75f.io.renatus;

import android.support.test.runner.AndroidJUnit4;

import junit.framework.TestResult;

import org.junit.Test;
import org.junit.runner.RunWith;

import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.MessageType;

/**
 * Instrumentation test, which will execute isOn an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public abstract class SimulationBaseTest {
	public abstract String getTestDescription();

	public abstract String getCCUStateFileName();

	public abstract String getSimulationFileName();

	public abstract TestResult analyzeTestResults(SimulationTestLog testLog);

	public abstract long testDuration();

	public abstract void reportTestResults(SimulationTestLog testLog, TestResult result);

	public abstract String[] graphColumns();

}


	@Test
	public void useAppContext() throws Exception
	{
		// Context of the app under test.
		CcuToCmOverUsbDatabaseSeedSnMessage_t ccuToCmOverUsbDatabaseSeedSnMessage_t =
				new CcuToCmOverUsbDatabaseSeedSnMessage_t();
		ccuToCmOverUsbDatabaseSeedSnMessage_t.smartNodeAddress.set(2000);
		ccuToCmOverUsbDatabaseSeedSnMessage_t.messageType
				.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
	}
}
