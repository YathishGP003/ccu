package a75f.io.device;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.zip.CRC32;

import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.device.serial.MessageType;

/**
 * Created by Yinten on 9/19/2017.
 */

public class HashTest
{
    
    public HashSet<Integer> seedMessages = new HashSet<>();
    
    
    @Test
    public void hashTest()
    {
        CRC32 crc = new CRC32();
        CcuToCmOverUsbDatabaseSeedSnMessage_t ccuSeed = new CcuToCmOverUsbDatabaseSeedSnMessage_t();
        int seedTest = 15;
        ccuSeed.smartNodeAddress.set(seedTest);
        Assert.assertFalse(seedMessages.contains(ccuSeed.getOrderedBuffer()));
        seedMessages.add(Arrays.hashCode(ccuSeed.getOrderedBuffer()));
        Assert.assertTrue(seedMessages.contains(Arrays.hashCode(ccuSeed.getOrderedBuffer()
                                                                                                )));
        
        
        CcuToCmOverUsbDatabaseSeedSnMessage_t ccuSeedDifferent = new
                                                                         CcuToCmOverUsbDatabaseSeedSnMessage_t();
//
//        Assert.assertSame(ccuSeed, seedMessages.get(ccuSeed.smartNodeAddress.get()));
//        Assert.assertNotSame(ccuSeed, ccuSeedDifferent);
//        Assert.assertNotSame(seedMessages.get(ccuSeed.smartNodeAddress.get()), ccuSeedDifferent);
//
//        Assert.assertTrue(ccuSeed.equals(seedMessages.get(ccuSeed.smartNodeAddress.get())));
//        Assert.assertNoteSame(ccuSeed.hashCode());
    }
    
    @Test
    public void testProto() {
    
        byte[] bytes = getByteArrayFromInt(6000);
        System.out.println(Arrays.toString(bytes));
    
        ByteBuffer wrapped = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN); // big-endian by default
        System.out.println(wrapped.getInt());
    }
    
    byte[] getByteArrayFromInt(int x) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(x).array();
    }
}
