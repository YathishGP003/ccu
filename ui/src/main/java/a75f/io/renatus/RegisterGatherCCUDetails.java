package a75f.io.renatus;

import android.app.Activity;
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
import android.widget.TextView;
import android.widget.Toast;

import org.projecthaystack.HGrid;
import org.projecthaystack.HRef;
import org.projecthaystack.io.HaystackToken;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.logic.L;
import a75f.io.logic.tuners.BuildingTuners;

public class RegisterGatherCCUDetails extends Activity {


    ProgressBar mProgressDialog;
    TextView mOrTextView;
    Button mUseExistingCCUButton;
    EditText mCCUNameET;
    EditText mInstallerEmailET;
    Button mCreateNewCCU;
    HGrid mCCUS;
    String mSiteId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_gather_ccu_details);
        mInstallerEmailET = findViewById(R.id.installer_email_et);
        mOrTextView = findViewById(R.id.or_textview);
        mUseExistingCCUButton = findViewById(R.id.use_existing_ccu);
        mProgressDialog = findViewById(R.id.progressbar);
        mCCUNameET = findViewById(R.id.ccu_name_et);
        mCreateNewCCU = findViewById(R.id.create_new);

        mCreateNewCCU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String installerEmail = mInstallerEmailET.getText().toString();
                String ccuName = mCCUNameET.getText().toString();
                String localId = CCUHsApi.getInstance().createCCU(ccuName, installerEmail);
                CCUHsApi.getInstance().addOrUpdateConfigProperty(HayStackConstants.CUR_CCU, HRef.make(localId));
                next();
            }
        });

        loadCCUs();
    }

    private void loadCCUs() {
        AsyncTask<Void, Void, HGrid> loadCCUGrids = new AsyncTask<Void, Void, HGrid>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showProgressDialog();
            }

            @Override
            protected HGrid doInBackground(Void... voids) {
                CCUHsApi.getInstance().getSiteId();


                return CCUHsApi.getInstance().getCCUs();
            }

            @Override
            protected void onPostExecute(HGrid hGrid) {
                super.onPostExecute(hGrid);
                hideProgressDialog();
                if (hGrid == null || hGrid.isEmpty() || hGrid.isErr()) {
                    Toast.makeText(RegisterGatherCCUDetails.this, "No CCUs available, please create one", Toast.LENGTH_SHORT).show();
                } else {
                    mCCUS = hGrid;
                    showCCUButton();
                }


            }
        };

        loadCCUGrids.execute();
    }

    private void showCCUButton() {
        mUseExistingCCUButton.setVisibility(View.VISIBLE);
        mOrTextView.setVisibility(View.VISIBLE);

        mUseExistingCCUButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CharSequence[] ccuStringArray = getCCUStringArray();
                AlertDialog.Builder builder = new AlertDialog.Builder(RegisterGatherCCUDetails.this);
                builder.setTitle(R.string.pick_ccu)
                        .setItems(ccuStringArray, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(RegisterGatherCCUDetails.this, mCCUS.row(which).dis(), Toast.LENGTH_SHORT).show();
                                CCUHsApi.getInstance().addOrUpdateConfigProperty(HayStackConstants.CUR_CCU, mCCUS.row(which).getRef("id"));
                                next();
                            }
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });
    }


    private void next() {

        AsyncTask<Void, Void, Void> saveState = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showProgressDialog();
            }

            @Override
            protected Void doInBackground(Void... voids) {
                BuildingTuners.getInstance();
                L.saveCCUState();
                CCUHsApi.getInstance().log();
                CCUHsApi.getInstance().syncEntityTree();


                return null;
            }

            @Override
            protected void onPostExecute(Void nul) {
                super.onPostExecute(nul);
                hideProgressDialog();
                Intent i = new Intent(RegisterGatherCCUDetails.this,
                        RenatusLandingActivity.class);

                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                finish();

            }
        };

        saveState.execute();




    }

    private CharSequence[] getCCUStringArray() {

        int numberOfCCUs = mCCUS.numRows();
        CharSequence[] ccuNames = new CharSequence[numberOfCCUs];
        for (int i = 0; i < numberOfCCUs; i++) {
            ccuNames[i] = mCCUS.row(i).dis();
        }

        return ccuNames;
    }

    private void showProgressDialog() {

            mProgressDialog.setVisibility(View.VISIBLE);
    }

    private void hideProgressDialog() {

            mProgressDialog.setVisibility(View.INVISIBLE);

    }


}
