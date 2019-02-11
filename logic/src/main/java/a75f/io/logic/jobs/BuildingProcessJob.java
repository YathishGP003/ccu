package a75f.io.logic.jobs;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import a75.io.algos.vav.VavTRSystem;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HisItem;
import a75f.io.logic.BaseJob;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
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
    
        HashMap site = CCUHsApi.getInstance().read("site");
        if (site.size() == 0) {
            Log.d("CCU","No Site Registered ! <-BuildingProcessJob ");
            return;
        }
    
        HashMap ccu = CCUHsApi.getInstance().read("ccu");
        if (ccu.size() == 0) {
            Log.d("CCU","No CCU Registered ! <-BuildingProcessJob ");
            return;
        }
    
        tsData = new HashMap();
        
        for (ZoneProfile profile : L.ccu().zoneProfiles) {
            Log.d("CCU", "updateZonePoints -> "+profile.getProfileType());
            profile.updateZonePoints();
        }
    
        if (!Globals.getInstance().isPubnubSubscribed())
        {
            CCUHsApi.getInstance().syncEntityTree();
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
                CCUHsApi.getInstance().syncHisData();
                L.ccu().systemProfile.doSystemControl();
                L.saveCCUState();
                
                /*if (Globals.getInstance().getApplicationContext().getResources().getBoolean(R.bool.write_ts))
                {
                    uploadTimeSeriesData();
                }*/
                
            }
        }.start();
        Log.d("CCU","<- BuildingProcessJob");
    }
    
    //TODO - TEST
    private void uploadTimeSeriesData() {
    
        Log.d("CCU","uploadTimeSeriesDataJob -> ");
        
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
            tsData.put("Analog1OutSignal", String.valueOf(L.ccu().systemProfile.getAnalog1Out()));
            tsData.put("Analog2OutSignal", String.valueOf(L.ccu().systemProfile.getAnalog2Out()));
            tsData.put("Analog3OutSignal", String.valueOf(L.ccu().systemProfile.getAnalog3Out()));
            tsData.put("Analog4OutSignal", String.valueOf(L.ccu().systemProfile.getAnalog4Out()));
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
    
        ArrayList<HashMap> hisPoints = CCUHsApi.getInstance().readAll("point and his");
        for (Map m : hisPoints)
        {
            String pointID = m.get("id").toString();
            if (CCUHsApi.getInstance().getGUID(pointID) != null)
            {
                HisItem sItem = CCUHsApi.getInstance().curRead(pointID);
                tsData.put(m.get("dis").toString(), String.valueOf(sItem.getVal()));
            }
        }
        /*String url = new a75f.io.api.haystack.sync.InfluxDbUtil.URLBuilder().setProtocol(a75f.io.api.haystack.sync.InfluxDbUtil.HTTP)
                                                                            .setHost("renatus-influxiprvgkeeqfgys.centralus.cloudapp.azure.com")
                                                                            .setPort(8086)
                                                                            .setOp(a75f.io.api.haystack.sync.InfluxDbUtil.WRITE)
                                                                            .setDatabse("haystack")
                                                                            .setUser("75f@75f.io")
                                                                            .setPassword("7575")
                                                                            .buildUrl();
    
        a75f.io.api.haystack.sync.InfluxDbUtil.writeData(url, "03RENATUS_CCU" , tsData, System.currentTimeMillis());*/
    
        /*String url = new InfluxDbUtil.URLBuilder().setProtocol(InfluxDbUtil.HTTPS)
                                                  .setHost("influx-a75f.aivencloud.com")
                                                  .setPort(27304)
                                                  .setOp(InfluxDbUtil.WRITE)
                                                  .setDatabse("defaultdb")
                                                  .setUser("avnadmin")
                                                  .setPassword("mhur2n42y4l58xlx")
                                                  .buildUrl();
        
        InfluxDbUtil.writeData(url, "RENATUS_DEMO", tsData, System.currentTimeMillis());*/
        Log.d("CCU","<- uploadTimeSeriesDataJob ");
        
    }
}
