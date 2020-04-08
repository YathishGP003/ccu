package a75f.io.device.bacnet;

import com.renovo.bacnet4j.LocalDevice;

/**
 * Created by samjithsadasivan on 9/19/18.
 */

public abstract class BACNetwork
{
    public abstract void sendMessage(LocalDevice bacnetDevice);
    
    public abstract void sendSystemControl(LocalDevice bacnetDevice);
}
