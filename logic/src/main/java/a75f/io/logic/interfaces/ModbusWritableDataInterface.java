package a75f.io.logic.interfaces;

import java.util.ArrayList;

/**
 * Used to implement modbus write in the device layer based on writable point changes from portals
 * received via messages.
 */
public interface ModbusWritableDataInterface {
    void writeRegister(String id );
    void writeSystemModbusRegister(String equipRef, ArrayList<String> registerList);
    void writeConnectModbusRegister(int slaveId, int registerAddress, double value);
    void writeToPCN();
}

