package a75f.io.renatus.views.MasterControl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import a75f.io.logic.L;
import a75f.io.renatus.R;


public class MasterControlView extends LinearLayout {

    private static final int ANGLE_WIDTH = 2;
    HorizontalScrollView mHorizontalScrollView;
    MasterControl masterControl;

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
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap equip = hayStack.read("equip and system");
        HashMap coolUL = CCUHsApi.getInstance().read("point and limit and max and cooling and user and equipRef == \"" + equip.get("id").toString() + "\"");
        HashMap heatUL = CCUHsApi.getInstance().read("point and limit and max and heating and user and equipRef == \"" + equip.get("id").toString() + "\"");
        HashMap coolLL = CCUHsApi.getInstance().read("point and limit and min and cooling and user and equipRef == \"" + equip.get("id").toString() + "\"");
        HashMap heatLL = CCUHsApi.getInstance().read("point and limit and min and heating and user and equipRef == \"" + equip.get("id").toString() + "\"");
        HashMap buildingMin = CCUHsApi.getInstance().read("building and limit and min and equipRef == \"" + L.ccu().systemProfile.getSystemEquipRef() + "\"");
        HashMap buildingMax = CCUHsApi.getInstance().read("building and limit and max and equipRef == \"" + L.ccu().systemProfile.getSystemEquipRef() + "\"");

        Log.d("Mahesh", " building limit  " + coolUL + " " + heatUL + " " + coolLL + " " + heatLL +" "+buildingMin+" "+buildingMax);
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
