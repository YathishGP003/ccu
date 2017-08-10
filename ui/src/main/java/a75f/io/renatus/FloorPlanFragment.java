package a75f.io.renatus;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import a75f.io.renatus.BLE.FragmentDeviceScan;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnFocusChange;

/**
 * Created by samjithsadasivan on 8/7/17.
 */

public class FloorPlanFragment extends Fragment
{
	private int nCurFloorIndex = 0;
	
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
	
	@BindView(R.id.floorList)
	ListView floorList;
	
	@BindView(R.id.roomList)
	ListView roomList;
	
	@BindView(R.id.moduleList)
	ListView moduleList;
	
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
		enableFloorButton();
		enableRoomBtn();
	}
	
	private void enableFloorButton() {
		addFloorBtn.setVisibility(View.VISIBLE);
		addFloorEdit.setVisibility(View.INVISIBLE);
	}
	
	private void enableFloorEdit() {
		addFloorBtn.setVisibility(View.INVISIBLE);
		addFloorEdit.setVisibility(View.VISIBLE);
	}
	
	private void enableRoomBtn() {
		addRoomBtn.setVisibility(View.VISIBLE);
		addRoomEdit.setVisibility(View.INVISIBLE);
	}
	
	private void enableRoomEdit() {
		addRoomBtn.setVisibility(View.INVISIBLE);
		addRoomEdit.setVisibility(View.VISIBLE);
	}
	
	@OnClick(R.id.addFloorBtn)
	public void handleFloorBtn()
	{
		enableFloorEdit();
		addFloorEdit.setText("");
		addFloorEdit.requestFocus();
		InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.showSoftInput(addFloorEdit, InputMethodManager.SHOW_IMPLICIT);
	}
	
	
	@OnEditorAction(R.id.addFloorEdit)
	public boolean handleFloorChange(TextView v, int actionId, KeyEvent event)
	{
		if (actionId == EditorInfo.IME_ACTION_DONE)
		{
			final int nID = FloorContainer.getInstance().addFloor(addFloorEdit.getText().toString());
			selectFloor(nID);
			//CCUKinveyInterface.updateFloor(FloorData.getFloorData().get(nID), false);
			InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			mgr.hideSoftInputFromWindow(addFloorEdit.getWindowToken(), 0);
			Toast.makeText(getActivity().getApplicationContext(), "Floor " + addFloorEdit.getText() + " added", Toast.LENGTH_SHORT).show();
			enableFloorButton();
			return true;
		}
		return false;
	}
	
	@OnFocusChange(R.id.addFloorEdit)
	public void handleFloorFocus(View v, boolean hasFocus) {
		if (!hasFocus) {
			enableFloorButton();
		}
	}
	
	@OnClick(R.id.addRoomBtn)
	public void handleRoomBtn() {
		enableRoomEdit();
		addRoomEdit.setText("");
		addRoomEdit.requestFocus();
		InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.showSoftInput(addRoomEdit, InputMethodManager.SHOW_IMPLICIT);
	}
	
	@OnEditorAction (R.id.addRoomEdit)
	public boolean handleRoomChange(TextView v, int actionId, KeyEvent event)
	{
		if (actionId == EditorInfo.IME_ACTION_DONE)
		{
			//Save room
			InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			mgr.hideSoftInputFromWindow(addFloorEdit.getWindowToken(), 0);
			enableRoomBtn();
			return true;
		}
		return false;
	}
	
	@OnFocusChange(R.id.addRoomEdit)
	public void handleRoomFocus(View v, boolean hasFocus) {
		if (!hasFocus) {
			enableRoomBtn();
		}
	}
	
	@OnClick (R.id.pairModuleBtn)
	public void startPairing() {
		FragmentDeviceScan.getInstance().show(getChildFragmentManager(), "dialog");
	}
	
	private void selectFloor(int position) {
		nCurFloorIndex = position;
		FloorContainer.getInstance().getFloorListAdapter().setSelectedItem(position);
		//tvAddRoom.setEnabled(true);
		//lvRoomList.setAdapter(FloorData.getFloorData().get(nCurFloorIndex).getRoomDataAdapter());
		//selectRoomItem(0);
	}
	
}
