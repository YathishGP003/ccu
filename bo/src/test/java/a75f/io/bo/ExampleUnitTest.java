package a75f.io.bo;

import org.junit.Test;

import a75f.io.bo.serial.SmartNodeControls_t;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void testLibraryIsWorking() throws Exception {
        SmartNodeControls_t smartNodeControls_t = new SmartNodeControls_t();
        smartNodeControls_t.analogOut1.set((short)1);
        smartNodeControls_t.analogOut2.set((short)2);
        smartNodeControls_t.smartNodeControls_extras.smartNodeControlsBitExtras.digitalOut1.set(1);
        smartNodeControls_t.smartNodeControls_extras.smartNodeControlsBitExtras.digitalOut2.set(1);
        //Smart Node Controls: 00 00 00 00 00 01 02 00 00 00 60
        System.out.println("Smart Node Controls: " + smartNodeControls_t.toString());
    }
}