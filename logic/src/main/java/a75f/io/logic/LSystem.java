package a75f.io.logic;

import android.util.Log;

import java.util.ArrayList;

import a75f.io.bo.building.Floor;
import a75f.io.bo.building.VAVSystemProfile;
import a75f.io.bo.building.VavProfile;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.ZoneProfile;

import static a75f.io.bo.building.definitions.ProfileType.VAV;
import static a75f.io.logic.L.ccu;

/**
 * Created by samjithsadasivan on 6/4/18.
 */

public class LSystem
{
    
    public static void handleSystemControl() {
        Log.d("VAV", "handleSystemControl");
        ccu().systemProfile.doSystemControl();
        doTempLoggingStuff();
    }
    
    //TODO- TEMP
    public static void doTempLoggingStuff()
    {
        if (L.ccu().systemProfile instanceof VAVSystemProfile)
        {
            VAVSystemProfile p = (VAVSystemProfile) L.ccu().systemProfile;
            
            ArrayList<String> data = new ArrayList<>();
            
            data.add(String.valueOf(p.minuteCounter));
            
            
            for (Floor floor : ccu().getFloors())
            {
                for (Zone zone : floor.mRoomList)
                {
                    for (ZoneProfile zp : zone.mZoneProfiles)
                    {
                        for (short node : zp.getNodeAddresses())
                        {
                            if (zp.getProfileType() == VAV) {
                                Log.i("VAV", "Add VAV CSV Logs");
                                VavProfile v = (VavProfile) zp;
                                data.add(String.valueOf(v.getSATRequest().requestHours));
                                
                            }
                            
                        }
                    }
                }
            }
            
            data.add(String.valueOf(p.getCurrentSAT()));
            p.csvLogger.writeRowData(data);
            p.csvLogger.dump();
            p.minuteCounter++;
        }
    }
}
