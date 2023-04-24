package a75f.io.logic.interfaces;

/**
 * Used to implement modbus write in the device layer based on writable point changes from portals
 * received via messages.
 */
public interface ModbusWritableDataInterface {
    void writeRegister(String id);
}
