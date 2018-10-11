package a75f.io.device;

import junit.framework.Assert;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.zip.CRC32;

import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;

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
}
