package a75f.io.renatus;

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
        //TODO: let's start with the no site button,
        //The site doesn't have a preconfig token and it doesn't have a
        //site id.
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
        AsyncTask<String, Void, HGrid> getHSClientTask = new AsyncTask<String, Void, HGrid>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                RegisterGatherDetails.this.showProgressDialog();

            }

            @Override
            protected HGrid doInBackground(String... strings) {
                HGrid hGrid = CCUHsApi.getInstance().getRemoteSite(strings[0]);

                if (hGrid == null || hGrid.isEmpty()) {
                    return null;
                } else {
                    return hGrid;
                }
            }

            @Override
            protected void onPostExecute(HGrid hGrid) {
                super.onPostExecute(hGrid);
                RegisterGatherDetails.this.hideProgressDialog();
                if (hGrid == null || hGrid.isEmpty()) {
                    Toast.makeText(RegisterGatherDetails.this, "No Site returned!", Toast.LENGTH_LONG).show();
                } else if (!hGrid.row(0).has("site")) {
                    Toast.makeText(RegisterGatherDetails.this, "Not a valid site returned!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(RegisterGatherDetails.this, "Site returned!", Toast.LENGTH_LONG).show();
                    showSiteDialog(hGrid);

                }
            }
        };


        getHSClientTask.execute(siteId);
    }

    private void showSiteDialog(HGrid hGrid) {

        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterGatherDetails.this);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {


                saveExistingSite(hGrid);
                Toast.makeText(RegisterGatherDetails.this, "Using this site!", Toast.LENGTH_LONG).show();
                // CCUHsApi.getInstance().addExistingSite(hGrid);
                // navigateToCCUScreen();

            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Toast.makeText(RegisterGatherDetails.this, "Canceled using this site!", Toast.LENGTH_LONG).show();
            }
        });

        builder.setTitle("Site");
        builder.setMessage(hGrid.row(0).toZinc());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void saveExistingSite(HGrid hGrid) {

        String siteIdVal = hGrid.row(0).getRef("id").val;
        System.out.println("Site ID val: " + siteIdVal);

        AsyncTask<String, Void, Boolean> syncSiteTask = new AsyncTask<String, Void, Boolean>() {
    
            ProgressDialog progressDlg = new ProgressDialog(getApplicationContext());

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                RegisterGatherDetails.this.showProgressDialog();
                //progressDlg.setMessage("Fetching Site data");
                //progressDlg.show();
    
                mUseExistingSiteButton.setEnabled(false);
                noSiteButton.setEnabled(false);
                
            }

            @Override
            protected Boolean doInBackground(String... strings) {
                String siteId = strings[0];
                boolean retVal = CCUHsApi.getInstance().syncExistingSite(siteId);
                Globals.getInstance().setSiteAlreadyCreated(true);
                //BuildingTuners.getInstance();
                L.ccu().systemProfile = new DefaultSystem();
                return retVal;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                super.onPostExecute(success);
                RegisterGatherDetails.this.hideProgressDialog();

                if (!success) {
                    Toast.makeText(RegisterGatherDetails.this, "The site failed to sync.", Toast.LENGTH_LONG).show();
                    mUseExistingSiteButton.setEnabled(true);
                    noSiteButton.setEnabled(true);
                    return;
                }

                Toast.makeText(RegisterGatherDetails.this, "Sync successful.", Toast.LENGTH_LONG).show();
                navigateToCCUScreen();
            }
        };

        syncSiteTask.execute(siteIdVal);
    }

    private void navigateToCCUScreen() {
        Intent intent = new Intent(RegisterGatherDetails.this, RegisterGatherCCUDetails.class);
        startActivity(intent);
    }
}
