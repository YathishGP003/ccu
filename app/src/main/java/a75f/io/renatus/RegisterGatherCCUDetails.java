package a75f.io.renatus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.client.HClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.SettingPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.diag.DiagEquip;
import a75f.io.logic.tuners.BuildingTuners;
import a75f.io.renatus.registration.FreshRegistration;
import a75f.io.renatus.util.Prefs;

import static a75f.io.logic.L.ccu;

public class RegisterGatherCCUDetails extends Activity {


    ProgressBar mProgressDialog;
    TextView    mOrTextView;
    Button      mUseExistingCCUButton;
    EditText    mCCUNameET;
    EditText    mInstallerEmailET;
    EditText    mManagerEmailET;
    Spinner     mAddressBandSpinner;
    Button      mCreateNewCCU;
    HGrid       mCCUS;
    Prefs prefs;
    String      mSiteId;
    String addressBandSelected = "1000";
    ArrayList<String> regAddressBands = new ArrayList<>();
    ArrayList<String> addressBand = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_gather_ccu_details);

        prefs = new Prefs(getApplicationContext());
        getRegisteredAddressBands();
        mInstallerEmailET = findViewById(R.id.installer_email_et);
        mManagerEmailET = findViewById(R.id.manager_email_et);
        mOrTextView = findViewById(R.id.or_textview);
        mUseExistingCCUButton = findViewById(R.id.use_existing_ccu);
        mAddressBandSpinner = findViewById(R.id.addressBandSpinner);
        mProgressDialog = findViewById(R.id.progressbar);
        mCCUNameET = findViewById(R.id.ccu_name_et);
        mCreateNewCCU = findViewById(R.id.create_new);

        HashMap site = CCUHsApi.getInstance().read("site");
        String ccuFmEmail = site.get("fmEmail") != null ? site.get("fmEmail").toString() : "";
        String ccuInstallerEmail = site.get("installerEmail") != null ? site.get("installerEmail").toString() : "";
        mInstallerEmailET.setText(ccuInstallerEmail);
        mManagerEmailET.setText(ccuFmEmail);
        mManagerEmailET.setEnabled(ccuFmEmail.isEmpty());
        mInstallerEmailET.setEnabled(ccuInstallerEmail.isEmpty());

        for (int addr = 1000; addr <= 10000; addr+=100)
        {
            addressBand.add(String.valueOf(addr));
        }
        ArrayAdapter<String> analogAdapter = new ArrayAdapter<String>(this, R.layout.spinner_dropdown_item, addressBand);
        analogAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        
        mAddressBandSpinner.setAdapter(analogAdapter);
        ccu().setSmartNodeAddressBand((short)1000);
        mAddressBandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                Log.d("CCU","AddressBandSelected : "+mAddressBandSpinner.getSelectedItem());
                if (i > 0)
                {
                    addressBandSelected = mAddressBandSpinner.getSelectedItem().toString();
                    L.ccu().setSmartNodeAddressBand(Short.parseShort(addressBandSelected));
                }
                
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
            }
        });

        mCreateNewCCU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ccuName = mCCUNameET.getText().toString();
                String installerEmail = mInstallerEmailET.getText().toString();
                String managerEmail = mManagerEmailET.getText().toString();
                if (ccuName.trim().length() == 0) {
                    Toast.makeText(RegisterGatherCCUDetails.this, "Enter CCU name", Toast.LENGTH_SHORT).show();
                    return;
                } else if (installerEmail.trim().length() == 0) {
                    Toast.makeText(RegisterGatherCCUDetails.this, "Enter Installer email", Toast.LENGTH_SHORT).show();
                    return;
                } else if (managerEmail.trim().length() == 0) {
                    Toast.makeText(RegisterGatherCCUDetails.this, "Enter Manager email", Toast.LENGTH_SHORT).show();
                    return;
                }


                L.ccu().setCCUName(ccuName);
                String localId = CCUHsApi.getInstance().createCCU(ccuName, installerEmail, DiagEquip.getInstance().create(), managerEmail);
                CCUHsApi.getInstance().addOrUpdateConfigProperty(HayStackConstants.CUR_CCU, HRef.make(localId));
                CCUHsApi.getInstance().registerCcu(installerEmail);
                prefs.setString("installerEmail", installerEmail);

                HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
                SettingPoint snBand = new SettingPoint.Builder()
                                                        .setDeviceRef(localId)
                                                        .setSiteRef(siteMap.get("id").toString())
                                                        .setDisplayName(ccuName+"-smartNodeBand")
                                                        .addMarker("snband").addMarker("sp").setVal(addressBandSelected).build();
                CCUHsApi.getInstance().addPoint(snBand);
                next();
            }
        });

        loadCCUs();
    }

    @SuppressLint("StaticFieldLeak")
    private void getRegisteredAddressBands() {
        regAddressBands.clear();
        new AsyncTask<String, Void,Void>(){

            @Override
            protected Void doInBackground(String... strings) {
                HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
                HashMap site = CCUHsApi.getInstance().read("site");
                String siteLUID = site.get("id").toString();
                String siteGUID = CCUHsApi.getInstance().getGUID(siteLUID);
                HDict tDict = new HDictBuilder().add("filter", "equip and group and siteRef == " + siteGUID).toDict();
                HGrid schedulePoint = hClient.call("read", HGridBuilder.dictToGrid(tDict));
                Iterator it = schedulePoint.iterator();
                while (it.hasNext())
                {
                    HRow r = (HRow) it.next();

                    if (r.getStr("group") != null) {
                        regAddressBands.add(r.getStr("group"));
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                //remove registered SmartNodeAddressBand
                for(int i = 0; i < regAddressBands.size(); i++)
                {
                    for(int j = 0; j < addressBand.size(); j++)
                    {
                        if(regAddressBands.get(i).equals(addressBand.get(j)))
                        {
                            addressBand.remove(regAddressBands.get(i));
                        }
                    }
                }
                ArrayAdapter<String> analogAdapter = new ArrayAdapter<String>(RegisterGatherCCUDetails.this, R.layout.spinner_dropdown_item, addressBand);
                analogAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

                mAddressBandSpinner.setAdapter(analogAdapter);
                addressBandSelected = addressBand.get(0);
                L.ccu().setSmartNodeAddressBand(Short.parseShort(addressBandSelected));
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
                //CCUHsApi.getInstance().getSiteId();
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

        loadCCUGrids.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void showCCUButton() {
        mUseExistingCCUButton.setVisibility(View.VISIBLE);
        mOrTextView.setVisibility(View.VISIBLE);

        mUseExistingCCUButton.setOnClickListener(v ->
                                                 {

                                                     CharSequence[] ccuStringArray = getCCUStringArray();
                                                     AlertDialog.Builder builder = new AlertDialog.Builder(RegisterGatherCCUDetails.this);
                                                     builder.setTitle(R.string.pick_ccu)
                                                             .setItems(ccuStringArray, (dialog, which) ->
                                                             {
                                                                 Toast.makeText(RegisterGatherCCUDetails.this, mCCUS.row(which).dis(), Toast.LENGTH_SHORT).show();
                                                                 CCUHsApi.getInstance().addOrUpdateConfigProperty(HayStackConstants.CUR_CCU, mCCUS.row(which).getRef("id"));
                                                                 next();
                                                             });
                                                     AlertDialog alertDialog = builder.create();
                                                     alertDialog.show();
                                                 });
    }


    private void next() {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showProgressDialog();
            }

            @Override
            protected Void doInBackground(Void... voids) {

                if(!Globals.getInstance().siteAlreadyCreated()) {
                    BuildingTuners.getInstance();
                    DefaultSchedules.setDefaultCoolingHeatingTemp();
                    DefaultSchedules.generateDefaultSchedule(false, null);
                }

                L.saveCCUState();
                CCUHsApi.getInstance().log();
                CCUHsApi.getInstance().syncEntityTree();


                return null;
            }

            @Override
            protected void onPostExecute(Void nul) {
                super.onPostExecute(nul);
                hideProgressDialog();
                prefs.setBoolean("CCU_SETUP", true);
                Intent i = new Intent(RegisterGatherCCUDetails.this,
                        FreshRegistration.class);
                i.putExtra("viewpager_position", 9);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                finish();

            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

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
