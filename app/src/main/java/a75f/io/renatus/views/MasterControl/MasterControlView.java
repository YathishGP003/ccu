package a75f.io.renatus.views.MasterControl;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Zone;
import a75f.io.logic.L;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.R;
import a75f.io.renatus.schedules.ScheduleUtil;

import static a75f.io.renatus.util.BitmapUtil.getBitmapFromVectorDrawable;


public class MasterControlView extends LinearLayout {

    private static final int ANGLE_WIDTH = 2;
    HorizontalScrollView mHorizontalScrollView;
    MasterControl masterControl;
    HashMap coolUL;
    HashMap heatUL;
    HashMap coolLL;
    HashMap heatLL;
    HashMap buildingMin;
    HashMap buildingMax;
    double hdb = 2.0;
    double cdb = 2.0;
    boolean isDeadBandWarning;
    private AlertDialog deadBandAlert;

    public MasterControlView(Context context) {
        super(context);
    }

    public MasterControlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public MasterControlView(Context context, AttributeSet attrs, int defStyleAttr) {
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


    private int mImageWidth = 40;
    private int mImagePadding = 25;

    private void add() {
        isDeadBandWarning = false;
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        int angleWH = (int) (ANGLE_WIDTH * displayMetrics.density);

        ImageButton arrowLeftImageButton = new ImageButton(getContext());
        ImageButton arrowRightImageButton = new ImageButton(getContext());

        arrowLeftImageButton.setImageBitmap(getBitmapFromVectorDrawable(getContext(), R.drawable.ic_angle_left));
        arrowRightImageButton.setImageBitmap(getBitmapFromVectorDrawable(getContext(), R.drawable.ic_angle_right));
        arrowLeftImageButton.setPadding(mImagePadding, mImagePadding, mImagePadding / 2, mImagePadding);
        arrowRightImageButton.setPadding(mImagePadding / 2, mImagePadding, mImagePadding, mImagePadding);

        arrowRightImageButton.setBackgroundColor(Color.TRANSPARENT);
        arrowLeftImageButton.setBackgroundColor(Color.TRANSPARENT);

        LayoutParams mArrowImageButtonLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        mArrowImageButtonLayoutParams.gravity = Gravity.CENTER_VERTICAL;

        mHorizontalScrollView = new HorizontalScrollView(this.getContext());
        mHorizontalScrollView.setFillViewport(true);
        mHorizontalScrollView.setHorizontalScrollBarEnabled(false);
        mHorizontalScrollView.setScrollBarSize(0);


        //mHorizontalScrollView.setEnabled(false);
        ViewGroup.LayoutParams masterControlLayoutParams =
                new ViewGroup.LayoutParams(0, LayoutParams.MATCH_PARENT);

        masterControl = new MasterControl(this.getContext());
        masterControl.setMinimumWidth(getMeasuredWidth() - (mImageWidth * 2));
        mHorizontalScrollView.addView(masterControl, masterControlLayoutParams);


        //Disable touch.
        masterControl.setOnTouchListener((v, event) -> false);

        this.setOrientation(LinearLayout.HORIZONTAL);

        LayoutParams horizontalScrollViewLayoutParams =
                new LayoutParams(0, LayoutParams.MATCH_PARENT);
        horizontalScrollViewLayoutParams.weight = 1;

        System.out.println("Is Measured Width Known: " + arrowLeftImageButton.getMeasuredWidth());
        this.addView(arrowLeftImageButton, mArrowImageButtonLayoutParams);
        this.addView(mHorizontalScrollView, horizontalScrollViewLayoutParams);
        this.addView(arrowRightImageButton, mArrowImageButtonLayoutParams);

        arrowLeftImageButton.setOnClickListener(v -> mHorizontalScrollView.arrowScroll(View.FOCUS_LEFT));

        arrowRightImageButton.setOnClickListener(v -> mHorizontalScrollView.arrowScroll(View.FOCUS_RIGHT));

        updateData();
    }

    private void updateData() {
        HashMap tuner = CCUHsApi.getInstance().read("equip and tuner");
        Equip p = new Equip.Builder().setHashMap(tuner).build();

        hdb = TunerUtil.getHeatingDeadband(p.getId());
        cdb = TunerUtil.getCoolingDeadband(p.getId());
        coolUL = CCUHsApi.getInstance().read("point and limit and max and cooling and user and equipRef == \"" + p.getId() + "\"");
        heatUL = CCUHsApi.getInstance().read("point and limit and max and heating and user and equipRef == \"" + p.getId() + "\"");
        coolLL = CCUHsApi.getInstance().read("point and limit and min and cooling and user and equipRef == \"" + p.getId() + "\"");
        heatLL = CCUHsApi.getInstance().read("point and limit and min and heating and user and equipRef == \"" + p.getId() + "\"");
        buildingMin = CCUHsApi.getInstance().read("building and limit and min and equipRef == \"" + p.getId() + "\"");
        buildingMax = CCUHsApi.getInstance().read("building and limit and max and equipRef == \"" + p.getId() + "\"");
        HashMap setbackMap = CCUHsApi.getInstance().read("unoccupied and setback and equipRef == \"" + p.getId() + "\"");
        HashMap zoneDiffMap = CCUHsApi.getInstance().read("building and zone and differential and equipRef == \"" + p.getId() + "\"");

        masterControl.setData((float) getTuner(heatLL.get("id").toString()), (float) getTuner(heatUL.get("id").toString()),
                (float) getTuner(coolLL.get("id").toString()), (float) getTuner(coolUL.get("id").toString()),
                (float) getTuner(buildingMin.get("id").toString()), (float) getTuner(buildingMax.get("id").toString()),
                (float) getTuner(setbackMap.get("id").toString()), (float) getTuner(zoneDiffMap.get("id").toString()));
    }

    public void setTuner(Dialog dialog) {
        float coolTempUL = masterControl.getUpperCoolingTemp();
        float coolTempLL = masterControl.getLowerCoolingTemp();
        float heatTempUL = masterControl.getUpperHeatingTemp();
        float heatTempLL = masterControl.getLowerHeatingTemp();

        ArrayList<String> warningMessage = new ArrayList<>();
        ArrayList<Schedule> schedules = new ArrayList<>();
        ArrayList<Zone> zoneList = new ArrayList<>();

        HashMap tuner = CCUHsApi.getInstance().read("equip and tuner");
        Equip p = new Equip.Builder().setHashMap(tuner).build();
        coolUL = CCUHsApi.getInstance().read("point and limit and max and cooling and user and equipRef == \"" + p.getId() + "\"");
        heatUL = CCUHsApi.getInstance().read("point and limit and max and heating and user and equipRef == \"" + p.getId() + "\"");
        coolLL = CCUHsApi.getInstance().read("point and limit and min and cooling and user and equipRef == \"" + p.getId() + "\"");
        heatLL = CCUHsApi.getInstance().read("point and limit and min and heating and user and equipRef == \"" + p.getId() + "\"");
        buildingMin = CCUHsApi.getInstance().read("building and limit and min and equipRef == \"" + p.getId() + "\"");
        buildingMax = CCUHsApi.getInstance().read("building and limit and max and equipRef == \"" + p.getId() + "\"");

        Schedule buildingSchedules = Schedule.getScheduleByEquipId(p.getId());
        schedules.add(buildingSchedules);

        // set schedule temps for building
        for (Schedule.Days buidlingdays : buildingSchedules.getDays()) {
            StringBuilder message = new StringBuilder("Building" + "\u0020" + ScheduleUtil.getDayString(buidlingdays.getDay() + 1) + "\u0020" + "Schedule");
            String coolValues = "";
            String heatValues = "";
            if (buidlingdays.getHeatingVal() < heatTempUL || buidlingdays.getHeatingVal() > heatTempLL) {
                double heatDTValue = getHeatDTemp(buidlingdays.getHeatingVal(), heatTempUL, heatTempLL);
                heatValues = "\u0020" +   "Heating ("+buidlingdays.getHeatingVal() + "\u0020" + "\u0020" + "to" + "\u0020" + "\u0020" + heatDTValue+")";

                buidlingdays.setHeatingVal(heatDTValue);
                if ((buidlingdays.getCoolingVal() - heatDTValue) < (float) (cdb + hdb)) {
                    displayDeadBandWarning(buidlingdays.getCoolingVal(), heatDTValue);
                    return;
                }
            }

            if (buidlingdays.getCoolingVal() < coolTempLL || buidlingdays.getCoolingVal() > coolTempUL) {
                double coolDTValue = getCoolDTemp(buidlingdays.getCoolingVal(), coolTempLL, coolTempUL);
                coolValues = "\u0020 " +   "Cooling ("+buidlingdays.getCoolingVal() + "\u0020" + "\u0020" + "to" + "\u0020" + "\u0020" + coolDTValue + ")";

                buidlingdays.setCoolingVal(coolDTValue);
                if ((coolDTValue - buidlingdays.getHeatingVal()) < (float) (cdb + hdb)) {
                    displayDeadBandWarning(coolDTValue, buidlingdays.getHeatingVal());
                    return;
                }
            }

            if (!TextUtils.isEmpty(coolValues) && !TextUtils.isEmpty(heatValues)) {
                message.append(coolValues).append(heatValues);
                warningMessage.add("\n" + message);
            } else if (!TextUtils.isEmpty(coolValues) && TextUtils.isEmpty(heatValues)) {
                message.append(coolValues);
                warningMessage.add("\n" + message);
            } else if (TextUtils.isEmpty(coolValues) && !TextUtils.isEmpty(heatValues)) {
                message.append(heatValues);
                warningMessage.add("\n" + message);
            }
        }

        // set schedule temps for Zones
        for (Floor floor : HSUtil.getFloors()) {
            zoneList = HSUtil.getZones(floor.getId());

            for (Zone zone : zoneList) {

                Schedule zoneSchedule = CCUHsApi.getInstance().getScheduleById(zone.getScheduleRef());
                ArrayList<Schedule.Days> scheduleDaysList = zoneSchedule.getDays();
                schedules.add(zoneSchedule);


                for (Schedule.Days days : scheduleDaysList) {
                    StringBuilder message = new StringBuilder(zone.getDisplayName() + "\u0020" + ScheduleUtil.getDayString(days.getDay() + 1) + "\u0020" + "Schedule");
                    String coolValues = "";
                    String heatValues = "";
                    if (days.getHeatingVal() < heatTempUL || days.getHeatingVal() > heatTempLL) {
                        double heatDTValue = getHeatDTemp(days.getHeatingVal(), heatTempUL, heatTempLL);
                        heatValues = "\u0020" +    "Heating ("+ days.getHeatingVal() + "\u0020" + "\u0020" + "to" + "\u0020" + "\u0020" + heatDTValue+")";

                        days.setHeatingVal(heatDTValue);

                        if ((days.getCoolingVal() - heatDTValue) < (float) (cdb + hdb)) {
                            displayDeadBandWarning(days.getCoolingVal(), heatDTValue);
                            return;
                        }
                    }

                    if (days.getCoolingVal() < coolTempLL || days.getCoolingVal() > coolTempUL) {
                        double coolDTValue = getCoolDTemp(days.getCoolingVal(), coolTempLL, coolTempUL);
                        coolValues = "\u0020 " +    "Cooling ("+days.getCoolingVal() + "\u0020" + "\u0020" + "to" + "\u0020" + "\u0020" + coolDTValue + ")";

                        days.setCoolingVal(coolDTValue);
                        if ((coolDTValue - days.getHeatingVal()) < (float) (cdb + hdb)) {
                            displayDeadBandWarning(coolDTValue, days.getHeatingVal());
                            return;
                        }
                    }

                    if (!TextUtils.isEmpty(coolValues) && !TextUtils.isEmpty(heatValues)) {
                        message.append(coolValues).append(heatValues);
                        warningMessage.add("\n" + message);
                    } else if (!TextUtils.isEmpty(coolValues) && TextUtils.isEmpty(heatValues)) {
                        message.append(coolValues);
                        warningMessage.add("\n" + message);
                    } else if (TextUtils.isEmpty(coolValues) && !TextUtils.isEmpty(heatValues)) {
                        message.append(heatValues);
                        warningMessage.add("\n" + message);
                    }
                }

            }
        }

        if (warningMessage.size() > 0) {
            disPlayWarningMessage(warningMessage, dialog, schedules, zoneList);
        } else {
            if (!isDeadBandWarning) {
                saveBuildingData(dialog);

                for (Zone zone : zoneList) {
                    setZoneData(zone.getId());
                }
            }
        }
    }

    private void displayDeadBandWarning(double coolingVal, double heatDTValue) {
        isDeadBandWarning = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("Cooling (" + coolingVal + ") and Heating (" + heatDTValue + ") difference should maintain to Deadband limit (" + (hdb + cdb) + ") !")
                .setCancelable(false)
                .setPositiveButton("Re-Edit", (dialog, id) -> {
                    isDeadBandWarning = false;
                    dialog.dismiss();
                });
        deadBandAlert = builder.create();

        if (deadBandAlert != null && !deadBandAlert.isShowing()) {
            deadBandAlert.show();
        }
    }

    private void disPlayWarningMessage(ArrayList<String> warningMessage, Dialog masterControlDialog, ArrayList<Schedule> schedules, ArrayList<Zone> zoneList) {
        final Dialog warningDialog = new Dialog(getContext());
        warningDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        warningDialog.setCancelable(false);
        warningDialog.setContentView(R.layout.dialog_warning_master_control);
        TextView warning = warningDialog.findViewById(R.id.warningMessage);
        warning.setText(warningMessage.toString().replace("[", "").replace("]", ""));

        warningDialog.findViewById(R.id.btnDiscard).setOnClickListener(view -> {
            if (masterControlDialog != null && masterControlDialog.isShowing()) {
                masterControlDialog.dismiss();
            }
            warningDialog.dismiss();
        });

        warningDialog.findViewById(R.id.btnEdit).setOnClickListener(view -> warningDialog.dismiss());

        warningDialog.findViewById(R.id.btnForceTrim).setOnClickListener(view -> {

            saveScheduleData(schedules, zoneList, masterControlDialog);
            warningDialog.dismiss();
            if (masterControlDialog != null && masterControlDialog.isShowing()) {
                masterControlDialog.dismiss();
            }
        });

        warningDialog.show();
    }

    private void saveScheduleData(ArrayList<Schedule> schedules, ArrayList<Zone> zoneList, Dialog masterControlDialog) {
        //TODO:
        for (Zone zone : zoneList) {
            setZoneData(zone.getId());
        }

        for (Schedule schedule : schedules) {

            if (schedule.isZoneSchedule()) {
                CCUHsApi.getInstance().updateZoneSchedule(schedule, schedule.getRoomRef());
            } else {
                CCUHsApi.getInstance().updateSchedule(schedule);
            }
            CCUHsApi.getInstance().syncEntityTree();
            ScheduleProcessJob.updateSchedules();
            L.saveCCUState();
        }

        saveBuildingData(masterControlDialog);
    }

    @SuppressLint("StaticFieldLeak")
    private void setZoneData(String roomRef) {
        //TODO:
        float coolTempUL = masterControl.getUpperCoolingTemp();
        float coolTempLL = masterControl.getLowerCoolingTemp();
        float heatTempUL = masterControl.getUpperHeatingTemp();
        float heatTempLL = masterControl.getLowerHeatingTemp();

        Equip p = HSUtil.getEquipFromZone(roomRef);

        HashMap zoneCoolUL = CCUHsApi.getInstance().read("point and limit and max and cooling and user and equipRef == \"" + p.getId() + "\"");
        HashMap zoneHeatUL = CCUHsApi.getInstance().read("point and limit and max and heating and user and equipRef == \"" + p.getId() + "\"");
        HashMap zoneCoolLL = CCUHsApi.getInstance().read("point and limit and min and cooling and user and equipRef == \"" + p.getId() + "\"");
        HashMap zoneHeatLL = CCUHsApi.getInstance().read("point and limit and min and heating and user and equipRef == \"" + p.getId() + "\"");

        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(final String... params) {
                CCUHsApi.getInstance().writePoint(zoneCoolUL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu", (double) coolTempUL, 0);
                CCUHsApi.getInstance().writeHisValById(zoneCoolUL.get("id").toString(), (double) coolTempUL);

                CCUHsApi.getInstance().writePoint(zoneCoolLL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu", (double) coolTempLL, 0);
                CCUHsApi.getInstance().writeHisValById(zoneCoolLL.get("id").toString(), (double) coolTempLL);

                CCUHsApi.getInstance().writePoint(zoneHeatUL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu", (double) heatTempUL, 0);
                CCUHsApi.getInstance().writeHisValById(zoneHeatUL.get("id").toString(), (double) heatTempUL);

                CCUHsApi.getInstance().writePoint(zoneHeatLL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu", (double) heatTempLL, 0);
                CCUHsApi.getInstance().writeHisValById(zoneHeatLL.get("id").toString(), (double) heatTempLL);

                L.saveCCUState();
                CCUHsApi.getInstance().syncEntityTree();
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    @SuppressLint("StaticFieldLeak")
    private void saveBuildingData(Dialog dialog) {
        float coolTempUL = masterControl.getUpperCoolingTemp();
        float coolTempLL = masterControl.getLowerCoolingTemp();
        float heatTempUL = masterControl.getUpperHeatingTemp();
        float heatTempLL = masterControl.getLowerHeatingTemp();
        float buildingTempUL = masterControl.getUpperBuildingTemp();
        float buildingTempLL = masterControl.getLowerBuildingTemp();

        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(final String... params) {
                CCUHsApi.getInstance().writePoint(coolUL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu", (double) coolTempUL, 0);
                CCUHsApi.getInstance().writeHisValById(coolUL.get("id").toString(), (double) coolTempUL);

                CCUHsApi.getInstance().writePoint(coolLL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu", (double) coolTempLL, 0);
                CCUHsApi.getInstance().writeHisValById(coolLL.get("id").toString(), (double) coolTempLL);

                CCUHsApi.getInstance().writePoint(heatUL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu", (double) heatTempUL, 0);
                CCUHsApi.getInstance().writeHisValById(heatUL.get("id").toString(), (double) heatTempUL);

                CCUHsApi.getInstance().writePoint(heatLL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu", (double) heatTempLL, 0);
                CCUHsApi.getInstance().writeHisValById(heatLL.get("id").toString(), (double) heatTempLL);

                CCUHsApi.getInstance().writePoint(buildingMax.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu", (double) buildingTempUL, 0);
                CCUHsApi.getInstance().writeHisValById(buildingMax.get("id").toString(), (double) buildingTempUL);

                CCUHsApi.getInstance().writePoint(buildingMin.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu", (double) buildingTempLL, 0);
                CCUHsApi.getInstance().writeHisValById(buildingMin.get("id").toString(), (double) buildingTempLL);

                L.saveCCUState();
                CCUHsApi.getInstance().syncEntityTree();
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                // continue what you are doing...
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    private double getCoolDTemp(double coolDT, double coolTempLL, double coolTempUL) {

        double distance1 = Math.abs(coolTempLL - coolDT);
        double distance2 = Math.abs(coolTempUL - coolDT);
        if (distance1 < distance2) {
            coolDT = coolTempLL;
        } else {
            coolDT = coolTempUL;
        }

        return coolDT;
    }

    public static double getTuner(String id) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList values = hayStack.readPoint(id);
        if (values != null && values.size() > 0) {
            for (int l = 1; l <= values.size(); l++) {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }

    private double getHeatDTemp(double heatDT, double heatTempUL, double heatTempLL) {
        double distance1 = Math.abs(heatTempLL - heatDT);
        double distance2 = Math.abs(heatTempUL - heatDT);
        if (distance1 < distance2) {
            heatDT = heatTempLL;
        } else {
            heatDT = heatTempUL;
        }
        return heatDT;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);


        float height = getDefaultSize(getSuggestedMinimumHeight(),
                heightMeasureSpec);
        float width = getDefaultSize(getSuggestedMinimumWidth(),
                widthMeasureSpec);

    }
}
