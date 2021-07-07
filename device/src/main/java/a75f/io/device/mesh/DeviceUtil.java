package a75f.io.device.mesh;

import java.util.Arrays;

import a75f.io.logic.bo.building.definitions.Port;

public class DeviceUtil {
    
    public static boolean isAnalog(String port) {
        return Arrays.stream(Port.values()).anyMatch(port::equals);
    }
    
    public static short mapAnalogOut(String type, short val) {
        val = (short)Math.min(val, 100);
        val = (short)Math.max(val, 0);
        switch (type) {
            case "0-10v":
                return val;
            case "10-0v":
                return (short) (100 - val);
            case "2-10v":
                return (short) (20 + scaleAnalog(val, 80));
            case "10-2v":
                return (short) (100 - scaleAnalog(val, 80));
        }
        return (short) 0;
    }
    
    public static short mapDigitalOut(String type, boolean val) {
        if (type.equals("Relay N/O")) {
            return (short) (val ? 1 : 0);
        } else if (type.equals("Relay N/C")) {
            return (short) (val ? 0 : 1);
        }
        return 0;
    }
    
    public static int scaleAnalog(short analog, int scale) {
        return (int) ((float) scale * ((float) analog / 100.0f));
    }
}
