package a75f.io.renatus.ENGG.logger;

/**
 * Created by samjithsadasivan on 8/17/17.
 */

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import a75f.io.renatus.R;

/**
 * Simple fraggment which contains a LogView and uses is to output log data it receives
 * through the LogNode interface.
 */
public class LogFragment extends Fragment {
	
	private LogView mLogView;
	private ScrollView mScrollView;
	
	public static LogFragment newInstance(){
		return new LogFragment();
	}
	
	public LogFragment() {}
	
	public View inflateViews() {
		mScrollView = new ScrollView(getActivity());
		ViewGroup.LayoutParams scrollParams = new ViewGroup.LayoutParams(
				                                                                ViewGroup.LayoutParams.MATCH_PARENT,
				                                                                ViewGroup.LayoutParams.MATCH_PARENT);
		mScrollView.setLayoutParams(scrollParams);
		
		mLogView = new LogView(getActivity());
		ViewGroup.LayoutParams logParams = new ViewGroup.LayoutParams(scrollParams);
		logParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		mLogView.setLayoutParams(logParams);
		mLogView.setClickable(true);
		mLogView.setFocusable(true);
		mLogView.setTypeface(Typeface.MONOSPACE);
		
		// Want to set padding as 16 dips, setPadding takes pixels.  Hooray math!
		int paddingDips = 16;
		double scale = getResources().getDisplayMetrics().density;
		int paddingPixels = (int) ((paddingDips * (scale)) + .5);
		mLogView.setPadding(paddingPixels, paddingPixels, paddingPixels, paddingPixels);
		mLogView.setCompoundDrawablePadding(paddingPixels);
		//mLogView.set
		mLogView.setGravity(Gravity.BOTTOM);
		mLogView.setBackgroundColor(Color.YELLOW);
		mLogView.setBackgroundResource(R.drawable.border);
		mLogView.setTextAppearance(getActivity(), android.R.style.TextAppearance_Holo_Medium);
		
		mScrollView.addView(mLogView);
		return mScrollView;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		
		View result = inflateViews();
		
		mLogView.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			
			@Override
			public void afterTextChanged(Editable s) {
				mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
			}
		});
		return result;
	}
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		initializeLogging();
	}
	
	public void initializeLogging() {
		// Using CcuLog, front-end to the logging chain, emulates android.util.log method signatures.
		// Wraps Android's native log framework
		LogWrapper logWrapper = new LogWrapper();
		CcuLog.setLogNode(logWrapper);
		
		MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
		logWrapper.setNext(msgFilter);
		
		// On screen logging via a fragment with a TextView.
		//LogFragment logFragment = (LogFragment) getSupportFragmentManager()
		//		                                        .findFragmentById(R.id.log_fragment);
		msgFilter.setNext(mLogView);
		
		CcuLog.i("Engg UI", "Ready");
	}
	
	public LogView getLogView() {
		return mLogView;
	}
}