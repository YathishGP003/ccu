package a75f.io.device;

import android.util.Log;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
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
        Log.d("CCU", "DeviceUpdateJob ->");
    
        HashMap site = CCUHsApi.getInstance().read("site");
        if (site == null || site.size() == 0) {
            Log.d("CCU","No Site Registered ! <-DeviceUpdateJob ");
            return;
        }
        deviceNw.sendMessage();
        deviceNw.sendSystemControl();
        Log.d("CCU", "<-DeviceUpdateJob ");
    }
}
