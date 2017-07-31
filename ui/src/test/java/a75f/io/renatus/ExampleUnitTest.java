package a75f.io.renatus;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.MessageType;
import a75f.io.bo.serial.SmartNodeControls_t;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {

        CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage = new CcuToCmOverUsbDatabaseSeedSnMessage_t();

        seedMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
        seedMessage.encryptionKey.set(0);

        seedMessage.smartNodeAddress.set(8000);
        seedMessage.controls.time.day.set((short) 1);
        seedMessage.controls.time.hours.set((short) 1);
        seedMessage.controls.time.minutes.set((short) 1);

        seedMessage.settings.ledBitmap.digitalOut1.set(1);
        seedMessage.controls.smartNodeControls_extras.digitalOut1.set(1);
        seedMessage.settings.ledBitmap.digitalOut2.set(1);
        System.out.println("Seed message: " + seedMessage.toString());
        System.out.println("Seed message size: " + seedMessage.size());




//        CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage =  new CcuToCmOverUsbDatabaseSeedSnMessage_t();
//        seedMessage.encryptionKey.set(0);
//
//        seedMessage.smartNodeAddress.set(8000);
//        seedMessage.controls.time.day.set((short) 1);
//        seedMessage.controls.time.hours.set((short) 1);
//        seedMessage.controls.time.minutes.set((short) 1);
//
//        seedMessage.settings.ledBitmap.digitalOut1.set(1);
//
//        System.out.println(seedMessage.toString());


        /*SmartNodeControls_t smartNodeControls_t = new SmartNodeControls_t();
        smartNodeControls_t.analogOut1.set((short)1);
        smartNodeControls_t.analogOut2.set((short)2);
        smartNodeControls_t.smartNodeControls_extras.smartNodeControlsBitExtras.digitalOut1.set(1);
        smartNodeControls_t.smartNodeControls_extras.smartNodeControlsBitExtras.digitalOut2.set(1);
        //Smart Node Controls: 00 00 00 00 00 01 02 00 00 00 60
        System.out.println("Smart Node Controls: " + smartNodeControls_t.toString());*/
    }
}