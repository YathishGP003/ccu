package a75f.io.renatus.registartion;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.renatus.R;
import butterknife.BindView;
import butterknife.ButterKnife;

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
    @BindView(R.id.textCMSerial)
    TextView mCMSerialNo;
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
        // Inflate the layout for this fragment
        //((FreshRegistration)getActivity()).showIcons(false);
        View rootView = inflater.inflate(R.layout.fragment_congrats, container, false);
        ButterKnife.bind(this, rootView);
        mContext = getContext().getApplicationContext();

        HashMap ccu = CCUHsApi.getInstance().read("device and ccu");
        HashMap site = CCUHsApi.getInstance().read("site");


        String buldingLimit = "60|85";
        String heatingLimit = "67|72";
        String coolingLimit = "72|76";
        String zoneRange = "63|82";
        String currentTemp = "73.0";
        String currentHumidity = "34%";
        String coolingStage1 = "OFF";
        String coolingStage2 = "OFF";
        String heatingStage1 = "OFF";
        String heatingStage2 = "OFF";
        String humidifierStatus = "OFF";
        String oaoStatus = "NO";
        String analogStatus = "-|0|-|-";

        mCCUName.setText(ccu.get("dis").toString());
        mSerialNo.setText(site.get("id").toString());
        mCMSerialNo.setText("d1C88dd514801d5f");
        mCMFirwareVer.setText("3.21.24.9");
        mBuildingLimits.setText(buldingLimit);
        mHeatingLimits.setText(heatingLimit);
        mCoolingLimits.setText(coolingLimit);
        mZoneRange.setText(zoneRange);
        mCurrentTemp.setText(currentTemp);
        mCurrentHumidity.setText(currentHumidity);
        mHVACEquip.setText(Html.fromHtml("<small><font color='#000000'>Cooling Stage 1 | "+"</font></small>"+"<font color='#E24301'> "+coolingStage1+"</font>"+
                                                "<small><font color='#000000'>   Cooling Stage 2 | "+"</font></small>"+"<font color='#E24301'> "+coolingStage2+"</font><br>"+
                                                "<small><font color='#000000'>Heating Stage 1 | "+"</font></small>"+"<font color='#E24301'> "+heatingStage1+"</font>"+
                                                "<small><font color='#000000'>   Heating Stage 2 | "+"</font></small>"+"<font color='#E24301'> "+heatingStage2+"</font><br>"+
                                                "<small><font color='#000000'>Humidifier | "+"</font></small>"+"<font color='#E24301'> "+humidifierStatus+"</font><br>"+
                                                "<small><font color='#000000'>OAO | "+"</font></small>"+"<font color='#E24301'> "+oaoStatus+"</font><br>"+
                                                "<small><font color='#000000'>Analog | "+"</font></small>"+"<font color='#E24301'> "+analogStatus+"</font><br>"
        ));
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
