package a75f.io.renatus;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.Node;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.logic.L;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.ZONEPROFILE.LightingZoneProfileFragment;
import butterknife.BindView;
import butterknife.ButterKnife;

import static a75f.io.renatus.R.id.roomName;

/**
 * Created by ryant on 9/27/2017.
 */

public class FragmentSelectDeviceType extends BaseDialogFragment
{
    public static final String ID = FragmentSelectDeviceType.class.getSimpleName();
    Zone         mZone;
    LightProfile mLightProfile;
    Node         mNode;
    short        mNodeAddress;
    
    String       mRoomName;
    String       mFloorName;
    
    @BindView(R.id.deviceTypeSelection)
    RadioGroup  deviceTypeSelection;
    @BindView(R.id.smartNode)
    RadioButton smartNode;
    @BindView(R.id.smartstat)
    RadioButton smartstat;
    @BindView(R.id.hia)
    RadioButton hia;
    @BindView(R.id.lpi)
    RadioButton lpi;
    
    public static FragmentSelectDeviceType newInstance(short meshAddress, String roomName, String mFloorName)
    {
        FragmentSelectDeviceType f = new FragmentSelectDeviceType();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, mFloorName);
        f.setArguments(bundle);
        return f;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.devicetypeselectiondialog, null);
        mNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mZone = L.findZoneByName(mFloorName, mRoomName);
        mLightProfile = (LightProfile) mZone.findProfile(ProfileType.LIGHT);
        mNode = mZone.getSmartNode(mNodeAddress);
        ButterKnife.bind(this, view);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle);
        alertBuilder.setTitle(mRoomName + " - " + "Select device type to pair");
        alertBuilder.setView(view);
        alertBuilder.setCancelable(false);
        
        alertBuilder.setPositiveButton("Pair", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                
                OpenModuleSelectionDialog();
                        
            }
        });
        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });
        return alertBuilder.create();
    }
    
    private void OpenModuleSelectionDialog()
    {
        
        if (smartNode.isChecked())
        {
            DialogSmartNodeProfiling wrmProfiling = DialogSmartNodeProfiling.newInstance(mNodeAddress, mRoomName, mFloorName);
            showDialogFragment(wrmProfiling, DialogSmartNodeProfiling.ID);
            
        }
        else if (smartstat.isChecked())
        {
            DialogSmartStatProfiling hiaselection = DialogSmartStatProfiling.newInstance(mNodeAddress, mRoomName, mFloorName);
            showDialogFragment(hiaselection, DialogSmartStatProfiling.ID);
        }
    }
}
