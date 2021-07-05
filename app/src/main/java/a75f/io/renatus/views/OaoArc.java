package a75f.io.renatus.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import a75f.io.renatus.R;
import a75f.io.renatus.util.BitmapUtil;
import a75f.io.renatus.util.CCUUiUtil;


/**
 * Created by mahesh on 29-08-2019.
 */
public class OaoArc extends View {

    private static final String TAG = SeekArc.class.getSimpleName();
    private static int INVALID_PROGRESS_VALUE = -1;
    // The initial rotational offset -90 means we start at 12 o'clock
    private final int mAngleOffset = -90;

    /**
     * The Maximum value that this SeekArc can be set to
     */
    private int mMax = 100;

    /**
     * The Current value that the SeekArc is set to
     */
    private int mProgress = 0;

    /**
     * The width of the progress line for this SeekArc
     */
    private int mProgressWidth = 4;

    /**
     * The Width of the background arc for the SeekArc
     */
    private int mArcWidth = 4;

    /**
     * The Angle to start drawing this Arc from
     */
    private int mStartAngle = 30;

    /**
     * The Angle through which to draw the arc (Max is 360)
     */
    private int mSweepAngle = 240;

    /**
     * The rotation of the SeekArc- 0 is twelve o'clock
     */
    private int mRotation = 0;

    // Internal variables
    private int mArcRadius = 0;
    private float mProgressSweep = 0;
    private RectF mArcRect = new RectF();
    private Rect bounds = new Rect();
    private Paint mArcPaint;
    private Paint mProgressPaint;
    private Paint mCurrentAirCo2TextPaint;
    private Paint mOAOTitleTextPaint;
    private Paint mUnitTextPaint;
    private Paint mTextPaint;
    private float mTranslateX;
    private float mTranslateY;
    private Bitmap mThumb;
    private float mScale = 0.0f;
    private Paint mThumbPaint;
    private float mThumbXPos;
    private float mThumbYPos;
    private int airCO2 = 0, angle = 0;

    public OaoArc(Context context) {
        super(context);
        init(context, null);
    }

    public OaoArc(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public OaoArc(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        Typeface latoLightFont = ResourcesCompat.getFont(getContext(), R.font.lato_light);
        final Resources res = getResources();
        float density = context.getResources().getDisplayMetrics().density;

        // Defaults, may need to link this into theme settings
        int arcColor = res.getColor(R.color.progress_gray);
        //int progressColor = res.getColor(R.color.daikin_75f);
        int progressColor =CCUUiUtil.getPrimaryThemeColor(getContext());

        mThumb = BitmapUtil.getBitmapFromVectorDrawable(getContext(), R.drawable.ic_divider_black);
        // Convert progress width to pixels for current density
        mProgressWidth = (int) (mProgressWidth * density);

        mProgress = (mProgress > mMax) ? mMax : mProgress;
        mProgress = (mProgress < 0) ? 0 : mProgress;

        mSweepAngle = (mSweepAngle > 360) ? 360 : mSweepAngle;
        mSweepAngle = (mSweepAngle < 0) ? 0 : mSweepAngle;

        mProgressSweep = (float) mProgress / mMax * mSweepAngle;

        mStartAngle = (mStartAngle > 360) ? 0 : mStartAngle;
        mStartAngle = (mStartAngle < 0) ? 0 : mStartAngle;

        mArcPaint = new Paint();
        mArcPaint.setColor(arcColor);
        mArcPaint.setAntiAlias(true);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(8);
        mArcPaint.setStrokeCap(Paint.Cap.ROUND);
        //mArcPaint.setAlpha(45);

        mProgressPaint = new Paint();
        mProgressPaint.setColor(progressColor);
        mProgressPaint.setAntiAlias(true);
        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setStrokeWidth(8);
        mProgressPaint.setStrokeCap(Paint.Cap.ROUND);

        mCurrentAirCo2TextPaint = new Paint();
        mCurrentAirCo2TextPaint.setTypeface(latoLightFont);
        mCurrentAirCo2TextPaint.setStyle(Paint.Style.FILL);
        mCurrentAirCo2TextPaint.setColor(Color.parseColor("#99000000"));
        mCurrentAirCo2TextPaint.setAntiAlias(true);
        mCurrentAirCo2TextPaint.setTextSize(42);

        mUnitTextPaint = new Paint();
        mUnitTextPaint.setTypeface(latoLightFont);
        mUnitTextPaint.setStyle(Paint.Style.FILL);
        mUnitTextPaint.setFakeBoldText(true);
        mUnitTextPaint.setColor(CCUUiUtil.getPrimaryThemeColor(getContext()));
        mUnitTextPaint.setAntiAlias(true);
        mUnitTextPaint.setTextSize(12);


        mTextPaint = new Paint();
        mTextPaint.setTypeface(latoLightFont);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setFakeBoldText(true);
        mTextPaint.setColor(Color.parseColor("#6D7780"));
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(12);

        mThumbPaint = new Paint();
        mThumbPaint.setAntiAlias(true);
        mThumbPaint.setColor(Color.BLACK);
        mThumbPaint.setStyle(Paint.Style.FILL);

        mOAOTitleTextPaint = new Paint();
        mOAOTitleTextPaint.setTypeface(latoLightFont);
        mOAOTitleTextPaint.setStyle(Paint.Style.FILL);
        mOAOTitleTextPaint.setColor(Color.parseColor("#99000000"));
        mOAOTitleTextPaint.setFakeBoldText(true);
        mOAOTitleTextPaint.setAntiAlias(true);
        mOAOTitleTextPaint.setTextSize(22);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw the arcs
        float arcSweep = mSweepAngle;
        float arcStart = mStartAngle + 120;

        canvas.drawArc(mArcRect, arcStart, arcSweep, false, mArcPaint);
        if (mProgressSweep > 0) {
            canvas.drawArc(mArcRect, arcStart, mProgressSweep, false,
                    mProgressPaint);
        }

        drawAirCo2Text(canvas);
        drawOaoTitle(canvas);

        drawInitText(canvas);

        drawIconByAngle(canvas, mThumb, angle, mArcRadius, mThumbPaint);
    }

    public void setData(int angle, int airCO2) {
        this.angle = angle;
        this.airCO2 = airCO2;
        invalidate();
    }

    private void drawOaoTitle(Canvas canvas) {
        String oaoTitle = "OAO";
        mOAOTitleTextPaint.getTextBounds(oaoTitle, 0, oaoTitle.length(), bounds);

        float yPos = getHeight() /1.1f;  // baseline
        canvas.drawText(oaoTitle, 75, yPos, mOAOTitleTextPaint);
    }

    private void drawAirCo2Text(Canvas canvas) {
        String curAirCo2 = String.valueOf(airCO2);

        mCurrentAirCo2TextPaint.getTextBounds(curAirCo2, 0, curAirCo2.length(), bounds);
        float xPositionOfCurrentTempText = mTranslateX - (bounds.width() / 2f);   //origin
        float yPositionOfCurrentTempText = mTranslateY + (bounds.height() / 2f);  // baseline
        canvas.drawText(curAirCo2, xPositionOfCurrentTempText, yPositionOfCurrentTempText, mCurrentAirCo2TextPaint);

        canvas.drawText("PPM", xPositionOfCurrentTempText + 25 * curAirCo2.length(), yPositionOfCurrentTempText - 20, mUnitTextPaint);
    }

    private void drawInitText(Canvas canvas) {

        float yPos = getHeight();  // baseline

        canvas.drawText("0 PPM", 0, yPos - 70, mTextPaint);
        canvas.drawText("2000 PPM", 145, yPos - 70, mTextPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final int height = getDefaultSize(getSuggestedMinimumHeight(),
                heightMeasureSpec);
        final int width = getDefaultSize(getSuggestedMinimumWidth(),
                widthMeasureSpec);
        final int min = Math.min(width, height);
        float top = 0;
        float left = 0;
        int arcDiameter = 0;

        mTranslateX = (width * 0.5f);
        mTranslateY = (height * 0.5f);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpWidth = width / displayMetrics.density;

        mScale = dpWidth / 180;

        arcDiameter = min - getPaddingLeft();
        mArcRadius = arcDiameter / 3;
        top = height / 2.0f - (arcDiameter / 2.0f);
        left = width / 2.0f - (arcDiameter / 2.0f);
        mArcRect.set(left + 5, top + 5, left + arcDiameter - 5, top + arcDiameter);

        float arcStart = mProgressSweep + mStartAngle + mRotation + 90;
        mThumbXPos = (float) (mArcRadius * Math.cos(Math.toRadians(arcStart)));
        mThumbYPos = (float) (mArcRadius * Math.sin(Math.toRadians(arcStart)));

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        invalidate();
    }

    private void updateProgress(int progress, boolean fromUser) {

        if (progress == INVALID_PROGRESS_VALUE) {
            return;
        }

        progress = (progress > mMax) ? mMax : progress;
        progress = (progress < 0) ? 0 : progress;
        mProgress = progress;

        mProgressSweep = (float) progress / mMax * mSweepAngle;

        invalidate();
    }

    public void setProgress(int progress) {
        updateProgress(progress, false);
    }

    public int getProgress() {
        return mProgress;
    }

    Matrix matrix = new Matrix();

    private void drawIconByAngle(Canvas canvas, Bitmap bitmap, int angle, float radius, Paint paint) {
        matrix.reset();

        int arcStart = (int) getAngle(angle);
        float mScaledICCTDrawable = bitmap.getHeight() / 2f;

        mThumbXPos = (int) ((mArcRadius + mScaledICCTDrawable) * Math.cos(Math.toRadians(arcStart - 90)));
        mThumbYPos = (int) ((mArcRadius + mScaledICCTDrawable) * Math.sin(Math.toRadians(arcStart - 90)));

        matrix.postScale(0.5f * mScale, 0.6f * mScale);
        matrix.postRotate(arcStart);
        matrix.postTranslate(mTranslateX - mThumbXPos, mTranslateY - mThumbYPos);


        canvas.drawBitmap(bitmap, matrix, paint);
    }

    public int getArcRotation() {
        return mRotation;
    }

    public void setArcRotation(int mRotation) {
        this.mRotation = mRotation;
    }

    public int getStartAngle() {
        return mStartAngle;
    }

    public void setStartAngle(int mStartAngle) {
        this.mStartAngle = mStartAngle;
    }

    public int getSweepAngle() {
        return mSweepAngle;
    }

    public void setSweepAngle(int mSweepAngle) {
        this.mSweepAngle = mSweepAngle;
    }

    public int getMax() {
        return mMax;
    }

    public void setMax(int mMax) {
        this.mMax = mMax;
    }

    private float getAngle(float ppmRead) {
        return (240.0f * (ppmRead / mMax) + 60.0f);
    }
}
