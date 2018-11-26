package a75f.io.renatus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Site;
import a75f.io.logic.tuners.BuildingTuners;

public class RegisterGatherSiteDetails extends Activity {


    /* Gather site details and add them to the local database,
       only when a user comes online and finishes registration
       does this database sync with the backend.
     */
    EditText mSiteName;
    EditText mSiteCity;
    EditText mSiteZip;
    EditText mCCUName;
    EditText mInstallerEmail;

    private static final String TAG = RegisterGatherSiteDetails.class.getSimpleName();

    Button mNext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register_gather_site_details);


        mSiteName = (EditText) findViewById(R.id.site_name_et);
        mSiteCity = (EditText) findViewById(R.id.site_city_et);
        mSiteZip = (EditText) findViewById(R.id.site_zip_et);
        mInstallerEmail = (EditText) findViewById(R.id.installer_email_et);
        mCCUName = (EditText) findViewById(R.id.ccu_name_et);

        mNext = (Button) findViewById(R.id.next_button);

        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String siteName = mSiteName.getText().toString();
                String siteCity = mSiteCity.getText().toString();
                String siteZip = mSiteZip.getText().toString();

                new Thread()
                {
                    @Override
                    public void run()
                    {
                        super.run();

                        saveSite(siteName, siteCity, siteZip);
                        saveCCU(mInstallerEmail, mCCUName);

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

    private void saveCCU(EditText mInstallerEmail, EditText mCCUName) {

    }

    private void next() {
        Intent i = new Intent(RegisterGatherSiteDetails.this,
                RenatusLandingActivity.class);


        startActivity(i);
    }

    /* This site never existed we are creating a new orphaned site. */
    public String saveSite(String siteName, String siteCity, String siteZip) {
        HashMap site = CCUHsApi.getInstance().read("site");

        Site s75f = new Site.Builder()
                .setDisplayName(siteName)
                .addMarker("site")
                .addMarker("orphan")
                .setGeoCity(siteCity)
                .setGeoState("MN")
                .setTz("Chicago")
                .setGeoZip(siteZip)
                .setArea(10000).build();
        String localSiteId = CCUHsApi.getInstance().addSite(s75f);

        Log.i(TAG, "LocalSiteID: " + localSiteId);
        CCUHsApi.getInstance().log();
        BuildingTuners.getInstance();//To init Building tuner
        CCUHsApi.getInstance().syncEntityTree();
        return localSiteId;
    }


    public void loadExistingSite(String siteId) {


        String siteIdAzure = "123";  //Azure ID
        //Get this site from azure.
        //Site azureSite = CCUHsApi.getInstance().getSiteFromAzure();

        //Pre-Populate these fields
        //Set it as an orphan site and orphan ccu

    }

}
