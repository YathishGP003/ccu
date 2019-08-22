package a75f.io.renatus.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import a75f.io.renatus.R;

import static a75f.io.renatus.util.BitmapUtil.getBitmapFromVectorDrawable;

/**
 * Created by mahesh on 15-08-2019.
 */
public class RangeBar extends View {

    //
    public static final float RECOMMENDED_WIDTH_DP = 500.0f;
    //
    private Paint mLinePaint;
    private Paint mTempIconPaint;
    Paint mTempLinePaint;
    Paint mTempPaint;
    Paint mDebugTextAlignCenterPaint;

    private int mViewHeight = 0;
    private int mHeatingBarDisplacement = 0;
    private int mCoolingBarDisplacement = 0;
    private int mHitBoxPadding = 0;

    //
    private float lowerHeatingTemp = 72;
    private float upperHeatingTemp = 64;
    private float lowerCoolingTemp = 74;
    private float upperCoolingTemp = 79;
    //
    int mPaddingPX = 0;
    int mViewWidth = 0;

    //Amount of degrees below the bottom reading and above the top reading.
    int mEdgeDegrees = 2;

    int mDefaultVisibleDegrees = 35;

    float mLowerBound = 55.0f;
    float mUpperBound = 90.0f;

    int mDegreeIncremntPX = 0;
    boolean mMeasured = false;

    double cdb = 2.0;
    double hdb = 2.0;

    private static final int PADDING_LEFT_RIGHT_PX = 20; //dp

    public RangeBar(Context context) {
        super(context);
        init();
    }

    private static final boolean DEBUG = true;

    enum RangeBarState {
        NONE,
        LOWER_HEATING_LIMIT,
        UPPER_HEATING_LIMIT,
        LOWER_COOLING_LIMIT,
        UPPER_COOLING_LIMIT
    }

    RectF[] hitBoxes = new RectF[RangeBarState.values().length];
    float[] temps = new float[RangeBarState.values().length];
    Bitmap[] bitmaps = new Bitmap[RangeBarState.values().length];
    Matrix matrix = new Matrix();

    private void drawSliderIcon(Canvas canvas, int yDisplacemnet, RangeBarState stateReflected) {

        matrix.reset();

        hitBoxes[stateReflected.ordinal()].set(0, 0, bitmaps[stateReflected.ordinal()].getWidth() * 1.5f, bitmaps[stateReflected.ordinal()].getHeight());

        int xPos = getPXForTemp(temps[stateReflected.ordinal()]);
        int yPos = getTempLineYLocation() - yDisplacemnet;

        matrix.postTranslate(xPos, yPos);
        canvas.drawBitmap(bitmaps[stateReflected.ordinal()], matrix, mTempPaint);

        matrix.reset();
        matrix.postTranslate(xPos - mHitBoxPadding + 5, yPos - mHitBoxPadding);

        //Make the hit boxes easy to click.
        matrix.preScale(2.5f, 2.0f);

        matrix.mapRect(hitBoxes[stateReflected.ordinal()]);
        canvas.drawRect(hitBoxes[stateReflected.ordinal()], mDebugBoxesPaint);

        //The 2 and 1.5 are used to slide the number on the bitmap image.
        //Text centered left to right and 1/3 the way down the icon.
        if (stateReflected == RangeBarState.LOWER_COOLING_LIMIT) {
            mTempIconPaint.setColor(getResources().getColor(R.color.max_temp));
        } else {
            mTempIconPaint.setColor(getResources().getColor(R.color.accent));
        }
        canvas.drawText(String.valueOf(roundToHalf(temps[stateReflected.ordinal()])),
                xPos + bitmaps[stateReflected.ordinal()].getWidth() / 2f,
                (yPos - 10f), mTempIconPaint);
    }

    private RangeBarState isHitBoxTouched(float x, float y) {
        for (int i = 0; i < hitBoxes.length; i++) {
            if (hitBoxes[i].contains(x, y)) {
                RangeBarState retVal = RangeBarState.values()[i];
                Log.d("RangeControl", "HitBox was selected: " + retVal.name());

                return retVal;
            }
        }

        return RangeBarState.NONE;
    }

    private RangeBarState mSelected = RangeBarState.NONE;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        Log.i("RangeControl", "X: " + event.getX() + " Y: " + event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mSelected = isHitBoxTouched(event.getX(), event.getY());
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
            case MotionEvent.ACTION_MOVE:
                Log.i("Movement", "mSelected - " + mSelected.name()
                        + " Temps: " + getTempForPX(event.getX()));
                if (getTempForPX(event.getX()) > mLowerBound && getTempForPX(event.getX()) < mUpperBound) {

                    Log.i("Movement", "Temps: " + getTempForPX((int) event.getX()));
                    if (mSelected == RangeBarState.LOWER_COOLING_LIMIT) {
                        if (getTempForPX(event.getX()) >= lowerCoolingTemp && getTempForPX(event.getX()) <= upperCoolingTemp) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                            temps[mSelected.ordinal()] = getTempForPX(event.getX());
                            if (temps[RangeBarState.LOWER_COOLING_LIMIT.ordinal()] - temps[RangeBarState.LOWER_HEATING_LIMIT.ordinal()] < (float) (cdb + hdb)) {
                                temps[RangeBarState.LOWER_HEATING_LIMIT.ordinal()] = (temps[RangeBarState.LOWER_COOLING_LIMIT.ordinal()] - (float) (cdb + hdb));
                            }
                        }
                    } else if (mSelected == RangeBarState.LOWER_HEATING_LIMIT) {
                        if (getTempForPX(event.getX()) >= upperHeatingTemp && getTempForPX(event.getX()) <= lowerHeatingTemp) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                            temps[mSelected.ordinal()] = getTempForPX(event.getX());
                            if (temps[RangeBarState.LOWER_COOLING_LIMIT.ordinal()] - temps[RangeBarState.LOWER_HEATING_LIMIT.ordinal()] < (float) (cdb + hdb)) {
                                temps[RangeBarState.LOWER_COOLING_LIMIT.ordinal()] = (temps[RangeBarState.LOWER_HEATING_LIMIT.ordinal()] + (float) (cdb + hdb));
                            }
                        }
                    }

                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                mSelected = RangeBarState.NONE;
                invalidate();
                break;
        }

        Log.d("RangeControl", "Touched: " + isHitBoxTouched(event.getX(), event.getY()).name());
        return true;
    }

    Paint mDebugBoxesPaint;


    private boolean mDataSet = false;

    // init temps
    public void setData(float lowerHeatingTemp, float upperHeatingTemp, float lowerCoolingTemp,
                        float upperCoolingTemp, float cdb, float hdb) {
        temps[RangeBarState.LOWER_HEATING_LIMIT.ordinal()] = lowerHeatingTemp;
        temps[RangeBarState.UPPER_HEATING_LIMIT.ordinal()] = upperHeatingTemp;
        temps[RangeBarState.LOWER_COOLING_LIMIT.ordinal()] = lowerCoolingTemp;
        temps[RangeBarState.UPPER_COOLING_LIMIT.ordinal()] = upperCoolingTemp;

        this.lowerHeatingTemp = lowerHeatingTemp;
        this.upperHeatingTemp = upperHeatingTemp;
        this.lowerCoolingTemp = lowerCoolingTemp;
        this.upperCoolingTemp = upperCoolingTemp;
        this.cdb = cdb;
        this.hdb = hdb;

        mDataSet = true;
        invalidate();
    }

    private void init() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.setNestedScrollingEnabled(true);

        }
        Typeface latoLightFont = ResourcesCompat.getFont(getContext(), R.font.lato_light);
        this.setBackgroundColor(Color.WHITE);

        for (int i = 0; i < hitBoxes.length; i++) {
            hitBoxes[i] = new RectF();
        }
        bitmaps[RangeBarState.LOWER_COOLING_LIMIT.ordinal()] = bitmaps[RangeBarState.UPPER_COOLING_LIMIT.ordinal()] =
                getBitmapFromVectorDrawable(getContext(), R.drawable.ic_cool_thumb);
        bitmaps[RangeBarState.LOWER_HEATING_LIMIT.ordinal()] = bitmaps[RangeBarState.UPPER_HEATING_LIMIT.ordinal()] =
                getBitmapFromVectorDrawable(getContext(), R.drawable.ic_heat_thumb);


        setData(lowerHeatingTemp, upperHeatingTemp, lowerCoolingTemp, upperCoolingTemp, (float) cdb, (float) hdb);


        mLinePaint = new Paint();
        mLinePaint.setColor(Color.parseColor("#939393"));
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStyle(Paint.Style.FILL);
        mLinePaint.setStrokeCap(Paint.Cap.BUTT);
        mLinePaint.setStrokeWidth(2);

        mDebugTextAlignCenterPaint = new Paint();
        mDebugTextAlignCenterPaint.setTypeface(latoLightFont);
        mDebugTextAlignCenterPaint.setStyle(Paint.Style.FILL);
        mDebugTextAlignCenterPaint.setColor(Color.parseColor("#000000"));
        mDebugTextAlignCenterPaint.setAntiAlias(true);
        mDebugTextAlignCenterPaint.setTextAlign(Paint.Align.CENTER);
        mDebugTextAlignCenterPaint.setTextSize(22);

        mTempPaint = new Paint();
        mTempPaint.setAntiAlias(true);
        mTempPaint.setStyle(Paint.Style.FILL);
        mTempPaint.setStrokeCap(Paint.Cap.BUTT);
        mTempPaint.setColor(Color.parseColor("#939393"));
        mTempPaint.setStrokeWidth(2);

        mTempLinePaint = new Paint();
        mTempLinePaint.setAntiAlias(true);
        mTempLinePaint.setColor(Color.TRANSPARENT);
        mTempLinePaint.setStyle(Paint.Style.FILL);
        mTempLinePaint.setStrokeWidth(2);


        mTempIconPaint = new Paint();
        mTempIconPaint.setColor(Color.BLACK);
        mTempIconPaint.setAntiAlias(true);
        mTempIconPaint.setStyle(Paint.Style.STROKE);
        mTempIconPaint.setTextSize(20);
        mTempIconPaint.setTextAlign(Paint.Align.CENTER);
        mTempIconPaint.setStyle(Paint.Style.FILL);

        mDebugBoxesPaint = new Paint();
        mDebugBoxesPaint.setColor(Color.TRANSPARENT);
        mDebugBoxesPaint.setAntiAlias(true);
        mDebugBoxesPaint.setStyle(Paint.Style.STROKE);
        mDebugBoxesPaint.setStrokeWidth(2);
        mDebugBoxesPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        mMeasured = true;

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int suggestedWidth = getDefaultSize(getSuggestedMinimumWidth(),
                widthMeasureSpec);
        mViewHeight = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        //Defaults to 900dp, but if the screen is squeezed less degrees can be shown.   This can be overrode.
        float visibleDegrees = (mDefaultVisibleDegrees +
                (mEdgeDegrees * 2)) *
                ((suggestedWidth / displayMetrics.density) / RECOMMENDED_WIDTH_DP);

        mDegreeIncremntPX = (int) (suggestedWidth / visibleDegrees);
        mPaddingPX = (int) (PADDING_LEFT_RIGHT_PX * displayMetrics.density);
        mViewWidth = Math.round((mUpperBound - mLowerBound) * mDegreeIncremntPX + (mPaddingPX) * 2);
        mHeatingBarDisplacement = (int) (20 * displayMetrics.density);
        mCoolingBarDisplacement = (int) (20 * displayMetrics.density);

        mHitBoxPadding = (int) (25 * displayMetrics.density);
        mPaddingBetweenCoolingBarAndSliderIcon = (int) (10 * displayMetrics.density);
        setMeasuredDimension(Math.round(mViewWidth), mViewHeight);
    }

    int mPaddingBetweenCoolingBarAndSliderIcon;


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


        if (mMeasured) {
            drawTempLine(canvas);

            drawTempGauge(canvas, temps[RangeBarState.LOWER_HEATING_LIMIT.ordinal()],
                    temps[RangeBarState.UPPER_HEATING_LIMIT.ordinal()]);

            drawTempGauge(canvas, temps[RangeBarState.LOWER_COOLING_LIMIT.ordinal()],
                    temps[RangeBarState.UPPER_COOLING_LIMIT.ordinal()]);

            drawSliderIcon(canvas, mCoolingBarDisplacement, RangeBarState.LOWER_COOLING_LIMIT);

            drawSliderIcon(canvas, mHeatingBarDisplacement, RangeBarState.LOWER_HEATING_LIMIT);

        }
    }


    private void drawTempGauge(Canvas canvas, float mLowerHeatingTemp, float mUpperHeatingTemp) {

        //draw two more circle for shadow effect
        canvas.drawLine(getPXForTemp(mLowerHeatingTemp), getTempLineYLocation(), getPXForTemp(mUpperHeatingTemp), getTempLineYLocation(), mTempLinePaint);

    }

    public float getLowerHeatingTemp() {
        return roundToHalf(temps[RangeBarState.LOWER_HEATING_LIMIT.ordinal()]);
    }

    public float getLowerCoolingTemp() {
        return roundToHalf(temps[RangeBarState.LOWER_COOLING_LIMIT.ordinal()]);
    }

    public void setLowerCoolingTemp(float lowerCoolingTemp) {
        temps[RangeBarState.LOWER_COOLING_LIMIT.ordinal()] = lowerCoolingTemp;
        invalidate();
    }

    public void setLowerHeatingTemp(float lowerHeatingTemp) {
        temps[RangeBarState.LOWER_HEATING_LIMIT.ordinal()] = lowerHeatingTemp;
        invalidate();
    }

    private int getPXForTemp(float temp) {
        return Math.round(mPaddingPX + mDegreeIncremntPX * (temp - mLowerBound));
    }

    private float getTempForPX(float px) {
        return ((px - mPaddingPX) / mDegreeIncremntPX) + mLowerBound;
    }


    private void drawTempLine(Canvas canvas) {
        //why 10
        canvas.drawLine(mPaddingPX, getTempLineYLocation(), mViewWidth, getTempLineYLocation(), mLinePaint);
    }

    private int getTempLineYLocation() {
        return mViewHeight / 2;
    }

    private static float roundToHalf(float d) {
        return Math.round(d * 2) / 2.0f;
    }

    public RangeBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RangeBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public RangeBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }
}