package a75f.io.renatus.views.MasterControl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logic.L;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.renatus.R;
import a75f.io.renatus.registartion.InstallerOptions;


public class MasterControlView extends LinearLayout{

    private static final int ANGLE_WIDTH = 2;
    HorizontalScrollView mHorizontalScrollView;
    MasterControl masterControl;
    HashMap coolUL;
    HashMap heatUL;
    HashMap coolLL;
    HashMap heatLL;
    HashMap buildingMin;
    HashMap buildingMax;

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
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip");

        for (HashMap m : equips) {
            Equip p = new Equip.Builder().setHashMap(m).build();

            if (p.getDisplayName().contains("BuildingTuner")){
                coolUL = CCUHsApi.getInstance().read("point and limit and max and cooling and user and equipRef == \"" + p.getId() + "\"");
                heatUL = CCUHsApi.getInstance().read("point and limit and max and heating and user and equipRef == \"" + p.getId() + "\"");
                coolLL = CCUHsApi.getInstance().read("point and limit and min and cooling and user and equipRef == \"" + p.getId() + "\"");
                heatLL = CCUHsApi.getInstance().read("point and limit and min and heating and user and equipRef == \"" + p.getId() + "\"");
                buildingMin = CCUHsApi.getInstance().read("building and limit and min and equipRef == \"" + p.getId() + "\"");
                buildingMax = CCUHsApi.getInstance().read("building and limit and max and equipRef == \"" + p.getId() + "\"");
            }
        }

        masterControl.setData((float) getTuner(heatLL.get("id").toString()), (float) getTuner(heatUL.get("id").toString()), (float) getTuner(coolLL.get("id").toString()), (float) getTuner(coolUL.get("id").toString()), (float) getTuner(buildingMin.get("id").toString()), (float) getTuner(buildingMax.get("id").toString()));
    }

    @SuppressLint("StaticFieldLeak")
    public void setTuner(){
        float coolTempUL = masterControl.getUpperCoolingTemp();
        float coolTempLL = masterControl.getLowerCoolingTemp();
        float heatTempUL = masterControl.getUpperHeatingTemp();
        float heatTempLL = masterControl.getLowerHeatingTemp();
        float buildingTempUL = masterControl.getUpperBuildingTemp();
        float buildingTempLL = masterControl.getLowerBuildingTemp();

        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground( final String ... params ) {
                CCUHsApi.getInstance().writePoint(coolUL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu", (double)coolTempUL, 0);
                CCUHsApi.getInstance().writeHisValById(coolUL.get("id").toString(), (double)coolTempUL);

                CCUHsApi.getInstance().writePoint(coolLL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu", (double)coolTempLL, 0);
                CCUHsApi.getInstance().writeHisValById(coolLL.get("id").toString(), (double)coolTempLL);

                CCUHsApi.getInstance().writePoint(heatUL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu", (double)heatTempUL, 0);
                CCUHsApi.getInstance().writeHisValById(heatUL.get("id").toString(), (double)heatTempUL);

                CCUHsApi.getInstance().writePoint(heatLL.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu", (double)heatTempLL, 0);
                CCUHsApi.getInstance().writeHisValById(heatLL.get("id").toString(), (double)heatTempLL);

                CCUHsApi.getInstance().writePoint(buildingMax.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu", (double)buildingTempUL, 0);
                CCUHsApi.getInstance().writeHisValById(buildingMax.get("id").toString(), (double)buildingTempUL);

                CCUHsApi.getInstance().writePoint(buildingMin.get("id").toString(), TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu", (double)buildingTempLL, 0);
                CCUHsApi.getInstance().writeHisValById(buildingMin.get("id").toString(), (double)buildingTempLL);

                return null;
            }

            @Override
            protected void onPostExecute( final Void result ) {
                // continue what you are doing...
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

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
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
