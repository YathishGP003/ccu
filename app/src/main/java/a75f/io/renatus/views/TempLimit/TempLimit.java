package a75f.io.renatus.views.TempLimit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import a75f.io.renatus.R;

/**
 * Created by mahesh on 27-08-2019.
 */
public class TempLimit extends View {

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
    private float lowerBuildingTemp = 55;
    private float upperBuildingTemp = 90;
    //
    int mPaddingPX = 0;
    int mViewWidth = 0;

    //Amount of degrees below the bottom reading and above the top reading.
    int mEdgeDegrees = 2;

    int mDefaultVisibleDegrees = 35;

    float mLowerBound = 33.0f;
    float mUpperBound = 110.0f;

    int mDegreeIncremntPX = 0;
    boolean mMeasured = false;

    private static final int PADDING_LEFT_RIGHT_PX = 20; //dp

    public TempLimit(Context context) {
        super(context);
        init();
    }

    private static final boolean DEBUG = true;

    enum TempLimitState {
        NONE,
        LOWER_HEATING_LIMIT,
        UPPER_HEATING_LIMIT,
        LOWER_COOLING_LIMIT,
        UPPER_COOLING_LIMIT,
        LOWER_BUILDING_LIMIT,
        UPPER_BUILDING_LIMIT
    }

    enum Direction {
        UP, DOWN
    }

    float[] temps = new float[TempLimitState.values().length];

    private void drawSliderIcon(Canvas canvas, int yDisplacemnet, TempLimitState stateReflected, int color, Direction direction) {

        int xPos;
        if (stateReflected == TempLimitState.LOWER_COOLING_LIMIT || stateReflected == TempLimitState.UPPER_HEATING_LIMIT) {
            xPos = getPXForTemp(temps[stateReflected.ordinal()]) - 15;
        } else {
            xPos = getPXForTemp(temps[stateReflected.ordinal()]) + 15;
        }

        int yPos = (direction == Direction.UP) ? getTempLineYLocation() - yDisplacemnet - 5 : getTempLineYLocation() + yDisplacemnet + 15;

        mTempIconPaint.setColor(color);
        canvas.drawText(String.valueOf((int) temps[stateReflected.ordinal()]),
                xPos, yPos, mTempIconPaint);
    }

    private TempLimitState mSelected = TempLimitState.NONE;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        return false;
    }


    private boolean mDataSet = false;

    // init temps
    public void setData(float lowerHeatingTemp, float upperHeatingTemp, float lowerCoolingTemp,
                        float upperCoolingTemp, float lowerBuildingTemp, float upperBuildingTemp) {
        temps[TempLimitState.LOWER_HEATING_LIMIT.ordinal()] = lowerHeatingTemp;
        temps[TempLimitState.UPPER_HEATING_LIMIT.ordinal()] = upperHeatingTemp;
        temps[TempLimitState.LOWER_COOLING_LIMIT.ordinal()] = lowerCoolingTemp;
        temps[TempLimitState.UPPER_COOLING_LIMIT.ordinal()] = upperCoolingTemp;
        temps[TempLimitState.LOWER_BUILDING_LIMIT.ordinal()] = lowerBuildingTemp;
        temps[TempLimitState.UPPER_BUILDING_LIMIT.ordinal()] = upperBuildingTemp;

        this.lowerHeatingTemp = lowerHeatingTemp;
        this.upperHeatingTemp = upperHeatingTemp;
        this.lowerCoolingTemp = lowerCoolingTemp;
        this.upperCoolingTemp = upperCoolingTemp;
        this.lowerBuildingTemp = lowerBuildingTemp;
        this.upperBuildingTemp = upperBuildingTemp;

        mDataSet = true;
        invalidate();
    }

    private void init() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.setNestedScrollingEnabled(true);

        }
        Typeface latoLightFont = ResourcesCompat.getFont(getContext(), R.font.lato_light);
        this.setBackgroundColor(Color.WHITE);


        setData(lowerHeatingTemp, upperHeatingTemp, lowerCoolingTemp, upperCoolingTemp, lowerBuildingTemp, upperBuildingTemp);


        mLinePaint = new Paint();
        mLinePaint.setColor(Color.parseColor("#b7b7b7"));
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
        mDebugTextAlignCenterPaint.setColor(Color.BLACK);
        mTempPaint.setStrokeCap(Paint.Cap.BUTT);
        mTempPaint.setStrokeWidth(8);

        mTempLinePaint = new Paint();
        mTempLinePaint.setAntiAlias(true);
        mTempLinePaint.setStyle(Paint.Style.FILL);
        mTempLinePaint.setStrokeWidth(2);


        mTempIconPaint = new Paint();
        mTempIconPaint.setColor(Color.BLACK);
        mTempIconPaint.setAntiAlias(true);
        mTempIconPaint.setStyle(Paint.Style.STROKE);
        mTempIconPaint.setTextSize(16);
        mTempIconPaint.setTextAlign(Paint.Align.CENTER);
        mTempIconPaint.setStyle(Paint.Style.FILL);
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

            drawTempGauge(canvas, temps[TempLimitState.LOWER_HEATING_LIMIT.ordinal()],
                    temps[TempLimitState.UPPER_HEATING_LIMIT.ordinal()], Color.parseColor("#e24725"), Direction.DOWN);

            drawTempGauge(canvas, temps[TempLimitState.LOWER_COOLING_LIMIT.ordinal()],
                    temps[TempLimitState.UPPER_COOLING_LIMIT.ordinal()], Color.parseColor("#5990B4"), Direction.UP);

            drawSliderIcon(canvas, mCoolingBarDisplacement, TempLimitState.LOWER_COOLING_LIMIT, Color.parseColor("#5990B4"), Direction.UP);

            drawSliderIcon(canvas, mHeatingBarDisplacement, TempLimitState.LOWER_HEATING_LIMIT, Color.parseColor("#e24725"), Direction.DOWN);

            drawSliderIcon(canvas, mCoolingBarDisplacement, TempLimitState.UPPER_COOLING_LIMIT, Color.parseColor("#5990B4"), Direction.UP);

            drawSliderIcon(canvas, mHeatingBarDisplacement, TempLimitState.UPPER_HEATING_LIMIT, Color.parseColor("#e24725"), Direction.DOWN);

            drawBuildingLimitCircle(canvas, TempLimitState.LOWER_BUILDING_LIMIT);
            drawBuildingLimitCircle(canvas, TempLimitState.UPPER_BUILDING_LIMIT);

        }
    }


    private void drawTempGauge(Canvas canvas, float mLowerHeatingTemp, float mUpperHeatingTemp, int color, Direction direction) {

        mTempLinePaint.setColor(color);
        canvas.drawLine(getPXForTemp(mLowerHeatingTemp), (direction == Direction.UP) ? getTempLineYLocation() - 30 : getTempLineYLocation() + 30, getPXForTemp(mUpperHeatingTemp), (direction == Direction.UP) ? getTempLineYLocation() - 30 : getTempLineYLocation() + 30, mTempLinePaint);
    }

    private void drawBuildingLimitCircle(Canvas canvas, TempLimitState controlState) {
        float temp = temps[controlState.ordinal()];
        mTempPaint.setColor(Color.BLACK);

        int xLoc = getPXForTemp(temp);
        int yLoc = getTempLineYLocation();

        canvas.drawText(String.valueOf((int) temps[controlState.ordinal()]),
                xLoc - 8, yLoc - 15, mTempPaint);

        canvas.drawCircle(xLoc, yLoc,
                4, mTempPaint);
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

    public TempLimit(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TempLimit(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TempLimit(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }
}