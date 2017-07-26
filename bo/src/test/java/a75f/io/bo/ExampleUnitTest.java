package a75f.io.bo;

import org.junit.Test;

import a75f.io.bo.serial.CCUtoCM.SmartNodeControls_t;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void testLibraryIsWorking() throws Exception {
        SmartNodeControls_t smartNodeControls_t = new SmartNodeControls_t();
        smartNodeControls_t.analogOut1 = (short)1;
        smartNodeControls_t.analogOut2 = 2;
    }
}