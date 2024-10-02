package a75f.io.renatus.registration;

import static com.raygun.raygun4android.RaygunClient.getApplicationContext;
import static a75f.io.logic.L.ccu;
import static a75f.io.renatus.util.CCUUtils.updateMigrationDiagWithAppVersion;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputLayout;

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
import a75f.io.logger.CcuLog;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.diag.DiagEquip;
import a75f.io.renatus.R;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.RxjavaUtil;
import a75f.io.util.ExecutorTask;

public class RegisterCCUToExistingSite extends DialogFragment {

    TextInputLayout mTextInputCCUName;
    EditText mCCUName;
    Spinner addressBandSpinner;
    EditText    mCCUNameET;
    EditText    mInstallerEmailET;
    EditText    mManagerEmailET;
    TextView mAddCCU;
    Prefs prefs;
    String addressBandSelected = "1000";
    ArrayList<String> regAddressBands = new ArrayList<>();
    ArrayList<String> addressBand = new ArrayList<>();
    HashMap<Object, Object> site;
    View toastWarning;

    public RegisterCCUToExistingSite() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_register_c_c_u_to_existing_site, container, false);

        mTextInputCCUName = rootView.findViewById(R.id.textInputCCUName);
        mCCUName = rootView.findViewById(R.id.editCCUName);
        addressBandSpinner = rootView.findViewById(R.id.addressBandSpinner);
        mInstallerEmailET = rootView.findViewById(R.id.installer_email_et);
        mManagerEmailET = rootView.findViewById(R.id.facility_manager_email_et);
        mCCUNameET = rootView.findViewById(R.id.editCCUName);
        mAddCCU = rootView.findViewById(R.id.add_ccu);

        prefs = new Prefs(getApplicationContext());
        getRegisteredAddressBand();
        site = CCUHsApi.getInstance().readEntity("site");
        String ccuFmEmail = site.get("fmEmail") != null ? site.get("fmEmail").toString() : "";
        String ccuInstallerEmail = site.get("installerEmail") != null ? site.get("installerEmail").toString() : "";
        mInstallerEmailET.setText(ccuInstallerEmail);
        mManagerEmailET.setText(ccuFmEmail);
        mManagerEmailET.setEnabled(ccuFmEmail.isEmpty());
        mInstallerEmailET.setEnabled(ccuInstallerEmail.isEmpty());

        toastWarning = getLayoutInflater().inflate(R.layout.custom_toast_layout_warning, rootView.findViewById(R.id.custom_toast_layout_warning));

        for (int addr = 1000; addr <= 10900; addr+=100) {
            addressBand.add(String.valueOf(addr));
        }
        ArrayAdapter<String> analogAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.spinner_dropdown_item, addressBand);
        analogAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        addressBandSpinner.setAdapter(analogAdapter);
        ccu().setSmartNodeAddressBand((short)1000);
        addressBandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                CcuLog.d(L.TAG_CCU,"AddressBandSelected : "+addressBandSpinner.getSelectedItem());
                if (i >= 0)
                {
                    addressBandSelected = addressBandSpinner.getSelectedItem().toString();
                    if(regAddressBands.contains(addressBandSelected)) {
                        Toast toast = new Toast(Globals.getInstance().getApplicationContext());
                        toast.setGravity(Gravity.BOTTOM, 50, 50);
                        toast.setView(toastWarning);
                        toast.setDuration(Toast.LENGTH_LONG);
                        toast.show();
                    }
                    L.ccu().setSmartNodeAddressBand(Short.parseShort(addressBandSelected));
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
            }
        });

        mAddCCU.setOnClickListener(v -> {
            String ccuName = mCCUNameET.getText().toString();
            String installerEmail = mInstallerEmailET.getText().toString();
            String managerEmail = mManagerEmailET.getText().toString();
            if (ccuName.trim().isEmpty()) {
                Toast.makeText(getApplicationContext(), "Enter CCU name", Toast.LENGTH_SHORT).show();
                return;
            } else if (installerEmail.trim().isEmpty()) {
                Toast.makeText(getApplicationContext(), "Enter Installer email", Toast.LENGTH_SHORT).show();
                return;
            } else if (managerEmail.trim().isEmpty()) {
                Toast.makeText(getApplicationContext(), "Enter Manager email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!CCUUiUtil.isInvalidName(ccuName)) {
                next(ccuName, installerEmail, managerEmail);
            }else{
                Toast.makeText(getApplicationContext(), "Please provide proper details",
                        Toast.LENGTH_SHORT).show();
            }
        });

        mTextInputCCUName.setErrorEnabled(true);
        mCCUName.setHint(getHTMLCodeForHints(R.string.ccu_name));
        mTextInputCCUName.setError(getString(R.string.ccu_nameq));


        ArrayList<String> spinnerArray = new ArrayList<>();
        for (int i = 1; i < 10; i ++) {
            spinnerArray.add(String.valueOf(i));
        }
        mCCUName.addTextChangedListener(new TextWatcher() {
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
                if (!mCCUName.getText().toString().trim().isEmpty()) {
                    if(CCUUiUtil.isInvalidName(mCCUName.getText().toString())){
                        mCCUName.setError(getString(R.string.error_invalid_ccu_name));
                    }
                }
            }
        });
        return rootView;
    }

    private void next(String ccuName, String installerEmail, String managerEmail) {
        ExecutorTask.executeAsync(
                ()-> ProgressDialogUtils.showProgressDialog(getActivity(),"Adding CCU"),
                ()->{
                    mAddCCU.setClickable(false);
                    ArrayList<HashMap<Object, Object> >ccus = CCUHsApi.getInstance().readAllEntities("device and ccu");
                    for(HashMap<Object, Object> ccu :ccus){
                        CCUHsApi.getInstance().deleteEntityLocally(ccu.get("id").toString());
                    }
                    L.saveCCUState();
                    L.ccu().setCCUName(ccuName);
                    String localId = CCUHsApi.getInstance().createCCU(ccuName, installerEmail, DiagEquip.getInstance().create(), managerEmail);
                    L.ccu().systemProfile = new DefaultSystem();
                    CCUHsApi.getInstance().addOrUpdateConfigProperty(HayStackConstants.CUR_CCU, HRef.make(localId));
                    CCUHsApi.getInstance().registerCcu(installerEmail);
                    prefs.setString("installerEmail", installerEmail);

                    HashMap<Object, Object> siteMap = CCUHsApi.getInstance().readEntity(Tags.SITE);
                    SettingPoint snBand = new SettingPoint.Builder()
                            .setDeviceRef(localId)
                            .setSiteRef(siteMap.get("id").toString())
                            .setDisplayName(ccuName + "-smartNodeBand")
                            .addMarker("snband").addMarker("sp").setVal(addressBandSelected).build();
                    CCUHsApi.getInstance().addPoint(snBand);
                    updateMigrationDiagWithAppVersion();
                    if(!Globals.getInstance().siteAlreadyCreated()) {
                        DefaultSchedules.setDefaultCoolingHeatingTemp();
                    }
                },
                ()->{
                    L.saveCCUState();
                    prefs.setBoolean("CCU_SETUP", true);
                    CCUHsApi.getInstance().log();
                    CCUHsApi.getInstance().syncEntityTree();
                    ProgressDialogUtils.hideProgressDialog();
                    ((FreshRegistration) requireActivity()).selectItem(21);
                }
        );
    }
    private Spanned getHTMLCodeForHints(int resource){
        return Html.fromHtml("<small><font color='#E24301'>" + getString(R.string.mandatory)
                + " " + "</font><?small>" + "<big><font color='##999999'>" + getString(resource) + "</font></big>");
    }

    private void getRegisteredAddressBand() {
        ExecutorTask.executeAsync(
                ()-> regAddressBands.clear(),
                ()->{
                    HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
                    String siteUID = CCUHsApi.getInstance().getSiteIdRef().toString();
                    HDict tDict = new HDictBuilder().add("filter", "snband and  siteRef == " + siteUID).toDict();
                    HGrid addressPoint = hClient.call("read", HGridBuilder.dictToGrid(tDict));
                    if(addressPoint == null) {
                        CcuLog.w(L.TAG_CCU_REGISTER_GATHER_DETAILS,"HGrid(schedulePoint) is null.");
                        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getActivity(),"Couldn't find the node address, Please choose the node address which is not used already.", Toast.LENGTH_LONG).show());
                    }
                    Iterator it = addressPoint.iterator();
                    while (it.hasNext())
                    {
                        HRow r = (HRow) it.next();
                        if (r.getStr("val") != null) {
                            regAddressBands.add(r.getStr("val"));
                        }
                    }
                },
                ()->{
                    regAddressBands.sort((s, t1) -> Integer.compare(Integer.parseInt(s), Integer.parseInt(t1)));
                    int selectedIndex = 0;
                    for(int i = 0; i < regAddressBands.size(); i++)
                    {
                        if(regAddressBands.get(i).equals(addressBand.get(selectedIndex))) {
                            selectedIndex++;
                        } else if(regAddressBands.get(i).compareTo(addressBand.get(selectedIndex))>0)
                            break;
                    }
                    ArrayAdapter<String> analogAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, addressBand);
                    analogAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                    addressBandSpinner.setAdapter(analogAdapter);
                    addressBandSelected = addressBand.get(selectedIndex);
                    L.ccu().setSmartNodeAddressBand(Short.parseShort(addressBandSelected));
                    addressBandSpinner.setSelection(selectedIndex);
                }
        );
    }
}

