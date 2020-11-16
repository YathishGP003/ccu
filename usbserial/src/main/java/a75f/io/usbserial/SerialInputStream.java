package a75f.io.usbserial;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;

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
                    Log.d("CCU_MODBUS"," SerialInputStream data "+data);
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
