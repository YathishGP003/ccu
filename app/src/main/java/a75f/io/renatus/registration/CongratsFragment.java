package a75f.io.renatus.registration;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.jsoup.helper.StringUtil;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.bo.util.SystemTemperatureUtil;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.R;
import a75f.io.renatus.util.CCUUiUtil;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;

import static a75f.io.renatus.views.MasterControl.MasterControlView.getTuner;

public class CongratsFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    @BindView(R.id.textCCUName)
    TextView mCCUName;
    @BindView(R.id.textSerialNo)
    TextView mSerialNo;
    @BindView(R.id.textCCUVer)
    TextView mCCUVersion;
    @BindView(R.id.textFirmware)
    TextView mCMFirwareVer;
    @BindView(R.id.textBuildingLimit)
    TextView mBuildingLimits;
    @BindView(R.id.textHeatingLimits)
    TextView mHeatingLimits;
    @BindView(R.id.textCoolingLimits)
    TextView mCoolingLimits;
    @BindView(R.id.textZoneRange)
    TextView mZoneRange;
    @BindView(R.id.textTemperature)
    TextView mCurrentTemp;
    @BindView(R.id.textHumidity)
    TextView mCurrentHumidity;
    @BindView(R.id.textHvac)
    TextView mHVACEquip;
    @BindView(R.id.labelHvac)
    TextView labelHvac;
    @BindView(R.id.textComfort)
    TextView mComfortSelector;

    Context            mContext;

    private static final String TAG = CongratsFragment.class.getSimpleName();

    public CongratsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment StartCCUFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CongratsFragment newInstance(String param1, String param2) {
        CongratsFragment fragment = new CongratsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_congrats, container, false);
        ButterKnife.bind(this, rootView);
        mContext = getContext().getApplicationContext();

        ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) rootView.getLayoutParams();
        p.setMargins(0, 0, 0, 80);

        HashMap ccu = CCUHsApi.getInstance().read("device and ccu");
        
        String buldingLimit;
        String heatingLimit;
        String coolingLimit;
        String zoneRange;
        String currentTemp;
        String currentHumidity;

        PackageManager pm = mContext.getPackageManager();
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo("a75f.io.renatus", 0);
            String str = pi.versionName + "." + String.valueOf(pi.versionCode);
            mCCUVersion.setText(str);
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        HashMap tuner = CCUHsApi.getInstance().read("equip and tuner");
        Equip eqp = new Equip.Builder().setHashMap(tuner).build();
        double buildingLimitMax =  BuildingTunerCache.getInstance().getBuildingLimitMax();
        double buildingLimitMin =  BuildingTunerCache.getInstance().getBuildingLimitMin();
        HashMap maxHeatMap =  CCUHsApi.getInstance().read("point and limit and max and heating and user");
        HashMap minHeatMap =  CCUHsApi.getInstance().read("point and limit and min and heating and user");
        HashMap maxCoolMap =  CCUHsApi.getInstance().read("point and limit and max and cooling and user");
        HashMap minCoolMap =  CCUHsApi.getInstance().read("point and limit and min and cooling and user");
        buldingLimit = (int)buildingLimitMin+"|"+ (int)buildingLimitMax;
        heatingLimit = (int)getTuner(minHeatMap.get("id").toString())+"|"+(int)getTuner(maxHeatMap.get("id").toString());
        coolingLimit = (int)getTuner(minCoolMap.get("id").toString())+"|"+(int)getTuner(maxCoolMap.get("id").toString());
        zoneRange = (int)getTuner(maxHeatMap.get("id").toString())+"|"+(int)getTuner(maxCoolMap.get("id").toString());
        currentTemp = String.valueOf((int) SystemTemperatureUtil.getCurrentTemp(eqp.getId()));
        if (L.ccu().systemProfile instanceof DefaultSystem) {
            currentHumidity = "0.0%";
        } else {
            currentHumidity = TunerUtil.readSystemUserIntentVal("humidity") + "%";
        }

        mCCUName.setText(ccu.get("dis").toString());
        String ccuUID = CCUHsApi.getInstance().getCcuRef().toString();
        mSerialNo.setText(ccuUID == null ? CCUHsApi.getInstance().getCcuRef().toString() :ccuUID);
        mCMFirwareVer.setText("NA");
        mBuildingLimits.setText(buldingLimit);
        mHeatingLimits.setText(heatingLimit);
        mCoolingLimits.setText(coolingLimit);
        mZoneRange.setText(zoneRange);
        mCurrentTemp.setText(currentTemp);
        mCurrentHumidity.setText(currentHumidity);
        labelHvac.setText(L.ccu().systemProfile.getProfileName()+":");
        String colorHex = CCUUiUtil.getColorCode(getContext());
        String status = L.ccu().systemProfile.getStatusMessage();
        if (L.ccu().systemProfile instanceof DefaultSystem) {
            mHVACEquip.setText(StringUtil.isBlank(status) ? "System is in gateway mode" : Html.fromHtml(status.replace("ON", "<font color='"+colorHex+"'>ON</font>")));
        }else {
            mHVACEquip.setText(StringUtil.isBlank(status) ? Html.fromHtml("<font color='"+colorHex+"'>OFF</font>") : Html.fromHtml(status.replace("ON","<font color='"+colorHex+"'>ON</font>").replace("OFF","<font color='"+colorHex+"'>OFF</font>")));
        }
        mComfortSelector.setText("Maximum Comfort");
        return rootView;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
