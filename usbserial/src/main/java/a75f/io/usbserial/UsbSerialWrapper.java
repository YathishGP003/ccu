package a75f.io.usbserial;

import com.x75f.modbus4j.serial.SerialPortWrapper;

import java.io.InputStream;
import java.io.OutputStream;

public class UsbSerialWrapper implements SerialPortWrapper {
    
    private int baudRate = 19200;
   
    private SerialOutputStream os;
    private SerialInputStream  is;
    
    public UsbSerialWrapper(SerialOutputStream os, SerialInputStream is) {
        this.os = os;
        this.is = is;
    }
    
    @Override public void close() throws Exception {
    }
    @Override public void open() throws Exception {
    }
    @Override public InputStream getInputStream() {
        return is;
    }
    @Override public OutputStream getOutputStream() {
        return os;
    }
    @Override public int getBaudRate() {
        return baudRate;
    }
    @Override public int getDataBits() {
        return 8;
    }
    @Override public int getStopBits() {
        return 1;
    }
    @Override public int getParity() {
        return 2;
    }
}
