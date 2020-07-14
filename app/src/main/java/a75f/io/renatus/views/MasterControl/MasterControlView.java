package a75f.io.renatus.views.MasterControl;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HList;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.UnknownRecException;
import org.projecthaystack.client.HClient;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.logger.CcuLog;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.R;
import a75f.io.renatus.schedules.ScheduleUtil;
import a75f.io.renatus.util.ProgressDialogUtils;

import static a75f.io.renatus.util.BitmapUtil.getBitmapFromVectorDrawable;


public class MasterControlView extends LinearLayout {

    private static final String LOG_PREFIX = MasterControlView.class.getSimpleName();
    HorizontalScrollView mHorizontalScrollView;
    MasterControl masterControl;
    HashMap coolingUpperLimit;
    HashMap heatingUpperLimit;
    HashMap coolingLowerLimit;
    HashMap heatingLowerLimit;
    HashMap buildingMin;
    HashMap buildingMax;
    HashMap setbackMap;
    HashMap zoneDiffMap;
    double hdb = 2.0;
    double cdb = 2.0;
    //
    private OnClickListener mOnClickListener;

    public MasterControlView(Context context) {
        super(context);
    }

    public MasterControlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MasterControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private boolean mAdded = false;

    private void init() {

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
        coolingUpperLimit = CCUHsApi.getInstance().read("point and limit and max and cooling and user");
        heatingUpperLimit = CCUHsApi.getInstance().read("point and limit and max and heating and user");
        coolingLowerLimit = CCUHsApi.getInstance().read("point and limit and min and cooling and user");
        heatingLowerLimit = CCUHsApi.getInstance().read("point and limit and min and heating and user");
        buildingMin = CCUHsApi.getInstance().read("building and limit and min");
        buildingMax = CCUHsApi.getInstance().read("building and limit and max");
        setbackMap = CCUHsApi.getInstance().read("unoccupied and setback and equipRef == \"" + p.getId() + "\"");
        zoneDiffMap = CCUHsApi.getInstance().read("building and zone and differential");

    }

    public void setTuner(Dialog dialog) {
        HashMap tuner = CCUHsApi.getInstance().read("equip and tuner");
        Equip p = new Equip.Builder().setHashMap(tuner).build();
        Schedule buildingSchedules = Schedule.getScheduleByEquipId(p.getId());

        // initial ccu setup building/zone schedules are empty
        if (buildingSchedules == null) {
            saveBuildingData(dialog);
            return;
        }

        getSchedule(CCUHsApi.getInstance().getGUID(p.getSiteRef()), dialog);
    }

    private void checkForSchedules(Dialog dialog, ArrayList<Schedule> schedulesList) {
        float coolingTemperatureUpperLimit = masterControl.getUpperCoolingTemp();
        float coolingTemperatureLowerLimit = masterControl.getLowerCoolingTemp();
        float heatingTemperatureUpperLimit = masterControl.getUpperHeatingTemp();
        float heatingTemperatureLowerLimit = masterControl.getLowerHeatingTemp();

        ArrayList<String> warningMessage = new ArrayList<>();
        ArrayList<Schedule> schedules = new ArrayList<>();
        ArrayList<Schedule> filterSchedules = new ArrayList<>();

        coolingUpperLimit = CCUHsApi.getInstance().read("point and limit and max and cooling and user");
        heatingUpperLimit = CCUHsApi.getInstance().read("point and limit and max and heating and user");
        coolingLowerLimit = CCUHsApi.getInstance().read("point and limit and min and cooling and user");
        heatingLowerLimit = CCUHsApi.getInstance().read("point and limit and min and heating and user");
        buildingMin = CCUHsApi.getInstance().read("building and limit and min");
        buildingMax = CCUHsApi.getInstance().read("building and limit and max");

        for (Schedule s : schedulesList) {
            if (s.isBuildingSchedule() && !s.isZoneSchedule()) {
                filterSchedules.add(s);
            } else if (!s.isBuildingSchedule() && s.isZoneSchedule() && s.getRoomRef() != null) {
                filterSchedules.add(s);
            }
        }

        CcuLog.i(LOG_PREFIX, "Filtered list to " + filterSchedules.size() + " building and zone schedules");


        // set schedule temps for building and Zones
        for (Schedule schedule : filterSchedules) {
            ArrayList<Schedule.Days> scheduleDaysList = schedule.getDays();
            schedules.add(schedule);

            for (Schedule.Days days : scheduleDaysList) {
                StringBuilder message = new StringBuilder(schedule.getDis() + "\u0020" + ScheduleUtil.getDayString(days.getDay() + 1) + "\u0020");
                String coolValues = "";
                String heatValues = "";
                if (days.getHeatingVal() < heatingTemperatureUpperLimit || days.getHeatingVal() > heatingTemperatureLowerLimit) {
                    double heatingDesiredTemperatureValue = getHeatingDesiredTemperature(days.getHeatingVal(), heatingTemperatureUpperLimit, heatingTemperatureLowerLimit);
                    heatValues = "\u0020" + "Heating (" + days.getHeatingVal() + "\u0020" + "\u0020" + "to" + "\u0020" + "\u0020" + heatingDesiredTemperatureValue + ")";

                    days.setHeatingVal(heatingDesiredTemperatureValue);
                }

                if (days.getCoolingVal() < coolingTemperatureLowerLimit || days.getCoolingVal() > coolingTemperatureUpperLimit) {
                    double coolingDesiredTemperatureValue = getCoolingDesiredTemperature(days.getCoolingVal(), coolingTemperatureLowerLimit, coolingTemperatureUpperLimit);
                    coolValues = "\u0020 " + "Cooling (" + days.getCoolingVal() + "\u0020" + "\u0020" + "to" + "\u0020" + "\u0020" + coolingDesiredTemperatureValue + ")";

                    days.setCoolingVal(coolingDesiredTemperatureValue);
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

        if (warningMessage.size() > 0) {
            disPlayWarningMessage(warningMessage, dialog, schedules);
        } else {
            if (filterSchedules.size() > 0) {
                saveScheduleData(filterSchedules, dialog);
            } else {
                saveBuildingData(dialog);
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    public void getSchedule(String siteRef, Dialog dialog) {
        ProgressDialogUtils.showProgressDialog(getContext(), "Fetching global schedules...");

        final ArrayList<Schedule> scheduleList = new ArrayList<>();
        new AsyncTask<String, Void, ArrayList<Schedule>>() {

            @Override
            protected ArrayList<Schedule> doInBackground(final String... params) {
                HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);

                HDict tDict = new HDictBuilder().add("filter", "schedule and days and siteRef == " + siteRef).toDict();
                HGrid schedulePoint = hClient.call("read", HGridBuilder.dictToGrid(tDict));
                if (schedulePoint != null) {
                    Iterator it = schedulePoint.iterator();
                    while (it.hasNext()) {
                        HRow r = (HRow) it.next();
                        scheduleList.add(new Schedule.Builder().setHDict(new HDictBuilder().add(r).toDict()).build());
                    }
                }

                CcuLog.i(LOG_PREFIX, "Retrieved schedule list of size " + scheduleList.size() + " for site " + siteRef);

                return scheduleList;
            }

            @Override
            protected void onPostExecute(ArrayList<Schedule> schedules) {
                ProgressDialogUtils.hideProgressDialog();
                if (scheduleList.isEmpty()) {
                    Toast.makeText(getContext(), "Unable to fetch schedules, please confirm your WiFi connectivity.", Toast.LENGTH_LONG).show();
                } else {
                    checkForSchedules(dialog, schedules);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");

    }

    public ArrayList<HashMap> readAll(HGrid grid) {
        ArrayList<HashMap> rowList = new ArrayList<>();
        try {
            if (grid != null) {
                Iterator it = grid.iterator();
                while (it.hasNext()) {
                    HashMap<Object, Object> map = new HashMap<>();
                    HRow r = (HRow) it.next();
                    HRow.RowIterator ri = (HRow.RowIterator) r.iterator();
                    while (ri.hasNext()) {
                        HDict.MapEntry m = (HDict.MapEntry) ri.next();
                        map.put(m.getKey(), m.getValue());
                    }
                    rowList.add(map);
                }
            }
        } catch (UnknownRecException e) {
            e.printStackTrace();
        }
        return rowList;
    }

    private void disPlayWarningMessage(ArrayList<String> warningMessage, Dialog masterControlDialog, ArrayList<Schedule> schedules) {
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

            saveScheduleData(schedules, masterControlDialog);
            warningDialog.dismiss();
            if (masterControlDialog != null && masterControlDialog.isShowing()) {
                masterControlDialog.dismiss();
            }
        });

        warningDialog.show();
    }

    private void saveScheduleData(ArrayList<Schedule> schedules, Dialog masterControlDialog) {
        for (Schedule schedule : schedules) {
            if (schedule.isZoneSchedule() && schedule.getRoomRef() != null) {
                String scheduleLuid = CCUHsApi.getInstance().getLUID("@" + schedule.getId());
                if (scheduleLuid != null && schedule.getRoomRef() != null) {
                    schedule.setId(scheduleLuid.replace("@", ""));
                    CCUHsApi.getInstance().updateZoneSchedule(schedule, schedule.getRoomRef());
                }
                syncZoneSchedules(schedule);
            } else {
                String scheduleLuid = CCUHsApi.getInstance().getLUID("@" + schedule.getId());
                if (scheduleLuid != null) {
                    schedule.setId(scheduleLuid.replace("@", ""));
                    CCUHsApi.getInstance().updateSchedule(schedule);
                }
                syncBuildingSchedules(schedule);
            }
        }

        saveBuildingData(masterControlDialog);
    }

    @SuppressLint("StaticFieldLeak")
    private void syncZoneSchedules(Schedule schedule) {
        ArrayList<HDict> entities = new ArrayList<>();
        String scheduleguid = CCUHsApi.getInstance().getGUID("@" + schedule.getId());
        if (scheduleguid != null) {
            schedule.setId(scheduleguid.replace("@", ""));
        }

        HDict[] days = new HDict[schedule.getDays().size()];

        for (int i = 0; i < schedule.getDays().size(); i++) {
            Schedule.Days day = schedule.getDays().get(i);
            HDictBuilder hDictDay = new HDictBuilder()
                    .add("day", HNum.make(day.getDay()))
                    .add("sthh", HNum.make(day.getSthh()))
                    .add("stmm", HNum.make(day.getStmm()))
                    .add("ethh", HNum.make(day.getEthh()))
                    .add("etmm", HNum.make(day.getEtmm()));
            if (day.getHeatingVal() != null)
                hDictDay.add("heatVal", HNum.make(day.getHeatingVal()));
            if (day.getCoolingVal() != null)
                hDictDay.add("coolVal", HNum.make(day.getCoolingVal()));
            if (day.getVal() != null)
                hDictDay.add("curVal", HNum.make(day.getVal()));

            //need boolean & string support
            if (day.isSunset()) hDictDay.add("sunset", day.isSunset());
            if (day.isSunrise()) hDictDay.add("sunrise", day.isSunrise());

            days[i] = hDictDay.toDict();
        }

        HList hList = HList.make(days);
        HDictBuilder zoneSchedule = new HDictBuilder()
                .add("id", HRef.copy(schedule.getId()))
                .add("unit", schedule.getUnit())
                .add("kind", schedule.getKind())
                .add("dis", schedule.getDis())
                .add("days", hList)
                .add("roomRef", HRef.copy(schedule.getRoomRef()))
                .add("siteRef", HRef.copy(schedule.getmSiteId()));

        for (String marker : schedule.getMarkers()) {
            zoneSchedule.add(marker);
        }
        entities.add(zoneSchedule.toDict());

        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(final String... params) {
                HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
                String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "addEntity", HZincWriter.gridToString(grid));
                if (response == null) {
                    CcuLog.i("CCU_HS_SYNC", "Aborting Schedule Sync");
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");

    }

    @SuppressLint("StaticFieldLeak")
    private void syncBuildingSchedules(Schedule schedule) {
        ArrayList<HDict> entities = new ArrayList<>();
        String scheduleguid = CCUHsApi.getInstance().getGUID("@" + schedule.getId());
        if (scheduleguid != null) {
            schedule.setId(scheduleguid.replace("@", ""));
        }

        HDict[] days = new HDict[schedule.getDays().size()];

        for (int i = 0; i < schedule.getDays().size(); i++) {
            Schedule.Days day = schedule.getDays().get(i);
            HDictBuilder hDictDay = new HDictBuilder()
                    .add("day", HNum.make(day.getDay()))
                    .add("sthh", HNum.make(day.getSthh()))
                    .add("stmm", HNum.make(day.getStmm()))
                    .add("ethh", HNum.make(day.getEthh()))
                    .add("etmm", HNum.make(day.getEtmm()));
            if (day.getHeatingVal() != null)
                hDictDay.add("heatVal", HNum.make(day.getHeatingVal()));
            if (day.getCoolingVal() != null)
                hDictDay.add("coolVal", HNum.make(day.getCoolingVal()));
            if (day.getVal() != null)
                hDictDay.add("curVal", HNum.make(day.getVal()));

            //need boolean & string support
            if (day.isSunset()) hDictDay.add("sunset", day.isSunset());
            if (day.isSunrise()) hDictDay.add("sunrise", day.isSunrise());

            days[i] = hDictDay.toDict();
        }

        HList hList = HList.make(days);
        HDictBuilder buildingSchedule = new HDictBuilder()
                .add("id", HRef.copy(schedule.getId()))
                .add("unit", schedule.getUnit())
                .add("kind", schedule.getKind())
                .add("dis", schedule.getDis())
                .add("days", hList)
                .add("siteRef", HRef.copy(schedule.getmSiteId()));

        for (String marker : schedule.getMarkers()) {
            buildingSchedule.add(marker);
        }
        entities.add(buildingSchedule.toDict());

        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(final String... params) {
                HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[0]));
                String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "addEntity", HZincWriter.gridToString(grid));
                if (response == null) {
                    CcuLog.i("CCU_HS_SYNC", "Aborting Schedule Sync");
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");

    }

    @SuppressLint("StaticFieldLeak")
    private void saveBuildingData(Dialog dialog) {
        float coolingTemperatureUpperLimit = masterControl.getUpperCoolingTemp();
        float coolingTemperatureLowerLimit = masterControl.getLowerCoolingTemp();
        float heatingTemperatureUpperLimit = masterControl.getUpperHeatingTemp();
        float heatingTemperatureLowerLimit = masterControl.getLowerHeatingTemp();
        float buildingTemperatureUpperLimit = masterControl.getUpperBuildingTemp();
        float buildingTemperatureLowerLimit = masterControl.getLowerBuildingTemp();

        mOnClickListener.onSaveClick(heatingTemperatureLowerLimit, heatingTemperatureUpperLimit, coolingTemperatureLowerLimit, coolingTemperatureUpperLimit, buildingTemperatureLowerLimit, buildingTemperatureUpperLimit,
                (float) getTuner(setbackMap.get("id").toString()), (float) getTuner(zoneDiffMap.get("id").toString()), (float) hdb, (float) cdb);

        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(final String... params) {
                HashMap ccu = CCUHsApi.getInstance().read("ccu");
                String ccuName = ccu.get("dis").toString();

                HashMap buildingCoolingUpperLimit = CCUHsApi.getInstance().read("point and limit and max and cooling and user");
                HashMap buildingHeatingUpperLimit = CCUHsApi.getInstance().read("point and limit and max and heating and user");
                HashMap buildingCoolingLowerLimit = CCUHsApi.getInstance().read("point and limit and min and cooling and user");
                HashMap buildingHeatingLowerLimit = CCUHsApi.getInstance().read("point and limit and min and heating and user");
                HashMap buildingMin = CCUHsApi.getInstance().read("building and limit and min");
                HashMap buildingMax = CCUHsApi.getInstance().read("building and limit and max");

                if (buildingCoolingUpperLimit.size() != 0) {
                    CCUHsApi.getInstance().writePoint(buildingCoolingUpperLimit.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu_" + ccuName, (double) coolingTemperatureUpperLimit, 0);
                    CCUHsApi.getInstance().writeHisValById(buildingCoolingUpperLimit.get("id").toString(), (double) coolingTemperatureUpperLimit);
                }

                if (buildingCoolingLowerLimit.size() != 0) {
                    CCUHsApi.getInstance().writePoint(buildingCoolingLowerLimit.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu_" + ccuName, (double) coolingTemperatureLowerLimit, 0);
                    CCUHsApi.getInstance().writeHisValById(buildingCoolingLowerLimit.get("id").toString(), (double) coolingTemperatureLowerLimit);
                }

                if (buildingHeatingUpperLimit.size() != 0) {
                    CCUHsApi.getInstance().writePoint(buildingHeatingUpperLimit.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu_" + ccuName, (double) heatingTemperatureUpperLimit, 0);
                    CCUHsApi.getInstance().writeHisValById(buildingHeatingUpperLimit.get("id").toString(), (double) heatingTemperatureUpperLimit);
                }

                if (buildingHeatingLowerLimit.size() != 0) {
                    CCUHsApi.getInstance().writePoint(buildingHeatingLowerLimit.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu_" + ccuName, (double) heatingTemperatureLowerLimit, 0);
                    CCUHsApi.getInstance().writeHisValById(buildingHeatingLowerLimit.get("id").toString(), (double) heatingTemperatureLowerLimit);
                }

                if (buildingMax.size() != 0) {
                    CCUHsApi.getInstance().writePoint(buildingMax.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu_" + ccuName, (double) buildingTemperatureUpperLimit, 0);
                    CCUHsApi.getInstance().writeHisValById(buildingMax.get("id").toString(), (double) buildingTemperatureUpperLimit);
                }

                if (buildingMin.size() != 0) {
                    CCUHsApi.getInstance().writePoint(buildingMin.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu_" + ccuName, (double) buildingTemperatureLowerLimit, 0);
                    CCUHsApi.getInstance().writeHisValById(buildingMin.get("id").toString(), (double) buildingTemperatureLowerLimit);
                }

                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                super.onPostExecute(result);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    private double getCoolingDesiredTemperature(double coolingDesiredTemperature, double coolingTemperatureLowerLimit,
                                                double coolingTemperatureUpperLimit) {

        double distance1 = Math.abs(coolingTemperatureLowerLimit - coolingDesiredTemperature);
        double distance2 = Math.abs(coolingTemperatureUpperLimit - coolingDesiredTemperature);
        if (distance1 < distance2) {
            coolingDesiredTemperature = coolingTemperatureLowerLimit;
        } else {
            coolingDesiredTemperature = coolingTemperatureUpperLimit;
        }

        return coolingDesiredTemperature;
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

    private double getHeatingDesiredTemperature(double heatingDesiredTemperature, double heatingTemperatureUpperLimit,
                                                double heatingTemperatureLowerLimit) {
        double distance1 = Math.abs(heatingTemperatureLowerLimit - heatingDesiredTemperature);
        double distance2 = Math.abs(heatingTemperatureUpperLimit - heatingDesiredTemperature);
        if (distance1 < distance2) {
            heatingDesiredTemperature = heatingTemperatureLowerLimit;
        } else {
            heatingDesiredTemperature = heatingTemperatureUpperLimit;
        }
        return heatingDesiredTemperature;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setOnClickChangeListener(OnClickListener l) {
        mOnClickListener = l;
    }

    public interface OnClickListener {
        void onSaveClick(float lowerHeatingTemp, float upperHeatingTemp, float lowerCoolingTemp,
                         float upperCoolingTemp, float lowerBuildingTemp, float upperBuildingTemp,
                         float setBack, float zoneDiff, float hdb, float cdb);
    }

    public void setMasterControl(float lowerHeatingTemp, float upperHeatingTemp, float lowerCoolingTemp,
                                 float upperCoolingTemp, float lowerBuildingTemp, float upperBuildingTemp,
                                 float setBack, float zoneDiff, float hdb, float cdb) {

        if (masterControl != null)
            masterControl.setData(lowerHeatingTemp, upperHeatingTemp,
                    lowerCoolingTemp, upperCoolingTemp,
                    lowerBuildingTemp, upperBuildingTemp,
                    setBack, zoneDiff, hdb, cdb);
    }
}
