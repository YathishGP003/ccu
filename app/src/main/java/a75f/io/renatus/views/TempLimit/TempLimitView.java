package a75f.io.renatus.views.TempLimit;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;

import static a75f.io.renatus.tuners.ExpandableTunerListAdapter.getTuner;

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

        this.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!mAdded) {
                    add();
                    mAdded = true;
                }

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
        HashMap tuner = CCUHsApi.getInstance().read("equip and tuner");
        Equip p = new Equip.Builder().setHashMap(tuner).build();

        HashMap coolUL = CCUHsApi.getInstance().read("point and limit and max and cooling and user");
        HashMap heatUL = CCUHsApi.getInstance().read("point and limit and min and heating and user");
        HashMap coolLL = CCUHsApi.getInstance().read("point and limit and min and cooling and user");
        HashMap heatLL = CCUHsApi.getInstance().read("point and limit and max and heating and user");
        HashMap buildingMin = CCUHsApi.getInstance().read("building and limit and min");
        HashMap buildingMax = CCUHsApi.getInstance().read("building and limit and max");

        setTempControl((float) getTuner(heatLL.get("id").toString()), (float) getTuner(heatUL.get("id").toString()),
                (float) getTuner(coolLL.get("id").toString()), (float) getTuner(coolUL.get("id").toString()),
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

