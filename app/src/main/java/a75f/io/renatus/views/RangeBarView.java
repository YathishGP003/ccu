package a75f.io.renatus.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Schedule;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.renatus.tuners.ExpandableTunerListAdapter.getTuner;

/**
 * Created by mahesh on 15-08-2019.
 */
public class RangeBarView extends LinearLayout {

    //
    RangeBar rangeBar;
    //
    double hdb = 2.0;
    double cdb = 2.0;
    double heatLL;
    double heatUL;
    double coolLL;
    double coolUL;
    private double coolValue = 74.0;
    private double heatValue = 70.0;
    private Schedule mSchedule;

    public RangeBarView(Context context) {
        super(context);
    }

    public RangeBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public RangeBarView(Context context, AttributeSet attrs, int defStyleAttr) {
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

        new ViewGroup.LayoutParams(0, LayoutParams.MATCH_PARENT);

        rangeBar = new RangeBar(this.getContext());
        rangeBar.setMinimumWidth(getMeasuredWidth());

        LayoutParams layoutParams =
                new LayoutParams(0, LayoutParams.MATCH_PARENT);
        layoutParams.weight = 1;
        this.addView(rangeBar, layoutParams);

         if (mSchedule!= null && !mSchedule.isBuildingSchedule() && mSchedule.isZoneSchedule()) {
            getZoneHeatAndCoolingDeadBand();
        } else  if (mSchedule!= null && mSchedule.isBuildingSchedule() && !mSchedule.isZoneSchedule()) {
            getBuildingHeatAndCoolingDeadBand();
        }

      //  rangeBar.setLowerCoolingTemp((float) coolValue);
      //  rangeBar.setLowerHeatingTemp((float) heatValue);
    }

    public void setLowerCoolingTemp(double lowerCoolTemp) {
        if (rangeBar != null){
            rangeBar.setLowerCoolingTemp((float) lowerCoolTemp);
        }
    }

    public void setLowerHeatingTemp(double lowerHeatTemp) {
        if (rangeBar != null){
            rangeBar.setLowerHeatingTemp((float) lowerHeatTemp);
        }
    }

    public float getHeatValue() {
        return rangeBar.getLowerHeatingTemp();
    }

    public float getCoolValue() {
        return rangeBar.getLowerCoolingTemp();
    }

    public void setZoneSchedule(Schedule schedule) {
        this.mSchedule = schedule;
    }

    private void getBuildingHeatAndCoolingDeadBand() {

        HashMap tuner = CCUHsApi.getInstance().read("equip and tuner");
        Equip p = new Equip.Builder().setHashMap(tuner).build();

        hdb = TunerUtil.getHeatingDeadband(p.getId());
        cdb = TunerUtil.getCoolingDeadband(p.getId());
        HashMap coolULMap = CCUHsApi.getInstance().read("point and limit and max and cooling and user");
        HashMap heatULMap = CCUHsApi.getInstance().read("point and limit and min and heating and user");
        HashMap coolLLMap = CCUHsApi.getInstance().read("point and limit and min and cooling and user");
        HashMap heatLLMap = CCUHsApi.getInstance().read("point and limit and max and heating and user");
        heatLL = getTuner(heatLLMap.get("id").toString());
        heatUL = getTuner(heatULMap.get("id").toString());
        coolLL = getTuner(coolLLMap.get("id").toString());
        coolUL = getTuner(coolULMap.get("id").toString());

        rangeBar.setData((float) heatLL, (float) heatUL, (float) coolLL, (float) coolUL, (float) cdb, (float) hdb);

        double diffValue = (coolLL - heatLL);
        if (diffValue <= (hdb + cdb)){
            double value = ((hdb + cdb) - diffValue)/2;
            coolValue = coolLL + value;
            heatValue = heatLL - value;
        } else {
            coolValue = coolLL;
            heatValue = heatLL;
        }

        rangeBar.setLowerHeatingTemp((float) heatValue);
        rangeBar.setLowerCoolingTemp((float) coolValue);
    }

    private void getZoneHeatAndCoolingDeadBand() {

        Equip p = HSUtil.getEquipFromZone(mSchedule.getRoomRef());

        hdb = TunerUtil.getZoneHeatingDeadband(mSchedule.getRoomRef());
        cdb = TunerUtil.getZoneCoolingDeadband(mSchedule.getRoomRef());
        HashMap coolULMap = CCUHsApi.getInstance().read("point and limit and max and cooling and user");
        HashMap heatULMap = CCUHsApi.getInstance().read("point and limit and min and heating and user");
        HashMap coolLLMap = CCUHsApi.getInstance().read("point and limit and min and cooling and user");
        HashMap heatLLMap = CCUHsApi.getInstance().read("point and limit and max and heating and user");
        HashMap coolDT = CCUHsApi.getInstance().read("point and desired and cooling and temp and equipRef == \""+p.getId()+"\"");
        HashMap heatDT = CCUHsApi.getInstance().read("point and desired and heating and temp and equipRef == \""+p.getId()+"\"");
        heatLL = getTuner(heatLLMap.get("id").toString());
        heatUL = getTuner(heatULMap.get("id").toString());
        coolLL = getTuner(coolLLMap.get("id").toString());
        coolUL = getTuner(coolULMap.get("id").toString());
        coolValue = getTuner(coolDT.get("id").toString());
        heatValue = getTuner(heatDT.get("id").toString());

        rangeBar.setData((float) heatLL, (float) heatUL, (float) coolLL, (float) coolUL, (float) cdb, (float) hdb);

        double diffValue = (coolLL - heatLL);
        if (diffValue <= (hdb + cdb)){
            double value = ((hdb + cdb) - diffValue)/2;
            coolValue = coolLL + value;
            heatValue = heatLL - value;
        }else {
            coolValue = coolLL;
            heatValue = heatLL;
        }
        rangeBar.setLowerHeatingTemp((float) heatValue);
        rangeBar.setLowerCoolingTemp((float) coolValue);
    }
}

