package a75f.io.renatus;


import static a75f.io.logic.bo.building.dab.DabProfile.CARRIER_PROD;
import static a75f.io.logic.bo.util.CCUUtils.isAdvanceHybridProfile;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.domain.util.CommonQueries;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.renatus.profiles.system.DabModulatingRtuFragment;
import a75f.io.renatus.profiles.system.DabStagedVfdRtuFragment;
import a75f.io.logic.util.onLoadingCompleteListener;
import a75f.io.renatus.profiles.system.VavModulatingRtuFragment;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.renatus.profiles.system.VavStagedVfdRtuFragment;
import a75f.io.renatus.profiles.system.advancedahu.dab.DabAdvancedHybridAhuFragment;
import a75f.io.renatus.profiles.system.advancedahu.vav.VavAdvancedHybridAhuFragment;
import a75f.io.renatus.profiles.system.externalahu.ExternalAhuFragment;
import a75f.io.renatus.registration.FreshRegistration;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.views.CustomSpinnerDropDownAdapter;
import butterknife.BindView;
import butterknife.ButterKnife;



/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class SystemProfileFragment extends Fragment implements onLoadingCompleteListener {
    @BindView(R.id.spinnerSystemProfile)
    Spinner spSystemProfile;

    @BindView(R.id.txt_header)
    TextView txtHeader;

    @BindView(R.id.spinnerLayout)
    LinearLayout spinnerLayout;
    boolean showDialogRequired = false;

    boolean checkDialogRequired = false;

    private boolean isFreshRegister;
   boolean isAdvancesHybridV1IsPaired = false;

    onLoadingCompleteListener dialogListener = this;

    public SystemProfileFragment() {
    }

    public static SystemProfileFragment newInstance() {
        return new SystemProfileFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profile_selector, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        ButterKnife.bind(this, view);
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
        isAdvancesHybridV1IsPaired = isAdvanceHybridProfile();
        List<String> profileTypeList;
//        if advancesHybridV1 is already paired ,not removing the profile name in the list .. else .. removing the profile name from the list
        if (isAdvancesHybridV1IsPaired) {
            profileTypeList = Arrays.asList(getResources().getStringArray(getSystemProfileArrayResource()));
        } else {
            profileTypeList = removeAdvancesHybridV1Profile();
        }
        ArrayAdapter<CharSequence> systemProfileSelectorAdapter = getAdapterValue(new ArrayList(profileTypeList));
        systemProfileSelectorAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spSystemProfile.setAdapter(systemProfileSelectorAdapter);
        CCUUiUtil.setSpinnerDropDownColor(spSystemProfile,getContext());

        spSystemProfile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (!isFreshRegister&& adapterView.getChildAt(0)!= null) {
                    ((TextView) adapterView.getChildAt(0)).setTextColor(getResources().getColor(R.color.black));
                    ((TextView) adapterView.getChildAt(0)).setTextSize(18);
                    spSystemProfile.getLayoutParams().width = 340;

                    if(adapterView.getSelectedItemPosition() == 0){
                        if (!getSystemEquipOAOAndBypassDamper()  && L.ccu().systemProfile .getProfileName() != "Default" && !showDialogRequired && canAddDABProfile() && canAddVAVProfile()) {

                            checkDialogRequired = true;
                            spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                    systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            // checking the Profile type DM or Non DM.Non DM profile we not have loading while selecting the profile.
                            //so i am showing the Alert dialog to the user to confirm the profile change.
                            //For DM system profile i have added the listener after loading screen completed it will show the dialog
                            if(getProfileType()) {
                                showAlertDialog();
                                checkDialogRequired = false;
                            }
                            return;
                        }
                    }
                }
                //if based on the profile type (advancesHybrid v1)  selection index is changed
                if (!isAdvancesHybridV1IsPaired) {
                    switch (i) {
                        case 0:
                            if (canAddDABProfile() && canAddVAVProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new DefaultSystemProfile()).commit();
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(0);
                                }

                            } else {
                                Toast.makeText(getActivity(), "Unpair all VAV or DAB Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;
                        case 1:
                            if (canAddVAVProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new VavModulatingRtuFragment(dialogListener)).commit();
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all DAB Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;
                        case 2:
                            if (canAddVAVProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new VavStagedVfdRtuFragment(dialogListener)).commit();
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all DAB Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;
                        case 3:
                            if (canAddVAVProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new VavAdvancedHybridAhuFragment(dialogListener)).commit();
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all DAB Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;
                        case 4:
                            if (canAddVAVProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new ExternalAhuFragment(ProfileType.vavExternalAHUController), "vavExternalAHUController").commit();
                                PreferenceUtil.setIsNewExternalAhu(true);
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all DAB Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;
                        case 5:
                            if (canAddDABProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new DabModulatingRtuFragment()).commit();
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all VAV Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;
                        case 6:
                            if (canAddDABProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new DabStagedVfdRtuFragment()).commit();
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all VAV Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;

                        case 7:
                            if (canAddDABProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new DabAdvancedHybridAhuFragment()).commit();
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all VAV Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;
                        case 8:
                            if (canAddDABProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new ExternalAhuFragment(ProfileType.dabExternalAHUController), "dabExternalAHUController").commit();
                                PreferenceUtil.setIsNewExternalAhu(true);
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all VAV Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;

                        case 9:
                            if (canAddVAVProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new VavIERtuProfile()).commit();
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all DAB Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;
                    }

                } else {
                    switch (i) {
                        case 0:
                            if (canAddDABProfile() && canAddVAVProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new DefaultSystemProfile()).commit();
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(0);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all VAV or DAB Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;
                        case 1:
                            if (canAddVAVProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new VavModulatingRtuFragment(dialogListener)).commit();
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all DAB Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;
                        case 2:
                            if (canAddVAVProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new VavStagedVfdRtuFragment(dialogListener)).commit();
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all DAB Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;
                        case 3:
                            if (canAddVAVProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new VavHybridRtuProfile()).commit();
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all DAB Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;
                        case 4:
                            if (canAddVAVProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new VavAdvancedHybridAhuFragment(dialogListener)).commit();
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all DAB Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;
                        case 5:
                            if (canAddVAVProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new ExternalAhuFragment(ProfileType.vavExternalAHUController), "vavExternalAHUController").commit();
                                PreferenceUtil.setIsNewExternalAhu(true);
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all DAB Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;
                        case 6:
                            if (canAddDABProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new DabModulatingRtuFragment()).commit();
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all VAV Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;
                        case 7:
                            if (canAddDABProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new DabStagedVfdRtuFragment()).commit();
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all VAV Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;
                        case 8:
                            if (canAddDABProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new DABHybridAhuProfile()).commit();
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all VAV Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;

                        case 9:
                            if (canAddDABProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new DabAdvancedHybridAhuFragment()).commit();
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all VAV Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;
                        case 10:
                            if (canAddDABProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new ExternalAhuFragment(ProfileType.dabExternalAHUController), "dabExternalAHUController").commit();
                                PreferenceUtil.setIsNewExternalAhu(true);
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all VAV Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;

                        case 11:
                            if (canAddVAVProfile()) {
                                if(!isSystemViewVisible())return;
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.profileContainer, new VavIERtuProfile()).commit();
                                if (SystemConfigMenuFragment.SystemConfigMenuFragmentHandler != null) {
                                    SystemConfigMenuFragment.SystemConfigMenuFragmentHandler.sendEmptyMessage(1);
                                }
                            } else {
                                Toast.makeText(getActivity(), "Unpair all DAB Zones and try", Toast.LENGTH_LONG).show();
                                spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                                        systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
                            }
                            break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        if(!isSystemViewVisible()) return; //if system view is not visible then we are not setting the system profile
        spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
    }
    private boolean getProfileType() {
        HashMap profileList = CCUHsApi.getInstance().read(CommonQueries.SYSTEM_PROFILE);
        //Non -DM profile and vavExternalAHUController and dabExternalAHUController we not have loading while selecting the profile
        if(profileList.get("domainName") == null || profileList.get("domainName").equals("vavExternalAHUController") || profileList.get("domainName").equals("dabExternalAHUController")){
            return true;
        }
        return false;
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
    public boolean getSystemEquipOAOAndBypassDamper() {
        ArrayList<HashMap> SystemEquip = CCUHsApi.getInstance().readAll("equip and (oao or bypassDamper)");
        for (HashMap equip : SystemEquip) {
            if (equip.get("oao") != null  || equip.get("bypassDamper") != null){
                return false;
            }
        }
        return true;
    }

    private void showAlertDialog() {
                new AlertDialog.Builder(getActivity())
                        .setTitle("Confirmation")
                        .setMessage("Changing the system profile to Default will delete the OAO/Bypass Damper profile.")
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            dialog.dismiss();
                            showDialogRequired = false;
                        })
                        .setPositiveButton("Proceed", (dialog, which) -> {
                            spSystemProfile.setSelection(0);
                            showDialogRequired = true;
                        })
                        .create().show();
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
    
    private int getSystemProfileArrayResource() {
        if (BuildConfig.BUILD_TYPE.equals("daikin_prod")) {
            return R.array.system_profile_select_daikin;
        }else if(BuildConfig.BUILD_TYPE.equalsIgnoreCase(CARRIER_PROD)){
            return R.array.system_profile_select_carrier;
        }
        return R.array.system_profile_select;
    }
    private CustomSpinnerDropDownAdapter getAdapterValue(ArrayList values) {
        return new CustomSpinnerDropDownAdapter(requireContext(), R.layout.spinner_dropdown_item, values);
    }

    @Override
    public void onLoadingComplete() {
        if(checkDialogRequired){
            checkDialogRequired = false;
            showAlertDialog();
        }
    }


    private List<String> removeAdvancesHybridV1Profile() {
        List<String> originalList = new ArrayList<>(Arrays.asList(getResources().getStringArray(getSystemProfileArrayResource())));
        List<String> updatedList = new ArrayList<>(originalList);
        updatedList.remove("VAV Advanced Hybrid AHU");
        if (BuildConfig.BUILD_TYPE.equalsIgnoreCase(CARRIER_PROD)) {
            updatedList.remove("VVT-C Advanced Hybrid AHU");
        } else {
            updatedList.remove("DAB Advanced Hybrid AHU");
        }
        return updatedList;
    }
    private boolean isSystemViewVisible(){
        View profileContainer = getActivity().findViewById(R.id.profileContainer);
        if(profileContainer == null) {
            CcuLog.d(L.TAG_CCU_SYSTEM,"profile Container is not found");
            return false;
        }
        return true;
    }

}