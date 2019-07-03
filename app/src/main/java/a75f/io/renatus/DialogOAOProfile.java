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
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.oao.OAOProfile;
import a75f.io.logic.bo.building.oao.OAOProfileConfiguration;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import butterknife.ButterKnife;

import static a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType.TwoToTenV;

/**
 * Created by samjithsadasivan on 6/5/18.
 */

public class DialogOAOProfile extends BaseDialogFragment
{
    public static final String ID = DialogOAOProfile.class.getSimpleName();
    
    private short    mSmartNodeAddress;
    private NodeType mNodeType;
    private OAOProfile mProfile;
    private OAOProfileConfiguration mProfileConfig;
    
    private Button setButton;
    
    String floorRef;
    String zoneRef;

    public DialogOAOProfile()
    {
    }
    
    public static DialogOAOProfile newInstance(short smartNodeAddress, String roomRef, String floorRef)
    {
        DialogOAOProfile f = new DialogOAOProfile();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, smartNodeAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomRef);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorRef);
        f.setArguments(bundle);
        return f;
    }
    
    @Override
    public String getIdString()
    {
        return ID;
    }
    
    
    @Override
    public void onStart()
    {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = 1165;//ViewGroup.LayoutParams.WRAP_CONTENT;
            int height = 672;//ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }
        //setTitle();
    }
    
    private void setTitle() {
        Dialog dialog = getDialog();
        
        if (dialog == null) {
            return;
        }
        dialog.setTitle("VAV Configuration");
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
        View view = inflater.inflate(R.layout.fragment_oao_config, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        ButterKnife.bind(this, view);
        mSmartNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        zoneRef = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        floorRef = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        return view;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        setButton = view.findViewById(R.id.setBtn);
        
        mProfile = L.ccu().oaoProfile;
    
        if (mProfile != null) {
            CcuLog.d(L.TAG_CCU_UI,  "Get OAOProfile: ");
            mProfileConfig = (OAOProfileConfiguration) mProfile.getProfileConfiguration(mSmartNodeAddress);
        } else {
            CcuLog.d(L.TAG_CCU_UI, "Create OAOProfile: ");
            mProfile = new OAOProfile();
        }
    
        if (mProfileConfig != null) {
            //init
        }
    
    
    
        setButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
            
                new AsyncTask<Void, Void, Void>() {
                
                    ProgressDialog progressDlg = new ProgressDialog(getActivity());
                
                    @Override
                    protected void onPreExecute() {
                        setButton.setEnabled(false);
                        progressDlg.setMessage("Saving OAO Configuration");
                        progressDlg.show();
                        super.onPreExecute();
                    }
                
                    @Override
                    protected Void doInBackground( final Void ... params ) {
                        setUpOAOProfile();
                        L.saveCCUState();
                    
                        return null;
                    }
                
                    @Override
                    protected void onPostExecute( final Void result ) {
                        progressDlg.dismiss();
                        DialogOAOProfile.this.closeAllBaseDialogFragments();
                        getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            
            }
        });
    }
    
    private void setUpOAOProfile() {
       
        OAOProfileConfiguration oaoConfig = new OAOProfileConfiguration();
    
        Output analog1Op = new Output();
        analog1Op.setAddress(mSmartNodeAddress);
        analog1Op.setPort(Port.ANALOG_OUT_ONE);
        analog1Op.mOutputAnalogActuatorType = TwoToTenV;
        oaoConfig.getOutputs().add(analog1Op);
    
        Output analog2Op = new Output();
        analog2Op.setAddress(mSmartNodeAddress);
        analog2Op.setPort(Port.ANALOG_OUT_TWO);
        analog2Op.mOutputAnalogActuatorType = TwoToTenV;
        oaoConfig.getOutputs().add(analog2Op);
    
        Output relay1Op = new Output();
        relay1Op.setAddress(mSmartNodeAddress);
        relay1Op.setPort(Port.RELAY_ONE);
        relay1Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;;
        oaoConfig.getOutputs().add(relay1Op);
    
        Output relay2Op = new Output();
        relay2Op.setAddress(mSmartNodeAddress);
        relay2Op.setPort(Port.RELAY_TWO);
        relay2Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;
        oaoConfig.getOutputs().add(relay2Op);
        
        oaoConfig.outsideDamperAtMinDrive = 2;
        oaoConfig.outsideDamperAtMaxDrive = 10;
        oaoConfig.returnDamperAtMinDrive = 2;
        oaoConfig.returnDamperAtMaxDrive = 10;
        
        if (mProfileConfig == null) {
            mProfile.addOaoEquip(mSmartNodeAddress, oaoConfig, floorRef, zoneRef );
        } else {
            mProfile.updateOaoEquip(oaoConfig);
        }
        L.ccu().oaoProfile = mProfile;
        CcuLog.d(L.TAG_CCU_UI, "Set OAO Config");
        
    }

    
}
