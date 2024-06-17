package a75f.io.renatus.views.TempLimit;

import static a75f.io.renatus.tuners.ExpandableTunerListAdapter.getTuner;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.renatus.views.MasterControl.MasterControlUtil;

/**
 * Created by mahesh on 27-08-2019.
 */
public class TempLimitView extends LinearLayout {
    //
    TempLimit tempControl;

    public TempLimitView(Context context) {
        super(context);
    }

    public TempLimitView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public TempLimitView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private boolean mAdded = false;

    private void init(AttributeSet attrs) {

        this.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (!mAdded) {
                add();
                mAdded = true;
            }

        });
    }

    private void add() {

        tempControl = new TempLimit(this.getContext());
        tempControl.setMinimumWidth(getMeasuredWidth());

        LayoutParams layoutParams =
                new LayoutParams(0, LayoutParams.MATCH_PARENT);
        layoutParams.weight = 1;
        this.addView(tempControl, layoutParams);

        updateData();
    }


    public void updateData() {
        HashMap<Object, Object> buildingMin = CCUHsApi.getInstance().readEntity("building and limit and min and not tuner");
        HashMap<Object, Object> buildingMax = CCUHsApi.getInstance().readEntity("building and limit and max and not tuner");
        if (buildingMin.isEmpty()) {
            buildingMin = CCUHsApi.getInstance().readEntity("building and limit and min");
            buildingMax = CCUHsApi.getInstance().readEntity("building and limit and max");
        }

        setTempControl((float)MasterControlUtil.zoneMaxHeatingVal(),
                (float) MasterControlUtil.zoneMinHeatingVal(),
                (float) MasterControlUtil.zoneMinCoolingVal(),
                (float) MasterControlUtil.zoneMaxCoolingVal(),
                (float) getTuner(buildingMin.get("id").toString()), (float) getTuner(buildingMax.get("id").toString()));
    }


    public void setTempControl(float lowerHeatingTemp, float upperHeatingTemp, float lowerCoolingTemp,
                               float upperCoolingTemp, float lowerBuildingTemp, float upperBuildingTemp){

        if (tempControl!=null)
            tempControl.setData(lowerHeatingTemp, upperHeatingTemp,
                lowerCoolingTemp, upperCoolingTemp,
                lowerBuildingTemp, upperBuildingTemp);
    }
}

