package a75f.io.renatus.views;
/**
 * Created by Yinten on 10/9/2017.
 */

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.text.DecimalFormat;

import a75f.io.api.haystack.Zone;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.RoomDataInterface;
import a75f.io.renatus.R;

public class SeekArc extends View implements RoomDataInterface
{

    private static final String  TAG                    = "Kumar";
    public static        double  LEFT_BOUND             = 210;
    public static        double  RIGHT_BOUND            = 150;
    private static       int     INVALID_PROGRESS_VALUE = -1;
    // The initial rotational offset -90 means we start at 12 o'clock
    private final        int     mAngleOffset           = -90;
    public               boolean mDetailedView          = false;
    public Canvas mCanvas;
    public DEVICE_TYPE mDeviceType = DEVICE_TYPE.PURE_DAB;
    Drawable nUnSelColor    = getContext().getResources().getDrawable(R.drawable.buttonback);
    Drawable nSelectedColor =
            getContext().getResources().getDrawable(R.drawable.buttonbackselected);
    Drawable nColor         = nSelectedColor;
    /**
     * The Drawable for the seek arc thumbnail
     */
    private Drawable mThumb;
    /**
     * The Maximum value that this SeekArc can be set to
     */
    private int     mMax               = 360;
    /**
     * The Current value that the SeekArc is set to
     */
    private int     mProgress          = 50;
    /**
     * The width of the progress line for this SeekArc
     */
    private int     mProgressWidth     = 0;
    /**
     * The Width of the background arc for the SeekArc
     */
    private int     mArcWidth          = 0;
    /**
     * The Angle to start drawing this Arc from
     */
    private int     mStartAngle        = -90;
    /**
     * The Angle through which to draw the arc (Max is 360)
     */
    private float   MAX_SWEEP_PROGRESS = 300;
    private int     mSweepAngle        = 300;
    /**
     * The rotation of the SeekArc- 0 is twelve o'clock
     */
    private int     mRotation          = 0;
    /**
     * Give the SeekArc rounded edges
     */
    private boolean mRoundedEdges      = false;
    /**
     * Enable touch inside the SeekArc
     */
    private boolean mTouchInside       = false;
    /**
     * Will the progress increase clockwise or anti-clockwise
     */
    private boolean mClockwise         = true;
    // Internal variables
    private int     mArcRadius         = 0;
    private float   mProgressSweep     = 0;
    private RectF   mArcRect           = new RectF();
    private RectF   mArcLimit          = new RectF();
    private RectF   mArcRectText       = new RectF();
    private RectF   mArcLimitBound     = new RectF();
    private Paint                   mArcPaint;
    private Paint                   mProgressPaint;
    private Paint                   mUserLimitProgressPaint;
    private Paint                   mDelimeterPaint;
    private Paint                   mDelimeterPaintPointValue;
    private Paint                   mProgressTextPaint;
    private Paint                   mUserLimitOutsideProgressPaint;
    private Paint                   mOuterTextPaint;
    private Paint                   mUserLimitTextPaint;
    private Paint                   mThumbeCircleTextPaint;
    private Paint                   mArcLimitPaint;
    private Paint                   mThumbOuterCirclePaint;
    private Paint                   mThumbInnerCirclePaint;
    private int                     mTranslateX;
    private int                     mTranslateY;
    private int                     mThumbXPos;
    private int                     mThumbYPos;
    private double                  mTouchAngle;
    private float                   mTouchIgnoreRadius;
    private OnSeekArcChangeListener mOnSeekArcChangeListener;
    private int width  = 0;
    private int height = 0;
    private float cx;
    private float cy;
    private boolean isFirstRun = true;
    private Path  path;
    //MARK
    //private ArrayList<FSVData> lcmdabfsv;
    //private ArrayList<FSVData> ifttdabfsv;
    private int   delimeterColor;
    private Paint mProgressLimitPaint;
    private boolean isTouched = false;
    private double mCurrentTemp;
    private double mDesireTemp;
    private float  mLimitStartAngle;
    private float  mLimitEndAngle;
    private float  mPathStartAngle;
    private float   angleProgress = 0.0f;
    private boolean mTouchOutSide = false;
    private float          mTouchIgnoreRadiusOutSide;
    private CountDownTimer timer;
    private boolean isTimerFinished     = false;
    private float   userLimitStartPoint = 0;
    private float   userLimitEndPoint   = 0;
    private double  originalDesireTemp  = 0;
    private int originalTranslateX;
    private int originalTranslateY;
    private int originalThumbXPos;
    private int originalThumbYPos;
    private int mProgressTextSize;
    private int mOuterTextSize;
    private int mUserLimitTextSize;
    private int mThumbCircleTextSize;
    private int mThumbOuterRadius;
    private int mThumbInnerRadius;
    private int mMarkerTextHeight;
    private int mStatusTextHeight;
    private int mMarkerTextWidthX;
    private int mMarkerTextWidthY;
    private int mStatusTextWidth;
    private int mStatusOutsideTextWidth;
    private int mTempTextWidth = 65;
    private int   mThumbXPos2;
    private int   mThumbYPos2;
    private int   mStatusOutsideTextHeight;
    private int   mTempTextHeight;
    private Paint mStatusTextTestPaint;
    private Paint mStatusTextPaint;
    private int   mStatusTextSize;
    private int   mThumbTextWidth;
    private int   mSmallThumbRadius;
    private Paint mSmallThumbPaint;
    private int   mThumbDifference;
    private int   mThumbTextHeight;
    private Paint mThumbOuterLimitCirclePaint;
    private Paint mThumbeOuterLimitCircleTextPaint;
    private double  originalCurrentTemp      = 0;
    private boolean isCurrBeyondLimit        = false;
    private boolean isMoveStarted            = false;
    private float   mGapAngle                = 7.5f;
    private double  mMiddleAngle             = 70;
    private double  mBuildingLimitStartAngle = 50;
    private double  mBuildingLimitEndAngle   = 90;
    private double  mLeftMarginAngle         = 48;
    //MARK
    //private RoomData roomData;
    //private CMData cmData;
    private int             nIndex;
    private TextPaint       mRoomTextPaint;
    private String          roomName;
    private OnClickListener mOnClickListener;
    private boolean isSensorPaired = false;
    private int mUserLimitDiff;
    private int mOutsideLimitDiffLeft;
    private int mOutsideLimitDiffRight;
    private boolean showCCUDial = false;
    //private SingleStageProfile mSSEProfile;
    private ZoneProfile zoneProfile;
    private Zone        mZone;


    public SeekArc(Context context)
    {
        super(context);
        init(context, null, R.attr.seekArcStyle);
    }


    public void init(Context context, AttributeSet attrs, int defStyle)
    {
        final Resources res = getResources();
        float density = context.getResources().getDisplayMetrics().density;
        // Defaults, may need to link this into theme settings
        int arcColor = res.getColor(R.color.outer_temp_text_darker_gray);
        //int progressColor = res.getColor(R.color.progress_color_orange);
        int progressColor = res.getColor(R.color.accent);
        int graycolor = res.getColor(R.color.dark_gray);
        int userLimitProgressColor = res.getColor(R.color.userlimit_outbound_color);
        //int thumbOuterColor = res.getColor(R.color.thummb_outer_orange);
        int thumbOuterColor = res.getColor(R.color.accent);
        int thumbInnerColor = res.getColor(R.color.thumb_inner_white);
        delimeterColor = res.getColor(R.color.progress_delimeter_white);
        @ColorInt int statusTempText =
                ContextCompat.getColor(context, R.color.outer_temp_text_darker_gray);
        //int outerTempText = res.getColor(R.color.progress_color_orange);
        int outerTempText = res.getColor(R.color.accent);
        int thumbOuterLimitColor = res.getColor(R.color.userlimit_outbound_color);
        int thumbHalfheight = 0;
        int thumbHalfWidth = 0;
        mThumb = res.getDrawable(R.drawable.seek_arc_control_selector);
        // Convert progress width to pixels for current density
        mProgressWidth = (int) (mProgressWidth * density);
        timer = new CountDownTimer(3000, 1000)
        {

            public void onTick(long millisUntilFinished)
            {
            }


            public void onFinish()
            {
                invalidate();
                isTimerFinished = true;
            }
        }.start();
        if (attrs != null)
        {
            // Attribute initialization
            final TypedArray a =
                    context.obtainStyledAttributes(attrs, R.styleable.SeekArc, defStyle, 0);
            Drawable thumb = a.getDrawable(R.styleable.SeekArc_thumb);
            if (thumb != null)
            {
                mThumb = thumb;
            }
            thumbHalfheight = (int) mThumb.getIntrinsicHeight() / 2;
            thumbHalfWidth = (int) mThumb.getIntrinsicWidth() / 2;
            mThumb.setBounds(-thumbHalfWidth, -thumbHalfheight, thumbHalfWidth, thumbHalfheight);
            mMax = a.getInteger(R.styleable.SeekArc_max, mMax);
            mProgress = a.getInteger(R.styleable.SeekArc_progress, mProgress);
            mProgressWidth =
                    (int) a.getDimension(R.styleable.SeekArc_progressWidth, mProgressWidth);
            mArcWidth = (int) a.getDimension(R.styleable.SeekArc_arcWidth, mArcWidth);
            mStartAngle = a.getInt(R.styleable.SeekArc_startAngle, mStartAngle);
            mSweepAngle = a.getInt(R.styleable.SeekArc_sweepAngle, mSweepAngle);
            mRotation = a.getInt(R.styleable.SeekArc_rotation, mRotation);
            mRoundedEdges = a.getBoolean(R.styleable.SeekArc_roundEdges, mRoundedEdges);
            mTouchInside = a.getBoolean(R.styleable.SeekArc_touchInside, mTouchInside);
            /*mTouchOutSide = a.getBoolean(R.styleable.SeekArc_touchInside,
                    mTouchOutSide);*/
            mClockwise = a.getBoolean(R.styleable.SeekArc_clockwise, mClockwise);
            mProgressTextSize =
                    (int) a.getDimension(R.styleable.SeekArc_progresstextsize, mProgressTextSize);
            mOuterTextSize =
                    (int) a.getDimension(R.styleable.SeekArc_outertextsize, mOuterTextSize);
            mStatusTextSize =
                    (int) a.getDimension(R.styleable.SeekArc_statustextsize, mStatusTextSize);
            mUserLimitTextSize =
                    (int) a.getDimension(R.styleable.SeekArc_userlimittextsize, mUserLimitTextSize);
            mThumbCircleTextSize =
                    (int) a.getDimension(R.styleable.SeekArc_thumbcircletextsize, mThumbCircleTextSize);
            mThumbOuterRadius =
                    (int) a.getDimension(R.styleable.SeekArc_thumbouterradius, mThumbOuterRadius);
            mThumbInnerRadius =
                    (int) a.getDimension(R.styleable.SeekArc_thumbinnerradius, mThumbInnerRadius);
            mMarkerTextHeight =
                    (int) a.getDimension(R.styleable.SeekArc_markertextheight, mMarkerTextHeight);
            mStatusTextHeight =
                    (int) a.getDimension(R.styleable.SeekArc_statustextheight, mStatusTextHeight);
            mMarkerTextWidthX =
                    (int) a.getDimension(R.styleable.SeekArc_markertextwidthx, mMarkerTextWidthX);
            mMarkerTextWidthY =
                    (int) a.getDimension(R.styleable.SeekArc_markertextwidthy, mMarkerTextWidthY);
            mStatusTextWidth =
                    (int) a.getDimension(R.styleable.SeekArc_statustextwidth, mStatusTextWidth);
            mStatusOutsideTextWidth =
                    (int) a.getDimension(R.styleable.SeekArc_statusoutsidetextwidth, mStatusOutsideTextWidth);
            mStatusOutsideTextHeight =
                    (int) a.getDimension(R.styleable.SeekArc_statusoutsidetextheight, mStatusOutsideTextHeight);
            mTempTextWidth =
                    (int) a.getDimension(R.styleable.SeekArc_temptextwidth, mTempTextWidth);
            mTempTextHeight =
                    (int) a.getDimension(R.styleable.SeekArc_temptextheight, mTempTextHeight);
            mThumbTextWidth =
                    (int) a.getDimension(R.styleable.SeekArc_thumbtextwidth, mThumbTextWidth);
            mThumbTextHeight =
                    (int) a.getDimension(R.styleable.SeekArc_thumbtextheight, mThumbTextHeight);
            mSmallThumbRadius =
                    (int) a.getDimension(R.styleable.SeekArc_smallthumbradius, mSmallThumbRadius);
            mThumbDifference =
                    (int) a.getDimension(R.styleable.SeekArc_thumbdifference, mThumbDifference);
            a.recycle();
        }
        mProgress = (mProgress > mMax) ? mMax : mProgress;
        mProgress = (mProgress < 0) ? 0 : mProgress;
        mSweepAngle = (mSweepAngle > 360) ? 360 : mSweepAngle;
        //mSweepAngle = (mSweepAngle < 0) ? 0 : mSweepAngle;
        mStartAngle = (mStartAngle > 360) ? 0 : mStartAngle;
        mStartAngle = (mStartAngle < 0) ? 0 : mStartAngle;
        mArcPaint = new Paint();
        mArcPaint.setColor(arcColor);
        mArcPaint.setAntiAlias(true);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(mArcWidth);
        mArcPaint.setStrokeCap(Paint.Cap.ROUND);
        mArcPaint.setAlpha(150);
        mProgressPaint = new Paint();
        mProgressPaint.setColor(progressColor);
        mProgressPaint.setAntiAlias(true);
        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setStrokeWidth(mProgressWidth);
        mProgressPaint.setStrokeCap(Paint.Cap.SQUARE);
        //        mProgressPaint.setAlpha(166);
        mUserLimitProgressPaint = new Paint();
        mUserLimitProgressPaint.setColor(userLimitProgressColor);
        mUserLimitProgressPaint.setAntiAlias(true);
        mUserLimitProgressPaint.setStyle(Paint.Style.STROKE);
        mUserLimitProgressPaint.setStrokeWidth(mProgressWidth);
        mUserLimitProgressPaint.setStrokeCap(Paint.Cap.SQUARE);
        mUserLimitProgressPaint.setAlpha(100);
        mDelimeterPaint = new Paint();
        mDelimeterPaint.setColor(delimeterColor);
        mDelimeterPaint.setAntiAlias(true);
        mDelimeterPaint.setStyle(Paint.Style.STROKE);
        mDelimeterPaint.setStrokeWidth(25);
        mDelimeterPaintPointValue = new Paint();
        mDelimeterPaintPointValue.setColor(delimeterColor);
        mDelimeterPaintPointValue.setAntiAlias(true);
        mDelimeterPaintPointValue.setStyle(Paint.Style.STROKE);
        mDelimeterPaintPointValue.setStrokeWidth(15);
        mProgressTextPaint = new Paint();
        mProgressTextPaint.setColor(progressColor);
        mProgressTextPaint.setTextAlign(Paint.Align.LEFT);
        mProgressTextPaint.setAntiAlias(true);
        mProgressTextPaint.setStyle(Paint.Style.FILL);
        mProgressTextPaint.setTextSize(mProgressTextSize);
        mUserLimitOutsideProgressPaint = new Paint();
        mUserLimitOutsideProgressPaint.setColor(userLimitProgressColor);
        mUserLimitOutsideProgressPaint.setAntiAlias(true);
        mUserLimitOutsideProgressPaint.setStyle(Paint.Style.FILL);
        mUserLimitOutsideProgressPaint.setTextSize(mProgressTextSize);
        mOuterTextPaint = new Paint();
        mOuterTextPaint.setColor(outerTempText);
        mOuterTextPaint.setAntiAlias(true);
        mOuterTextPaint.setTextAlign(Paint.Align.LEFT);
        mOuterTextPaint.setStyle(Paint.Style.FILL);
        mOuterTextPaint.setTextSize(mOuterTextSize);
        mStatusTextTestPaint = new Paint();
        mStatusTextTestPaint.setColor(Color.BLACK);
        mStatusTextTestPaint.setTextAlign(Paint.Align.LEFT);
        mStatusTextTestPaint.setStyle(Paint.Style.FILL);
        mStatusTextTestPaint.setTextSize(57);
        mStatusTextPaint = new Paint();
        mStatusTextPaint.setColor(statusTempText);
        mStatusTextPaint.setAntiAlias(true);
        mStatusTextPaint.setTextAlign(Paint.Align.LEFT);
        mStatusTextPaint.setStyle(Paint.Style.FILL);
        mStatusTextPaint.setTextSize(mStatusTextSize);
        mRoomTextPaint = new TextPaint();
        mRoomTextPaint.setColor(graycolor);
        mRoomTextPaint.setAntiAlias(true);
        mRoomTextPaint.setTextAlign(Paint.Align.LEFT);
        mRoomTextPaint.setStyle(Paint.Style.FILL);
        mRoomTextPaint.setTextSize(mStatusTextSize);
        mRoomTextPaint.setLinearText(true);
        mUserLimitTextPaint = new Paint();
        mUserLimitTextPaint.setColor(progressColor);
        mUserLimitTextPaint.setAntiAlias(true);
        mUserLimitTextPaint.setTextAlign(Paint.Align.LEFT);
        mUserLimitTextPaint.setStyle(Paint.Style.FILL);
        mUserLimitTextPaint.setTextSize(mOuterTextSize);
        mThumbeCircleTextPaint = new Paint();
        mThumbeCircleTextPaint.setColor(progressColor);
        mThumbeCircleTextPaint.setAntiAlias(true);
        mThumbeCircleTextPaint.setStyle(Paint.Style.FILL);
        mThumbeCircleTextPaint.setTextSize(mThumbCircleTextSize);
        mProgressLimitPaint = new Paint();
        mProgressLimitPaint.setColor(arcColor);
        mProgressLimitPaint.setAntiAlias(true);
        mProgressLimitPaint.setStyle(Paint.Style.STROKE);
        mProgressLimitPaint.setStrokeWidth(30);
        mArcLimitPaint = new Paint();
        mArcLimitPaint.setColor(arcColor);
        mArcLimitPaint.setAntiAlias(true);
        mArcLimitPaint.setStyle(Paint.Style.STROKE);
        mArcLimitPaint.setStrokeWidth(5);
        mThumbOuterCirclePaint = new Paint();
        mThumbOuterCirclePaint.setColor(thumbOuterColor);
        mThumbOuterCirclePaint.setAntiAlias(true);
        mThumbOuterCirclePaint.setStyle(Paint.Style.FILL);
        mSmallThumbPaint = new Paint();
        mSmallThumbPaint.setColor(thumbOuterColor);
        mSmallThumbPaint.setAntiAlias(true);
        mSmallThumbPaint.setStyle(Paint.Style.FILL);
        //        mSmallThumbPaint.setAlpha(150);
        mThumbInnerCirclePaint = new Paint();
        mThumbInnerCirclePaint.setColor(thumbInnerColor);
        mThumbInnerCirclePaint.setAntiAlias(true);
        mThumbInnerCirclePaint.setStyle(Paint.Style.FILL);
        mThumbOuterLimitCirclePaint = new Paint();
        mThumbOuterLimitCirclePaint.setColor(thumbOuterLimitColor);
        mThumbOuterLimitCirclePaint.setAntiAlias(true);
        mThumbOuterLimitCirclePaint.setStyle(Paint.Style.FILL);
        mThumbeOuterLimitCircleTextPaint = new Paint();
        mThumbeOuterLimitCircleTextPaint.setColor(thumbOuterLimitColor);
        mThumbeOuterLimitCircleTextPaint.setAntiAlias(true);
        mThumbeOuterLimitCircleTextPaint.setStyle(Paint.Style.FILL);
        mThumbeOuterLimitCircleTextPaint.setTextSize(mThumbCircleTextSize);
    }


    public SeekArc(Context context, AttributeSet attrs, ZoneProfile zoneProfile)
    {
        super(context, attrs);
        this.zoneProfile = zoneProfile;
        if (zoneProfile != null)
        {
            zoneProfile.setZoneProfileInterface(this);
            mDesireTemp = L.getDesiredTemp(zoneProfile);
        }
        init(context, attrs, R.attr.seekArcStyle);
        
    }


    public SeekArc(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }


    @Override
    public void refreshView()
    {
        this.post(new Runnable()
        {

            @Override
            public void run()
            {
                if (zoneProfile != null)
                {
                    setCurrentTemp(zoneProfile.getDisplayCurrentTemp());
                    //setDesireTemp(L.getDesiredTemp(zoneProfile));
                }
                invalidate();
            }
        });
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                setDetailedView(false);
                getParent().requestDisallowInterceptTouchEvent(false);
                isFirstRun = false;
                isTimerFinished = true;
                if (!isSensorPaired /*&& (nIndex != 1)*/)
                {
                    isMoveStarted = isThumbPressed(event.getX(), event.getY());
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isMoveStarted)
                {
                    isTouched = true;
                    onStartTrackingTouch();
                    setDetailedView(true);
                    getParent().requestDisallowInterceptTouchEvent(true);
                    updateOnTouch(event);
                    isTimerFinished = false;
                    timer.cancel();
                }
                break;
            case MotionEvent.ACTION_UP:
                setDetailedView(false);
                getParent().requestDisallowInterceptTouchEvent(false);
                setPressed(false);
                if (isTouched)
                {
                    timer.start();
                    onStopTrackingTouch();
                }
                else if (!isFirstRun && !ignoreTouch(event.getX(), event.getY()))
                {
                    mOnClickListener.onClick(this);
                }
                isMoveStarted = false;
                isTouched = false;
                break;
        }
        return true;
    }


    @Override
    protected void onDraw(Canvas canvas)
    {
        Rect bounds = new Rect();
        this.setBackgroundDrawable(nColor);
        originalCurrentTemp = getCurrentTemp();
        if (getCurrentTemp() < mBuildingLimitStartAngle)
        {
            setCurrentTemp(mBuildingLimitStartAngle);
        }
        else if (getCurrentTemp() > mBuildingLimitEndAngle)
        {
            setCurrentTemp(mBuildingLimitEndAngle);
            if (roomName.equals("CCU") && showCCUDial)
            {
                setDesireTemp(mBuildingLimitEndAngle);
            }
        }
        if (isSensorPaired || ( /*((nIndex == 1) &&*/ (originalCurrentTemp < 1)))
        {
            setCurrentTemp(0.0);
        }
        prepareAngle();
        width = getWidth();
        height = getHeight();
        cx = width / 2;
        cy = height / 2;
        mCanvas = canvas;
        // Draw the arcs
        //The basic arc
        canvas.drawArc(mArcRect, getmPathStartAngle(), 300, false, mArcPaint);
        //Delimeters in the basic arc
        if (isDetailedView())
        {
            for (float i = getmPathStartAngle(); i <= getmPathStartAngle() + 300; i = i + mGapAngle)
            {
                canvas.drawArc(mArcRect, i, 0.5f, false, mDelimeterPaint);
            }
            for (float i = getmPathStartAngle() + (mGapAngle / 2);
                 i <= getmPathStartAngle() + 300 - (mGapAngle / 2); i = i + mGapAngle)
            {
                canvas.drawArc(mArcRect, i, 0.5f, false, mDelimeterPaintPointValue);
            }
        }
        int tempStartAngle = (int) (210 + mGapAngle *
                                          ((getCurrentTemp() < 1 ? mBuildingLimitStartAngle
                                                    : getCurrentTemp()) -
                                           mBuildingLimitStartAngle));
        if (tempStartAngle > 180)
        {
            tempStartAngle = tempStartAngle - 360;
        }
        float sweepAngle = 0.0f;
        if (mTouchAngle > 180)
        {
            sweepAngle = (float) mTouchAngle - tempStartAngle - 360;
        }
        else
        {
            sweepAngle = (float) mTouchAngle - tempStartAngle;
        }
        if (mTouchAngle == 0)
        {
            if (getDesireTemp() >= getUserLimitStartPoint() &&
                getDesireTemp() <= getUserLimitEndPoint())
            {
                canvas.drawArc(mArcRect, mAngleOffset + tempStartAngle, (float) ((getDesireTemp() -
                                                                                  (getCurrentTemp() <
                                                                                   1
                                                                                           ? mBuildingLimitStartAngle
                                                                                           : getCurrentTemp())) *
                                                                                 mGapAngle), false, mProgressPaint);
                updateProgress((int) ((getDesireTemp() - mMiddleAngle) * mGapAngle), false);
            }
            else
            {
                canvas.drawArc(mArcRect, mAngleOffset + tempStartAngle, (float) ((getDesireTemp() -
                                                                                  (getCurrentTemp() <
                                                                                   1
                                                                                           ? mBuildingLimitStartAngle
                                                                                           : getCurrentTemp())) *
                                                                                 mGapAngle), false, mUserLimitProgressPaint);
                updateProgress((int) ((getDesireTemp() - mMiddleAngle) * mGapAngle), false);
            }
        }
        else if (isTouched)
        {
            if (getDesireTemp() >= getUserLimitStartPoint() &&
                getDesireTemp() <= getUserLimitEndPoint())
            {
                invalidate();
                canvas.drawArc(mArcRect,
                        mAngleOffset + tempStartAngle, sweepAngle, false, mProgressPaint);
            }
            else
            {
                invalidate();
                canvas.drawArc(mArcRect,
                        mAngleOffset + tempStartAngle, sweepAngle, false, mUserLimitProgressPaint);
            }
        }
        else if (!isTouched)
        {
            if (getDesireTemp() >= getUserLimitStartPoint() &&
                getDesireTemp() <= getUserLimitEndPoint())
            {
                canvas.drawArc(mArcRect, mAngleOffset + tempStartAngle, (float) ((getDesireTemp() -
                                                                                  (getCurrentTemp() <
                                                                                   1
                                                                                           ? mBuildingLimitStartAngle
                                                                                           : getCurrentTemp())) *
                                                                                 mGapAngle), false, mProgressPaint);
                updateProgress((int) ((getDesireTemp() - mMiddleAngle) * mGapAngle), false);
            }
            else
            {
                if (getCurrentTemp() < getmBuildingLimitEndAngle())
                {
                    canvas.drawArc(mArcRect,
                            mAngleOffset + tempStartAngle, (float) ((getDesireTemp() -
                                                                     (getCurrentTemp() < 1
                                                                              ? mBuildingLimitStartAngle
                                                                              : getCurrentTemp())) *
                                                                    mGapAngle), false, mUserLimitProgressPaint);
                }
                else
                {
                    canvas.drawArc(mArcRect,
                            mAngleOffset + tempStartAngle, (float) ((getDesireTemp() -
                                                                     getmBuildingLimitEndAngle()) *
                                                                    mGapAngle), false, mUserLimitProgressPaint);
                }
                updateProgress((int) ((getDesireTemp() - mMiddleAngle) * mGapAngle), false);
                angleProgress = (float) originalDesireTemp;
            }
        }
        //canvas for user Arc limit
        if (isDetailedView())
        {
            if (isTouched)
            {
                canvas.drawArc(mArcLimit,
                        mAngleOffset + getLimitStartAngle(),
                        getLimitSweepAngle() + 1, false, mArcLimitPaint);
                canvas.drawArc(mArcLimitBound,
                        mAngleOffset + getLimitStartAngle(), 2, false, mProgressLimitPaint);
                canvas.drawArc(mArcLimitBound,
                        mAngleOffset + getLimitEndAngle(), 2, false, mProgressLimitPaint);
            }
        }
        if (mTouchAngle > 180 && mTouchAngle < 360)
        {
            angleProgress = (float) (((float) ((mTouchAngle - tempStartAngle) / mGapAngle) +
                                      (getCurrentTemp() < 1 ? mBuildingLimitStartAngle
                                               : getCurrentTemp())) - mLeftMarginAngle);
        }
        else
        {
            angleProgress = (float) ((float) ((mTouchAngle - tempStartAngle) / mGapAngle) +
                                     (getCurrentTemp() < 1 ? mBuildingLimitStartAngle
                                              : getCurrentTemp()));
        }
        //Format to represent the angle progress
        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.applyPattern("##.#");
        angleProgress = Float.parseFloat(decimalFormat.format(angleProgress));
        float d = angleProgress - (long) angleProgress;
        if (angleProgress < getUserLimitStartPoint() || angleProgress > getUserLimitEndPoint())
        {
            angleProgress = Math.round(angleProgress);
        }
        else
        {
            if (d == 0.5)
            {
                angleProgress = (float) (Math.round(angleProgress) - 0.5);
            }
            else if (d > 0.1 && d < 0.5)
            {
                angleProgress = (float) (Math.round(angleProgress) + 0.5);
            }
            else
            {
                angleProgress = Math.round(angleProgress);
            }
        }
        if (isFirstRun)
        {
            angleProgress = (float) getDesireTemp();
            originalDesireTemp = getDesireTemp();
            originalTranslateX = mTranslateX;
            originalTranslateY = mTranslateY;
            originalThumbXPos = mThumbXPos;
            originalThumbYPos = mThumbYPos;
        }
        if (!(isFirstRun || isTimerFinished))
        {
            if (getDesireTemp() >= getUserLimitStartPoint() &&
                getDesireTemp() <= getUserLimitEndPoint())
            {
                if (angleProgress >= getUserLimitStartPoint() &&
                    angleProgress <= getUserLimitEndPoint())
                {
                    originalDesireTemp = angleProgress;
                }
                Log.d("VAV-TEMP", " Set desired "+originalDesireTemp+" desired "+getDesireTemp()+" angle "+angleProgress);
                String curTemp = "Desired";
                mStatusTextPaint.getTextBounds(curTemp, 0, curTemp.length(), bounds);
                canvas.drawText(curTemp,
                        cx - (bounds.width() / 2),
                        cy + mStatusTextHeight - (bounds.height() / 2), mStatusTextPaint);
                curTemp = Double.toString(originalDesireTemp);
                mProgressTextPaint.getTextBounds(curTemp, 0, curTemp.length(), bounds);
                canvas.drawText(curTemp,
                        cx - (bounds.width() / 2),
                        cy + mTempTextHeight - (bounds.height() / 2), mProgressTextPaint);
            }
            else if (!isTouched)
            {
                mDesireTemp = originalDesireTemp;
            }
            else
            {
                mDesireTemp = angleProgress;
                if (angleProgress >= getmBuildingLimitStartAngle() &&
                    angleProgress <= getmBuildingLimitEndAngle())
                {
                    String curTemp = "Outside User Limit";
                    mUserLimitTextPaint.getTextBounds(curTemp, 0, curTemp.length(), bounds);
                    canvas.drawText(curTemp,
                            cx - (bounds.width() / 2), cy + mStatusOutsideTextHeight -
                                                       (bounds.height() / 2), mUserLimitTextPaint);
                    curTemp = Double.toString(angleProgress);
                    mUserLimitOutsideProgressPaint
                            .getTextBounds(curTemp, 0, curTemp.length(), bounds);
                    canvas.drawText(curTemp,
                            cx - (bounds.width() / 2), cy + mTempTextHeight - (bounds.height() /
                                                                               2), mUserLimitOutsideProgressPaint);
                }
            }
        }
        if (isTouched)
        {
            Log.d("VAV-TEMP","isTouched");
            if (getDesireTemp() >= getUserLimitStartPoint() &&
                getDesireTemp() <= getUserLimitEndPoint())
            {
                String curTemp = "Desired";
                mStatusTextPaint.getTextBounds(curTemp, 0, curTemp.length(), bounds);
                canvas.drawText(curTemp,
                        cx - (bounds.width() / 2),
                        cy + mStatusTextHeight - (bounds.height() / 2), mStatusTextPaint);
                curTemp = Double.toString(angleProgress);
                mProgressTextPaint.getTextBounds(curTemp, 0, curTemp.length(), bounds);
                canvas.drawText(curTemp,
                        cx - (bounds.width() / 2),
                        cy + mTempTextHeight - (bounds.height() / 2), mProgressTextPaint);
            }
            else if (angleProgress >= getmBuildingLimitStartAngle() &&
                     angleProgress <= getmBuildingLimitEndAngle())
            {
                String curTemp = Double.toString(angleProgress);
                mUserLimitOutsideProgressPaint.getTextBounds(curTemp, 0, curTemp.length(), bounds);
                canvas.drawText(
                        "" + angleProgress,
                        cx - (bounds.width() / 2), cy + mTempTextHeight - (bounds.height() /
                                                                           2), mUserLimitOutsideProgressPaint);
                curTemp = "Outside User Limit";
                mUserLimitTextPaint.getTextBounds(curTemp, 0, curTemp.length(), bounds);
                canvas.drawText(curTemp,
                        cx - (bounds.width() / 2),
                        cy + mStatusOutsideTextHeight - (bounds.height() / 2), mUserLimitTextPaint);
            }
            mDesireTemp = angleProgress;
        }
        if (isFirstRun)
        {
            Log.i(TAG, "Is first run");
            String firstTemp;
            if (!isSensorPaired)
            {
                firstTemp = "Current";
                mStatusTextPaint.getTextBounds(firstTemp, 0, firstTemp.length(), bounds);
                Log.i(TAG, "Text Bounds: " + bounds.toString());
                canvas.drawText(firstTemp,
                        cx - (bounds.width() / 2),
                        cy + mStatusTextHeight - (bounds.height() / 2), mStatusTextPaint);
            }
            else
            {
                firstTemp = "No Sensor Paired";
                mStatusTextPaint.setTextSize(14);
                mStatusTextPaint.setColor(Color.RED);
                mStatusTextPaint.getTextBounds(firstTemp, 0, firstTemp.length(), bounds);
                //canvas.drawText("No Sensor Paired", cx - (bounds.width()/2) , cy + mStatusTextHeight - (bounds.height()/2), mStatusTextPaint);
                canvas.drawText(firstTemp,
                        cx - (bounds.width() / 2),
                        cy + mStatusTextHeight - (bounds.height()), mStatusTextPaint);
            }
            if (isCurrBeyondLimit)
            {
                //NOTE cmData = zoneProfile, roomData = mZone ?
//                if (showCCUDial)
//                {
//                    firstTemp = Double.toString(mSSEProfile.getCMCurrentTemp(true)); //MARK
//                }
                if (zoneProfile != null)
                {
                    firstTemp = Double.toString(zoneProfile.getDisplayCurrentTemp()); //MARK
                }
                else
                {
                    firstTemp = Double.toString(originalCurrentTemp);
                }
                mProgressTextPaint.getTextBounds(firstTemp, 0, firstTemp.length(), bounds);
                canvas.drawText(firstTemp,
                        cx - (bounds.width() / 2),
                        cy + mTempTextHeight - (bounds.height() / 2), mProgressTextPaint);
            }
            else
            {
                firstTemp = Double.toString(getDesireTemp());
                mProgressTextPaint.getTextBounds(firstTemp, 0, firstTemp.length(), bounds);
                canvas.drawText(firstTemp,
                        cx - (bounds.width() / 2),
                        cy + mTempTextHeight - (bounds.height() / 2), mProgressTextPaint);
            }
        }
        if (isTimerFinished)
        {
            String curTemp;
            if (!isSensorPaired)
            {
                curTemp = "Current";
                mStatusTextPaint.getTextBounds(curTemp, 0, curTemp.length(), bounds);
                float x = cx - (bounds.width() / 2);
                float y = cy + mStatusTextHeight - (bounds.height() / 2);
                canvas.drawText(curTemp, x, y, mStatusTextPaint);
            }
            else
            {
                curTemp = "No Sensor Paired";
                mStatusTextPaint.setTextSize(14);
                mStatusTextPaint.setColor(Color.RED);
                mStatusTextPaint.getTextBounds(curTemp, 0, curTemp.length(), bounds);
                canvas.drawText(curTemp,
                        cx - (bounds.width() / 2),
                        cy + mStatusTextHeight - (bounds.height()), mStatusTextPaint);
            }
            /*if (isCurrBeyondLimit)
            {
//                if (showCCUDial)
//                {
//                    curTemp = Double.toString(mSSEProfile.getCMCurrentTemp(true));
//                }
                if (zoneProfile != null)
                {
                    curTemp = Double.toString(zoneProfile.getDisplayCurrentTemp());
                }
                else
                {
                    curTemp = Double.toString(originalCurrentTemp);
                }
                mProgressTextPaint.getTextBounds(curTemp, 0, curTemp.length(), bounds);
                canvas.drawText(curTemp,
                        cx - (bounds.width() / 2),
                        cy + mTempTextHeight - (bounds.height() / 2), mProgressTextPaint);
            }
            else
            {
                curTemp = Double.toString(getDesireTemp());
                mProgressTextPaint.getTextBounds(curTemp, 0, curTemp.length(), bounds);
                canvas.drawText(curTemp,
                        cx - (bounds.width() / 2),
                        cy + mTempTextHeight - (bounds.height() / 2), mProgressTextPaint);
            }*/
    
            if (zoneProfile != null)
            {
                curTemp = Double.toString(zoneProfile.getDisplayCurrentTemp());
                if (mDesireTemp != 0 && mDesireTemp != L.getDesiredTemp(zoneProfile))
                {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground( final Void ... params ) {
                            L.setDesiredTemp(mDesireTemp, zoneProfile); //TODO - Optimize
                            return null;
                        }
        
                        @Override
                        protected void onPostExecute( final Void result ) {
                            // continue what you are doing...
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    
                    Log.d("CCU_TEMP", " mDesireTemp " + mDesireTemp);
                }
            }
            else
            {
                curTemp = Double.toString(originalCurrentTemp);
            }
            mProgressTextPaint.getTextBounds(curTemp, 0, curTemp.length(), bounds);
            canvas.drawText(curTemp,
                    cx - (bounds.width() / 2),
                    cy + mTempTextHeight - (bounds.height() / 2), mProgressTextPaint);
        }
        //outer arc numbers from 50 - 90
        if (isTouched)
        {
            canvas.drawText(
                    "" + Math.round(getmBuildingLimitStartAngle()),
                    cx - mMarkerTextWidthX, cy + mMarkerTextHeight, mStatusTextPaint);
            canvas.drawText(
                    "" + Math.round(getmBuildingLimitEndAngle()),
                    cx + mMarkerTextWidthY, cy + mMarkerTextHeight, mStatusTextPaint);
        }
        //Thumb circle
        if (isDetailedView())
        {
            if (isTouched)
            {
                canvas.drawCircle(
                        mTranslateX - mThumbXPos,
                        mTranslateY - mThumbYPos, mSmallThumbRadius, mSmallThumbPaint);
                if (getDesireTemp() >= getUserLimitStartPoint() &&
                    getDesireTemp() <= getUserLimitEndPoint())
                {
                    canvas.drawCircle(
                            mTranslateX - mThumbXPos2,
                            mTranslateY - mThumbYPos2, mThumbOuterRadius, mThumbOuterCirclePaint);
                    canvas.drawCircle(
                            mTranslateX - mThumbXPos2,
                            mTranslateY - mThumbYPos2, mThumbInnerRadius, mThumbInnerCirclePaint);
                    canvas.drawText(
                            "" + getDesireTemp(),
                            mTranslateX - mThumbXPos2 - mThumbTextWidth,
                            mTranslateY - mThumbYPos2 + mThumbTextHeight, mThumbeCircleTextPaint);
                }
                else
                {
                    if (getDesireTemp() > getmBuildingLimitStartAngle() &&
                        getDesireTemp() < getmBuildingLimitEndAngle())
                    {
                        canvas.drawCircle(
                                mTranslateX - mThumbXPos2, mTranslateY -
                                                           mThumbYPos2, mThumbOuterRadius, mThumbOuterLimitCirclePaint);
                        canvas.drawCircle(
                                mTranslateX - mThumbXPos2, mTranslateY -
                                                           mThumbYPos2, mThumbInnerRadius, mThumbInnerCirclePaint);
                        canvas.drawText(
                                "" + getDesireTemp(),
                                mTranslateX - mThumbXPos2 - mThumbTextWidth,
                                mTranslateY - mThumbYPos2 +
                                mThumbTextHeight, mThumbeOuterLimitCircleTextPaint);
                    }
                }
            }
            else if (!isTouched)
            {
                if (getDesireTemp() >= getUserLimitStartPoint() &&
                    getDesireTemp() <= getUserLimitEndPoint())
                {
                    canvas.drawCircle(
                            mTranslateX - mThumbXPos,
                            mTranslateY - mThumbYPos, mThumbOuterRadius, mThumbOuterCirclePaint);
                    canvas.drawCircle(
                            mTranslateX - mThumbXPos,
                            mTranslateY - mThumbYPos, mThumbInnerRadius, mThumbInnerCirclePaint);
                    canvas.drawText(
                            "" + getDesireTemp(),
                            mTranslateX - mThumbXPos - mThumbTextWidth,
                            mTranslateY - mThumbYPos + mThumbTextHeight, mThumbeCircleTextPaint);
                }
                else
                {
                    canvas.drawCircle(
                            originalTranslateX - originalThumbXPos, originalTranslateY -
                                                                    originalThumbYPos, mThumbOuterRadius, mThumbOuterCirclePaint);
                    canvas.drawCircle(
                            originalTranslateX - originalThumbXPos, originalTranslateY -
                                                                    originalThumbYPos, mThumbInnerRadius, mThumbInnerCirclePaint);
                    angleProgress = (float) originalDesireTemp;
                    if (isFirstRun)
                    {
                        canvas.drawText(
                                "" + getDesireTemp(),
                                originalTranslateX - originalThumbXPos - mThumbTextWidth,
                                originalTranslateY - originalThumbYPos +
                                mThumbTextHeight, mThumbeCircleTextPaint);
                    }
                    else
                    {
                        canvas.drawText(
                                "" + originalDesireTemp,
                                originalTranslateX - originalThumbXPos - mThumbTextWidth,
                                originalTranslateY - originalThumbYPos +
                                mThumbTextHeight, mThumbeCircleTextPaint);
                    }
                }
            }
        }
        else
        {
            if (!isSensorPaired)
            {
                canvas.drawCircle(
                        mTranslateX - mThumbXPos,
                        mTranslateY - mThumbYPos, mThumbOuterRadius, mThumbOuterCirclePaint);
                canvas.drawCircle(
                        mTranslateX - mThumbXPos,
                        mTranslateY - mThumbYPos, mThumbInnerRadius, mThumbInnerCirclePaint);
                canvas.drawText(
                        "" + getDesireTemp(),
                        mTranslateX - mThumbXPos - mThumbTextWidth,
                        mTranslateY - mThumbYPos + mThumbTextHeight, mThumbeCircleTextPaint);
            }
        }
        if (roomName != null)
        {
            mRoomTextPaint.getTextBounds(roomName, 0, roomName.length(), bounds);
            if (roomName.length() < 20)
            {
                canvas.drawText(roomName,
                        cx - (bounds.width() / 2),
                        cy + mStatusTextHeight - (bounds.height() / 2) + 75, mRoomTextPaint);
            }
            else
            {
                Rect b = canvas.getClipBounds();
                CharSequence txt = TextUtils.ellipsize(roomName, mRoomTextPaint,
                        b.width() - 30, TextUtils.TruncateAt.END);
                canvas.drawText(txt, 0, txt.length(), 30,
                        cy + mStatusTextHeight - (bounds.height() / 2) + 75, mRoomTextPaint);
            }
        }
        isFirstRun = false;
    }


    @Override
    protected void drawableStateChanged()
    {
        super.drawableStateChanged();
        if (mThumb != null && mThumb.isStateful())
        {
            int[] state = getDrawableState();
            mThumb.setState(state);
        }
        invalidate();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        final int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        final int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int min = Math.min(width, height);
        float top = 0;
        float left = 0;
        int arcDiameter = 0;
        int arcStart = 0;
        mTranslateX = (int) (width * 0.5f);
        mTranslateY = (int) (height * 0.5f);
        arcDiameter = min - getPaddingLeft();
        mArcRadius = arcDiameter / 2;
        top = height / 2 - (arcDiameter / 2);
        left = width / 2 - (arcDiameter / 2);
        mArcRect.set(left, top, left + arcDiameter, top + arcDiameter);
        mArcLimit.set(left + 25, top + 25, left + arcDiameter - 25, top + arcDiameter - 25);
        mArcLimitBound.set(left + 20, top + 20, left + arcDiameter - 20, top + arcDiameter - 20);
        mArcRectText.set(left - 80, top - 80, left + arcDiameter + 80, top + arcDiameter + 80);
        arcStart = (int) mProgressSweep + 90;
        mThumbXPos = (int) (mArcRadius * Math.cos(Math.toRadians(arcStart)));
        mThumbYPos = (int) (mArcRadius * Math.sin(Math.toRadians(arcStart)));
        mThumbXPos2 = (int) ((mArcRadius + mThumbDifference) * Math.cos(Math.toRadians(arcStart)));
        mThumbYPos2 = (int) ((mArcRadius + mThumbDifference) * Math.sin(Math.toRadians(arcStart)));
        setTouchInSide(mTouchInside);
        setTouchOutSide(mTouchOutSide);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


    public void setTouchInSide(boolean isEnabled)
    {
        int thumbHalfheight = (int) mThumb.getIntrinsicHeight() / 2;
        int thumbHalfWidth = (int) mThumb.getIntrinsicWidth() / 2;
        mTouchInside = isEnabled;
        if (mTouchInside)
        {
            mTouchIgnoreRadius = (float) mArcRadius / 4;
        }
        else
        {
            // Don't use the exact radius makes interaction too tricky
            mTouchIgnoreRadius = mArcRadius - Math.min(thumbHalfWidth, thumbHalfheight);
        }
    }


    public void setTouchOutSide(boolean isEnabled)
    {
        int thumbHalfheight = (int) mThumb.getIntrinsicHeight() / 2;
        int thumbHalfWidth = (int) mThumb.getIntrinsicWidth() / 2;
        mTouchOutSide = false;
        /*if (mTouchOutSide) {
            mTouchIgnoreRadiusOutSide = (float) mArcRadius / 4;


        } else {*/
        // Don't use the exact radius makes interaction too tricky
        mTouchIgnoreRadiusOutSide = mArcRadius - Math.min(thumbHalfWidth, thumbHalfheight);
        //}
    }


    public double getCurrentTemp()
    {
        return mCurrentTemp;
    }


    public void prepareAngle()
    {
        mGapAngle = (float) (300 / (mBuildingLimitEndAngle - mBuildingLimitStartAngle));
        mMiddleAngle = mBuildingLimitStartAngle +
                       ((mBuildingLimitEndAngle - mBuildingLimitStartAngle) / 2);
        mLeftMarginAngle = (mBuildingLimitEndAngle - mBuildingLimitStartAngle) * 1.2;
    }


    public float getmPathStartAngle()
    {
        return mPathStartAngle;
    }


    public void setmPathStartAngle(float mPathStartAngle)
    {
        this.mPathStartAngle = mPathStartAngle;
    }


    public boolean isDetailedView()
    {
        return mDetailedView;
    }


    public double getDesireTemp()
    {
        return mDesireTemp;
    }


    public float getUserLimitEndPoint()
    {
        return userLimitEndPoint;
    }


    public float getUserLimitStartPoint()
    {
        return userLimitStartPoint;
    }


    public double getmBuildingLimitEndAngle()
    {
        return Math.round(mBuildingLimitEndAngle);
    }

	/*private float valuePerDegree() {

        return (float) mMax / mSweepAngle;
	}*/


    public float getLimitSweepAngle()
    {
        if (this.mLimitStartAngle < 180 && this.mLimitEndAngle < 180)
        {
            return this.mLimitEndAngle - this.mLimitStartAngle;
        }
        else if ((this.mLimitStartAngle > 180 && this.mLimitStartAngle < 360) &&
                 (this.mLimitEndAngle > 180 && this.mLimitEndAngle < 360))
        {
            return this.mLimitEndAngle - this.mLimitStartAngle;
        }
        if (this.mLimitStartAngle == 360 || this.mLimitStartAngle == 0)
        {
            return Math.abs(0 + this.mLimitEndAngle);
        }
        else
        {
            float startLimitAngle = 360 - this.mLimitStartAngle;
            float ret = Math.abs(startLimitAngle + this.mLimitEndAngle);
            if (ret <= 360)
            {
                return ret;
            }
            else
            {
                return ret - 360;
            }
        }
    }


    public double getmBuildingLimitStartAngle()
    {
        return Math.round(mBuildingLimitStartAngle);
    }


    public void setmBuildingLimitStartAngle(double mBuildingLimitStartAngle)
    {
        this.mBuildingLimitStartAngle = mBuildingLimitStartAngle;
    }


    public void setmBuildingLimitEndAngle(double mBuildingLimitEndAngle)
    {
        this.mBuildingLimitEndAngle = mBuildingLimitEndAngle;
    }


    public void setUserLimitStartPoint(float userLimitStartPoint)
    {
        this.userLimitStartPoint = userLimitStartPoint;
    }


    public void setUserLimitEndPoint(float userLimitEndPoint)
    {
        this.userLimitEndPoint = userLimitEndPoint;
    }


    public void setDesireTemp(double DesireTemp)
    {
        this.mDesireTemp = DesireTemp;
    }


    public void setDetailedView(boolean isDetailedView)
    {
        this.mDetailedView = isDetailedView;
        if (isDetailedView)
        {
            setArcWidth(13);
        }
        else
        {
            setArcWidth(5);
        }
    }


    public void setCurrentTemp(double CurrentTemp)
    {
        this.mCurrentTemp = CurrentTemp;
    }


    private boolean isThumbPressed(float xpos, float ypos)
    {
        double curTouch = getTouchDegrees(xpos, ypos);
        int progress = getProgressForAngle(curTouch);
        int arcStart = (int) progress + 90;
        int curThumbXPos = (int) (mArcRadius * Math.cos(Math.toRadians(arcStart)));
        int curThumbYpos = (int) (mArcRadius * Math.sin(Math.toRadians(arcStart)));
        //double distance = (Math.sqrt(Math.pow((mThumbXPos2 - mThumbXPos), 2) + Math.pow((mThumbYPos2 - mThumbYPos), 2))) * 2; //always 52 approximately
        return ((32 > Math.abs((curThumbXPos - mThumbXPos) + (curThumbYpos - mThumbYPos))) &&
                (32 > Math.abs((curThumbXPos - mThumbXPos) - (curThumbYpos - mThumbYPos))));
    }


    private void onStartTrackingTouch()
    {
        if (mOnSeekArcChangeListener != null)
        {
            mOnSeekArcChangeListener.onStartTrackingTouch(this);
        }
    }


    private void updateOnTouch(MotionEvent event)
    {
        if (!isMoveStarted && !isTouched)
        {
            return;
        }
        mTouchAngle = getTouchDegrees(event.getX(), event.getY());
        if (getLimitStartAngle() < 180 && getLimitEndAngle() < 180)
        {
            //both in 0 ~ 150
            if ((mTouchAngle > (getLimitStartAngle() + (mUserLimitDiff / 2))) &&
                (mTouchAngle < (getLimitEndAngle() + mUserLimitDiff / 2)))
            {
                int progs = (int) (mTouchAngle - getLimitStartAngle()) / mUserLimitDiff;
                mTouchAngle = getLimitStartAngle() + (progs * mUserLimitDiff);
            }
            else
            {
                if (mTouchAngle > 180)
                {
                    if (mTouchAngle >= 210 && mTouchAngle <= 360)
                    {
                        int leftprogs =
                                (int) (mTouchAngle - getLimitEndAngle()) / mOutsideLimitDiffLeft;
                        mTouchAngle = getLimitEndAngle() + (leftprogs * mOutsideLimitDiffLeft);
                    }
                }
                else
                {
                    if (mTouchAngle < getLimitStartAngle() && mTouchAngle > 0)
                    {
                        int leftprogs = (int) (360 + mTouchAngle - getLimitStartAngle()) /
                                        mOutsideLimitDiffLeft;
                        mTouchAngle =
                                getLimitStartAngle() + (leftprogs * mOutsideLimitDiffLeft) - 360;
                    }
                    else
                    {
                        int rightprogs =
                                (int) (mTouchAngle - getLimitEndAngle()) / mOutsideLimitDiffRight;
                        mTouchAngle = getLimitEndAngle() + (rightprogs * mOutsideLimitDiffRight);
                    }
                }
            }
        }
        else if ((getLimitStartAngle() > 180) && (getLimitStartAngle() < 360) &&
                 (getLimitEndAngle() > 180) && (getLimitEndAngle() < 360))
        {
            //between 210 ~ 360
            if ((mTouchAngle > (getLimitStartAngle() + (mUserLimitDiff / 2))) &&
                (mTouchAngle < (getLimitEndAngle() + (mUserLimitDiff / 2))))
            {
                int progs = (int) (mTouchAngle - getLimitStartAngle()) / mUserLimitDiff;
                mTouchAngle = getLimitStartAngle() + (progs * mUserLimitDiff);
            }
            else
            {
                if (mTouchAngle > 180)
                {
                    if (mTouchAngle > 210 &&
                        mTouchAngle < (getLimitStartAngle() + (mUserLimitDiff / 2)))
                    {
                        int leftprogs = (int) (mTouchAngle - 210) / mOutsideLimitDiffLeft;
                        mTouchAngle = 210 + (leftprogs * mOutsideLimitDiffLeft);
                    }
                    else if (mTouchAngle > getLimitEndAngle() && mTouchAngle < 360)
                    {
                        int rightprogs =
                                (int) (mTouchAngle - getLimitEndAngle()) / mOutsideLimitDiffRight;
                        mTouchAngle = getLimitEndAngle() + (rightprogs * mOutsideLimitDiffRight);
                    }
                }
                else
                {
                    int rightprogs =
                            (int) (360 + mTouchAngle - getLimitEndAngle()) / mOutsideLimitDiffRight;
                    mTouchAngle = getLimitEndAngle() + (rightprogs * mOutsideLimitDiffRight) - 360;
                }
            }
        }
        else /*if(getLimitStartAngle() > 180 && getLimitEndAngle()< 180)*/
        {
            //Start angle and end angle in two different
            if ((mTouchAngle > (getLimitStartAngle() + (mUserLimitDiff / 2)) && mTouchAngle <= 360))
            {
                int progs = (int) (mTouchAngle - getLimitStartAngle()) / mUserLimitDiff;
                mTouchAngle = getLimitStartAngle() + (progs * mUserLimitDiff);
            }
            else if (mTouchAngle > 0 && (mTouchAngle < (getLimitEndAngle() + (mUserLimitDiff / 2))))
            {
                int progs = (int) mTouchAngle / mUserLimitDiff;
                mTouchAngle = (progs * mUserLimitDiff);
            }
            else if (mTouchAngle > 210 &&
                     mTouchAngle < (getLimitStartAngle() + (mUserLimitDiff / 2)))
            {
                int leftprogs = (int) (mTouchAngle - 210) / mOutsideLimitDiffLeft;
                mTouchAngle = 210 + (leftprogs * mOutsideLimitDiffLeft);
            }
            else
            {
                int rightprogs = (int) (mTouchAngle - getLimitEndAngle()) / mOutsideLimitDiffRight;
                mTouchAngle = getLimitEndAngle() + (rightprogs * mOutsideLimitDiffRight);
            }
        }
        if (mTouchAngle > RIGHT_BOUND && mTouchAngle < 180)
        {
            mTouchAngle = RIGHT_BOUND;
        }
        if (mTouchAngle > 180 && mTouchAngle < LEFT_BOUND)
        {
            mTouchAngle = LEFT_BOUND;
        }
        int progress = getProgressForAngle(mTouchAngle);
        setPressed(true);
        onProgressRefresh(progress, true);
    }


    private void onStopTrackingTouch()
    {
        if (mOnSeekArcChangeListener != null)
        {
            mOnSeekArcChangeListener.onStopTrackingTouch(this);
        }
    }


    private boolean ignoreTouch(float xPos, float yPos)
    {
        boolean ignore = false;
        float x = xPos - mTranslateX;
        float y = yPos - mTranslateY;
        float touchRadius = (float) Math.sqrt(((x * x) + (y * y)));
        if (touchRadius > mTouchIgnoreRadiusOutSide)
        {
            ignore = true;
            //isTouched = false;
        }
        return ignore;
    }


    /* MARK
    public void setdablcmfsv(ArrayList<FSVData> lcmdabfsv) {
        this.lcmdabfsv = lcmdabfsv;
        if (lcmdabfsv != null && lcmdabfsv.size() != 0) {
            mDeviceType = DEVICE_TYPE.LCM_DAB;
        }
    }




    public ArrayList<FSVData> getlcmdabfsv() {
        return lcmdabfsv;
    }


    public void setdabifttfsv(ArrayList<FSVData> ifttdabfsv) {
        this.ifttdabfsv = ifttdabfsv;

        if (ifttdabfsv != null && ifttdabfsv.size() != 0) {
            mDeviceType = DEVICE_TYPE.IFTT_DAB;
        }
    }


    public ArrayList<FSVData> getifttdabfsv() {
        return ifttdabfsv;
    }
    */


    private double getTouchDegrees(float xPos, float yPos)
    {
        float x = xPos - mTranslateX;
        float y = yPos - mTranslateY;
        double angle = Math.toDegrees(Math.atan2(y, x) + (Math.PI / 2) - Math.toRadians(mRotation));
        if (angle < 0)
        {
            angle = 360 + angle;
        }
        return angle;
    }


    private int getProgressForAngle(double angle)
    {
        int touchProgress = (int) Math.round(1 * angle);
        touchProgress = (touchProgress < 0) ? INVALID_PROGRESS_VALUE : touchProgress;
        touchProgress = (touchProgress > mMax) ? INVALID_PROGRESS_VALUE : touchProgress;
        return touchProgress;
    }


    public float getLimitEndAngle()
    {
        return this.mLimitEndAngle;
    }


    public float getLimitStartAngle()
    {
        return this.mLimitStartAngle;
    }


    public void setLimitStartAngle(float LimitStartAngle)
    {
        setUserLimitStartPoint(LimitStartAngle);
        float tempLimitStartAngle =
                (float) (210 + mGapAngle * (LimitStartAngle - mBuildingLimitStartAngle));
        if (tempLimitStartAngle < 360)
        {
            this.mLimitStartAngle = tempLimitStartAngle;
        }
        else
        {
            this.mLimitStartAngle = tempLimitStartAngle - 360;
        }
        //LEFT_BOUND = mLimitStartAngle;
    }


    private void onProgressRefresh(int progress, boolean fromUser)
    {
        updateProgress(progress, fromUser);
    }


    private void updateProgress(int progress, boolean fromUser)
    {
        // Log.d(TAG,"Update Prigress----============>>> "+progress);
        if (progress == INVALID_PROGRESS_VALUE)
        {
            return;
        }
        if (mOnSeekArcChangeListener != null)
        {
            mOnSeekArcChangeListener.onProgressChanged(this, progress, fromUser);
        }
        progress = (progress > mMax) ? mMax : progress;
        if (progress > 180)
        {
            progress = progress - 360;
        }
        mProgress = progress;
        mProgressSweep = progress;
        updateThumbPosition();
        invalidate();
    }


    private void updateThumbPosition()
    {
        int thumbAngle = (int) (mStartAngle + mProgressSweep + mRotation + 90);
        //int thumbAngle = (int) (180 + dSweepAngle + mRotation + 90);
        mThumbXPos = (int) (mArcRadius * Math.cos(Math.toRadians(thumbAngle)));
        mThumbYPos = (int) (mArcRadius * Math.sin(Math.toRadians(thumbAngle)));
        mThumbXPos2 =
                (int) ((mArcRadius + mThumbDifference) * Math.cos(Math.toRadians(thumbAngle)));
        mThumbYPos2 =
                (int) ((mArcRadius + mThumbDifference) * Math.sin(Math.toRadians(thumbAngle)));
    }


    public void setLimitEndAngle(float LimitEndAngle)
    {
        setUserLimitEndPoint(LimitEndAngle);
        float tempLimitEndAngle =
                (float) (210 + mGapAngle * (LimitEndAngle - mBuildingLimitStartAngle));
        if (tempLimitEndAngle <= 360)
        {
            this.mLimitEndAngle = tempLimitEndAngle;
        }
        else
        {
            this.mLimitEndAngle = tempLimitEndAngle - 360;
        }
        //RIGHT_BOUND = mLimitEndAngle;
    }


    /**
     * Sets a listener to receive notifications of changes to the SeekArc's
     * progress level. Also provides notifications of when the user starts and
     * stops a touch gesture within the SeekArc.
     *
     * @param l The seek bar notification listener
     * @see //SeekArc.OnSeekBarChangeListener
     */
    public void setOnSeekArcChangeListener(OnSeekArcChangeListener l)
    {
        mOnSeekArcChangeListener = l;
    }


    public void setOnClickChangeListener(OnClickListener l)
    {
        mOnClickListener = l;
    }


    public void setProgress(int progress)
    {
        updateProgress(progress, false);
    }


    public int getProgressWidth()
    {
        return mProgressWidth;
    }


    public void setProgressWidth(int mProgressWidth)
    {
        this.mProgressWidth = mProgressWidth;
        mProgressPaint.setStrokeWidth(mProgressWidth);
    }


    public int getArcWidth()
    {
        return mArcWidth;
    }


    public void setArcWidth(int mArcWidth)
    {
        this.mArcWidth = mArcWidth;
        mArcPaint.setStrokeWidth(mArcWidth);
        this.mProgressWidth = mArcWidth;
        mProgressPaint.setStrokeWidth(mArcWidth);
        mUserLimitProgressPaint.setStrokeWidth(mArcWidth);
    }


    public void setCMDataToSeekArc(ZoneProfile data, String zoneName, int index)
    {
        this.zoneProfile = data;
        this.nIndex = index;
        this.roomName = data.getProfileType().name();//TODO
        showCCUDial = true;
        data.setZoneProfileInterface(this);
        setDetailedView(false);
        setLimitbounds();
    }


    public void setLimitbounds()
    {
        if ((getLimitEndAngle() > 180 && getLimitStartAngle() > 180) ||
            (getLimitEndAngle() < 180 && getLimitStartAngle() < 180))
        {
            float diff = (getLimitEndAngle() - getLimitStartAngle()) /
                         (((getUserLimitEndPoint() - getUserLimitStartPoint()) * 2) + 1);
            mUserLimitDiff = Math.round(1 * diff) + 1;
        }
        else
        {
            float diff = (360 - getLimitStartAngle() + getLimitEndAngle()) /
                         (((getUserLimitEndPoint() - getUserLimitStartPoint()) * 2) + 1);
            mUserLimitDiff = Math.round(1 * diff) + 1;
        }
        if (getLimitStartAngle() > 180)
        {
            double leftdiff = (getLimitStartAngle() - 210) /
                              ((getUserLimitStartPoint() - getmBuildingLimitStartAngle()) + 1);
            mOutsideLimitDiffLeft = (int) Math.round(1 * leftdiff) + 1;
        }
        else
        {
            double leftdiff = (360 + getLimitStartAngle() - 210) /
                              ((getUserLimitStartPoint() - getmBuildingLimitStartAngle()) + 1);
            mOutsideLimitDiffLeft = (int) Math.round(leftdiff) + 1;
        }
        if (getLimitEndAngle() > 180)
        {
            double rightdiff = ((360 - getLimitEndAngle()) + 150) /
                               ((getmBuildingLimitEndAngle() - getUserLimitEndPoint()) + 1);
            mOutsideLimitDiffRight = (int) Math.round(rightdiff) + 1;
        }
        else
        {
            double rightdiff = (150 - getLimitEndAngle()) /
                               ((getmBuildingLimitEndAngle() - getUserLimitEndPoint()) + 1);
            mOutsideLimitDiffRight = (int) Math.round(rightdiff) + 1;
        }
    }


//    public Zone getRoomData()
//    {
//        return mZone;
//    }
//
//
//    public void setRoomData(Zone data)
//    {
//        this.mZone = data;
//        data.setRoomDataInterface(this);
//        roomName = mZone.roomName;
//        setDetailedView(false);
//        setLimitbounds();
//    }
//

    public ZoneProfile getZoneProfile()
    {
        return zoneProfile;
    }


    public int getIndex()
    {
        return nIndex;
    }


    public void SetSelected(boolean bSelected)
    {
        if (bSelected)
        {
            nColor = nUnSelColor;
        }
        else
        {
            nColor = nSelectedColor;
        }
        invalidate();
    }


    public boolean getIsSensorPaired()
    {
        return this.isSensorPaired;
    }


    public void setIsSensorPaired(boolean isPaired)
    {
        isSensorPaired = isPaired;
    }


    public boolean getIsCCU()
    {
        return this.showCCUDial;
    }


    public void setZone(Zone zone)
    {
        this.mZone = zone;
    }

    public Zone getZone()
    {
        return mZone;
    }


    public static enum DEVICE_TYPE
    {
        PURE_DAB, LCM_DAB, IFTT_DAB
    }

    public interface OnClickListener
    {
        void onClick(SeekArc seekArc);
    }

    public interface OnSeekArcChangeListener
    {

        /**
         * Notification that the progress level has changed. Clients can use the
         * fromUser parameter to distinguish user-initiated changes from those
         * that occurred programmatically.
         *
         * @param seekArc The SeekArc whose progress has changed
         * @param progress      The current progress level. This will be in the range
         *                      0..max where max was set by
         *                      //   {@link //ProgressArc#setMax(int)}. (The default value for
         *                      max is 100.)
         * @param fromUser      True if the progress change was initiated by the user.
         */
        void onProgressChanged(SeekArc seekArc, int progress, boolean fromUser);

        /**
         * Notification that the user has started a touch gesture. Clients may
         * want to use this to disable advancing the seekbar.
         *
         * @param seekArc The SeekArc in which the touch gesture began
         */
        void onStartTrackingTouch(SeekArc seekArc);

        /**
         * Notification that the user has finished a touch gesture. Clients may
         * want to use this to re-enable advancing the seekarc.
         *
         * @param seekArc The SeekArc in which the touch gesture began
         */
        void onStopTrackingTouch(SeekArc seekArc);
    }
}
