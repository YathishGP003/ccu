package a75f.io.logic;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import a75.io.algos.vav.VavTRSystem;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.bo.building.Floor;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.ZoneProfile;

import static a75f.io.logic.L.ccu;

/**
 * Created by samjithsadasivan on 9/14/18.
 */

public class BuildingProcessJob extends BaseJob
{
    HashMap<String, String> tsData;
    
    @Override
    public void doJob() {
        Log.d("CCU","BuildingProcessJob ->");
    
        tsData = new HashMap();
    
        for(Floor floor : L.ccu().getFloors())
        {
            for(Zone zone : floor.mZoneList)
            {
                //zone.updatePoints();
                for (ZoneProfile zp : zone.mZoneProfiles)
                {
                    Log.d("VAV"," updatePoints "+zp.getNodeAddresses());
                    zp.updateZonePoints();
                    HashMap<String, Double> tsdata = zp.getTSData();
                    if (tsdata != null) {
                        for (Map.Entry<String, Double> entry : tsdata.entrySet()) {
                            tsData.put(entry.getKey(),String.valueOf(entry.getValue()));
                        }
                    }
                }
            }
        }
    
        CCUHsApi.getInstance().syncEntityTree();
        
        uploadTimeSeriesData();
        Log.d("CCU","<- BuildingProcessJob");
    }
    
    private void uploadTimeSeriesData() {
    
        Log.d("CCU","uploadTimeSeriesData -> ");
    
        CCUHsApi.getInstance().syncHisData();
    
        ArrayList<HashMap> logPoints = CCUHsApi.getInstance().readAll("point and logical");
    
        for(HashMap point : logPoints) {
            String val = String.valueOf(CCUHsApi.getInstance().readHisValById(point.get("id").toString()));
            tsData.put(point.get("dis").toString(), val);
        }
        
        if (L.ccu().systemProfile.trSystem instanceof VavTRSystem)
        {
            VavTRSystem p = (VavTRSystem) ccu().systemProfile.trSystem;
            tsData.put("SAT", String.valueOf(p.getCurrentSAT()));
            tsData.put("CO2",String.valueOf(p.getCurrentCO2()));
            tsData.put("SP",String.valueOf(p.getCurrentSp()));
            tsData.put("HWST",String.valueOf(p.getCurrentHwst()));
        }
    
        String url = new InfluxDbUtil.URLBuilder().setProtocol(InfluxDbUtil.HTTPS)
                                                  .setHost("influx-a75f.aivencloud.com")
                                                  .setPort(27304)
                                                  .setOp(InfluxDbUtil.WRITE)
                                                  .setDatabse("defaultdb")
                                                  .setUser("avnadmin")
                                                  .setPassword("mhur2n42y4l58xlx")
                                                  .buildUrl();
        
        Log.d("CCU"," Write Influx "+tsData);
        InfluxDbUtil.writeData(url, "RENATUS_TEST", tsData, System.currentTimeMillis());
        Log.d("CCU","<- uploadTimeSeriesData ");
        
    }
}
