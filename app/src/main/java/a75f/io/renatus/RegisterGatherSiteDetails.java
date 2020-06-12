package a75f.io.renatus;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.HashMap;
import java.util.TimeZone;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Site;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.tuners.BuildingTuners;

public class RegisterGatherSiteDetails extends Activity {


    /* Gather site details and add them to the local database,
       only when a user comes online and finishes registration
       does this database sync with the backend.
     */
    EditText             mSiteName;
    EditText             mSiteCity;
    EditText             mSiteZip;
    Spinner              mTimeZoneSelector;
    ArrayAdapter<String> timeZoneAdapter;

    private static final String TAG = RegisterGatherSiteDetails.class.getSimpleName();

    Button mNext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register_gather_site_details);

        mSiteName = (EditText) findViewById(R.id.site_name_et);
        mSiteCity = (EditText) findViewById(R.id.site_city_et);
        mSiteZip = (EditText) findViewById(R.id.site_zip_et);


        mNext = (Button) findViewById(R.id.next_button);
        mTimeZoneSelector = findViewById(R.id.timeZoneSelector);

        populateAndUpdateTimeZone();
        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNext.setEnabled(false);
                String siteName = mSiteName.getText().toString();
                String siteCity = mSiteCity.getText().toString();
                String siteZip = mSiteZip.getText().toString();

                new Thread() {
                    @Override
                    public void run() {
                        super.run();

                        saveSite(siteName, siteCity, siteZip);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                next();
                            }
                        });
                    }
                }.start();

            }
        });
    }
    
    private void populateAndUpdateTimeZone()
    {
        timeZoneAdapter= new ArrayAdapter <String> (this, android.R.layout.simple_spinner_item );
        timeZoneAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        String[] tzIds = TimeZone.getAvailableIDs();
        
        for(int i = 0; i < tzIds.length; i++) {
            timeZoneAdapter.add(tzIds[i]);
        }
        
        mTimeZoneSelector = findViewById(R.id.timeZoneSelector);
        mTimeZoneSelector.setAdapter(timeZoneAdapter);
        
        mTimeZoneSelector.setSelection(timeZoneAdapter.getPosition(TimeZone.getDefault().getID()));
    }

    private void next() {
        Intent i = new Intent(RegisterGatherSiteDetails.this,
        RegisterGatherCCUDetails.class);
        startActivity(i);
        finish();
    }

    public String saveSite(String siteName, String siteCity, String siteZip) {
        HashMap site = CCUHsApi.getInstance().read("site");
    
        String tzID = mTimeZoneSelector.getSelectedItem().toString();
        AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        am.setTimeZone(tzID);
        
        
        Site s75f = new Site.Builder()
                .setDisplayName(siteName)
                .addMarker("site")
                .setGeoCity(siteCity)
                .setGeoState("MN")
                .setTz(tzID.substring(tzID.lastIndexOf("/") + 1))//Haystack requires tz area string.
                .setGeoZip(siteZip)
                .setArea(10000).build();
        String localSiteId = CCUHsApi.getInstance().addSite(s75f);
        Log.i(TAG, "LocalSiteID: " + localSiteId + " tz " + s75f.getTz());
        BuildingTuners.getInstance();
        //SystemEquip.getInstance();
        Log.i(TAG, "LocalSiteID: " + localSiteId);
        CCUHsApi.getInstance().log();
        L.ccu().systemProfile = new DefaultSystem();
        CCUHsApi.getInstance().saveTagsData();
        return localSiteId;
    }


}
