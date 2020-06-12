package a75f.io.renatus;

import a75f.io.api.haystack.BuildConfig;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.constants.CcuFieldConstants;
import a75f.io.constants.HttpConstants;
import a75f.io.constants.SiteFieldConstants;
import a75f.io.logger.CcuLog;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.projecthaystack.HGrid;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.DefaultSystem;

public class RegisterGatherDetails extends Activity {

    ProgressBar mProgressDialog;
    Button mUseExistingSiteButton;
    Button noSiteButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register_gather_details);
        Globals.getInstance().setSiteAlreadyCreated(false);
        EditText siteIdEditText = (EditText) findViewById(R.id.site_id_edittext);
        noSiteButton = (Button) findViewById(R.id.no_site_button);
        mProgressDialog = (ProgressBar) findViewById(R.id.progressbar);
        mUseExistingSiteButton = (Button) findViewById(R.id.use_existing_site_button);
        mUseExistingSiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
                if (siteIdEditText.getText().toString().trim().length() != 24) {//TODO-TEMP
                    Toast.makeText(getApplicationContext(), "Invalid site ID",Toast.LENGTH_SHORT).show();
                    return;
                }
                loadExistingSite(siteIdEditText.getText().toString());
            }
        });

        noSiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(RegisterGatherDetails.this,
                        RegisterGatherSiteDetails.class);
                startActivity(i);
                finish();
            }
        });


    }

    private void showProgressDialog() {
            mProgressDialog.setVisibility(View.VISIBLE);
    }

    private void hideProgressDialog() {
            mProgressDialog.setVisibility(View.INVISIBLE);
    }


    public void loadExistingSite(String siteId) {
        
        //TODO: harden for null string.
        AsyncTask<String, Void, String> loadSiteAsyncTask = new AsyncTask<String, Void, String>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                RegisterGatherDetails.this.showProgressDialog();

            }

            @Override
            protected String doInBackground(String... strings) {
                String siteId = strings[0];
                String siteIdResponse = null;

                if (StringUtils.isNotBlank(siteId)) {
                    String httpResponse = HttpUtil.executeJson(
                            CCUHsApi.getInstance().getAuthenticationUrl() + "sites/" + siteId,
                            null, BuildConfig.CARETAKER_API_KEY,
                            true, HttpConstants.HTTP_METHOD_GET
                    );
                    if (StringUtils.isNotBlank(httpResponse)) {
                        siteIdResponse = getSiteIdFromJson(httpResponse);
                    }
                } else {
                    Toast.makeText(RegisterGatherDetails.this, "Unable to load site provided. Please try again or provide a different site ID.", Toast.LENGTH_LONG).show();
                }

                return siteIdResponse;
            }

            @Override
            protected void onPostExecute(String siteId) {
                super.onPostExecute(siteId);
                RegisterGatherDetails.this.hideProgressDialog();

                if (StringUtils.isNotBlank(siteId)) {
                    Toast.makeText(RegisterGatherDetails.this, "Site returned", Toast.LENGTH_LONG).show();
                    showSiteDialog(siteId);
                } else {
                    Toast.makeText(RegisterGatherDetails.this, "Site not found", Toast.LENGTH_LONG).show();
                }
            }
        };

        loadSiteAsyncTask.execute(siteId);
    }

    private String getSiteIdFromJson(String loadSiteResponse) {
        String siteId = null;

        try {
            JSONObject loadSiteJsonResponse = new JSONObject(loadSiteResponse);
            if (loadSiteJsonResponse != null) {
                siteId = loadSiteJsonResponse.getString(SiteFieldConstants.ID);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            siteId = null;
        }

        return siteId;
    }

    private void showSiteDialog(String siteId) {

        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterGatherDetails.this);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                saveExistingSite(siteId);
                Toast.makeText(RegisterGatherDetails.this, "Thank you for confirming using this site", Toast.LENGTH_LONG).show();

            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Toast.makeText(RegisterGatherDetails.this, "Canceled use of existing site", Toast.LENGTH_LONG).show();
            }
        });

        builder.setTitle("Site");
        builder.setMessage("Registering for site ID " + siteId);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void saveExistingSite(String siteId) {

        CcuLog.d("ADD_CCU_EXISTING","Existing site ID used to register for existing site is: " + siteId);

        AsyncTask<String, Void, Boolean> syncSiteTask = new AsyncTask<String, Void, Boolean>() {
    
            ProgressDialog progressDlg = new ProgressDialog(getApplicationContext());

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                RegisterGatherDetails.this.showProgressDialog();
                mUseExistingSiteButton.setEnabled(false);
                noSiteButton.setEnabled(false);
                
            }

            @Override
            protected Boolean doInBackground(String... strings) {
                String siteId = strings[0];
                boolean retVal = false;
                if (StringUtils.isNotBlank(siteId)) {
                    retVal = CCUHsApi.getInstance().syncExistingSite(siteId);
                    Globals.getInstance().setSiteAlreadyCreated(true);
                    L.ccu().systemProfile = new DefaultSystem();
                }
                return retVal;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                super.onPostExecute(success);
                RegisterGatherDetails.this.hideProgressDialog();

                if (success) {
                    Toast.makeText(RegisterGatherDetails.this, "Synchronizing the site with the 75F Cloud was successful.", Toast.LENGTH_LONG).show();
                    navigateToCCUScreen();
                } else {
                    Toast.makeText(RegisterGatherDetails.this, "Synchronizing the site with the 75F Cloud was not successful. Please try choosing a different site for registering this CCU.", Toast.LENGTH_LONG).show();
                    mUseExistingSiteButton.setEnabled(true);
                    noSiteButton.setEnabled(true);
                }
            }
        };

        syncSiteTask.execute(siteId);
    }

    private void navigateToCCUScreen() {
        Intent intent = new Intent(RegisterGatherDetails.this, RegisterGatherCCUDetails.class);
        startActivity(intent);
    }
}
