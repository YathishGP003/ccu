package a75f.io.device.mesh;

import java.util.Arrays;

import a75f.io.logic.bo.building.definitions.Port;

public class DeviceUtil {
    
    public static boolean isAnalog(String port) {
       return (port.equals(Port.ANALOG_OUT_ONE.name())
               ||port.equals(Port.ANALOG_OUT_TWO.name())
               ||port.equals(Port.ANALOG_OUT_THREE.name())
               ||port.equals(Port.ANALOG_OUT_FOUR.name()));
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
            default:
            String [] arrOfStr = type.split("-");
            if (arrOfStr.length == 2)
            {
                if (arrOfStr[1].contains("v")) {
                    arrOfStr[1] = arrOfStr[1].replace("v", "");
                }
                int min = (int)Double.parseDouble(arrOfStr[0]);
                int max = (int)Double.parseDouble(arrOfStr[1]);
                if (max > min) {
                    return (short) (min * 10 + (max - min ) * 10 * val/100);
                } else {
                    return (short) (min * 10 - (min - max ) * 10 * val/100);
                }
            }
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
