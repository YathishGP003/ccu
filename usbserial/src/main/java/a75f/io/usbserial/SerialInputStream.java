package a75f.io.usbserial;
import static com.felhr.usbserial.UsbSerialDevice.TAG_CCU_MODBUS;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;

import a75f.io.logger.CcuLog;

public class SerialInputStream extends InputStream {
    public final LinkedBlockingQueue<Byte> dataStream;
    
    public SerialInputStream() {
        dataStream = new LinkedBlockingQueue<Byte>();
    }
    
    @Override
    public int read() throws IOException {
        synchronized (dataStream) {
            try {
                if (dataStream.size() > 0) {
                    int data = dataStream.take() & 0xFF;
                    CcuLog.d(TAG_CCU_MODBUS," SerialInputStream data "+data);
                    return data; //Return unsigned byte value by masking off the high order bytes in the returned int
                } else {
                    return -1;
                }
            }
            catch (InterruptedException e) {
                throw new IOException(e);
            }
        }
    }
    
    @Override
    public int available() throws IOException {
        synchronized (dataStream) {
            return this.dataStream.size();
        }
    }
    
    //TODO- TEMP
    public void feedSerialData(byte[] data) {
        for (byte b : data) {
            dataStream.add(b);
        }
    }
}
