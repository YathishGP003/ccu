package a75f.io.renatus.views.MasterControl;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsius;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.domain.api.Domain;
import a75f.io.logger.CcuLog;
import a75f.io.logic.bo.util.UnitUtils;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.renatus.R;
import a75f.io.renatus.registration.InstallerOptions;
import a75f.io.renatus.schedules.ScheduleUtil;
import a75f.io.renatus.util.ProgressDialogUtils;

import static a75f.io.logic.bo.util.UnitUtils.isCelsiusTunerAvailableStatus;
import static a75f.io.renatus.util.BitmapUtil.getBitmapFromVectorDrawable;


public class MasterControlView extends LinearLayout {

    private static final String LOG_PREFIX = MasterControlView.class.getSimpleName();
    HorizontalScrollView mHorizontalScrollView;
    MasterControl masterControl;
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
        mHorizontalScrollView.setForegroundGravity(Gravity.CENTER_VERTICAL);


        //mHorizontalScrollView.setEnabled(false);
        ViewGroup.LayoutParams masterControlLayoutParams =
                new ViewGroup.LayoutParams(0, LayoutParams.MATCH_PARENT);

        masterControl = new MasterControl(this.getContext());
        masterControl.setMinimumWidth(getMeasuredWidth() - (mImageWidth * 2));
        mHorizontalScrollView.addView(masterControl, masterControlLayoutParams);


        //Disable touch.
        // masterControl.setOnTouchListener((v, event) -> false);

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

        hdb = Domain.buildingEquip.getHeatingDeadband().readPriorityVal();
        cdb = Domain.buildingEquip.getCoolingDeadband().readPriorityVal();

        setbackMap = CCUHsApi.getInstance().readEntity("unoccupied and setback and default");
        zoneDiffMap = CCUHsApi.getInstance().readEntity("building and zone and differential");

    }

    public void setTuner(Dialog dialog) {
        HDict tuner = CCUHsApi.getInstance().readHDict("equip and tuner");
        Equip p = new Equip.Builder().setHDict(tuner).build();
        saveBuildingData(dialog);
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
    @SuppressLint("StaticFieldLeak")
    private void saveBuildingData(Dialog dialog) {
        float coolingTemperatureUpperLimit = masterControl.getUpperCoolingTemp();
        float coolingTemperatureLowerLimit = masterControl.getLowerCoolingTemp();
        float heatingTemperatureUpperLimit = masterControl.getUpperHeatingTemp();
        float heatingTemperatureLowerLimit = masterControl.getLowerHeatingTemp();
        float buildingTemperatureUpperLimit = masterControl.getUpperBuildingTemp();
        float buildingTemperatureLowerLimit = masterControl.getLowerBuildingTemp();

        mOnClickListener.onSaveClick(heatingTemperatureLowerLimit,
                heatingTemperatureUpperLimit,
                coolingTemperatureLowerLimit,
                coolingTemperatureUpperLimit,
                buildingTemperatureLowerLimit,
                buildingTemperatureUpperLimit,
                (float) Domain.buildingEquip.getUnoccupiedZoneSetback().readPriorityVal(),
                (float) Domain.buildingEquip.getBuildingToZoneDifferential().readPriorityVal(),
                (float) hdb, (float) cdb);

        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(final String... params) {
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                super.onPostExecute(result);
                Toast.makeText(getContext(),"Building limits has been successfully updated",Toast.LENGTH_LONG).show();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
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

    public void saveUserLimitChange(int whichLimit, String adapterVal) {
        float val ;
        val = (float) MasterControlUtil.getAdapterFarhenheitVal(adapterVal);

        masterControl.temps[whichLimit] = val;
        if(whichLimit == MasterControl.MasterControlState.LOWER_BUILDING_LIMIT.ordinal()){
            Domain.buildingEquip.getBuildingLimitMin().writeVal(16, val);
        } else if(whichLimit == MasterControl.MasterControlState.UPPER_BUILDING_LIMIT.ordinal()){
            Domain.buildingEquip.getBuildingLimitMax().writeVal(16, val);
        } else if(whichLimit == MasterControl.MasterControlState.LOWER_HEATING_LIMIT.ordinal()){
            ArrayList<HashMap<Object, Object>> allHeatingLowerLimit =
                    CCUHsApi.getInstance().readAllEntities("schedulable and point and limit and max and heating and user");
            HSUtil.writeValToALLLevel16(allHeatingLowerLimit,val);
        } else if(whichLimit == MasterControl.MasterControlState.UPPER_HEATING_LIMIT.ordinal()){
            ArrayList<HashMap<Object, Object>> allHeatingupperLimit =
                    CCUHsApi.getInstance().readAllEntities("schedulable and point and limit and min and heating and user");
            HSUtil.writeValToALLLevel16(allHeatingupperLimit,val);
        } else if(whichLimit == MasterControl.MasterControlState.LOWER_COOLING_LIMIT.ordinal()){
            ArrayList<HashMap<Object, Object>> allCoolingLowerLimit =
                    CCUHsApi.getInstance().readAllEntities("schedulable and point and limit and min and cooling and user");
            HSUtil.writeValToALLLevel16(allCoolingLowerLimit,val);
        } else if(whichLimit == MasterControl.MasterControlState.UPPER_COOLING_LIMIT.ordinal()){
            ArrayList<HashMap<Object, Object>> allCoolingUpperLimit =
                    CCUHsApi.getInstance().readAllEntities("schedulable and point and limit and max and cooling and user");
            HSUtil.writeValToALLLevel16(allCoolingUpperLimit,val);
        }
    }

    public void updateDeadBand(String tag, String adapterVal){
        float val ;
        val = (float) MasterControlUtil.getAdapterFarhenheitVal(adapterVal);
        ArrayList<HashMap<Object, Object>> alldeadBands =
                CCUHsApi.getInstance().readAllEntities("schedulable and "+tag+" and deadband");
        HSUtil.writeValToALLLevel16(alldeadBands,val);
    }

    public void updateUnoccupiedZoneSetBack(String adapterVal){
        float val ;
        val = (float) MasterControlUtil.getAdapterFarhenheitVal(adapterVal);
        ArrayList<HashMap<Object, Object>> unoccupiedZoneObj = CCUHsApi.getInstance().readAllEntities(
                "schedulable and unoccupied and setback");
        HSUtil.writeValToALLLevel16(unoccupiedZoneObj,val);
    }

    public void updateBuildingToZoneDiff(String adapterVal){
        float val ;
        val = (float) MasterControlUtil.getAdapterFarhenheitVal(adapterVal);
        Domain.buildingEquip.getBuildingToZoneDifferential().writeVal(16, val);
    }


}
