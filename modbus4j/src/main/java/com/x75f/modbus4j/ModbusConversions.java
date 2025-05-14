package com.x75f.modbus4j;


/**
 * Created by Manjunath K on 18-12-2023.
 */
public class ModbusConversions {


    // Convert a float to two Modbus registers (16 bits each)
    public static short[] floatToRegistersLittleEndian(float value) {
        int intValue = Float.floatToIntBits(value);
        return new short[]{
                (short) (intValue & 0xFFFF),
                (short) (intValue >>> 16)
        };
    }

    // Convert two Modbus registers to a float
    public static short[] floatToRegistersBigEndian(float value) {
        int intValue = Float.floatToIntBits(value);
        return new short[]{
                (short) (intValue >>> 16),
                (short) (intValue & 0xFFFF),

        };
    }
    public static float registersToFloat(short[] registers) {
        int intValue = ((registers[1] & 0xFFFF) << 16) | (registers[0] & 0xFFFF);
        return Float.intBitsToFloat(intValue);
    }
}