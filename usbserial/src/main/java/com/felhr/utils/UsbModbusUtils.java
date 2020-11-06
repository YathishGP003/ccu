package com.felhr.utils;

public class UsbModbusUtils {
    /** Constant <code>READ_COILS=1</code> */
    public static final byte READ_COILS = 1;
    /** Constant <code>READ_DISCRETE_INPUTS=2</code> */
    public static final byte READ_DISCRETE_INPUTS = 2;
    /** Constant <code>READ_HOLDING_REGISTERS=3</code> */
    public static final byte READ_HOLDING_REGISTERS = 3;
    /** Constant <code>READ_INPUT_REGISTERS=4</code> */
    public static final byte READ_INPUT_REGISTERS = 4;
    /** Constant <code>WRITE_COIL=5</code> */
    public static final byte WRITE_COIL = 5;
    /** Constant <code>WRITE_REGISTER=6</code> */
    public static final byte WRITE_REGISTER = 6;
    /** Constant <code>READ_EXCEPTION_STATUS=7</code> */
    public static final byte READ_EXCEPTION_STATUS = 7;
    /** Constant <code>WRITE_COILS=15</code> */
    public static final byte WRITE_COILS = 15;
    /** Constant <code>WRITE_REGISTERS=16</code> */
    public static final byte WRITE_REGISTERS = 16;
    /** Constant <code>REPORT_SLAVE_ID=17</code> */
    public static final byte REPORT_SLAVE_ID = 17;
    /** Constant <code>WRITE_MASK_REGISTER=22</code> */
    public static final byte WRITE_MASK_REGISTER = 22;

    public static int validateFunctionCode(int id) {
        switch (id) {
            case READ_COILS:
                return UsbModbusUtils.READ_COILS;
            case READ_DISCRETE_INPUTS:
                return UsbModbusUtils.READ_DISCRETE_INPUTS;
            case READ_HOLDING_REGISTERS:
                return UsbModbusUtils.READ_HOLDING_REGISTERS;
            case READ_INPUT_REGISTERS:
                return UsbModbusUtils.READ_INPUT_REGISTERS;
            case WRITE_COIL:
                return UsbModbusUtils.WRITE_COIL;
            case WRITE_REGISTER:
                return UsbModbusUtils.WRITE_REGISTER;
            case READ_EXCEPTION_STATUS:
                return UsbModbusUtils.READ_EXCEPTION_STATUS;
            case WRITE_COILS:
                return UsbModbusUtils.WRITE_COILS;
            case WRITE_REGISTERS:
                return UsbModbusUtils.WRITE_REGISTERS;
            case REPORT_SLAVE_ID:
                return UsbModbusUtils.REPORT_SLAVE_ID;
            case WRITE_MASK_REGISTER:
                return UsbModbusUtils.WRITE_MASK_REGISTER;
        }
        return -1;
    }
    public static boolean validSlaveId(int id){
        return ((id > 0) && (id < 210)) ;
    }
    /**
     * <p>toString.</p>
     *
     * @param code a byte.
     * @return a {@link String} object.
     */
    public static String toString(byte code) {
        return Integer.toString(code & 0xff);
    }
}
