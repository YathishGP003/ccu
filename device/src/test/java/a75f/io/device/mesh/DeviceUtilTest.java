package a75f.io.device.mesh;

import junit.framework.TestCase;

/**
 * Created by Manjunath K on 27-04-2022.
 */
public class DeviceUtilTest extends TestCase {

    public void testGetPercentageFromVoltage() {

        assertEquals(DeviceUtil.getPercentageFromVoltage(5,"0-10v"),50.0);
        assertEquals(DeviceUtil.getPercentageFromVoltage(5,"10-0v"),50.0);
        assertEquals(DeviceUtil.getPercentageFromVoltage(6,"2-10v"),50.0);
        assertEquals(DeviceUtil.getPercentageFromVoltage(6,"10-2v"),50.0);
    }
}