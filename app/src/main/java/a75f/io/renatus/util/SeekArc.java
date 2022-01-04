package a75f.io.renatus.util;


import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import androidx.annotation.ColorInt;
import androidx.core.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;


import java.text.DecimalFormat;
import java.util.HashMap;

import a75f.io.renatus.R;


/*
    1)  4 Tasks for haystack completed that aren't deployed.  (Validation Madhu mentioned)


    //############################# DISCUSSION POINTS #########################################
            a) Decimal to show .5 or not?
            b) Do we need a single temp mode?

    2)  Work in Progress for SeekArc & Progress Bar

    TODO:
    //Not operator for Madhu -- file ticket assigned it to me.
    //Email with Madhu for web project.
    //Auth token -- call microsoft to see if we can get it smaller.
    //HisRead requests -- speak with madhu
    //Measurements & Sharding Influx needs some research -- speak with madhu.

 */


//DONE TODO:  Block scrolling when it's outside of ranges.
//DONE TODO:  Make thumb targets larger.
//DONE TODO:  Make 3 Test Screens of different sizes for the controls app demo.
//DONE TODO:  Try to better match the design document.
//DONE: TODO: Add ratio to documentation to clip off bottom
//DONE: TODO:  Flip cooling and heating around on current screen.
//DONE: TODO:  Finish Text to specs on PDF document.
//DONE SOMEWHAT: TODO:  Set up minimalistic view per document  https://xd.adobe.com/view/da1e6d70-ac06-484a-57a9-dfcc02718cd8-ac2f/


//TODO 30m:  Fix any memory leaks.
//TODO 45m:  Optimize by doing upfront calc.
//TODO 30m:  Cooling text

//DONE //TODO 45m:  Set up API for other developers to interact with data.
//DONE TODO: ?m:  Add Animations to transistion between non-detailed view & detailed view like https://xd.adobe.com/view/da1e6d70-ac06-484a-57a9-dfcc02718cd8-ac2f/.

//DONE TODO 30m:  Cursor lands on half a decimal at tick marks.   Need to reverse that.
//SCARY TODO ?m:  Animate the numbers upwards and downwards.


public class SeekArc extends View
{

    private static final float   INNER_GREY_STROKE_WIDTH        = 40.0f;
    public static final  int     SPACE_BETWEEN_TWO_WORD_STRINGS = 10;
    private static       int     INVALID_PROGRESS_VALUE         = -1;
    private static       int     PADDING_BETWEEN_TEXT           = 20;
    private final        int     mAngleOffset                   = -90;
    private              Handler handler                        = new Handler();
    float   currentAnimationTime        = 0;
    float   endAnimation                = 160;
    long    animationInterpolator       = 20;
    int     startHeight;
    int     startWidth;
    int     endWidth;
    int     endHeight;
    float   widthStepper;
    float   heightStepper;
    boolean mTransistionDetailViewAtEnd = false;




    private Bitmap mCoolingRectangle;
    private Bitmap mHeatingRectangle;
    private Bitmap mHeatingProgressCircle;
    private Bitmap mCoolingProgressCircle;
    private Bitmap mCurrentTempRectangle;
    private Bitmap mRedLimitNonDetailedView;
    private Bitmap mGreyLimitNonDetailedView;

    /**
     * The Maximum value that this SeekArc can be set to
     */
    private int mMax = 360;

    /**
     * The Angle to start drawing this Arc from
     */
    private int mStartAngle = -90;

    /**
     * The Angle through which to draw the arc (Max is 360)
     */
    private int mSweepAngle = 300;


    /**
     * Give the SeekArc rounded edges
     */
    private boolean mRoundedEdges = false;

    /**
     * Will the progress increase clockwise or anti-clockwise
     */
    private boolean mClockwise = true;


    public boolean mDetailedView = false;


    // Internal variables
    private float mArcOuterRadius   = 0.0f;
    private float mArcRadius        = 0.0f;
    private RectF mArcOuterRect     = new RectF();
    private RectF mArcInnerRect     = new RectF();
    private RectF mArcLineInnerRect = new RectF();
    private RectF mArcRect          = new RectF();
    private RectF mArcLimit         = new RectF();
    private RectF mArcRectText      = new RectF();
    private RectF mArcLimitBound    = new RectF();
    private Paint mArcPaint;
    private Paint mDelimeterPaint;

    private Paint mCurrentTemperatureTextPaint;
    private Paint mDesiredHeatingSmallTextPaint;
    private Paint mDesiredCoolingSmallTextPaint;

    private Paint mCurrentTemperatureStringTextPaint;


    private Paint mHeatingArcPaint;
    private Paint mCoolingArcPaint;


    private float                       mTranslateX;
    private float                       mTranslateY;
    private OnTemperatureChangeListener mOnTemperatureChangeListener;
    private float                       cx;
    private float                       cy;
    private Paint                       mInnerGreyArcPaint;
    private Paint                       mInnerGreyArcLinePaint;
    private boolean                     isTouched = false;


    private float mHeatingLowerLimit;
    private float mHeatingUpperLimit;
    private float mCoolingLowerLimit;
    private float mCoolingUpperLimit;
    private float mCoolingDeadBand = 2.0f;
    private float mHeatingDeadBand = 2.0f;
    private float mCoolingDesiredTemp;
    private float mHeatingDesiredTemp;
    private float mCurrentTemp;

    private float mLimitCoolingStartAngle;
    private float mLimitHeatingStartAngle;
    private float mLimitCoolingEndAngle;
    private float mLimitHeatingEndAngle;

    private float   mPathStartAngle;
    private boolean isViewMeasured = false;


    private int   mOuterTextSize;
    private int   mUserLimitTextSize;
    private int   mThumbCircleTextSize;
    private int   mThumbOuterRadius;
    private int   mThumbInnerRadius;
    private int   mMarkerTextHeight;
    private int   mStatusTextHeight;
    private int   mMarkerTextWidthX;
    private int   mMarkerTextWidthY;
    private int   mStatusTextWidth;
    private int   mStatusOutsideTextWidth;
    private int   mTempTextWidth = 65;
    private int   mStatusOutsideTextHeight;
    private int   mTempTextHeight;
    private int   mStatusTextSize;
    private int   mThumbTextWidth;
    private int   mSmallThumbRadius;
    private Paint mSmallThumbPaint;
    private float mThumbDifference;
    private float mThumbTextHeight;

    private boolean isMoveStarted = false;
    private float   mGapAngle     = 7.5f;


    private float mBuildingLowerTempLimit = 50.0f;
    private float mBuildingUpperTempLimit = 90.0f;

    private float paddingBetweenTextDP = 0;
    private float mScale               = 0.0f;

    HashMap<ProgressType, Float> mProgresses = new HashMap<>();
    private Paint mInbetweenPaint;
    private float mScaledSliderOffset = 0.0f;
    private boolean isSense = false;

    public void setSense(boolean val){
        isSense = val;
    }


    public interface OnTemperatureChangeListener
    {
        void onTemperatureChange(SeekArc seekArc, float coolingDesiredTemp, float heatingDesiredTemp, boolean syncToHaystack);
    }

    RectF mCoolingTargetRect;
    RectF mTargetRect;

    public SeekArc(Context context)
    {
        super(context);
        init(context, null, R.attr.seekArcStyle);
    }

    public SeekArc(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs, R.attr.seekArcStyle);
    }

    public SeekArc(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }


    //Reused objects in onDraw.  They are cleared at the start of each onDraw.
    Rect bounds             = new Rect();
    Rect mCoolingTextBounds = new Rect();
    Rect mHeatingTextBounds = new Rect();

    Matrix mCurrentBitmapMatrix;

    RectF mCoolingTargetRectTransformed;
    RectF mHeatingTargetRectTransformed;


    float mSeperationBetweenArcAndOuterGauge = 0.0f;


    public void init(Context context, AttributeSet attrs, int defStyle)
    {
        mCurrentBitmapMatrix = new Matrix();

        mHeatingRectangle = drawableToBitmap(context.getDrawable(R.drawable.ic_heating_dt_two));
        //mHeatingRectangle = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_heating_dt_two);
        mCoolingRectangle = drawableToBitmap(context.getDrawable(R.drawable.ic_cooling_dt_two));
        //mCoolingRectangle = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_cooling_dt_two);
        mHeatingProgressCircle = drawableToBitmap(context.getDrawable(R.drawable.ic_heating_slider));
        //mHeatingProgressCircle = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_heating_slider);
        mCoolingProgressCircle = drawableToBitmap(context.getDrawable(R.drawable.ic_cooling_slider));
        //mCoolingProgressCircle = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_cooling_slider);
        mCurrentTempRectangle = drawableToBitmap(context.getDrawable(R.drawable.ic_ct_two));
        //mCurrentTempRectangle = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_ct_two);
        mRedLimitNonDetailedView = drawableToBitmap(context.getDrawable(R.drawable.ic_ct_red_two));
        //mRedLimitNonDetailedView = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_ct_red_two);
        mGreyLimitNonDetailedView = drawableToBitmap(context.getDrawable(R.drawable.ic_ct_grey));
        //mGreyLimitNonDetailedView = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_ct_grey);


        // Defaults, may need to link this into theme settings

        if (attrs != null)
        {
            // Attribute initialization
            final TypedArray a = context.obtainStyledAttributes(attrs,
                                                                R.styleable.SeekArc, defStyle, 0);


            mMax = a.getInteger(R.styleable.SeekArc_max, mMax);
            mStartAngle = a.getInt(R.styleable.SeekArc_startAngle, mStartAngle);
            mSweepAngle = a.getInt(R.styleable.SeekArc_sweepAngle, mSweepAngle);
            mRoundedEdges = a.getBoolean(R.styleable.SeekArc_roundEdges,
                                         mRoundedEdges);

            mClockwise = a.getBoolean(R.styleable.SeekArc_clockwise,
                                      mClockwise);
            mOuterTextSize = (int) a.getDimension(R.styleable.SeekArc_outertextsize, mOuterTextSize);
            mStatusTextSize = (int) a.getDimension(R.styleable.SeekArc_statustextsize, mStatusTextSize);
            mUserLimitTextSize = (int) a.getDimension(R.styleable.SeekArc_userlimittextsize, mUserLimitTextSize);
            mThumbCircleTextSize = (int) a.getDimension(R.styleable.SeekArc_thumbcircletextsize, mThumbCircleTextSize);
            mThumbOuterRadius = (int) a.getDimension(R.styleable.SeekArc_thumbouterradius, mThumbOuterRadius);
            mThumbInnerRadius = (int) a.getDimension(R.styleable.SeekArc_thumbinnerradius, mThumbInnerRadius);
            mMarkerTextHeight = (int) a.getDimension(R.styleable.SeekArc_markertextheight, mMarkerTextHeight);
            mStatusTextHeight = (int) a.getDimension(R.styleable.SeekArc_statustextheight, mStatusTextHeight);
            mMarkerTextWidthX = (int) a.getDimension(R.styleable.SeekArc_markertextwidthx, mMarkerTextWidthX);
            mMarkerTextWidthY = (int) a.getDimension(R.styleable.SeekArc_markertextwidthy, mMarkerTextWidthY);
            mStatusTextWidth = (int) a.getDimension(R.styleable.SeekArc_statustextwidth, mStatusTextWidth);
            mStatusOutsideTextWidth = (int) a.getDimension(R.styleable.SeekArc_statusoutsidetextwidth, mStatusOutsideTextWidth);
            mStatusOutsideTextHeight = (int) a.getDimension(R.styleable.SeekArc_statusoutsidetextheight, mStatusOutsideTextHeight);


            mTempTextWidth = (int) a.getDimension(R.styleable.SeekArc_temptextwidth, mTempTextWidth);
            mTempTextHeight = (int) a.getDimension(R.styleable.SeekArc_temptextheight, mTempTextHeight);
            mThumbTextWidth = (int) a.getDimension(R.styleable.SeekArc_thumbtextwidth, mThumbTextWidth);
            mThumbTextHeight = (int) a.getDimension(R.styleable.SeekArc_thumbtextheight, mThumbTextHeight);
            mSmallThumbRadius = (int) a.getDimension(R.styleable.SeekArc_smallthumbradius, mSmallThumbRadius);
            mThumbDifference = (int) a.getDimension(R.styleable.SeekArc_thumbdifference, mThumbDifference);
            a.recycle();
        }

        mSweepAngle = (mSweepAngle > 360) ? 360 : mSweepAngle;
        mStartAngle = (mStartAngle > 360) ? 0 : mStartAngle;
        mStartAngle = (mStartAngle < 0) ? 0 : mStartAngle;


        mCoolingTargetRect = new RectF();
        mCoolingTargetRectTransformed = new RectF();
        mTargetRect = new RectF();
        mHeatingTargetRectTransformed = new RectF();

        mCoolingTargetRect.set(0, 0, mCoolingRectangle.getWidth(), mCoolingRectangle.getHeight());
        mTargetRect.set(0, 0, mHeatingRectangle.getWidth(), mHeatingRectangle.getHeight());

        readyPaint();

        mProgresses.put(ProgressType.COOLING, 0f);
        mProgresses.put(ProgressType.HEATING, 0f);
        mProgresses.put(ProgressType.CURRENT, 0f);
        mPathStartAngle = 120;


    }


    public void setData(boolean detailedView, float buildingLowerLimit, float buildingUpperLimit, float heatingLowerLimit,
                        float heatingUpperLimit, float coolingLowerLimit, float coolingUpperLimit, float heatingDesiredTemp,
                        float coolingDesiredTemp, float currentTemp, float heatingDeadBand, float coolingDeadBand)
    {

        CcuLog.i(L.TAG_CCU_UI, " SeekArc setData heatingLowerLimit "+heatingLowerLimit+" heatingUpperLimit "+heatingUpperLimit
                            +" coolingLowerLimit "+coolingLowerLimit+" coolingUpperLimit "+coolingUpperLimit +" " +
                               "heatingDeadBand "+heatingDeadBand+" coolingDeadBand "+coolingDeadBand);
        mHeatingDeadBand = heatingDeadBand;
        mCoolingDeadBand = coolingDeadBand;
        mBuildingLowerTempLimit = buildingLowerLimit;
        mBuildingUpperTempLimit = buildingUpperLimit;

        mHeatingLowerLimit = heatingLowerLimit;
        mHeatingUpperLimit = heatingUpperLimit;

        mCoolingLowerLimit = coolingLowerLimit;
        mCoolingUpperLimit = coolingUpperLimit;

        mCurrentTemp = currentTemp;
        mCoolingDesiredTemp = coolingDesiredTemp;
        mHeatingDesiredTemp = heatingDesiredTemp;
        mDetailedView = detailedView;
        mHeatingLowerLimit = heatingLowerLimit;
        mCoolingLowerLimit = coolingLowerLimit;
        mHeatingUpperLimit = heatingUpperLimit;
        mCoolingUpperLimit = coolingUpperLimit;
        isDataSet = true;
        prepareAngle();
        invalidate();

    }

    boolean isDataSet = false;


    float mDelimiterSize      = 0.0f;
    float mScaledICCTDrawable = 0.0f;

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isViewMeasured && !isDataSet) {
            return;

        }

        //Dotted ARC circle 300 degrees

        drawSemiArc(canvas, isDetailedView());

        //outer arc numbers from 50 - 90
        if (isDetailedView()) drawWhiteDelimiters(canvas);
        if (isSense) {
            drawCurrentTemp(canvas, getCurrentTemp());
        } else {

            if (!inHeatingSelectionMode && !inCoolingSelectionMode) {
                drawArcBetween(canvas, getHeatingDesiredTemp(), getCoolingDesiredTemp(), mInbetweenPaint);
                drawCurrentTemp(canvas, getCurrentTemp());
            }
        }


        //drawArcBetween(canvas, getHeatingDesiredTemp(), getCoolingDesiredTemp(), mInbetweenPaint);

        if (isDetailedView()) {

            if (!isSense) {
                float coolingModeTemp = inCoolingSelectionMode ? getCoolingModeTempTemperature() : getCoolingDesiredTemp();

                if (!inCoolingSelectionMode) {
                    drawCoolingDesiredIcon(canvas, coolingModeTemp);
                }

                if (inCoolingSelectionMode) {
                    drawCoolingLimitBar(canvas);
                    drawCoolingSliderIcon(canvas, coolingModeTemp);
                    checkForCoolingLine(canvas, coolingModeTemp);
                    drawCoolingText(canvas, coolingModeTemp);
                }


                float heatingModeTemp = inHeatingSelectionMode ? getHeatingModeTempTemperature() : getHeatingDesiredTemp();

                if (!inHeatingSelectionMode) {
                    drawHeatingDesiredIcon(canvas, heatingModeTemp);
                }

                if (inHeatingSelectionMode) {
                    drawHeatingLimitBar(canvas);
                    drawHeatingSliderIcon(canvas, heatingModeTemp);
                    checkForHeatingLine(canvas, heatingModeTemp);
                    drawHeatingText(canvas, heatingModeTemp);
                }
            }
        } else {
            if (!isSense) {
                drawIconByTemp(canvas, mGreyLimitNonDetailedView, getHeatingDesiredTemp(),
                        mArcRadius - mScaledICCTDrawable, mSmallThumbPaint);
                drawIconByTemp(canvas, mGreyLimitNonDetailedView, getCoolingDesiredTemp(),
                        mArcRadius - mScaledICCTDrawable, mSmallThumbPaint);


                if ((getCurrentTemp() > 0) && (getCurrentTemp() < getHeatingDesiredTemp())) {
                    int prevColor = mInbetweenPaint.getColor();
                    mInbetweenPaint.setColor(Color.parseColor("#e24301"));
                    drawArcBetween(canvas, mCurrentTemp, mHeatingDesiredTemp, mInbetweenPaint);
                    mInbetweenPaint.setColor(prevColor);
                } else if ((getCurrentTemp() > 0) && (getCurrentTemp() > getCoolingDesiredTemp())) {

                    int prevColor = mInbetweenPaint.getColor();
                    mInbetweenPaint.setColor(Color.parseColor("#e24301"));
                    drawArcBetween(canvas, getCoolingDesiredTemp(), mCurrentTemp, mInbetweenPaint);
                    mInbetweenPaint.setColor(prevColor);
                }
            }

        }

        //Draw icon at current temperature
        if (isSense) {
            if((getCurrentTemp() > 0) && (getCurrentTemp() > getBuildingLowerTempLimit()) && (getCurrentTemp() < getBuildingUpperTempLimit())) {
                drawIconByTemp(canvas, mCurrentTempRectangle, mCurrentTemp, mArcRadius - mScaledICCTDrawable, mSmallThumbPaint);
            }
        } else {
            if ((getCurrentTemp() > 0) && (getCurrentTemp() > getBuildingLowerTempLimit()) && (getCurrentTemp() < getBuildingUpperTempLimit())) {
                drawIconByTemp(canvas, isDetailedView() ? mCurrentTempRectangle : mRedLimitNonDetailedView,
                        mCurrentTemp, mArcRadius - mScaledICCTDrawable, mSmallThumbPaint);
            }
        }


    }

    public void drawCurrentTemp(Canvas canvas, float currentTemp)
    {
        if (isDetailedView())
            drawCurrentTempTextDetailed(canvas);
        else
            drawCurrentTempTextNotDetailed(canvas, currentTemp);
    }


    private void drawCurrentTempTextNotDetailed(Canvas canvas, float currentTemp)
    {
        String curTemp = String.valueOf(roundToHalf(currentTemp));
        mCurrentTemperatureTextPaint.getTextBounds(curTemp, 0, curTemp.length(), bounds);
        float xPositionOfCurrentTempText = cx - (bounds.width() / 2f);  //origin
        float yPositionOfCurrentTempText = cy + (bounds.height() / 2f);  // baseline
        canvas.drawText(curTemp, xPositionOfCurrentTempText, yPositionOfCurrentTempText, mCurrentTemperatureTextPaint);
    }

    //Refactor to move out mem leaks
    private void drawCurrentTempTextDetailed(Canvas canvas)
    {
        if (isSense) {
            String currentTempText = String.valueOf(getCurrentTemp());
            mCurrentTemperatureTextPaint.getTextBounds(currentTempText, 0, currentTempText.length(), bounds);

            float widthOfText = mHeatingTextBounds.width() + paddingBetweenTextDP + bounds.width();
            float heightOfCurrentText = bounds.height();
            float heightOfStackedText = mCoolingTextBounds.height() + paddingBetweenTextDP + mHeatingTextBounds.height();

            float heightToUse = Math.max(heightOfCurrentText, heightOfStackedText);
            float widthToUse = widthOfText;

            float xPositionOfCurrentText = cx - (widthToUse / 2f);
            float yPositionOfCurrentText = cy + (heightToUse / 2f);
            float widthOfCurText = bounds.width();


            float centerOfCurrentTempHorizontal = xPositionOfCurrentText + (widthOfCurText / 2);
            float yPositionOfBottomOfCurrentTemp = yPositionOfCurrentText + (heightOfCurrentText / 2);

            String curString = "CURRENT";
            mCurrentTemperatureStringTextPaint.getTextBounds(curString, 0, curString.length(), bounds);

            float xPositionOfCurrentStringText = centerOfCurrentTempHorizontal - (bounds.width() / 2);
            float yPositionOfCurrentStringText = yPositionOfBottomOfCurrentTemp;

            float heightOfCurrentStringText = bounds.height();

            String tempString = "TEMP";

            mCurrentTemperatureStringTextPaint.getTextBounds(tempString, 0, tempString.length(), bounds);


            float yPositionOfTempStringText = yPositionOfCurrentStringText + heightOfCurrentStringText + SPACE_BETWEEN_TWO_WORD_STRINGS;
            float xPositionOfTempStringText = centerOfCurrentTempHorizontal - (bounds.width() / 2);

            canvas.drawText(curString, xPositionOfCurrentStringText, yPositionOfCurrentStringText, mCurrentTemperatureStringTextPaint);
            canvas.drawText(tempString, xPositionOfTempStringText, yPositionOfTempStringText, mCurrentTemperatureStringTextPaint);
            canvas.drawText(currentTempText, xPositionOfCurrentText, yPositionOfCurrentText, mCurrentTemperatureTextPaint);

        } else {
            String coolingDesiredText = String.valueOf(getCoolingDesiredTemp());
            String heatingDesiredText = String.valueOf(getHeatingDesiredTemp());
            String currentTempText = String.valueOf(getCurrentTemp());

             mDesiredCoolingSmallTextPaint.getTextBounds(coolingDesiredText, 0, coolingDesiredText.length(), mCoolingTextBounds);
             mDesiredHeatingSmallTextPaint.getTextBounds(heatingDesiredText, 0, heatingDesiredText.length(), mHeatingTextBounds);
            mCurrentTemperatureTextPaint.getTextBounds(currentTempText, 0, currentTempText.length(), bounds);

            float widthOfText = mHeatingTextBounds.width() + paddingBetweenTextDP + bounds.width();
            float heightOfCurrentText = bounds.height();
            float heightOfStackedText = mCoolingTextBounds.height() + paddingBetweenTextDP + mHeatingTextBounds.height();

            float heightToUse = Math.max(heightOfCurrentText, heightOfStackedText);
            float widthToUse = widthOfText;

            float xPositionOfCurrentText = cx - (widthToUse / 2f);
            float yPositionOfCurrentText = cy + (heightToUse / 2f);


            float xPositionOfCoolingText = cx + (widthToUse / 2.0f) - mCoolingTextBounds.width();
            float yPositionOfCoolingText = cy + (heightToUse / 2.0f) - mCoolingTextBounds.height() - paddingBetweenTextDP;

            float xPositionOfHeatingText = cx + (widthToUse / 2.0f) - mCoolingTextBounds.width();
            float yPositionOfHeatingText = cy + (heightToUse / 2.0f) + mCoolingTextBounds.height() - paddingBetweenTextDP;

            float widthOfCurText = bounds.width();


            float centerOfCurrentTempHorizontal = xPositionOfCurrentText + (widthOfCurText / 2);
            float yPositionOfBottomOfCurrentTemp = yPositionOfCurrentText + (heightOfCurrentText / 2);

            String curString = "CURRENT";
            mCurrentTemperatureStringTextPaint.getTextBounds(curString, 0, curString.length(), bounds);

            float xPositionOfCurrentStringText = centerOfCurrentTempHorizontal - (bounds.width() / 2);
            float yPositionOfCurrentStringText = yPositionOfBottomOfCurrentTemp;

            float heightOfCurrentStringText = bounds.height();

            String tempString = "TEMP";

            mCurrentTemperatureStringTextPaint.getTextBounds(tempString, 0, tempString.length(), bounds);


            float yPositionOfTempStringText = yPositionOfCurrentStringText + heightOfCurrentStringText + SPACE_BETWEEN_TWO_WORD_STRINGS;
            float xPositionOfTempStringText = centerOfCurrentTempHorizontal - (bounds.width() / 2);

            canvas.drawText(curString, xPositionOfCurrentStringText, yPositionOfCurrentStringText, mCurrentTemperatureStringTextPaint);
            canvas.drawText(tempString, xPositionOfTempStringText, yPositionOfTempStringText, mCurrentTemperatureStringTextPaint);
            canvas.drawText(currentTempText, xPositionOfCurrentText, yPositionOfCurrentText, mCurrentTemperatureTextPaint);
            canvas.drawText(coolingDesiredText, xPositionOfCoolingText, yPositionOfCoolingText, mDesiredHeatingSmallTextPaint);
            canvas.drawText(heatingDesiredText, xPositionOfHeatingText, yPositionOfHeatingText, mDesiredCoolingSmallTextPaint);
        }

    }


    //Refactor to move out mem leaks
    private void drawArcBetween(Canvas canvas, float heatingDesiredTemp, float coolingDesiredTemp, Paint paint)
    {
        float upperAngle = getAngle(heatingDesiredTemp) + 90;
        float lowerAngle = getAngle(coolingDesiredTemp) + 90;

        canvas.drawArc(mArcRect, lowerAngle, upperAngle - lowerAngle, false, paint);
    }


    private void drawTempModeText(Canvas canvas, float mTemp, String top, String bottom,
                                  Paint tempPaint, Paint descriptionPaint,
                                  @ColorInt int tempPaintColor, @ColorInt int descriptionColor)
    {

        String curTemp = String.valueOf(roundToHalf(mTemp));
        tempPaint.getTextBounds(curTemp, 0, curTemp.length(), bounds);
        float xPositionOfCurrentTempText = cx - (bounds.width() / 2f);
        float yPositionOfCurrentTempText = cy + bounds.height();

        descriptionPaint.getTextBounds(top, 0, top.length(), bounds);

        float xPositionOfCurrentText = cx - (bounds.width() / 2f);
        float yPositionOfCurrentText = yPositionOfCurrentTempText + bounds.height() + paddingBetweenTextDP;

        descriptionPaint.getTextBounds(bottom, 0, bottom.length(), bounds);


        float xPositionOfDesiredText = cx - (bounds.width() / 2f);
        float yPositionOfDesiredText = yPositionOfCurrentText + bounds.height() + (paddingBetweenTextDP / 2f);


        int prevCurrentTempColor   = mCurrentTemperatureTextPaint.getColor();
        int prevCurrentStringColor = mCurrentTemperatureStringTextPaint.getColor();

        mCurrentTemperatureTextPaint.setColor(tempPaintColor);
        mCurrentTemperatureStringTextPaint.setColor(descriptionColor);

        canvas.drawText(top, xPositionOfCurrentText, yPositionOfCurrentText, mCurrentTemperatureStringTextPaint);
        canvas.drawText(bottom, xPositionOfDesiredText, yPositionOfDesiredText, mCurrentTemperatureStringTextPaint);
        canvas.drawText(curTemp, xPositionOfCurrentTempText, yPositionOfCurrentTempText, mCurrentTemperatureTextPaint);

        mCurrentTemperatureTextPaint.setColor(prevCurrentTempColor);
        mCurrentTemperatureStringTextPaint.setColor(prevCurrentStringColor);

    }

    private void drawHeatingText(Canvas canvas, float heatingModeTemp)
    {
        drawTempModeText(canvas, heatingModeTemp, "HEATING", "DESIRED",
                         mCurrentTemperatureTextPaint, mCurrentTemperatureStringTextPaint,
                         Color.parseColor("#e04626"), Color.parseColor("#e04626"));
    }

    private void drawCoolingText(Canvas canvas, float coolingModeTemp)
    {

        drawTempModeText(canvas, coolingModeTemp, "COOLING", "DESIRED",
                         mCurrentTemperatureTextPaint, mCurrentTemperatureStringTextPaint,
                         Color.parseColor("#92bed9"), Color.parseColor("#92bed9"));
    }

    private static float roundToHalf(float d)
    {
        //return Math.round(d * 2) / 2.0f;
        return (float)CCUUtils.roundTo2Decimal((d * 2) / 2.0f);

    }

    private void drawSliderIcon(Canvas canvas, float temp, Bitmap bitmap)
    {
        matrix.reset();

        float angle = getAngle(temp - .2f);
        int   XPos  = (int) ((mArcOuterRadius + mScaledSliderOffset) * Math.cos(Math.toRadians(angle - 90)));
        int   YPos  = (int) ((mArcOuterRadius + mScaledSliderOffset) * Math.sin(Math.toRadians(angle - 90)));
        matrix.postScale(1.0f * mScale, 1.0f * mScale);
        matrix.postRotate(angle + 180);
        matrix.postTranslate(mTranslateX - XPos, mTranslateY - YPos);
        canvas.drawBitmap(bitmap, matrix, mSmallThumbPaint);
    }

    private void drawHeatingSliderIcon(Canvas canvas, float heatingModeTemp)
    {
        drawSliderIcon(canvas, heatingModeTemp, mHeatingProgressCircle);
    }

    private void drawCoolingSliderIcon(Canvas canvas, float coolingModeTemp)
    {
        drawSliderIcon(canvas, coolingModeTemp, mCoolingProgressCircle);
    }


    Matrix matrix = new Matrix();

    private void drawIconByTemp(Canvas canvas, Bitmap bitmap, float temp, float radius, Paint paint)
    {
        float angle = getAngle(temp);
        drawIconByAngle(canvas, bitmap, angle, radius, paint);
    }

    private void drawIconByAngle(Canvas canvas, Bitmap bitmap, float angle, float radius, Paint paint)
    {

        //TODO: refactor op
        matrix.reset();
        int XPos = (int) (radius * Math.cos(Math.toRadians(angle - 90)));
        int YPos = (int) (radius * Math.sin(Math.toRadians(angle - 90)));
        matrix.postScale(1.0f * mScale, 1.0f * mScale);
        matrix.postRotate(angle);
        matrix.postTranslate(mTranslateX - XPos, mTranslateY - YPos);
        canvas.drawBitmap(bitmap, matrix, paint);
    }


    private void drawDesiredIcon(Canvas canvas, float temp, Bitmap bitmap, RectF target)
    {
        matrix.reset();

        /* Double the box size, so the targets are easier to click and then
        reduce them by half.   Keep the view scaling alive during this so
        the doubled his box scales as well.
         */
        matrix.postScale(2.0f, 2.0f);
        float angle = getAngle(temp);

        int XPos = (int) ((mArcRadius + mScaledDrawableOffset) * Math.cos(Math.toRadians(angle - 90)));
        int YPos = (int) ((mArcRadius + mScaledDrawableOffset) * Math.sin(Math.toRadians(angle - 90)));
        matrix.postRotate(angle + 180);
        matrix.postTranslate(mTranslateX - XPos, mTranslateY - YPos);
        matrix.mapRect(target, mTargetRect);


        matrix.preScale(.5f * mScale, .5f * mScale);

        canvas.drawBitmap(bitmap, matrix, mSmallThumbPaint);

    }

    private void drawHeatingDesiredIcon(Canvas canvas, float heatingModeTemp)
    {
        drawDesiredIcon(canvas, heatingModeTemp, mHeatingRectangle, mHeatingTargetRectTransformed);
    }

    private void drawCoolingDesiredIcon(Canvas canvas, float coolingModeTemp)
    {
        drawDesiredIcon(canvas, coolingModeTemp, mCoolingRectangle, mCoolingTargetRectTransformed);
    }

    private boolean checkHeatingModeTemperature(float mCoolingTemp)
    {

        System.out.println("Heating Angle: " + mCoolingTemp);
        if ((mCoolingTemp - (mHeatingDeadBand + mCoolingDeadBand)) < getHeatingDesiredTemp() && (mCoolingTemp - (mHeatingDeadBand + mCoolingDeadBand)) >= mHeatingLowerLimit)
        {
            //setHeatingDesiredTemp(roundToHalf(mCoolingTemp) - (mHeatingDeadBand + mCoolingDeadBand));
            setHeatingDesiredTemp(mCoolingTemp - (mHeatingDeadBand + mCoolingDeadBand),false);
            return true;
        } else if ((mCoolingTemp - (mHeatingDeadBand + mCoolingDeadBand)) > mHeatingUpperLimit) {
            return true;
        }
        return false;
    }

    private void checkForCoolingLine(Canvas canvas, float mCoolingTemp)
    {
        if (mCoolingLowerLimit > (mCoolingTemp - mHeatingDeadBand - mCoolingDeadBand))
        {
            float upperAngle = getAngle(mCoolingLowerLimit) + 90;
            float lowerAngle = getAngle(mCoolingTemp - mHeatingDeadBand - mCoolingDeadBand) + 90;
            canvas.drawArc(mArcOuterRect, lowerAngle, upperAngle - lowerAngle, false, mInnerGreyArcLinePaint);

        }

    }

    private void checkForHeatingLine(Canvas canvas, float mHeatingTemp)
    {
        if (mHeatingUpperLimit < (mHeatingTemp + mHeatingDeadBand + mCoolingDeadBand))
        {
            float upperAngle = getAngle(mHeatingUpperLimit) + 90;
            float lowerAngle = getAngle(mHeatingTemp + mHeatingDeadBand + mCoolingDeadBand) + 90;
            canvas.drawArc(mArcOuterRect, lowerAngle, upperAngle - lowerAngle, false, mInnerGreyArcLinePaint);

        }

    }

    private boolean checkCoolingModeTemperature(float mHeatingTemp)
    {
        if ((mHeatingTemp + (mHeatingDeadBand + mCoolingDeadBand)) >= getCoolingDesiredTemp() && (mHeatingTemp + (mHeatingDeadBand + mCoolingDeadBand)) <= mCoolingUpperLimit)
        {
            //setCoolingDesiredTemp(roundToHalf(mHeatingTemp) + (mHeatingDeadBand + mCoolingDeadBand));
            setCoolingDesiredTemp(mHeatingTemp + (mHeatingDeadBand + mCoolingDeadBand),false);
            return true;
        } else if ((mHeatingTemp + (mHeatingDeadBand + mCoolingDeadBand)) < mCoolingUpperLimit) {
            return true;
        }
        return false;
    }

    private float getCoolingModeTempTemperature()
    {
        float coolingTemperature = getTemperature(mProgresses.get(ProgressType.COOLING));

        if (coolingTemperature >= mCoolingUpperLimit)
        {
            coolingTemperature = mCoolingUpperLimit;
        } else if (coolingTemperature <= mCoolingLowerLimit)
        {
            coolingTemperature = mCoolingLowerLimit;
        }

        if (checkHeatingModeTemperature(coolingTemperature)) {
            return coolingTemperature;
        } else {
            return mCoolingDesiredTemp;
        }
    }


    private float getHeatingModeTempTemperature()
    {
        float heatingTemperature = getTemperature(mProgresses.get(ProgressType.HEATING));

        if (heatingTemperature >= mHeatingUpperLimit)
        {
            heatingTemperature = mHeatingUpperLimit;
        } else if (heatingTemperature <= mHeatingLowerLimit)
        {
            heatingTemperature = mHeatingLowerLimit;
        }

        if (checkCoolingModeTemperature(heatingTemperature)) {
            return heatingTemperature;
        } else {
            return mHeatingDesiredTemp;
        }
    }


    private float getAngle(float temperature)
    {
        return (300.0f * ((temperature - getBuildingLowerTempLimit()) / (getBuildingUpperTempLimit() - getBuildingLowerTempLimit()))) + 30.0f;
    }

    private float getTemperature(float angle)
    {
        float angleProgress = roundToHalf( getBuildingLowerTempLimit() + (getBuildingUpperTempLimit() - getBuildingLowerTempLimit()) * ((angle - 30.0f) / 300.0f));

//Format to represent the angle progress for 0.5 degree increment
        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.applyPattern("##.#");

        angleProgress = Float.parseFloat(decimalFormat.format(angleProgress));
        float d = angleProgress - (long) angleProgress;
        if (d == 0.5) {

            angleProgress = (float) (Math.round(angleProgress) - 0.5);
        } else if (d > 0.1 && d < 0.5) {
            angleProgress = (float) (Math.round(angleProgress) + 0.5);
        } else {
            angleProgress = Math.round(angleProgress);
        }
        return angleProgress;
    }

    private void drawSemiArc(Canvas canvas, boolean detailedView)
    {
        canvas.drawArc(mArcRect, getPathStartAngle(), 300, false, mArcPaint);
    }

    private void drawWhiteDelimiters(Canvas canvas)
    {
        for (float i = getPathStartAngle(); i <= getPathStartAngle() + 300; i = i + mGapAngle)
        {
            canvas.drawArc(mArcRect, i, mDelimiterSize, false, mDelimeterPaint);
        }
    }

    private void drawHeatingLimitBar(Canvas canvas)
    {

        //This is the bar between the user limits, which isn't correct right now.   We need a cooling 7 heating verysion of this.
        canvas.drawArc(mArcOuterRect, mAngleOffset + mLimitHeatingStartAngle, getLimitHeatingSweepAngle(), false, mHeatingArcPaint);

        //This draws the two lines for the inner gauge
        canvas.drawArc(mArcInnerRect, mAngleOffset + mLimitHeatingStartAngle, 1, false, mInnerGreyArcPaint);
        canvas.drawArc(mArcInnerRect, mAngleOffset + mLimitHeatingEndAngle, 1, false, mInnerGreyArcPaint);

        //This draws the line between the two lines for the inner gauge.
        canvas.drawArc(mArcLineInnerRect, mAngleOffset + mLimitHeatingStartAngle, getLimitHeatingSweepAngle() + 1, false, mInnerGreyArcLinePaint);
    }

    private void readyPaint()
    {

        Typeface latoLightFont = ResourcesCompat.getFont(getContext(), R.font.lato_light);

        mDesiredHeatingSmallTextPaint = new Paint();
        mDesiredHeatingSmallTextPaint.setTypeface(latoLightFont);
        mDesiredHeatingSmallTextPaint.setStyle(Paint.Style.FILL);
        mDesiredHeatingSmallTextPaint.setColor(Color.parseColor("#92bed9"));
        mDesiredHeatingSmallTextPaint.setAntiAlias(true);

        mCurrentTemperatureTextPaint = new Paint();
        mCurrentTemperatureTextPaint.setTypeface(latoLightFont);
        mCurrentTemperatureTextPaint.setStyle(Paint.Style.FILL);
        mCurrentTemperatureTextPaint.setColor(Color.parseColor("#99000000"));
        mCurrentTemperatureTextPaint.setAntiAlias(true);

        mDesiredCoolingSmallTextPaint = new Paint();
        mDesiredCoolingSmallTextPaint.setTypeface(latoLightFont);
        mDesiredCoolingSmallTextPaint.setStyle(Paint.Style.FILL);
        mDesiredCoolingSmallTextPaint.setColor(Color.parseColor("#e04626"));
        mDesiredCoolingSmallTextPaint.setAntiAlias(true);

        mCurrentTemperatureStringTextPaint = new Paint();
        mCurrentTemperatureStringTextPaint.setTypeface(latoLightFont);
        mCurrentTemperatureStringTextPaint.setStyle(Paint.Style.FILL);
        mCurrentTemperatureStringTextPaint.setColor(Color.parseColor("#99000000"));
        mCurrentTemperatureStringTextPaint.setAntiAlias(true);

        mInnerGreyArcPaint = new Paint();
        mInnerGreyArcPaint.setColor(Color.GRAY);
        mInnerGreyArcPaint.setAntiAlias(true);
        mInnerGreyArcPaint.setStyle(Paint.Style.STROKE);

        mInnerGreyArcLinePaint = new Paint();
        mInnerGreyArcLinePaint.setColor(Color.GRAY);
        mInnerGreyArcLinePaint.setAntiAlias(true);
        mInnerGreyArcLinePaint.setStyle(Paint.Style.STROKE);

        mSmallThumbPaint = new Paint();
        mSmallThumbPaint.setAntiAlias(true);
        mSmallThumbPaint.setStyle(Paint.Style.FILL);

        mCoolingArcPaint = new Paint();
        mCoolingArcPaint.setColor(Color.parseColor("#92bed9"));
        mCoolingArcPaint.setAntiAlias(true);
        mCoolingArcPaint.setStyle(Paint.Style.STROKE);
        mCoolingArcPaint.setStrokeCap(Paint.Cap.ROUND);
        mCoolingArcPaint.setAlpha(150);

        mHeatingArcPaint = new Paint();
        mHeatingArcPaint.setColor(Color.parseColor("#e24301"));
        mHeatingArcPaint.setAntiAlias(true);
        mHeatingArcPaint.setStyle(Paint.Style.STROKE);
        mHeatingArcPaint.setStrokeCap(Paint.Cap.ROUND);
        mHeatingArcPaint.setAlpha(150);

        mArcPaint = new Paint();
        mArcPaint.setColor(Color.parseColor("#4C6d6e71"));
        mArcPaint.setAntiAlias(true);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeCap(Paint.Cap.ROUND);

        mInbetweenPaint = new Paint();
        mInbetweenPaint.setColor(Color.parseColor("#bcbec0"));
        mInbetweenPaint.setStyle(Paint.Style.STROKE);
        mInbetweenPaint.setAntiAlias(true);

        mDelimeterPaint = new Paint();
        mDelimeterPaint.setColor(Color.parseColor("#99FFFFFF"));
        mDelimeterPaint.setAntiAlias(true);
        mDelimeterPaint.setStyle(Paint.Style.STROKE);

    }


    private void drawCoolingLimitBar(Canvas canvas)
    {
        canvas.drawArc(mArcOuterRect, mAngleOffset + mLimitCoolingStartAngle, getLimitCoolingSweepAngle(), false, mCoolingArcPaint);

        //This draws the two lines for the inner gauge
        canvas.drawArc(mArcInnerRect, mAngleOffset + mLimitCoolingStartAngle, 1, false, mInnerGreyArcPaint);
        canvas.drawArc(mArcInnerRect, mAngleOffset + mLimitCoolingEndAngle, 1, false, mInnerGreyArcPaint);

        //This draws the line between the two lines for the inner gauge.
        canvas.drawArc(mArcLineInnerRect, mAngleOffset + mLimitCoolingStartAngle, getLimitCoolingSweepAngle() + 1, false, mInnerGreyArcLinePaint);

    }

    private void prepareAngle()
    {
        mGapAngle = (300.0f / (mBuildingUpperTempLimit - mBuildingLowerTempLimit));

        mLimitHeatingStartAngle = preCalcAngle(mHeatingLowerLimit);
        mLimitHeatingEndAngle = preCalcAngle(mHeatingUpperLimit);

        mLimitCoolingStartAngle = preCalcAngle(mCoolingLowerLimit);
        mLimitCoolingEndAngle = preCalcAngle(mCoolingUpperLimit);
    }

    private float mScaledDrawableOffset = 0.0f;
    private float mArcDiameter          = 0.0f;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        float height = getDefaultSize(getSuggestedMinimumHeight(),
                                      heightMeasureSpec);
        float width = getDefaultSize(getSuggestedMinimumWidth(),
                                     widthMeasureSpec);
        final float min = Math.max(width, height);
        float       top;
        float       left;

        cx = width / 2;
        cy = width / 2;


        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float          dpWidth        = width / displayMetrics.density;

        mScale = dpWidth / 512.0f;

        float px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                62,
                getResources().getDisplayMetrics()) * mScale;


        mArcDiameter = min - Math.max(Math.max(getPaddingLeft(), getPaddingRight()), Math.max(getPaddingTop(), getPaddingBottom())) - 2 * px;
        mSeperationBetweenArcAndOuterGauge = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                40,
                getResources().getDisplayMetrics()) * mScale;

        mTranslateX = (width * 0.5f);
        mTranslateY = (width * 0.5f);

        top = width / 2.0f - (mArcDiameter / 2.0f);
        left = width / 2.0f - (mArcDiameter / 2.0f);

        mArcRect.set(left, top, left + mArcDiameter, top + mArcDiameter);

        mArcOuterRect.set(left - mSeperationBetweenArcAndOuterGauge, top - mSeperationBetweenArcAndOuterGauge, left + mArcDiameter + mSeperationBetweenArcAndOuterGauge, top + mArcDiameter + mSeperationBetweenArcAndOuterGauge);
        mArcInnerRect.set(left + 40, top + 40, left + mArcDiameter - 40, top + mArcDiameter - 40);

        float strokeAdjustment = (INNER_GREY_STROKE_WIDTH) * 1.5f;
        mArcLineInnerRect.set(left + strokeAdjustment, top + strokeAdjustment, left + mArcDiameter - strokeAdjustment, top + mArcDiameter - strokeAdjustment);

        mArcLimit.set(left + 25, top + 25, left + mArcDiameter - 25, top + mArcDiameter - 25);
        mArcLimitBound.set(left + 20, top + 20, left + mArcDiameter - 20, top + mArcDiameter - 20);
        mArcRectText.set(left - 80, top - 80, left + mArcDiameter + 80, top + mArcDiameter + 80);

        scaleFonts();

        isViewMeasured = true;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


    private void scaleFonts()
    {
        Resources r = getResources();
        float     px;

        paddingBetweenTextDP = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, PADDING_BETWEEN_TEXT * mScale, r.getDisplayMetrics());

        /* Lato Light 113px #000000 60% Opacity  x99 = .6 * 255 */

        px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                101.7f,
                r.getDisplayMetrics()) * mScale;

        mCurrentTemperatureTextPaint.setTextSize(px);

        /* Lato Light 42px #92bed9 100% Opacity */
        px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                37.8f,
                r.getDisplayMetrics()) * mScale;

        mDesiredHeatingSmallTextPaint.setTextSize(px);

        /* Lato Light 42px #e04626 100% Opacity */
        px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                37.8f,
                r.getDisplayMetrics()) * mScale;

        mDesiredCoolingSmallTextPaint.setTextSize(px);

        //Lato Light 22px #000000 //60% Opacity
        px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                19.8f,
                r.getDisplayMetrics()) * mScale;
        mCurrentTemperatureStringTextPaint.setTextSize(px);

        mInnerGreyArcPaint.setStrokeWidth(INNER_GREY_STROKE_WIDTH);
        mInnerGreyArcLinePaint.setStrokeWidth(2);

        px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                9f,
                r.getDisplayMetrics()) * mScale;

        mCoolingArcPaint.setStrokeWidth(px);
        mHeatingArcPaint.setStrokeWidth(px);

        px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                14f,
                r.getDisplayMetrics()) * mScale;


        mArcPaint.setStrokeWidth(px);
        //9px width #6d6e71 30% Opacity 0xF4 ALPHA


        //Lato Light 22px #000000 //60% Opacity
        px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                24f,
                r.getDisplayMetrics()) * mScale;
        mScaledICCTDrawable = px;
        mInbetweenPaint.setStrokeWidth(px);

        px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                20f,
                r.getDisplayMetrics()) * mScale;

        mDelimeterPaint.setStrokeWidth(px);
        mDelimiterSize = 1 * mScale;


        //Lato Light 22px #000000 //60% Opacity
        mScaledICCTDrawable = (mCurrentTempRectangle.getHeight() / 2.0f) * mScale;

        //Convert DP to PX and scaled down by half, so it can be used in the radius calculation when locating the X,Y coordinates
        //to draw this drawable.   This is needed, so when transistion the onClick from the listview the view will naturally grow,
        //in this manner the small undetailed version of the ARC can be animated to the detailed view when it's clicked and
        //the animation will naturally scale instead of being chopppy or jumping between.   According to Shilpa's latest
        //specs from the arch, this should animate when selected.
        mScaledDrawableOffset = (mCoolingRectangle.getHeight() / 2.0f) * mScale;

        mScaledSliderOffset = (mCoolingProgressCircle.getHeight() / 2.0f) * mScale;
        mArcRadius = (mArcDiameter / 2.0f);
        mArcOuterRadius = mArcRadius + mSeperationBetweenArcAndOuterGauge;

    }


    boolean inCoolingSelectionMode = false;
    boolean inHeatingSelectionMode = false;

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                //setDetailedView(false);
                //mAngleProgress = 0.0f;
                if (isDetailedView() && !inCoolingSelectionMode && !inHeatingSelectionMode)
                {
                    inCoolingSelectionMode = isCoolingPressed(event.getX(), event.getY());
                    inHeatingSelectionMode = isHeatingPressed(event.getX(), event.getY());
                }

                getParent().requestDisallowInterceptTouchEvent(false);

                isMoveStarted = inCoolingSelectionMode || inHeatingSelectionMode;

                break;
            case MotionEvent.ACTION_MOVE:

                if (isMoveStarted)
                {
                    isTouched = true;

                    getParent().requestDisallowInterceptTouchEvent(true);
                    updateOnTouch(inCoolingSelectionMode, inHeatingSelectionMode, event);
                }
                break;
            case MotionEvent.ACTION_UP:

                getParent().requestDisallowInterceptTouchEvent(false);
                setPressed(false);


                if (inCoolingSelectionMode)
                {
                    setCoolingDesiredTemp(roundToHalf(getCoolingModeTempTemperature()),true);
                }
                if (inHeatingSelectionMode)
                {
                    setHeatingDesiredTemp(roundToHalf(getHeatingModeTempTemperature()),true);
                }


                if (!inCoolingSelectionMode && !inHeatingSelectionMode)
                {
                    System.out.println("OnClickCalled");
                    callOnClick();
                }
                inCoolingSelectionMode = inHeatingSelectionMode = false;


                isMoveStarted = false;
                isTouched = false;
                break;
        }

        return true;
    }

    @Override
    protected void drawableStateChanged()
    {
        super.drawableStateChanged();
        invalidate();
    }

    private void updateOnTouch(boolean inCoolingSelectionMode, boolean inHeatingSelectionMode, MotionEvent event)
    {
        if (!isMoveStarted && !isTouched)
        {
            return;
        }

        float currentAngle = (float) getTouchDegrees(event.getX(), event.getY());

        int progress = getProgressForAngle(currentAngle);

        setPressed(true);
        if (progress != INVALID_PROGRESS_VALUE)
        {
            if (inCoolingSelectionMode)
            {
                mProgresses.put(ProgressType.COOLING, currentAngle);

            } else if (inHeatingSelectionMode)
            {
                mProgresses.put(ProgressType.HEATING, currentAngle);
            }

            invalidate();
        }
    }

    private boolean isCoolingPressed(float xpos, float ypos)
    {

        return mCoolingTargetRectTransformed.contains(xpos, ypos);

    }

    private boolean isHeatingPressed(float xpos, float ypos)
    {
        return mHeatingTargetRectTransformed.contains(xpos, ypos);
    }


    private double getTouchDegrees(float xPos, float yPos)
    {
        float x     = xPos - mTranslateX;
        float y     = yPos - mTranslateY;
        float angle = (float) Math.toDegrees(Math.atan2(y, x) + (Math.PI / 2.0f) - Math.toRadians(180));


        if (angle < 0)
        {
            angle = 360.0f + angle;
        }

        while (angle > 360)
        {
            angle = angle - 360;
        }

        Log.i("DEGREES", "touchDegrees: " + angle);
        return angle;
    }

    private int getProgressForAngle(double angle)
    {
        int touchProgress = (int) Math.round(1 * angle);


        touchProgress = (Double.isNaN(angle)) ? INVALID_PROGRESS_VALUE : touchProgress;
        touchProgress = (touchProgress < 0) ? INVALID_PROGRESS_VALUE
                                            : touchProgress;
        touchProgress = (touchProgress > mMax) ? INVALID_PROGRESS_VALUE
                                               : touchProgress;

        return touchProgress;
    }


    enum ProgressType
    {
        COOLING, HEATING, CURRENT
    }

    /**
     * Sets a listener to receive notifications of changes to the SeekArc's
     * progress level. Also provides notifications of when the user starts and
     * stops a touch gesture within the SeekArc.
     *
     * @param l The seek bar notification listener
     * @see SeekArc.OnTemperatureChangeListener
     */
    public void setOnTemperatureChangeListener(OnTemperatureChangeListener l)
    {
        mOnTemperatureChangeListener = l;
    }

    public void setCoolingLowerLimit(float userLimitCoolingStartPoint)
    {
        this.mCoolingLowerLimit = userLimitCoolingStartPoint;
        prepareAngle();
        invalidate();
    }

    public void setCoolingUpperLimit(float userLimitCoolingEndPoint)
    {
        this.mCoolingUpperLimit = userLimitCoolingEndPoint;
        prepareAngle();
        invalidate();
    }


    public void setHeatingLowerLimit(float userLimitHeatingStartPoint)
    {
        mHeatingLowerLimit = userLimitHeatingStartPoint;
        prepareAngle();
        invalidate();
    }


    public void setHeatingUpperLimit(float userLimitHeatingEndPoint)
    {

        mHeatingUpperLimit = userLimitHeatingEndPoint;
        prepareAngle();
        invalidate();
    }

    private float preCalcAngle(float tempPoint)
    {

        float tempAngle = 210.0f + mGapAngle * (tempPoint - mBuildingLowerTempLimit);
        if (tempAngle < 360)
        {
            return tempAngle;
        } else
        {
            return tempAngle - 360;
        }

    }


    private float getSweepAngle(float startAngle, float endAngle)
    {
        if (startAngle < 180.0f && endAngle < 180.0f)
        {
            return endAngle - startAngle;
        } else if ((startAngle > 180 && startAngle < 360) && (endAngle > 180 && endAngle < 360))
        {
            return endAngle - startAngle;
        }


        if (startAngle == 360.0f || startAngle == 0.0f)
        {
            return Math.abs(0 + endAngle);
        } else
        {
            float startLimitAngle = 360.0f - startAngle;
            float ret             = Math.abs(startLimitAngle + endAngle);
            if (ret <= 360)
                return ret;
            else
                return ret - 360;
        }
    }

    private float getLimitHeatingSweepAngle()
    {
        return getSweepAngle(mLimitHeatingStartAngle, mLimitHeatingEndAngle);
    }

    private float getLimitCoolingSweepAngle()
    {
        return getSweepAngle(mLimitCoolingStartAngle, mLimitCoolingEndAngle);
    }


    private float getPathStartAngle()
    {
        return mPathStartAngle;
    }


    public boolean isDetailedView()
    {
        return mDetailedView;
    }

    public void setDetailedView(boolean isDetailedView)
    {
        this.mDetailedView = isDetailedView;
        invalidate();
    }


    public float getBuildingLowerTempLimit()
    {
        return mBuildingLowerTempLimit;
    }

    public void setBuildingLowerTempLimit(float buildingLowerTempLimit)
    {
        this.mBuildingLowerTempLimit = buildingLowerTempLimit;
        prepareAngle();
        invalidate();
    }

    public float getBuildingUpperTempLimit()
    {
        return mBuildingUpperTempLimit;
    }

    public void setBuildingUpperTempLimit(float buildingUpperTempLimit)
    {
        this.mBuildingUpperTempLimit = buildingUpperTempLimit;
        prepareAngle();
        invalidate();
    }


    public float getCoolingDesiredTemp()
    {
        return mCoolingDesiredTemp;
    }

    public void setCoolingDesiredTemp(float coolingDesiredTemp,boolean syncToHaystack)
    {
        if (mCoolingDesiredTemp == coolingDesiredTemp)
            return;

        float deadBandSum = mHeatingDeadBand + mCoolingDeadBand;
        if (getHeatingDesiredTemp() == mHeatingLowerLimit && ((coolingDesiredTemp - getHeatingDesiredTemp()) < deadBandSum)){
            coolingDesiredTemp = coolingDesiredTemp + (deadBandSum - (coolingDesiredTemp - getHeatingDesiredTemp()));
        }

        this.mCoolingDesiredTemp = coolingDesiredTemp;
        mOnTemperatureChangeListener.onTemperatureChange(this, getCoolingDesiredTemp(), getHeatingDesiredTemp(),syncToHaystack);
        invalidate();
    }


    public float getHeatingDesiredTemp()
    {
        return mHeatingDesiredTemp;
    }

    public void setHeatingDesiredTemp(float heatingDesiredTemp, boolean syncToHaystack)
    {
        if (mHeatingDesiredTemp == heatingDesiredTemp)
            return;

        if (getCoolingDesiredTemp() == mCoolingUpperLimit && ((getCoolingDesiredTemp() - heatingDesiredTemp) < (mHeatingDeadBand + mCoolingDeadBand))){
            heatingDesiredTemp =  heatingDesiredTemp - ((mHeatingDeadBand + mCoolingDeadBand) - (getCoolingDesiredTemp() - heatingDesiredTemp));
        }

        this.mHeatingDesiredTemp = heatingDesiredTemp;
        mOnTemperatureChangeListener.onTemperatureChange(this, getCoolingDesiredTemp(), getHeatingDesiredTemp(),syncToHaystack);
        invalidate();
    }


    public float getCurrentTemp()
    {
        return mCurrentTemp;
    }

    public void setCurrentTemp(float CurrentTemp)
    {
        this.mCurrentTemp = CurrentTemp;
        invalidate();
    }


    public float getCoolingDeadBand()
    {
        return mCoolingDeadBand;
    }

    public void setCoolingDeadBand(float coolingDeadBand)
    {
        this.mCoolingDeadBand = coolingDeadBand;
        invalidate();
    }

    public float getHeatingDeadBand()
    {
        return mHeatingDeadBand;
    }

    public void setHeatingDeadBand(float heatingDeadBand)
    {
        this.mHeatingDeadBand = heatingDeadBand;
        invalidate();
    }

    private Runnable runnable = new Runnable()
    {
        @Override
        public void run()
        {

            currentAnimationTime += animationInterpolator;

            if (endAnimation >= currentAnimationTime)
            {

                getLayoutParams().height = (int) (startHeight + (currentAnimationTime / endAnimation) * heightStepper);
                getLayoutParams().width = (int) (startWidth + (currentAnimationTime / endAnimation) * widthStepper);
                requestLayout();

                handler.postDelayed(this, animationInterpolator);
            } else
            {
                finishAnimation();
            }
        }
    };

    private void finishAnimation()
    {
        currentAnimationTime = 0;


        if (mTransistionDetailViewAtEnd)
        {
            setDetailedView(!isDetailedView());
        }

        mTransistionDetailViewAtEnd = false;
    }


    public void modifySize(float multiplier, boolean transistionDetailViewAtEnd)
    {
        isViewMeasured = false;
        currentAnimationTime = 0;

        mTransistionDetailViewAtEnd = transistionDetailViewAtEnd;
        startHeight = getLayoutParams().height;
        startWidth = getLayoutParams().width;

        endHeight = (int) (getLayoutParams().height * multiplier);
        endWidth = (int) (getLayoutParams().width * multiplier);

        heightStepper = (endHeight - startHeight);
        widthStepper = (endWidth - startWidth);

        handler.postDelayed(runnable, animationInterpolator);
    }

    public void scaletoBig()
    {
        isViewMeasured = false;
        currentAnimationTime = 0;

        startHeight = getLayoutParams().height;
        startWidth = getLayoutParams().width;

        endHeight = (int) (getLayoutParams().height * 1.30);
        endWidth = (int) (getLayoutParams().width * 1.30);

        heightStepper = (endHeight - startHeight);
        widthStepper = (endWidth - startWidth);

        handler.postDelayed(runnable, animationInterpolator);
    }

    public void scaletoSmall()
    {
        isViewMeasured = false;
        currentAnimationTime = 0;

        startHeight = getLayoutParams().height;
        startWidth = getLayoutParams().width;

        endHeight = (int) (getLayoutParams().height / 1.30);
        endWidth = (int) (getLayoutParams().width / 1.30);

        heightStepper = (endHeight - startHeight);
        widthStepper = (endWidth - startWidth);

        handler.postDelayed(runnable, animationInterpolator);
    }

    public void scaletoNormal(int height,int width)
    {
        isViewMeasured = false;
        currentAnimationTime = 0;

        //startHeight = height;
        //startWidth = width;
/*
        endHeight = (int)(height * 1.5);//(int) (getLayoutParams().height);
        endWidth = (int)(width * 1.5);//(int) (getLayoutParams().width) ;

        startHeight = getLayoutParams().height;
        startWidth = getLayoutParams().width;

        heightStepper = (endHeight - startHeight);
        widthStepper =  (endWidth - startWidth);*/

        startHeight = height;
        startWidth = width;

        endHeight = (int) (height / 1.30);
        endWidth = (int) (height / 1.30);

        heightStepper = (endHeight - startHeight);
        widthStepper = (endWidth - startWidth);

        handler.postDelayed(runnable, animationInterpolator);
    }

    public void scaletoNormalBig(int height,int width)
    {
        isViewMeasured = false;
        currentAnimationTime = 0;
/*
        startHeight = getLayoutParams().height;
        startWidth = getLayoutParams().width;

        endHeight = (int) (getLayoutParams().height * 1.30);
        endWidth = (int) (getLayoutParams().width * 1.30);

        heightStepper = (endHeight - startHeight);
        widthStepper = (endWidth - startWidth);*/

        startHeight = height;
        startWidth = width;

        endHeight = (int) (height * 1.35);
        endWidth = (int) (height * 1.35);

        heightStepper = (endHeight - startHeight);
        widthStepper = (endWidth - startWidth);

        handler.postDelayed(runnable, animationInterpolator);
    }



    public static Bitmap drawableToBitmap (Drawable drawable) {

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public void setSenseData(boolean detailedView, float curTemp){
        mCurrentTemp = curTemp;
        mDetailedView = detailedView;
        isDataSet = true;
        prepareAngle();
        invalidate();

    }

}
