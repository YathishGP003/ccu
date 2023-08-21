package a75f.io.renatus.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Schedule;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.renatus.views.MasterControl.MasterControlUtil;

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
    private Schedule.Days mDay;

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

        if (mSchedule!= null && !mSchedule.isBuildingSchedule() && mSchedule.isZoneSchedule() && MasterControlUtil.isMigrated()) {
            getZoneHeatAndCoolingDeadBand();
        } else  if (mSchedule!= null && mSchedule.isBuildingSchedule()) {
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

    public void setUnOccupiedFragment(boolean unOccupiedSetBackFragment) {
        if (rangeBar != null){
            RangeBar.setUnOccupiedFragment(unOccupiedSetBackFragment);
        }
    }

    public void setLowerHeatingTemp(double lowerHeatTemp) {
        if (rangeBar != null){
            rangeBar.setLowerHeatingTemp((float) lowerHeatTemp);
        }
    }
    public void setHeatingLimitMin(double heatingLimitMin) {
        if (rangeBar != null){
            rangeBar.setHeatingLimitMin((float) heatingLimitMin);
        }
    }
    public void setCoolingLimitMax(double coolingLimitMax) {
        if (rangeBar != null){
            rangeBar.setCoolingLimitMax((float) coolingLimitMax);
        }
    }
    public void setHeatingLimitMax(double heatingLimitMax) {
        if (rangeBar != null){
            rangeBar.setHeatingLimitMax((float) heatingLimitMax);
        }
    }
    public void setCoolingLimitMin(double coolingLimitMin) {
        if (rangeBar != null){
            rangeBar.setCoolingLimitMin((float) coolingLimitMin);
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

    public void setmDay(Schedule.Days mday) {
        this.mDay = mday;
    }
    private void getBuildingHeatAndCoolingDeadBand() {

        BuildingTunerCache buildingTunerCache = BuildingTunerCache.getInstance();
        HashMap<Object,Object> coolDB = CCUHsApi.getInstance().readEntity("point and cooling and deadband and schedulable and default");
        HashMap<Object,Object> heatDB = CCUHsApi.getInstance().readEntity("point and heating and deadband and schedulable and default");

        hdb = HSUtil.getLevelValueFrom16(heatDB.get("id").toString());
        cdb = HSUtil.getLevelValueFrom16(coolDB.get("id").toString());
        heatLL = buildingTunerCache.getMaxHeatingUserLimit().floatValue();
        heatUL = buildingTunerCache.getMinHeatingUserLimit().floatValue();
        coolLL = buildingTunerCache.getMinCoolingUserLimit().floatValue();
        coolUL = buildingTunerCache.getMaxCoolingUserLimit().floatValue();

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

        String roomRef = mSchedule.getRoomRef();
        Equip p = HSUtil.getEquipFromZone(mSchedule.getRoomRef());

        HashMap<Object,Object> coolDB = CCUHsApi.getInstance().readEntity("point and cooling and deadband and schedulable and zone and roomRef == \""+roomRef+"\"");
        HashMap<Object,Object> heatDB = CCUHsApi.getInstance().readEntity("point and heating and deadband and schedulable and zone and roomRef == \""+roomRef+"\"");

        hdb = (float) HSUtil.getPriorityVal(heatDB.get("id").toString());
        cdb = (float) HSUtil.getPriorityVal(coolDB.get("id").toString());
        HashMap<Object,Object> coolULMap = CCUHsApi.getInstance().readEntity("schedulable and point and limit and max and cooling and user and zone and roomRef == \""+roomRef+"\"");
        HashMap<Object,Object> heatULMap = CCUHsApi.getInstance().readEntity("schedulable and point and limit and min and heating and user and zone and roomRef == \""+roomRef+"\"");
        HashMap<Object,Object> coolLLMap = CCUHsApi.getInstance().readEntity("schedulable and point and limit and min and cooling and user and zone and roomRef == \""+roomRef+"\"");
        HashMap<Object,Object> heatLLMap = CCUHsApi.getInstance().readEntity("schedulable and point and limit and max and heating and user and zone and roomRef == \""+roomRef+"\"");
        heatLL = HSUtil.getPriorityVal(heatLLMap.get("id").toString());
        heatUL = HSUtil.getPriorityVal(heatULMap.get("id").toString());
        coolLL = HSUtil.getPriorityVal(coolLLMap.get("id").toString());
        coolUL = HSUtil.getPriorityVal(coolULMap.get("id").toString());
        if(mDay != null) {
            heatLL = mDay.getHeatingUserLimitMax();
            heatUL = mDay.getHeatingUserLimitMin();
            coolUL = mDay.getCoolingUserLimitMax();
            coolLL = mDay.getCoolingUserLimitMin();
            hdb = mDay.getHeatingDeadBand();
            cdb = mDay.getCoolingDeadBand();
        }
        HashMap<Object, Object> coolDT = CCUHsApi.getInstance().readEntity("point and desired and cooling and temp and equipRef == \""+p.getId()+"\"");
        HashMap<Object, Object> heatDT = CCUHsApi.getInstance().readEntity("point and desired and heating and temp and equipRef == \""+p.getId()+"\"");

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

