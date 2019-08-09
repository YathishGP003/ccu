package a75f.io.renatus.registartion;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

import org.projecthaystack.HGrid;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.SettingPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.tuners.BuildingTuners;
import a75f.io.renatus.R;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.views.MasterControl.MasterControlView;

import static a75f.io.logic.L.ccu;

public class InstallerOptions extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    ImageView imageGoback;
    ImageView imageTemp;
    Button mNext;
    Context mContext;
    Spinner mAddressBandSpinner;
    HGrid mCCUS;
    HGrid mSite;
    String mSiteId;
    String addressBandSelected = "1000";
    Prefs prefs;
    String localSiteID;
    String CCU_ID = "";
    private boolean isFreshRegister;

    private static final String TAG = InstallerOptions.class.getSimpleName();

    public InstallerOptions() {
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
    public static InstallerOptions newInstance(String param1, String param2) {
        InstallerOptions fragment = new InstallerOptions();
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
        View rootView = inflater.inflate(R.layout.fragment_installeroption, container, false);

        mContext = getContext().getApplicationContext();

        prefs = new Prefs(mContext);
        isFreshRegister = getActivity() instanceof FreshRegistration;
        CCU_ID = prefs.getString("CCU_ID");

        if (!isFreshRegister) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) rootView.getLayoutParams();
            p.setMargins(50, 0, 0, 0);
        }

        imageGoback = rootView.findViewById(R.id.imageGoback);
        mAddressBandSpinner = rootView.findViewById(R.id.spinnerAddress);
        mNext = rootView.findViewById(R.id.buttonNext);
        imageTemp = rootView.findViewById(R.id.imageTemp);

        ArrayList<String> addressBand = new ArrayList<>();
        //addressBand.add("Select SmartNode Address Band");
        for (int addr = 1000; addr <= 9900; addr += 100) {
            addressBand.add(String.valueOf(addr));
        }

        ArrayAdapter<String> analogAdapter = new ArrayAdapter<String>(mContext, R.layout.spinner_item, addressBand);
        analogAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        mAddressBandSpinner.setAdapter(analogAdapter);


        HashMap ccu = CCUHsApi.getInstance().read("ccu");
        //if ccu exists
        if (ccu.size() > 0) {
            for (String addBand : addressBand) {
                //Short addB = L.ccu().getSmartNodeAddressBand();
                String addB = String.valueOf(L.ccu().getSmartNodeAddressBand());
                if (addBand.equals(addB)) {
                    mAddressBandSpinner.setSelection(analogAdapter.getPosition(addBand));
                    break;
                }
            }
        } else {
            ccu().setSmartNodeAddressBand((short) 1000);
        }

        mAddressBandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d("CCU", "AddressBandSelected : " + mAddressBandSpinner.getSelectedItem());
                if (i > 0) {
                    addressBandSelected = mAddressBandSpinner.getSelectedItem().toString();
                    L.ccu().setSmartNodeAddressBand(Short.parseShort(addressBandSelected));
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

      /*  imageGoback.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                ((FreshRegistration)getActivity()).selectItem(3);
            }
        });*/
        if (isFreshRegister) mNext.setVisibility(View.VISIBLE);
        else mNext.setVisibility(View.GONE);
        mNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (ccu.size() == 0) {
                    return;
                }
                String ccuId = ccu.get("id").toString();
                ccuId = ccuId.replace("@", "");
                String ccuName = ccu.get("dis").toString();
                CCUHsApi.getInstance().addOrUpdateConfigProperty(HayStackConstants.CUR_CCU, HRef.make(ccuId));
                HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
                SettingPoint snBand = new SettingPoint.Builder()
                        .setDeviceRef(ccuId)
                        .setSiteRef(siteMap.get("id").toString())
                        .setDisplayName(ccuName + "-smartNodeBand")
                        .addMarker("snband").addMarker("sp").setVal(addressBandSelected).build();
                CCUHsApi.getInstance().addPoint(snBand);
                // TODO Auto-generated method stub
                goTonext();
            }
        });

        if (!isFreshRegister){
            ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and zone");
            if (equips != null && equips.size()>0){
                mAddressBandSpinner.setEnabled(false);
            } else {
                mAddressBandSpinner.setEnabled(true);
            }
        }

        imageTemp.setOnClickListener(view -> {
            openMasterControllerDialog();
        });

        return rootView;
    }

    //custom dialog to control building temperature
    private void openMasterControllerDialog() {
        if (isAdded()) {

            final Dialog dialog = new Dialog(getActivity());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.dialog_master_control);
            MasterControlView masterControlView = dialog.findViewById(R.id.masterControlView);

            dialog.findViewById(R.id.btnCancel).setOnClickListener(view -> dialog.dismiss());
            dialog.findViewById(R.id.btnClose).setOnClickListener(view -> dialog.dismiss());

            dialog.findViewById(R.id.btnSet).setOnClickListener(view -> {
                masterControlView.setTuner();

                dialog.dismiss();
            });

            dialog.show();
        }
    }


   /* @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            prefs = new Prefs(getContext().getApplicationContext());
            localSiteID = prefs.getString("SITE_ID");
            loadSiteDetails();
        }
    }*/

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
    /* This site never existed we are creating a new orphaned site. */

    /*  private void goTonext() {
          //Intent i = new Intent(mContext, RegisterGatherCCUDetails.class);
          //startActivity(i);
          ((FreshRegistration)getActivity()).selectItem(5);
      }
  */
    private void goTonext() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                //showProgressDialog();
            }

            @Override
            protected Void doInBackground(Void... voids) {

                if (!Globals.getInstance().siteAlreadyCreated()) {
                    BuildingTuners.getInstance();
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
                //hideProgressDialog();
                prefs.setBoolean("PROFILE_SETUP", false);
                prefs.setBoolean("CCU_SETUP", true);
                ((FreshRegistration) getActivity()).selectItem(5);

            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }
}
