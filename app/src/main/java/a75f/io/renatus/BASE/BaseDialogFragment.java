package a75f.io.renatus.BASE;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import a75f.io.renatus.R;
import a75f.io.renatus.SystemConfigMenuFragment;
import a75f.io.renatus.modbus.ModbusConfigView;
import a75f.io.renatus.modbus.util.ModbusLevel;
import a75f.io.renatus.profiles.oao.OAOProfileFragment;
import a75f.io.renatus.profiles.vav.BypassConfigFragment;
import a75f.io.renatus.tuners.TunerFragment;

/**
 * Created by Yinten isOn 8/11/2017.
 */

public abstract class BaseDialogFragment extends DialogFragment {
	protected void setTitle(String title) {
		getDialog().setTitle(title);
	}

	protected void showDialogFragment(DialogFragment dialogFragment, String id) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag(id);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		// Create and show the dialog.
		dialogFragment.show(getFragmentManager(), id);
	}

	protected void removeDialogFragment(String id) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag(id);
		if (prev instanceof DialogFragment) {
			ft.remove(prev).commitAllowingStateLoss();
		}
	}


	protected ProgressDialog mProgress;

	protected void showProgressDialog() {
		if (mProgress == null) {
			mProgress = new ProgressDialog(BaseDialogFragment.this.getActivity());
		}
		if (!mProgress.isShowing()) {
			mProgress.show();
		}
	}

	protected void dismissProgressDialog() {
		if (mProgress != null && mProgress.isShowing()) {
			mProgress.dismiss();
		}
	}

	public abstract String getIdString();

	@Override
	public void onPause() {
		super.onPause();
		dismissProgressDialog();
	}

	public void closeAllBaseDialogFragments() {
		for (Fragment fragment : getFragmentManager().getFragments()) {
			if (fragment instanceof BaseDialogFragment && !(omitDialogFragment(fragment))) {
				((BaseDialogFragment) fragment).dismiss();
				removeDialogFragment(((BaseDialogFragment) fragment).getIdString());
			}
		}


	}

	public void showSuccessToast(String profileName, int nodeAddress, Context context) {
		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.custom_layout_ccu_successful_update, null);

		TextView text = layout.findViewById(R.id.custom_toast_message_detail);

		String message = profileName + " (" + nodeAddress + ") configurations saved successfully";
		text.setText(message);

		Toast toast = new Toast(context);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(layout);
		toast.show();
	}

	private boolean omitDialogFragment(Fragment fragment) {

		if (fragment instanceof ModbusConfigView) {
			ModbusConfigView modbusConfigView = (ModbusConfigView) fragment;
			Bundle args = modbusConfigView.getArguments();
			// case : 1 while pairing the terminal modbus -->not to remove the system level modbus config dialog if the system level modbus config dialog is there  --> isSystemLevelModbus : false
			// case : 2 while pairing the system modbus -->remove the system level modbus config dialog --> isSystemLevelModbus : true
			if (args != null && args.getInt("MODBUS_LEVEL") == ModbusLevel.SYSTEM.ordinal()) {
				return !SystemConfigMenuFragment.isSystemLevel;
			}
			return false;
		} else if ((fragment instanceof OAOProfileFragment || fragment instanceof BypassConfigFragment) && SystemConfigMenuFragment.isSystemLevel) {
			return false;
		}
		return fragment instanceof TunerFragment || fragment instanceof OAOProfileFragment || fragment instanceof BypassConfigFragment;
	}
}
