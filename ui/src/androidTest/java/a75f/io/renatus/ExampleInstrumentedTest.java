package a75f.io.renatus;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import a75f.io.bo.LittleEndianTestStruct;
import a75f.io.bo.TestStruct;
import a75f.io.bo.serial.MessageType;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.

        TestStruct testStruct = new TestStruct();


        testStruct.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);

        
        Log.i("TEST", testStruct.toString());

        LittleEndianTestStruct littleEndianTestStruct = new LittleEndianTestStruct();

        littleEndianTestStruct.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);

        littleEndianTestStruct.smartNodeAddress.set(2000);

        Log.i("TEST", littleEndianTestStruct.toString());
    }
}
