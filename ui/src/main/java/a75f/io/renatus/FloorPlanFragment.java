package a75f.io.renatus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import a75f.io.renatus.BLE.FragmentDeviceScan;
import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by samjithsadasivan on 8/7/17.
 */

public class FloorPlanFragment extends Fragment
{
	@BindView (R.id.addFloorBtn)
	ImageButton addFloorBtn;
	
	@BindView (R.id.addRoomBtn)
	ImageButton addRoomBtn;
	
	@BindView (R.id.pairModuleBtn)
	ImageButton pairModuleBtn;
	
	@BindView (R.id.addFloorEdit)
	EditText addFloorEdit;
	
	@BindView (R.id.addRoomEdit)
	EditText addRoomEdit;
	
	@BindView (R.id.addModuleEdit)
	EditText addModuleEdit;
	
	public static FloorPlanFragment newInstance(){
		return new FloorPlanFragment();
	}
	
	public FloorPlanFragment(){
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_floorplan, container, false);
		ButterKnife.bind (this, rootView);
		return rootView;
	}
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		setUpViews();
	}
	
	public void setUpViews() {
		addFloorEdit.setVisibility(View.GONE);
		addRoomEdit.setVisibility(View.GONE);
	}
	
	@OnClick (R.id.pairModuleBtn)
	public void startPairing() {
		FragmentDeviceScan.getInstance().show(getChildFragmentManager(), "dialog");
	}
}
