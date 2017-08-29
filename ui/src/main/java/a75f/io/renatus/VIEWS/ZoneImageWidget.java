package a75f.io.renatus.VIEWS;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.ZoneProfile;
import a75f.io.renatus.R;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan on 8/24/17.
 */

public class ZoneImageWidget extends RelativeLayout
{
	
	@BindView(R.id.zoneImage)
	ImageView zoneImage;
	
	@BindView(R.id.zoneName)
	TextView zoneName;
	
	@BindView(R.id.zoneRefTemp)
	TextView zoneRefTemp;
	
	private Context            mContext;
	private OnClickListener    mOnClickListener;
	private ZoneProfile           mProfile;
	private int mIndex;
	
	public ZoneImageWidget(Context context, ZoneProfile profile) {
		super(context);
		mContext = context;
		this.mProfile = profile;
		init();
		
	}
	
	public void init() {
		View v = inflate(getContext(), R.layout.widget_imageview, this);
		ButterKnife.bind(v, this);
		zoneName.setText(mProfile.mModuleName);
		
		if (mProfile instanceof LightProfile) {
			zoneImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.light_orange));
		}
	}
	
	public interface OnClickListener {
		void onClick(ZoneImageWidget v);
	}
	
	
	public void setOnClickChangeListener(ZoneImageWidget.OnClickListener l) {
		mOnClickListener = l;
	}
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				break;
			case MotionEvent.ACTION_MOVE:
				break;
			case MotionEvent.ACTION_UP:
				mOnClickListener.onClick(this);
				break;
		}
		return true;
	}
	
	public ZoneProfile getProfile() {
		return mProfile;
	}
	
	public int getIndex() {
		return mIndex;
	}
	
	
	public void setSelected(boolean bSelected) {
		if (bSelected)
			this.setBackgroundColor(getContext().getResources().getColor(R.color.orange_multi));
		else
			this.setBackgroundColor(getContext().getResources().getColor(R.color.transparent));
		invalidate();
	}
}
