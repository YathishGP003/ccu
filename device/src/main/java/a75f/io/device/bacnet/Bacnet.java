package a75f.io.device.bacnet;


import com.renovo.bacnet4j.LocalDevice;

import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.device.mesh.LSerial;
import a75f.io.device.serial.CcuToCmOverUsbCmRelayActivationMessage_t;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.DefaultSystem;

/**
 * Created by samjithsadasivan on 9/19/18.
 */

public class Bacnet extends BACNetwork
{

    @Override
    public void sendMessage(LocalDevice bacnetDevice) {
        BACnetUtils.setLocalDevice(bacnetDevice);
        //boolean bSeedMessage = LSerial.getInstance().isReseedMessage();
        try
        {
            new BACnetAlerts(bacnetDevice);
            for (Floor floor : HSUtil.getFloors())
            {
                for (Zone zone : HSUtil.getZones(floor.getId()))
                {
                    CcuLog.d(L.TAG_CCU_DEVICE,"=============Bacnet Zone: " + zone.getDisplayName() + " ==================");
                    if(bacnetDevice != null) {
                        String objectName = floor.getDisplayName() + "_" + zone.getDisplayName();
                        new ZonePoints(bacnetDevice, zone, objectName);
                        new BACnetScheduler(bacnetDevice, zone);
                        /*if ( LSerial.getInstance().isReseedMessage())
                            new BACnetScheduler(bacnetDevice, zone);//Why new?? every time do we create new??*/
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }finally {
            //LSerial.getInstance().setResetSeedMessage(false);
        }
    }

    public void sendSystemControl(LocalDevice bacnetDevice) {
        CcuLog.d(L.TAG_CCU_DEVICE, "BACnet SendSystemControl");
        if(L.ccu().systemProfile instanceof DefaultSystem)
            return;
        CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
        if(bacnetDevice != null)
            new SystemPoints(bacnetDevice);

    }
}
