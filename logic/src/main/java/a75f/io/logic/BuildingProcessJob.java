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
    
        if (!Globals.getInstance().isPubnubSubscribed())
        {
            CCUHsApi.getInstance().syncEntityTree();
            HashMap site = CCUHsApi.getInstance().read("site");
            String siteLUID = site.get("id").toString();
            String siteGUID = CCUHsApi.getInstance().getGUID(siteLUID);
            if (siteGUID != null && siteGUID != "") {
                Globals.getInstance().registerSiteToPubNub(siteGUID);
            }
        }
    
        new Thread()
        {
            @Override
            public void run()
            {
                super.run();
                uploadTimeSeriesData();
                
            }
        }.start();
        Log.d("CCU","<- BuildingProcessJob");
    }
    
    private void uploadTimeSeriesData() {
    
        Log.d("CCU","uploadTimeSeriesDataJob -> ");
    
        CCUHsApi.getInstance().syncHisData();
        
        if (L.ccu().systemProfile.trSystem instanceof VavTRSystem)
        {
            VavTRSystem p = (VavTRSystem) ccu().systemProfile.trSystem;
            tsData.put("TR_SAT", String.valueOf(p.getCurrentSAT()));
            tsData.put("TR_CO2",String.valueOf(p.getCurrentCO2()));
            tsData.put("TR_SP",String.valueOf(p.getCurrentSp()));
            tsData.put("TR_HWST",String.valueOf(p.getCurrentHwst()));
        }
        
        if (L.ccu().systemProfile != null)
        {
            tsData.put("Analog1OutSignal", String.valueOf(10 * L.ccu().systemProfile.analog1OutSignal));
            tsData.put("Analog2OutSignal", String.valueOf(10 * L.ccu().systemProfile.analog2OutSignal));
            tsData.put("Analog3OutSignal", String.valueOf(10 * L.ccu().systemProfile.analog3OutSignal));
            tsData.put("Analog4OutSignal", String.valueOf(10 * L.ccu().systemProfile.analog4OutSignal));
        }
        
        ArrayList<HashMap> points = CCUHsApi.getInstance().readAll("point and writable");
        for (Map m : points)
        {
            ArrayList values = CCUHsApi.getInstance().readPoint(m.get("id").toString());
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    tsData.put(m.get("dis").toString(), valMap.get("val").toString());
                }
            }
        }
        
        String url = new a75f.io.api.haystack.sync.InfluxDbUtil.URLBuilder().setProtocol(a75f.io.api.haystack.sync.InfluxDbUtil.HTTP)
                                                                            .setHost("renatus-influxiprvgkeeqfgys.centralus.cloudapp.azure.com")
                                                                            .setPort(8086)
                                                                            .setOp(a75f.io.api.haystack.sync.InfluxDbUtil.WRITE)
                                                                            .setDatabse("haystack")
                                                                            .setUser("75f@75f.io")
                                                                            .setPassword("7575")
                                                                            .buildUrl();
    
        a75f.io.api.haystack.sync.InfluxDbUtil.writeData(url, "01RENATUS_CCU" , tsData, System.currentTimeMillis());
    
        /*String url = new InfluxDbUtil.URLBuilder().setProtocol(InfluxDbUtil.HTTPS)
                                                  .setHost("influx-a75f.aivencloud.com")
                                                  .setPort(27304)
                                                  .setOp(InfluxDbUtil.WRITE)
                                                  .setDatabse("defaultdb")
                                                  .setUser("avnadmin")
                                                  .setPassword("mhur2n42y4l58xlx")
                                                  .buildUrl();
        
        Log.d("CCU"," Write Influx "+tsData);
        InfluxDbUtil.writeData(url, "RENATUS_TEST", tsData, System.currentTimeMillis());*/
        Log.d("CCU","<- uploadTimeSeriesDataJob ");
        
    }
}
