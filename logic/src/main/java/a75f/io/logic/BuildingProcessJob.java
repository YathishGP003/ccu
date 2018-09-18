package a75f.io.logic;

import android.util.Log;

import a75f.io.logic.bo.building.Floor;
import a75f.io.logic.bo.building.Zone;

/**
 * Created by samjithsadasivan on 9/14/18.
 */

public class BuildingProcessJob extends BaseJob
{
    @Override
    public void doJob() {
        Log.d("CCU","do BuildingProcessJob");
    
        for(Floor floor : L.ccu().getFloors())
        {
            for(Zone zone : floor.mRoomList)
            {
                zone.updatePoints();
            }
        }
    
    }
}
