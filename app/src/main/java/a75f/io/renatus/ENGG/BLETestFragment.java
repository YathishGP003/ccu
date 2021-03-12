package a75f.io.renatus.ENGG;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.Arrays;
import java.util.List;

import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.renatus.BLE.FragmentDeviceScan;
import a75f.io.renatus.R;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by samjithsadasivan isOn 8/17/17.
 */

public class BLETestFragment extends Fragment
{
    List<String> channels =
            Arrays.asList("1000", "2000", "3000", "4000", "5000", "6000", "7000", "8000", "9000");
    
    @BindView(R.id.roomName)
    EditText roomName;
    
    @BindView(R.id.channelSelector)
    Spinner channelSelector;
    
    @BindView(R.id.pairButton)
    Button pairButton;
    
    private int channelSelection;
    private NodeType mNodeType = NodeType.SMART_NODE;
    
    
    public BLETestFragment()
    {
    }
    
    
    public static BLETestFragment newInstance()
    {
        return new BLETestFragment();
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_bletest, container, false);
        ButterKnife.bind(this, rootView);
        return rootView;
    }
    
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        initChannelSpinner();
    }
    
    
    private void initChannelSpinner()
    {
        ArrayAdapter<String> dataAdapter =
                new ArrayAdapter<>(this.getActivity(), android.R.layout.simple_spinner_item, channels);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        channelSelector.setAdapter(dataAdapter);
        channelSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                channelSelection = position;
            }
            
            
            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });
    }
    
    
    @OnClick(R.id.pairButton)
    public void pairModule()
    {
        FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan
                                                        .getInstance(Short.parseShort(channels.get(channelSelection)), roomName.getText()
                                                                                                                               .toString(), "75F", mNodeType, ProfileType.LIGHT);
        showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
    }
    
    
    private void showDialogFragment(DialogFragment dialogFragment, String id)
    {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(id);
        if (prev != null)
        {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        // Create and show the dialog.
        dialogFragment.show(ft, id);
    }
}
