package a75f.io.renatus.views.MasterControl;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HHisItem;
import org.projecthaystack.HList;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.UnknownRecException;
import org.projecthaystack.client.HClient;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Zone;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.R;
import a75f.io.renatus.schedules.ScheduleUtil;
import a75f.io.renatus.util.ProgressDialogUtils;

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
        coolUL = CCUHsApi.getInstance().read("point and limit and max and cooling and user");
        heatUL = CCUHsApi.getInstance().read("point and limit and max and heating and user");
        coolLL = CCUHsApi.getInstance().read("point and limit and min and cooling and user");
        heatLL = CCUHsApi.getInstance().read("point and limit and min and heating and user");
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
        if (buildingSchedules == null){
            saveBuildingData(dialog, p.getSiteRef());
            return;
        }

        getSchedule(CCUHsApi.getInstance().getGUID(p.getSiteRef()),dialog);
    }

    private void checkForSchedules(Dialog dialog, ArrayList<Schedule> schedulesList) {
        float coolTempUL = masterControl.getUpperCoolingTemp();
        float coolTempLL = masterControl.getLowerCoolingTemp();
        float heatTempUL = masterControl.getUpperHeatingTemp();
        float heatTempLL = masterControl.getLowerHeatingTemp();

        ArrayList<String> warningMessage = new ArrayList<>();
        ArrayList<Schedule> schedules = new ArrayList<>();
        ArrayList<Zone> zoneList = new ArrayList<>();

        ArrayList<Schedule> filterSchedules = new ArrayList<>();

        HashMap tuner = CCUHsApi.getInstance().read("equip and tuner");
        Equip p = new Equip.Builder().setHashMap(tuner).build();

        coolUL = CCUHsApi.getInstance().read("point and limit and max and cooling and user");
        heatUL = CCUHsApi.getInstance().read("point and limit and max and heating and user");
        coolLL = CCUHsApi.getInstance().read("point and limit and min and cooling and user");
        heatLL = CCUHsApi.getInstance().read("point and limit and min and heating and user");
        buildingMin = CCUHsApi.getInstance().read("building and limit and min");
        buildingMax = CCUHsApi.getInstance().read("building and limit and max");

        for (Schedule s: schedulesList){
            if(s.isBuildingSchedule() && !s.isZoneSchedule()){
                filterSchedules.add(s);
            } else if (!s.isBuildingSchedule() && s.isZoneSchedule() && s.getRoomRef() != null){
                filterSchedules.add(s);
            }
        }

        // set schedule temps for building and Zones
        for (Schedule schedule : filterSchedules) {
            ArrayList<Schedule.Days> scheduleDaysList = schedule.getDays();
            schedules.add(schedule);

            for (Schedule.Days days : scheduleDaysList) {
                StringBuilder message = new StringBuilder(schedule.getDis()+ "\u0020" + ScheduleUtil.getDayString(days.getDay() + 1) + "\u0020");
                String coolValues = "";
                String heatValues = "";
                if (days.getHeatingVal() < heatTempUL || days.getHeatingVal() > heatTempLL) {
                    double heatDTValue = getHeatDTemp(days.getHeatingVal(), heatTempUL, heatTempLL);
                    heatValues = "\u0020" +    "Heating ("+ days.getHeatingVal() + "\u0020" + "\u0020" + "to" + "\u0020" + "\u0020" + heatDTValue+")";

                    days.setHeatingVal(heatDTValue);
                }

                if (days.getCoolingVal() < coolTempLL || days.getCoolingVal() > coolTempUL) {
                    double coolDTValue = getCoolDTemp(days.getCoolingVal(), coolTempLL, coolTempUL);
                    coolValues = "\u0020 " +    "Cooling ("+days.getCoolingVal() + "\u0020" + "\u0020" + "to" + "\u0020" + "\u0020" + coolDTValue + ")";

                    days.setCoolingVal(coolDTValue);
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
            disPlayWarningMessage(warningMessage, dialog, schedules, zoneList);
        } else {
            if (filterSchedules.size()> 0) {
                saveScheduleData(filterSchedules, zoneList, dialog);
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    public void getSchedule(String siteRef, Dialog dialog) {
        ProgressDialogUtils.showProgressDialog(getContext(), "Fetching global schedule data...");

        final ArrayList<Schedule> scheduleList = new ArrayList<>();
        new AsyncTask<String, Void, ArrayList<Schedule>>() {

            @Override
            protected ArrayList<Schedule> doInBackground(final String... params) {
                HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);

                HDict tDict = new HDictBuilder().add("filter", "schedule and days and siteRef == " + siteRef).toDict();
                HGrid schedulePoint = hClient.call("read", HGridBuilder.dictToGrid(tDict));
                Iterator it = schedulePoint.iterator();
                while (it.hasNext())
                {
                    HRow r = (HRow) it.next();
                    scheduleList.add(new Schedule.Builder().setHDict(new HDictBuilder().add(r).toDict()).build());
                }

                return scheduleList;
            }

            @Override
            protected void onPostExecute(ArrayList<Schedule> schedules) {
                ProgressDialogUtils.hideProgressDialog();
                checkForSchedules(dialog, schedules);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");

    }

    public ArrayList<HashMap> readAll(HGrid grid)
    {
        //CcuLog.d("CCU_HS", "Read Query: " + query);
        ArrayList<HashMap> rowList = new ArrayList<>();
        try
        {
            if (grid != null)
            {
                Iterator it = grid.iterator();
                while (it.hasNext())
                {
                    HashMap<Object, Object> map = new HashMap<>();
                    HRow                    r   = (HRow) it.next();
                    HRow.RowIterator        ri  = (HRow.RowIterator) r.iterator();
                    while (ri.hasNext())
                    {
                        HDict.MapEntry m = (HDict.MapEntry) ri.next();
                        map.put(m.getKey(), m.getValue());
                    }
                    rowList.add(map);
                }
            }
        }
        catch (UnknownRecException e)
        {
            e.printStackTrace();
        }
        return rowList;
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
        for (Schedule schedule : schedules) {
            if (schedule.isZoneSchedule() && schedule.getRoomRef()!= null) {
                    setZoneData(masterControlDialog, schedule.getRoomRef());
                    String scheduleLuid = CCUHsApi.getInstance().getLUID("@" + schedule.getId());
                    if (scheduleLuid != null && schedule.getRoomRef() != null) {
                        schedule.setId(scheduleLuid.replace("@", ""));
                        CCUHsApi.getInstance().updateZoneSchedule(schedule, schedule.getRoomRef());
                    }
                syncZoneSchedules(schedule);
            } else {
                String scheduleLuid = CCUHsApi.getInstance().getLUID("@"+schedule.getId());
                if (scheduleLuid != null) {
                    schedule.setId(scheduleLuid.replace("@",""));
                    CCUHsApi.getInstance().updateSchedule(schedule);
                }
                syncBuildingSchedules(schedule);
            }
        }

        saveBuildingData(masterControlDialog, schedules.get(0).getmSiteId());
    }

    @SuppressLint("StaticFieldLeak")
    private void syncZoneSchedules(Schedule schedule) {
        ArrayList<HDict> entities = new ArrayList<>();
        String scheduleguid = CCUHsApi.getInstance().getGUID("@"+schedule.getId());
        if ( scheduleguid != null){
            schedule.setId(scheduleguid.replace("@",""));
        }

        HDict[] days = new HDict[schedule.getDays().size()];

        for (int i = 0; i < schedule.getDays().size(); i++)
        {
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
                .add("roomRef",HRef.copy(schedule.getRoomRef()))
                .add("siteRef", HRef.copy(schedule.getmSiteId()));

        for (String marker : schedule.getMarkers())
        {
            zoneSchedule.add(marker);
        }
        entities.add( zoneSchedule.toDict());

        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(final String... params) {
                HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
                String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "addEntity", HZincWriter.gridToString(grid));
                if (response == null)
                {
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
        String scheduleguid = CCUHsApi.getInstance().getGUID("@"+schedule.getId());
        if ( scheduleguid != null){
            schedule.setId(scheduleguid.replace("@",""));
        }

        HDict[] days = new HDict[schedule.getDays().size()];

        for (int i = 0; i < schedule.getDays().size(); i++)
        {
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

        for (String marker : schedule.getMarkers())
        {
            buildingSchedule.add(marker);
        }
        entities.add(buildingSchedule.toDict());

        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(final String... params) {
                HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[0]));
                String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "addEntity", HZincWriter.gridToString(grid));
                if (response == null)
                {
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
    private void setZoneData(Dialog masterControlDialog, String roomRef) {
        //TODO:
        float coolTempUL = masterControl.getUpperCoolingTemp();
        float coolTempLL = masterControl.getLowerCoolingTemp();
        float heatTempUL = masterControl.getUpperHeatingTemp();
        float heatTempLL = masterControl.getLowerHeatingTemp();

        if (masterControlDialog != null && masterControlDialog.isShowing()) {
            masterControlDialog.dismiss();
        }

        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(final String... params) {
                HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
                HashMap ccu = CCUHsApi.getInstance().read("ccu");
                String ccuName = ccu.get("dis").toString();
                HDict tDict = new HDictBuilder().add("filter", "equip and roomRef == " + roomRef).toDict();
                HGrid hGrid = hClient.call("read", HGridBuilder.dictToGrid(tDict));
                ArrayList<HashMap> gridList = readAll(hGrid);

                if (gridList.size() == 0) {
                    return null;
                }
                Equip p = new Equip.Builder().setHashMap(gridList.get(0)).build();

                HashMap zoneCoolUL = read("point and limit and max and cooling and user");
                HashMap zoneHeatUL = read("point and limit and max and heating and user");
                HashMap zoneCoolLL = read("point and limit and min and cooling and user");
                HashMap zoneHeatLL = read("point and limit and min and heating and user");

                if (zoneCoolUL.size() != 0) {
                    writePoint(zoneCoolUL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu_"+ccuName, (double) coolTempUL, 0);
                    writeHisValById(zoneCoolUL.get("id").toString(), (double) coolTempUL);
                }

                if (zoneCoolLL.size() != 0) {
                    writePoint(zoneCoolLL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu_"+ccuName, (double) coolTempLL, 0);
                    writeHisValById(zoneCoolLL.get("id").toString(), (double) coolTempLL);
                }

                if (zoneHeatUL.size() != 0) {
                    writePoint(zoneHeatUL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu_"+ccuName, (double) heatTempUL, 0);
                    writeHisValById(zoneHeatUL.get("id").toString(), (double) heatTempUL);
                }

                if (zoneHeatLL.size() != 0) {
                    writePoint(zoneHeatLL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu_"+ccuName, (double) heatTempLL, 0);
                    writeHisValById(zoneHeatLL.get("id").toString(), (double) heatTempLL);
                }

                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                super.onPostExecute(result);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    @SuppressLint("StaticFieldLeak")
    private void saveBuildingData(Dialog dialog, String siteRef) {
        float coolTempUL = masterControl.getUpperCoolingTemp();
        float coolTempLL = masterControl.getLowerCoolingTemp();
        float heatTempUL = masterControl.getUpperHeatingTemp();
        float heatTempLL = masterControl.getLowerHeatingTemp();
        float buildingTempUL = masterControl.getUpperBuildingTemp();
        float buildingTempLL = masterControl.getLowerBuildingTemp();

        mOnClickListener.onSaveClick(heatTempLL, heatTempUL,coolTempLL,coolTempUL,buildingTempLL,buildingTempUL,
                (float) getTuner(setbackMap.get("id").toString()),(float) getTuner(zoneDiffMap.get("id").toString()),(float)hdb, (float)cdb);

        if (dialog!= null && dialog.isShowing()) {
            dialog.dismiss();
        }

        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(final String... params) {
                HashMap ccu = CCUHsApi.getInstance().read("ccu");
                String ccuName = ccu.get("dis").toString();

                HashMap tuner = CCUHsApi.getInstance().read("equip and tuner");
                Equip p = new Equip.Builder().setHashMap(tuner).build();
                String gUid  = CCUHsApi.getInstance().getGUID(p.getId());

                HashMap buildingCoolUL = read("point and limit and max and cooling and user");
                HashMap buildingHeatUL = read("point and limit and max and heating and user");
                HashMap buildingCoolLL = read("point and limit and min and cooling and user");
                HashMap buildingHeatLL = read("point and limit and min and heating and user");
                HashMap buildingMin = read("building and limit and min");
                HashMap buildingMax = read("building and limit and max");

                if (buildingCoolUL.size() != 0) {
                    writePoint(buildingCoolUL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu_"+ccuName, (double) coolTempUL, 0);
                    writeHisValById(buildingCoolUL.get("id").toString(), (double) coolTempUL);
                }

                if (buildingCoolLL.size() != 0) {
                    writePoint(buildingCoolLL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu_"+ccuName, (double) coolTempLL, 0);
                    writeHisValById(buildingCoolLL.get("id").toString(), (double) coolTempLL);
                }

                if (buildingHeatUL.size() != 0) {
                    writePoint(buildingHeatUL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu_"+ccuName, (double) heatTempUL, 0);
                    writeHisValById(buildingHeatUL.get("id").toString(), (double) heatTempUL);
                }

                if (buildingHeatLL.size() != 0) {
                    writePoint(buildingHeatLL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu_"+ccuName, (double) heatTempLL, 0);
                    writeHisValById(buildingHeatLL.get("id").toString(), (double) heatTempLL);
                }

                if (buildingMax.size() != 0) {
                    writePoint(buildingMax.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu_"+ccuName, (double) buildingTempUL, 0);
                    writeHisValById(buildingMax.get("id").toString(), (double) buildingTempUL);
                }

                if (buildingMin.size() != 0) {
                    writePoint(buildingMin.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu_"+ccuName, (double) buildingTempLL, 0);
                    writeHisValById(buildingMin.get("id").toString(), (double) buildingTempLL);
                }

                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                super.onPostExecute(result);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    public void writePoint(String guid, int level, String who, double value, int duration)
    {
        HNum val = HNum.make(value);
        HNum dur = HNum.make(duration);
        if (dur.unit == null) {
            dur = HNum.make(dur.val ,"ms");
        }

        HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        String lUid = CCUHsApi.getInstance().getLUID(guid);

        if (lUid != null){
            CCUHsApi.getInstance().getHSClient().pointWrite(HRef.copy(lUid), level, who, val, dur);
        }

        hClient.pointWrite(HRef.copy(guid), level, who, val, dur);

        if (guid != null)
        {
            HDictBuilder b = new HDictBuilder().add("id", HRef.copy(guid)).add("level", level).add("who", who).add("val", val).add("duration", dur);
            HDict[] dictArr  = {b.toDict()};
            String  response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "pointWrite", HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
            CcuLog.d("CCU_HS", "Response: \n" + response +" guid:\n" + guid);
        }
    }

    public synchronized void writeHisValById(String id, Double val)
    {
        HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        hClient.hisWrite(HRef.copy(id), new HHisItem[]{HHisItem.make(HDateTime.make(System.currentTimeMillis()), HNum.make(val))});
    }

    /**
     * Read the first matching record
     */
    @SuppressLint("StaticFieldLeak")
    public HashMap read(String query)
    {
        //CcuLog.d("CCU_HS", "Read Query: " + query);
        HashMap<Object, Object> map = new HashMap<>();
        HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HDict tDict = new HDictBuilder().add("filter", query).toDict();
        HGrid hGrid = hClient.call("read", HGridBuilder.dictToGrid(tDict));
        hGrid.dump();
        Iterator it   = hGrid.iterator();
        while (it.hasNext())
        {
            HRow                    r   = (HRow) it.next();
            HRow.RowIterator        ri  = (HRow.RowIterator) r.iterator();
            while (ri.hasNext())
            {
                HDict.MapEntry m = (HDict.MapEntry) ri.next();
                map.put(m.getKey(), m.getValue());
            }
        }
        return map;
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

    public void setOnClickChangeListener(OnClickListener l)
    {
        mOnClickListener = l;
    }

    public interface OnClickListener
    {
        void onSaveClick(float lowerHeatingTemp, float upperHeatingTemp, float lowerCoolingTemp,
                     float upperCoolingTemp, float lowerBuildingTemp, float upperBuildingTemp,
                     float setBack, float zoneDiff, float hdb, float cdb);
    }

    public void setMasterControl(float lowerHeatingTemp, float upperHeatingTemp, float lowerCoolingTemp,
                               float upperCoolingTemp, float lowerBuildingTemp, float upperBuildingTemp,
                               float setBack, float zoneDiff, float hdb, float cdb){

        if (masterControl != null)
            masterControl.setData(lowerHeatingTemp, upperHeatingTemp,
                    lowerCoolingTemp, upperCoolingTemp,
                    lowerBuildingTemp, upperBuildingTemp,
                    setBack,zoneDiff,hdb,cdb);
    }
}
