package a75f.io.renatus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import android.text.Editable;
import android.text.TextWatcher;
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
import a75f.io.constants.CcuFieldConstants;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.logic.CCUBaseConfigurationBuilder;
import a75f.io.domain.logic.DiagEquipConfigurationBuilder;
import a75f.io.domain.util.ModelLoader;
import a75f.io.logger.CcuLog;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.tuners.TunerEquip;
import a75f.io.renatus.registration.FreshRegistration;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.Prefs;
import io.seventyfivef.domainmodeler.client.ModelDirective;
import a75f.io.util.ExecutorTask;

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
    String addressBandSelected = "1000";
    ArrayList<String> regAddressBands = new ArrayList<>();
    ArrayList<String> addressBand = new ArrayList<>();
    HashMap site;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CCUUiUtil.setThemeDetails(this);
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

        site = CCUHsApi.getInstance().read("site");
        String ccuFmEmail = site.get(CcuFieldConstants.FACILITY_MANAGER_EMAIL) != null ? site.get(CcuFieldConstants.FACILITY_MANAGER_EMAIL).toString() : "";
        String ccuInstallerEmail = site.get(CcuFieldConstants.INSTALLER_EMAIL) != null ? site.get(CcuFieldConstants.INSTALLER_EMAIL).toString() : "";
        mInstallerEmailET.setText(ccuInstallerEmail);
        mManagerEmailET.setText(ccuFmEmail);
        mManagerEmailET.setEnabled(ccuFmEmail.isEmpty());
        mInstallerEmailET.setEnabled(ccuInstallerEmail.isEmpty());

        for (int addr = 1000; addr <= 10900; addr+=100)
        {
            addressBand.add(String.valueOf(addr));
        }
        ArrayAdapter<String> analogAdapter = new ArrayAdapter<>(this, R.layout.spinner_dropdown_item, addressBand);
        analogAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        
        mAddressBandSpinner.setAdapter(analogAdapter);
        ccu().setAddressBand((short)1000);
        mAddressBandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                CcuLog.d("CCU","AddressBandSelected : "+mAddressBandSpinner.getSelectedItem());
                if (i > 0)
                {
                    addressBandSelected = mAddressBandSpinner.getSelectedItem().toString();
                    L.ccu().setAddressBand(Short.parseShort(addressBandSelected));
                }
                
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
            }
        });

        mCCUNameET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No implementation
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No implementation
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(mCCUNameET.getText().toString().trim().isEmpty()){
                    mCCUNameET.setError("Enter CCU name");
                }
                if(CCUUiUtil.isInvalidName(mCCUNameET.getText().toString())) {
                    mCCUNameET.setError(getString(R.string.error_invalid_ccu_name));
                }
            }
        });

        mCreateNewCCU.setOnClickListener(v -> {
            String ccuName = mCCUNameET.getText().toString();
            String installerEmail = mInstallerEmailET.getText().toString();
            String managerEmail = mManagerEmailET.getText().toString();
            if (ccuName.trim().isEmpty()) {
                Toast.makeText(RegisterGatherCCUDetails.this, "Enter CCU name", Toast.LENGTH_SHORT).show();
                return;
            } else if (installerEmail.trim().isEmpty()) {
                Toast.makeText(RegisterGatherCCUDetails.this, "Enter Installer email", Toast.LENGTH_SHORT).show();
                return;
            } else if (managerEmail.trim().isEmpty()) {
                Toast.makeText(RegisterGatherCCUDetails.this, "Enter Manager email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!CCUUiUtil.isInvalidName(ccuName)) {
                mCreateNewCCU.setClickable(false);
                ArrayList<HashMap<Object, Object> >ccus = CCUHsApi.getInstance().readAllEntities("device and ccu");
                for(HashMap<Object, Object> ccu :ccus){
                    CCUHsApi.getInstance().deleteEntityLocally(ccu.get("id").toString());
                }
                L.saveCCUState();
                L.ccu().setCCUName(ccuName);
                String ccuId = getCcuId(ccuName, installerEmail, managerEmail);
                L.ccu().systemProfile = new DefaultSystem();
                CCUHsApi.getInstance().addOrUpdateConfigProperty(HayStackConstants.CUR_CCU, HRef.make(ccuId));
                CCUHsApi.getInstance().registerCcu(installerEmail);
                prefs.setString(CcuFieldConstants.INSTALLER_EMAIL, installerEmail);
                Domain.ccuEquip.updateAddressBand(addressBandSelected);
                L.ccu().setAddressBand(Short.parseShort(addressBandSelected));
                next();
            }else{
                Toast.makeText(RegisterGatherCCUDetails.this, "Please provide proper details",
                        Toast.LENGTH_SHORT).show();
            }
        });

        loadCCUs();
    }

    @NonNull
    private static String getCcuId(String ccuName, String installerEmail, String managerEmail) {
        DiagEquipConfigurationBuilder diagEquipConfigurationBuilder = new DiagEquipConfigurationBuilder(CCUHsApi.getInstance());
        CCUBaseConfigurationBuilder ccuBaseConfigurationBuilder = new CCUBaseConfigurationBuilder(CCUHsApi.getInstance());
        ModelDirective ccuBaseConfigurationModel = ModelLoader.INSTANCE.getCCUBaseConfigurationModel();
        String diagEquipId = diagEquipConfigurationBuilder.createDiagEquipAndPoints(ccuName);
        String ccuId = ccuBaseConfigurationBuilder.createCCUBaseConfiguration(ccuName,
                installerEmail, managerEmail, diagEquipId, ccuBaseConfigurationModel);
        return ccuId;
    }

    @SuppressLint("StaticFieldLeak")
    private void getRegisteredAddressBands() {
        regAddressBands.clear();
        ExecutorTask.executeAsync(
            () -> {
                HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
                String siteUID = CCUHsApi.getInstance().getSiteIdRef().toString();
                HDict tDict = new HDictBuilder().add("filter", "domainName == \"" +
                        DomainName.addressBand + "\" and siteRef == " + siteUID).toDict();
                HGrid schedulePoint = hClient.call("read", HGridBuilder.dictToGrid(tDict));
                if(schedulePoint == null) {
                    CcuLog.w("RegisterGatherCCUDetails","HGrid(schedulePoint) is null.");
                    RegisterGatherCCUDetails.this.runOnUiThread( () ->
                            Toast.makeText(RegisterGatherCCUDetails.this,
                                    "Couldn't find the node address, Please choose the node " +
                                            "address which is not used already.", Toast.LENGTH_LONG).show());
                    return;
                }
                Iterator it = schedulePoint.iterator();
                while (it.hasNext())
                {
                    HRow r = (HRow) it.next();
                    if (r.has("val")) {
                        regAddressBands.add(r.get("val").toString());
                    }
                }
            },
            () -> {
                for(int i = 0; i < regAddressBands.size(); i++) {
                    for(int j = 0; j < addressBand.size(); j++) {
                        if(regAddressBands.get(i).equals(addressBand.get(j))) {
                            addressBand.remove(regAddressBands.get(i));
                        }
                    }
                }
                ArrayAdapter<String> analogAdapter = new ArrayAdapter<>(RegisterGatherCCUDetails.this, R.layout.spinner_dropdown_item, addressBand);
                analogAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

                mAddressBandSpinner.setAdapter(analogAdapter);
                addressBandSelected = addressBand.get(0);
                L.ccu().setAddressBand(Short.parseShort(addressBandSelected));
            }
       );
    }

    private void loadCCUs() {

        ExecutorTask.executeAsync(
            () -> showProgressDialog(),
            () -> mCCUS = CCUHsApi.getInstance().getCCUs(),
            () -> {
                hideProgressDialog();
                if (mCCUS == null || mCCUS.isEmpty() || mCCUS.isErr()) {
                    //Toast.makeText(RegisterGatherCCUDetails.this, "No CCUs available, please create one", Toast.LENGTH_SHORT).show();
                } else {
                    showCCUButton();
                }

            }

        );
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
        ExecutorTask.executeAsync(
            () -> showProgressDialog(),
            () -> {
                if(!Globals.getInstance().siteAlreadyCreated()) {
                    TunerEquip.INSTANCE.initialize(CCUHsApi.getInstance(), false);
                    DefaultSchedules.setDefaultCoolingHeatingTemp();
                }
            },
            () -> {
                hideProgressDialog();
                prefs.setBoolean("CCU_SETUP", true);

                AlertDialog.Builder builder
                        = new AlertDialog
                        .Builder(RegisterGatherCCUDetails.this);
                builder.setMessage("Are you sure you want to add "+mCCUNameET.getText().toString()+" to site "+site.get("dis"));
                builder.setTitle("ADD CCU");
                builder.setCancelable(false);
                builder.setPositiveButton(
                                "Yes",
                                (dialog, which) -> {
                                    L.saveCCUState();
                                    CCUHsApi.getInstance().log();
                                    CCUHsApi.getInstance().syncEntityTree();
                                    // When the user click yes button
                                    // then app will close
                                    Intent i = new Intent(RegisterGatherCCUDetails.this,
                                            FreshRegistration.class);
                                    i.putExtra("viewpager_position", 21);
                                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(i);
                                    finish();
                                });

                builder.setNegativeButton("No",(dialog,which)->{
                    mCreateNewCCU.setClickable(true);
                    dialog.dismiss();
                });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        );
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
