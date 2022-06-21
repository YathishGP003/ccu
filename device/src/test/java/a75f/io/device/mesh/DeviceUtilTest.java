package a75f.io.device.mesh;

import junit.framework.TestCase;

import a75f.io.constants.DeviceFieldConstants;

/**
 * Created by Manjunath K on 27-04-2022.
 */
public class DeviceUtilTest extends TestCase {

    public void testParseNodeStatusMessage() {
        assertEquals(DeviceUtil.parseNodeStatusMessage(1), DeviceFieldConstants.CAUSE0);
        assertEquals(DeviceUtil.parseNodeStatusMessage(9), DeviceFieldConstants.CAUSE1);
        assertEquals(DeviceUtil.parseNodeStatusMessage(17), DeviceFieldConstants.CAUSE2);
        assertEquals(DeviceUtil.parseNodeStatusMessage(25), DeviceFieldConstants.CAUSE3);
        assertEquals(DeviceUtil.parseNodeStatusMessage(33), DeviceFieldConstants.CAUSE4);
        assertEquals(DeviceUtil.parseNodeStatusMessage(41), DeviceFieldConstants.CAUSE5);
        assertEquals(DeviceUtil.parseNodeStatusMessage(49), DeviceFieldConstants.CAUSE6);
        assertEquals(DeviceUtil.parseNodeStatusMessage(57), DeviceFieldConstants.CAUSE7);
        assertEquals(DeviceUtil.parseNodeStatusMessage(255), DeviceFieldConstants.NO_INFO);

    }

    public void testGetCause() {
        assertEquals(DeviceUtil.getCause(0), DeviceFieldConstants.CAUSE0);
        assertEquals(DeviceUtil.getCause(1), DeviceFieldConstants.CAUSE1);
        assertEquals(DeviceUtil.getCause(2), DeviceFieldConstants.CAUSE2);
        assertEquals(DeviceUtil.getCause(3), DeviceFieldConstants.CAUSE3);
        assertEquals(DeviceUtil.getCause(4), DeviceFieldConstants.CAUSE4);
        assertEquals(DeviceUtil.getCause(5), DeviceFieldConstants.CAUSE5);
        assertEquals(DeviceUtil.getCause(6), DeviceFieldConstants.CAUSE6);
        assertEquals(DeviceUtil.getCause(7), DeviceFieldConstants.CAUSE7);
        assertEquals(DeviceUtil.getCause(8), DeviceFieldConstants.NO_INFO);
    }
    public void testGetPercentageFromVoltage() {

        assertEquals(DeviceUtil.getPercentageFromVoltage(5, "0-10v"), 50.0);
        assertEquals(DeviceUtil.getPercentageFromVoltage(5, "10-0v"), 50.0);
        assertEquals(DeviceUtil.getPercentageFromVoltage(6, "2-10v"), 50.0);
        assertEquals(DeviceUtil.getPercentageFromVoltage(6, "10-2v"), 50.0);
    }
}