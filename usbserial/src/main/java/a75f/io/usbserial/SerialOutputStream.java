package a75f.io.usbserial;

import java.io.IOException;
import java.io.OutputStream;

public class SerialOutputStream extends OutputStream {
    UsbModbusService usbService;
    public SerialOutputStream(UsbModbusService service) {
        usbService = service;
    }
    
    @Override
    public void write(int arg0) throws IOException {
        byte[] b = {(byte) arg0};
        usbService.modbusWrite(b);
        //write to serial port
    }
    
    public void write(byte[] data) throws IOException {
        usbService.modbusWrite(data);
        //write to serial port
    }
    
    @Override
    public void flush() {
    }
}
