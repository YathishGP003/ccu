package a75f.io.renatus;

import android.os.Bundle;

import a75f.io.api.haystack.CCUHsApi;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.logic.L;
import a75f.io.renatus.registration.FreshRegistration;
import a75f.io.renatus.util.CCUUiUtil;
import butterknife.BindView;
import butterknife.ButterKnife;



/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class SystemProfileFragment extends Fragment {
    @BindView(R.id.spinnerSystemProfile)
    Spinner spSystemProfile;

    @BindView(R.id.txt_header)
    TextView txtHeader;

    @BindView(R.id.spinnerLayout)
    LinearLayout spinnerLayout;

    private boolean isFreshRegister;

    public SystemProfileFragment() {
    }

    public static SystemProfileFragment newInstance() {
        return new SystemProfileFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profile_selector, container, false);
        ButterKnife.bind(this, rootView);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        isFreshRegister = getActivity() instanceof FreshRegistration;

        if (!isFreshRegister) {
            txtHeader.setVisibility(View.GONE);
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            p.setMargins(0, 30, 0, 0);

            ViewGroup.MarginLayoutParams spinner = (ViewGroup.MarginLayoutParams) spinnerLayout.getLayoutParams();
            spinner.setMargins(0,0,50,0);
        } else {
            txtHeader.setVisibility(View.VISIBLE);
        }

        ArrayAdapter<CharSequence> systemProfileSelectorAdapter = ArrayAdapter.createFromResource(this.getActivity(), R.array.system_profile_select, R.layout.spinner_dropdown_item);
        systemProfileSelectorAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spSystemProfile.setAdapter(systemProfileSelectorAdapter);
        spSystemProfile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (!isFreshRegister&& adapterView.getChildAt(0)!= null) {
                    ((TextView) adapterView.getChildAt(0)).setTextColor(CCUUiUtil.getPrimaryThemeColor(getContext()));
                    ((TextView) adapterView.getChildAt(0)).setTextSize(18);
                    spSystemProfile.getLayoutParams().width = 340;
                }
                switch (i) {
                    case 0:
                        if(canAddDABProfile() && canAddVAVProfile()){
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.profileContainer, new DefaultSystemProfile()).commit();
                        } else {
                            Toast.makeText(getActivity(),"Unpair all VAV or DAB Zones and try",Toast.LENGTH_LONG).show();
                            spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                    systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                        }
                        break;
                    case 1:
                        if (canAddVAVProfile()) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.profileContainer, new VavStagedRtuProfile()).commit();
                        } else {
                            Toast.makeText(getActivity(),"Unpair all DAB Zones and try",Toast.LENGTH_LONG).show();
                            spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                    systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                        }
                        break;

                    case 2:
                        if (canAddVAVProfile()) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.profileContainer, new VavAnalogRtuProfile()).commit();
                        } else {
                            Toast.makeText(getActivity(),"Unpair all DAB Zones and try",Toast.LENGTH_LONG).show();
                            spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                    systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                        }
                        break;
                    case 3:
                        if (canAddVAVProfile()) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.profileContainer, new VavStagedRtuWithVfdProfile()).commit();
                        } else {
                            Toast.makeText(getActivity(),"Unpair all DAB Zones and try",Toast.LENGTH_LONG).show();
                            spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                    systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                        }
                        break;
                    case 4:
                        if (canAddVAVProfile()) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.profileContainer, new VavHybridRtuProfile()).commit();
                        } else {
                            Toast.makeText(getActivity(),"Unpair all DAB Zones and try",Toast.LENGTH_LONG).show();
                            spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                    systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                        }
                        break;
                    case 5:
                        if (canAddDABProfile()) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.profileContainer, new DABStagedProfile()).commit();
                        }else {
                            Toast.makeText(getActivity(),"Unpair all VAV " +
                                    " and try",Toast.LENGTH_LONG).show();
                            spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                    systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                        }
                        break;
                    case 6:
                        if (canAddDABProfile()) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.profileContainer, new DABFullyAHUProfile()).commit();
                        }else {
                            Toast.makeText(getActivity(),"Unpair all VAV Zones and try",Toast.LENGTH_LONG).show();
                            spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                    systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                        }
                        break;
                    case 7:
                        if (canAddDABProfile()) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.profileContainer, new DABStagedRtuWithVfdProfile()).commit();
                        }else {
                            Toast.makeText(getActivity(),"Unpair all VAV Zones and try",Toast.LENGTH_LONG).show();
                            spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                    systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                        }
                        break;
                    case 8:
                        if (canAddDABProfile()) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.profileContainer, new DABHybridAhuProfile()).commit();
                        }else {
                            Toast.makeText(getActivity(),"Unpair all VAV Zones and try",Toast.LENGTH_LONG).show();
                            spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                    systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                        }
                        break;
                    case 9:
                        if (canAddVAVProfile()) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.profileContainer, new VavIERtuProfile()).commit();
                        } else {
                            Toast.makeText(getActivity(), "Unpair all DAB Zones and try", Toast.LENGTH_LONG).show();
                            spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                    systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                        }
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
    }
    
    private boolean canAddVAVProfile() {
        ArrayList<HashMap> zoneEquips = CCUHsApi.getInstance().readAll("equip and zone");
        for (HashMap equip : zoneEquips) {
            if (equip.containsKey("dab") || equip.containsKey("dualDuct")) {
                return false;
            }
        }
        return true;
    }
    
    
    private boolean canAddDABProfile() {
        ArrayList<HashMap> zoneEquips = CCUHsApi.getInstance().readAll("equip and zone");
        for (HashMap equip : zoneEquips) {
            if (equip.containsKey("vav")) {
                return false;
            }
        }
        return true;
    }
}
