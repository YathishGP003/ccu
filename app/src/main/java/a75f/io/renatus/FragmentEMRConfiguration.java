package a75f.io.renatus;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.erm.EmrProfile;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import butterknife.BindView;
import butterknife.ButterKnife;

public class FragmentEMRConfiguration extends BaseDialogFragment
{
    public static final String TAG = "EmrCondif";
    public static final String ID = FragmentEMRConfiguration.class.getSimpleName();
    
    @BindView(R.id.setBtn)
    Button setButton;
    
    private short    mSmartNodeAddress;
    private NodeType mNodeType;
    
    String floorRef;
    String zoneRef;
    
    private EmrProfile mEmrProfile;
    
    @Override
    public String getIdString()
    {
        return ID;
    }
    
    public FragmentEMRConfiguration()
    {
    }
    
    public static FragmentEMRConfiguration newInstance(short smartNodeAddress, String roomName, NodeType nodeType, String floorName)
    {
        FragmentEMRConfiguration f = new FragmentEMRConfiguration();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, smartNodeAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
        bundle.putString(FragmentCommonBundleArgs.NODE_TYPE, nodeType.toString());
        f.setArguments(bundle);
        return f;
    }
    
    @Override
    public void onStart()
    {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null)
        {
            int width = ViewGroup.LayoutParams.WRAP_CONTENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }
        setTitle();
    }
    
    private void setTitle() {
        Dialog dialog = getDialog();
        
        if (dialog == null) {
            return;
        }
        dialog.setTitle("ENERGY METER");
        TextView titleView = this.getDialog().findViewById(android.R.id.title);
        if(titleView != null)
        {
            titleView.setGravity(Gravity.CENTER);
            titleView.setTextColor(getResources().getColor(R.color.progress_color_orange));
        }
        int titleDividerId = getContext().getResources()
                                         .getIdentifier("titleDivider", "id", "android");
        
        View titleDivider = dialog.findViewById(titleDividerId);
        if (titleDivider != null) {
            titleDivider.setBackgroundColor(getContext().getResources()
                                                        .getColor(R.color.transparent));
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_emr_config, container, false);
        mSmartNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        zoneRef = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        floorRef = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mNodeType = NodeType.valueOf(getArguments().getString(FragmentCommonBundleArgs.NODE_TYPE));
        ButterKnife.bind(this, view);
        return view;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
    
        mEmrProfile = (EmrProfile) L.getProfile(mSmartNodeAddress);
        
        setButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (mEmrProfile != null) {
                    FragmentEMRConfiguration.this.closeAllBaseDialogFragments();
                    getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                    return;
                }
                
                new AsyncTask<String, Void, Void>() {
                    
                    ProgressDialog progressDlg = new ProgressDialog(getActivity());
                    
                    @Override
                    protected void onPreExecute() {
                        setButton.setEnabled(false);
                        progressDlg.setMessage("Saving EMR Configuration");
                        progressDlg.show();
                        super.onPreExecute();
                    }
                    
                    @Override
                    protected Void doInBackground( final String ... params ) {
                        setupEmrProfile();
                        L.saveCCUState();
                        
                        return null;
                    }
                    
                    @Override
                    protected void onPostExecute( final Void result ) {
                        progressDlg.dismiss();
                        FragmentEMRConfiguration.this.closeAllBaseDialogFragments();
                        getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
            }
        });
        
    }
    
    public void setupEmrProfile() {
        
        mEmrProfile = new EmrProfile();
        mEmrProfile.addEmrEquip(mSmartNodeAddress,floorRef, zoneRef);
        L.ccu().zoneProfiles.add(mEmrProfile);
        CcuLog.d(L.TAG_CCU_UI, "Set Emr Config: Profiles - "+L.ccu().zoneProfiles.size());
    }
}
