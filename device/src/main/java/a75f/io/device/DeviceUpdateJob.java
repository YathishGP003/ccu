package a75f.io.device;

import a75f.io.device.mesh.MeshNetwork;

/**
 * Created by samjithsadasivan on 9/19/18.
 */

public class DeviceUpdateJob extends BaseJob
{
    DeviceNetwork deviceNw;
    public DeviceUpdateJob()
    {
        super();
        deviceNw = new MeshNetwork();//TODO- TEMP
    }
    
    public void doJob()
    {
        deviceNw.sendMessage();
    }
}
