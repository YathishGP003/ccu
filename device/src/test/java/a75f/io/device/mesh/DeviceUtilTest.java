package a75f.io.device.mesh;

import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import a75f.io.constants.DeviceFieldConstants;
import a75f.io.device.serial.SnRebootIndicationMessage_t;
import a75f.io.logic.diag.otastatus.OtaStatus;

/**
 * Created by Manjunath K on 27-04-2022.
 */
public class DeviceUtilTest extends TestCase {

    public void testParseNodeStatusMessage() {
        assertEquals(DeviceUtil.parseNodeStatusMessage(1, 1000), DeviceFieldConstants.CAUSE0);
        assertEquals(DeviceUtil.parseNodeStatusMessage(9, 1000), DeviceFieldConstants.CAUSE1);
        assertEquals(DeviceUtil.parseNodeStatusMessage(17, 1000), DeviceFieldConstants.CAUSE2);
        assertEquals(DeviceUtil.parseNodeStatusMessage(25, 1000), DeviceFieldConstants.CAUSE3);
        assertEquals(DeviceUtil.parseNodeStatusMessage(33, 1000), DeviceFieldConstants.CAUSE4);
        assertEquals(DeviceUtil.parseNodeStatusMessage(41, 1000), DeviceFieldConstants.CAUSE5);
        assertEquals(DeviceUtil.parseNodeStatusMessage(49, 1000), DeviceFieldConstants.CAUSE6);
        assertEquals(DeviceUtil.parseNodeStatusMessage(57, 1000), DeviceFieldConstants.CAUSE7);
        assertEquals(DeviceUtil.parseNodeStatusMessage(255, 1000), DeviceFieldConstants.NO_INFO);

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

    public void testRebootOtaStatus() {
        SnRebootIndicationMessage_t msg = new SnRebootIndicationMessage_t();

        byte[] eventBytes = new byte[] {
                26, 8, 7, 0, 64, 0, 0, 1, 7, 0, 3, 0, 27, -15, -126, 5, 78, 70, 54, 83, 32, 32, 32,
                80, 20, 67, 19, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

        msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);
        OtaStatus status = DeviceUtil.getNodeStatus(msg.nodeStatus.get());
        assertEquals(status,OtaStatus.NO_INFO);

        eventBytes = new byte[] {
                26, 8, 7, 0, 64, 0, 0, 1, 7, 0, 3, 0, 27, -15, -126, 5, 78, 70, 54, 83, 32, 32, 32,
                80, 20, 67, 19, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 };


        msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);
        status = DeviceUtil.getNodeStatus(msg.nodeStatus.get());
        assertEquals(status,OtaStatus.NODE_STATUS_VALUE_FW_OTA_SUCCESSFUL);


        eventBytes = new byte[] {
                26, 8, 7, 0, 64, 0, 0, 1, 7, 0, 3, 0, 27, -15, -126, 5, 78, 70, 54, 83, 32, 32, 32,
                80, 20, 67, 19, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9 };


        msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);
        status = DeviceUtil.getNodeStatus(msg.nodeStatus.get());
        assertEquals(status,OtaStatus.NODE_STATUS_VALUE_FW_OTA_FAIL_REBOOT_INTERRUPTION);

        eventBytes = new byte[] {
                26, 8, 7, 0, 64, 0, 0, 1, 7, 0, 3, 0, 27, -15, -126, 5, 78, 70, 54, 83, 32, 32, 32,
                80, 20, 67, 19, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 17 };


        msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);
        status = DeviceUtil.getNodeStatus(msg.nodeStatus.get());
        assertEquals(status,OtaStatus.NODE_STATUS_VALUE_FW_OTA_FAIL_NOT_FOR_ME_DEV_TYPE);


        eventBytes = new byte[] {
                26, 8, 7, 0, 64, 0, 0, 1, 7, 0, 3, 0, 27, -15, -126, 5, 78, 70, 54, 83, 32, 32, 32,
                80, 20, 67, 19, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 25 };

        msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);
        status = DeviceUtil.getNodeStatus(msg.nodeStatus.get());
        assertEquals(status,OtaStatus.NODE_STATUS_VALUE_FW_OTA_FAIL_NOT_FOR_ME_FW_VERSION);



        eventBytes = new byte[] {
                26, 8, 7, 0, 64, 0, 0, 1, 7, 0, 3, 0, 27, -15, -126, 5, 78, 70, 54, 83, 32, 32, 32,
                80, 20, 67, 19, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 33 };

        msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);
        status = DeviceUtil.getNodeStatus(msg.nodeStatus.get());
        assertEquals(status,OtaStatus.NODE_STATUS_VALUE_FW_OTA_FAIL_IMAGE_SIZE);


        eventBytes = new byte[] {
                26, 8, 7, 0, 64, 0, 0, 1, 7, 0, 3, 0, 27, -15, -126, 5, 78, 70, 54, 83, 32, 32, 32,
                80, 20, 67, 19, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 41 };

        msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);
        status = DeviceUtil.getNodeStatus(msg.nodeStatus.get());
        assertEquals(status,OtaStatus.NODE_STATUS_VALUE_FW_OTA_FAIL_EXT_FLASH_ERROR);


        eventBytes = new byte[] {
                26, 8, 7, 0, 64, 0, 0, 1, 7, 0, 3, 0, 27, -15, -126, 5, 78, 70, 54, 83, 32, 32, 32,
                80, 20, 67, 19, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 49 };

        msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);
        status = DeviceUtil.getNodeStatus(msg.nodeStatus.get());
        assertEquals(status,OtaStatus.NODE_STATUS_VALUE_FW_OTA_FAIL_IMAGE_VERIFICATION);

        eventBytes = new byte[] {
                26, 8, 7, 0, 64, 0, 0, 1, 7, 0, 3, 0, 27, -15, -126, 5, 78, 70, 54, 83, 32, 32, 32,
                80, 20, 67, 19, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 57 };

        msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);
        status = DeviceUtil.getNodeStatus(msg.nodeStatus.get());
        assertEquals(status,OtaStatus.NODE_STATUS_VALUE_FW_OTA_FAIL_INACTIVITY_TIMEOUT);
    }

    public void testGetPercentageFromVoltage() {

        assertEquals(DeviceUtil.getPercentageFromVoltage(5, "0-10v"), 50.0);
        assertEquals(DeviceUtil.getPercentageFromVoltage(5, "10-0v"), 50.0);
        assertEquals(DeviceUtil.getPercentageFromVoltage(6, "2-10v"), 50.0);
        assertEquals(DeviceUtil.getPercentageFromVoltage(6, "10-2v"), 50.0);
    }

    public void testThermistorVal() {
        double tempVal = ThermistorUtil.getThermistorValueToTemp(617580);
        System.out.println(tempVal);
    }
}