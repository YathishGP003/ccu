package a75f.io.renatus.views.MasterControl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;

import a75f.io.renatus.R;


public class MasterControl extends View {

    public static final float RECOMMENDED_WIDTH_DP = 870.0f;
    public static final float TEXT_PADDING_LEFT_RIGHT_DP = 4.0f;
    public static final float ARROW_IMAGE_WIDTH = 6; // dp
    public static final float SETTLED_BUILDING_LIMITS_H = 90;
    public static final float SETTLED_ENERGY_SAVINGS_LIMITS_H = 55;

   /* public static final float SETTLED_ENERGY_SAVINGS_LIMITS_L = 55;
    public static final float SETTLED_ENERGY_SAVINGS_LIMITS_R = 80;*/

    private Paint mLinePaint;
    private Paint mDelimeterPaint;
    private Paint mDebugTextPaint;
    private Paint mTempIconPaint;
    private Bitmap mArrowHeadLeftBitmap;
    private Bitmap mArrowHeadRightBitmap;

    Paint mTempLinePaint;
    Paint mTempPaint;
    Paint mSmallLinePaint;
    Paint mDebugTextAlignCenterPaint;
    TextPaint mDiffTextPaint;

    private int mViewHeight = 0;
    private int mTextPadding = 0;
    private int mArrowImageWidth = 0;
    private int mBuildingLimitSpacing;
    private int mEnergySavingsSpacing;
    private int mTempCircleRadius = 0;
    private int mHeatingBarDisplacement = 0;
    private int mCoolingBarDisplacement = 0;
    private int mBuildingLimitCircleRadius = 0;
    private int mTempLineHeight = 0;
    private int mArrowTextSize = 0;
    private int mArrowLineSize = 0;
    private int mHitBoxPadding = 0;

    //
    private float lowerHeatingTemp = 70;
    private float upperHeatingTemp = 65;
    private float lowerCoolingTemp = 70;
    private float upperCoolingTemp = 75;
    private float lowerBuildingTemp = 55;
    private float upperBuildingTemp = 90;

    Rect bounds = new Rect();

    int mPaddingPX = 0;
    int mViewWidth = 0;

    //Amount of degrees below the bottom reading and above the top reading.
    int mEdgeDegrees = 2;

    int mDefaultStartDegree = 55;
    int mDefaultVisibleDegrees = 35;

    float mLowerBound = 32.0f;
    float mUpperBound = 110.0f;

    float mZoneDifferential = 3.0f;
    float mSetBack = 5.0f;

    int mDegreeIncremntPX = 0;
    boolean mMeasured = false;

    private static final int PADDING_LEFT_RIGHT_PX = 20; //dp

    public MasterControl(Context context) {
        super(context);
        init();
    }

    private static final boolean DEBUG = true;

    enum MasterControlState {
        NONE,
        LOWER_BUILDING_LIMIT,
        UPPER_BUILDING_LIMIT,
        LOWER_HEATING_LIMIT,
        UPPER_HEATING_LIMIT,
        LOWER_COOLING_LIMIT,
        UPPER_COOLING_LIMIT
    }

    RectF[] hitBoxes = new RectF[MasterControlState.values().length];
    float[] temps = new float[MasterControlState.values().length];
    Bitmap[] bitmaps = new Bitmap[MasterControlState.values().length];
    Matrix matrix = new Matrix();


    enum Direction {
        UP, DOWN
    }

    private void drawSliderIcon(Canvas canvas, Direction direction, int yDisplacemnet, MasterControlState stateReflected) {

        matrix.reset();

        hitBoxes[stateReflected.ordinal()].set(0, 0, bitmaps[stateReflected.ordinal()].getWidth(), bitmaps[stateReflected.ordinal()].getHeight());

        int xPos = getPXForTemp(temps[stateReflected.ordinal()]) - bitmaps[stateReflected.ordinal()].getWidth() / 2;
        int yPos = getTempLineYLocation() + yDisplacemnet + (direction == Direction.UP ? -bitmaps[stateReflected.ordinal()].getHeight()
                : bitmaps[stateReflected.ordinal()].getHeight());

        if (direction == Direction.DOWN)
            matrix.postRotate(180, bitmaps[stateReflected.ordinal()].getWidth() / 2, bitmaps[stateReflected.ordinal()].getHeight() / 3);
        matrix.postTranslate(xPos, yPos);
        canvas.drawBitmap(bitmaps[stateReflected.ordinal()], matrix, mTempPaint);

        matrix.reset();
        matrix.postTranslate(xPos - mHitBoxPadding, yPos - mHitBoxPadding);

        //Make the hit boxes easy to click.
        matrix.preScale(2.0f, 2.0f);

        matrix.mapRect(hitBoxes[stateReflected.ordinal()]);
        canvas.drawRect(hitBoxes[stateReflected.ordinal()], mDebugBoxesPaint);

        //The 2 and 1.5 are used to slide the number on the bitmap image.
        //Text centered left to right and 1/3 the way down the icon.
        canvas.drawText(String.valueOf(Math.round(temps[stateReflected.ordinal()])),
                xPos + bitmaps[stateReflected.ordinal()].getWidth() / 2,
                (float) (yPos + (direction == Direction.UP ?
                        bitmaps[stateReflected.ordinal()].getHeight() / 2 :
                        bitmaps[stateReflected.ordinal()].getHeight() / 3)), mTempIconPaint);
    }

    private MasterControlState isHitBoxTouched(float x, float y) {
        for (int i = 0; i < hitBoxes.length; i++) {
            if (hitBoxes[i].contains(x, y)) {
                MasterControlState retVal = MasterControlState.values()[i];
                Log.d("MasterControl", "HitBox was selected: " + retVal.name());

                return retVal;
            }
        }

        return MasterControlState.NONE;
    }

    private MasterControlState mSelected = MasterControlState.NONE;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        Log.i("MasterControl", "X: " + event.getX() + " Y: " + event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mSelected = isHitBoxTouched(event.getX(), event.getY());
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
            case MotionEvent.ACTION_MOVE:
                Log.i("Movement", "mSelected - " + mSelected.name()
                        + " Temps: " + getTempForPX((int) event.getX()));
                if (getTempForPX((int) event.getX()) > mLowerBound && getTempForPX((int) event.getX()) < mUpperBound) {

                    Log.i("Movement", "Temps: " + getTempForPX((int) event.getX()));
                    if (mSelected == MasterControlState.LOWER_COOLING_LIMIT) {
                        if (getTempForPX((int) event.getX()) >= temps[MasterControlState.LOWER_HEATING_LIMIT.ordinal()] && getTempForPX((int) event.getX()) <= temps[MasterControlState.UPPER_COOLING_LIMIT.ordinal()]) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                            temps[mSelected.ordinal()] = getTempForPX((int) event.getX());
                        } else {
                            mSelected = MasterControlState.UPPER_COOLING_LIMIT;
                        }
                    } else if (mSelected == MasterControlState.UPPER_COOLING_LIMIT) {
                        if (getTempForPX((int) event.getX()) >= temps[MasterControlState.LOWER_COOLING_LIMIT.ordinal()] && getTempForPX((int) event.getX()) <= upperCoolingTemp) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                            temps[mSelected.ordinal()] = getTempForPX((int) event.getX());
                        }
                    } else if (mSelected == MasterControlState.LOWER_HEATING_LIMIT) {
                        if (getTempForPX((int) event.getX()) >= temps[MasterControlState.UPPER_HEATING_LIMIT.ordinal()] && getTempForPX((int) event.getX()) <= temps[MasterControlState.LOWER_COOLING_LIMIT.ordinal()]) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                            temps[mSelected.ordinal()] = getTempForPX((int) event.getX());
                        } else {
                            mSelected = MasterControlState.UPPER_HEATING_LIMIT;
                        }
                    } else if (mSelected == MasterControlState.UPPER_HEATING_LIMIT) {
                        if (getTempForPX((int) event.getX()) <= temps[MasterControlState.LOWER_HEATING_LIMIT.ordinal()] && getTempForPX((int) event.getX()) >= upperHeatingTemp) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                            temps[mSelected.ordinal()] = getTempForPX((int) event.getX());
                        }
                    } else if (mSelected == MasterControlState.LOWER_BUILDING_LIMIT) {
                        if (getTempForPX((int) event.getX()) <= ((upperHeatingTemp - mSetBack) - mZoneDifferential)) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                            temps[mSelected.ordinal()] = getTempForPX((int) event.getX());
                        }
                    } else if (mSelected == MasterControlState.UPPER_BUILDING_LIMIT) {
                        if (getTempForPX((int) event.getX()) >= (upperCoolingTemp + mSetBack + mZoneDifferential)) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                            temps[mSelected.ordinal()] = getTempForPX((int) event.getX());
                        }
                    }

                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                mSelected = MasterControlState.NONE;
                invalidate();
                break;
        }

        Log.d("MasterControl", "Touched: " + isHitBoxTouched(event.getX(), event.getY()).name());
        return true;
    }

    Paint mDebugBoxesPaint;


    private boolean mDataSet = false;

    // init temps
    public void setData(float lowerHeatingTemp, float upperHeatingTemp, float lowerCoolingTemp,
                        float upperCoolingTemp, float lowerBuildingTemp, float upperBuildingTemp) {
        temps[MasterControlState.LOWER_HEATING_LIMIT.ordinal()] = lowerHeatingTemp;
        temps[MasterControlState.UPPER_HEATING_LIMIT.ordinal()] = upperHeatingTemp;
        temps[MasterControlState.LOWER_COOLING_LIMIT.ordinal()] = lowerCoolingTemp;
        temps[MasterControlState.UPPER_COOLING_LIMIT.ordinal()] = upperCoolingTemp;
        temps[MasterControlState.LOWER_BUILDING_LIMIT.ordinal()] = lowerBuildingTemp;
        temps[MasterControlState.UPPER_BUILDING_LIMIT.ordinal()] = upperBuildingTemp;

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

        for (int i = 0; i < hitBoxes.length; i++) {
            hitBoxes[i] = new RectF();
        }

        mArrowHeadLeftBitmap = getBitmapFromVectorDrawable(getContext(), R.drawable.ic_arrowhead_left);
        mArrowHeadRightBitmap = getBitmapFromVectorDrawable(getContext(), R.drawable.ic_arrowhead_right);

        bitmaps[MasterControlState.UPPER_BUILDING_LIMIT.ordinal()] = bitmaps[MasterControlState.LOWER_BUILDING_LIMIT.ordinal()] =
                getBitmapFromVectorDrawable(getContext(), R.drawable.ic_black_pin_seekbar);
        bitmaps[MasterControlState.LOWER_COOLING_LIMIT.ordinal()] = bitmaps[MasterControlState.UPPER_COOLING_LIMIT.ordinal()] =
                getBitmapFromVectorDrawable(getContext(), R.drawable.ic_blue_pin_seekbar);
        bitmaps[MasterControlState.LOWER_HEATING_LIMIT.ordinal()] = bitmaps[MasterControlState.UPPER_HEATING_LIMIT.ordinal()] =
                getBitmapFromVectorDrawable(getContext(), R.drawable.ic_orange_pin_seekbar);


        setData(lowerHeatingTemp, upperHeatingTemp, lowerCoolingTemp, upperCoolingTemp, lowerBuildingTemp, upperBuildingTemp);


        mLinePaint = new Paint();
        mLinePaint.setColor(Color.parseColor("#4C6d6e71"));
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mLinePaint.setStrokeWidth(24.0f);

        mDelimeterPaint = new Paint();
        mDelimeterPaint.setColor(Color.parseColor("#99FFFFFF"));
        mDelimeterPaint.setAntiAlias(true);
        mDelimeterPaint.setStyle(Paint.Style.STROKE);
        mDelimeterPaint.setStrokeWidth(4.0f);

        mDebugTextPaint = new Paint();
        mDebugTextPaint.setTypeface(latoLightFont);
        mDebugTextPaint.setStyle(Paint.Style.FILL);
        mDebugTextPaint.setColor(Color.parseColor("#000000"));
        mDebugTextPaint.setAntiAlias(true);
        mDebugTextPaint.setTextSize(22);

        mDebugTextAlignCenterPaint = new Paint();
        mDebugTextAlignCenterPaint.setTypeface(latoLightFont);
        mDebugTextAlignCenterPaint.setStyle(Paint.Style.FILL);
        mDebugTextAlignCenterPaint.setColor(Color.parseColor("#000000"));
        mDebugTextAlignCenterPaint.setAntiAlias(true);
        mDebugTextAlignCenterPaint.setTextAlign(Paint.Align.CENTER);
        mDebugTextAlignCenterPaint.setTextSize(22);

        mDiffTextPaint = new TextPaint();
        mDiffTextPaint.setTypeface(latoLightFont);
        mDiffTextPaint.setStyle(Paint.Style.FILL);
        mDiffTextPaint.setColor(Color.parseColor("#000000"));
        mDiffTextPaint.setAntiAlias(true);
        mDiffTextPaint.setTextAlign(Paint.Align.CENTER);
        mDiffTextPaint.setTextSize(20);


        mSmallLinePaint = new Paint();
        mSmallLinePaint.setColor(Color.parseColor("#4C6d6e71"));
        mSmallLinePaint.setAntiAlias(true);
        mSmallLinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mSmallLinePaint.setStrokeCap(Paint.Cap.BUTT);


        mTempPaint = new Paint();
        mTempPaint.setColor(Color.parseColor("#4C6d6e71"));
        mTempPaint.setAntiAlias(true);
        mTempPaint.setStyle(Paint.Style.FILL);
        mTempPaint.setStrokeCap(Paint.Cap.BUTT);
        mTempPaint.setStrokeWidth(1.0f);

        mTempLinePaint = new Paint();
        mTempLinePaint.setColor(Color.parseColor("#4C6d6e71"));
        mTempLinePaint.setAntiAlias(true);
        mTempLinePaint.setStyle(Paint.Style.FILL);
        mTempLinePaint.setStrokeWidth(6.0f);


        mTempIconPaint = new Paint();
        mTempIconPaint.setColor(Color.WHITE);
        mTempIconPaint.setAntiAlias(true);
        mTempIconPaint.setStyle(Paint.Style.STROKE);
        mTempIconPaint.setStrokeWidth(0.5f);
        mTempIconPaint.setTextAlign(Paint.Align.CENTER);
        mTempIconPaint.setTypeface(latoLightFont);
        mTempIconPaint.setStyle(Paint.Style.FILL);


        mDebugBoxesPaint = new Paint();
        mDebugBoxesPaint.setColor(Color.TRANSPARENT);
        mDebugBoxesPaint.setAntiAlias(true);
        mDebugBoxesPaint.setStyle(Paint.Style.STROKE);
        mDebugBoxesPaint.setStrokeWidth(2.0f);
        mDebugBoxesPaint.setTextAlign(Paint.Align.CENTER);
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
        mTextPadding = (int) (TEXT_PADDING_LEFT_RIGHT_DP * displayMetrics.density);
        mViewWidth = Math.round((mUpperBound - mLowerBound) * mDegreeIncremntPX + (mPaddingPX) * 2);
        mArrowImageWidth = (int) (ARROW_IMAGE_WIDTH * displayMetrics.density);
        mBuildingLimitSpacing = (int) (SETTLED_BUILDING_LIMITS_H * displayMetrics.density);
        mEnergySavingsSpacing = (int) (SETTLED_ENERGY_SAVINGS_LIMITS_H * displayMetrics.density);
        mTempCircleRadius = (int) (7 * displayMetrics.density);
        mHeatingBarDisplacement = (int) (20 * displayMetrics.density);
        mCoolingBarDisplacement = (int) (-20 * displayMetrics.density);
        mBuildingLimitCircleRadius = (int) (7 * displayMetrics.density);
        mTempLineHeight = (int) (7 * displayMetrics.density);
        mArrowTextSize = (int) (12 * displayMetrics.density);

        mHitBoxPadding = (int) (25 * displayMetrics.density);
        mArrowLineSize = (int) (1 * displayMetrics.density);
        mPaddingBetweenCoolingBarAndSliderIcon = (int) (10 * displayMetrics.density);
        setMeasuredDimension(Math.round(mViewWidth), mViewHeight);

        scrollToDefaultStartLocation();
    }

    int mPaddingBetweenCoolingBarAndSliderIcon;

    private void scrollToDefaultStartLocation() {
        if ((((HorizontalScrollView) getParent()).getMeasuredWidth() > 0)) {
            mMeasured = true;
            invalidate();
            MasterControl.this.post(new Runnable() {
                @Override
                public void run() {
                    ((HorizontalScrollView) MasterControl.this.getParent())
                            .scrollTo(getPXForTemp(mDefaultStartDegree - mEdgeDegrees), 0);
                }
            });
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


        if (mMeasured) {
            drawTempLine(canvas);
            drawWhiteDelimiters(canvas);
            drawArrowText(canvas, "BUILDING LIMITS", mBuildingLimitSpacing + 50, temps[MasterControlState.LOWER_BUILDING_LIMIT.ordinal()],
                    temps[MasterControlState.UPPER_BUILDING_LIMIT.ordinal()],
                    Color.parseColor("#5E000000"),
                    Color.parseColor("#5E231f20"));

            if (temps[MasterControlState.LOWER_COOLING_LIMIT.ordinal()] == upperCoolingTemp) {
                drawArrowText(canvas, "5\u00B0 SETBACK", mEnergySavingsSpacing - 18, temps[MasterControlState.LOWER_COOLING_LIMIT.ordinal()],
                        temps[MasterControlState.LOWER_COOLING_LIMIT.ordinal()] + mSetBack,
                        Color.parseColor("#5E000000"),
                        Color.parseColor("#5E231f20"));
            }

            if (temps[MasterControlState.UPPER_HEATING_LIMIT.ordinal()] == upperHeatingTemp) {
                drawArrowText(canvas, "5\u00B0 SETBACK", mEnergySavingsSpacing - 18, (temps[MasterControlState.UPPER_HEATING_LIMIT.ordinal()] - mSetBack),
                        upperHeatingTemp,
                        Color.parseColor("#5E000000"),
                        Color.RED);
            }

            if (temps[MasterControlState.UPPER_HEATING_LIMIT.ordinal()] == upperHeatingTemp && temps[MasterControlState.LOWER_BUILDING_LIMIT.ordinal()] == (getEnergySavingLowerLimit() - mZoneDifferential)) {
                drawArrowDiffText(canvas, "BUILDING\nZONE\nDIFFERENTIAL", mEnergySavingsSpacing - 18, temps[MasterControlState.LOWER_BUILDING_LIMIT.ordinal()],
                        upperHeatingTemp - mSetBack,
                        Color.parseColor("#5E000000"),
                        Color.RED);
            }

            if (temps[MasterControlState.LOWER_COOLING_LIMIT.ordinal()] == upperCoolingTemp && temps[MasterControlState.UPPER_BUILDING_LIMIT.ordinal()] == (getEnergySavingUpperLimit() + mZoneDifferential)) {
                drawArrowDiffText(canvas, "BUILDING\nZONE\nDIFFERENTIAL", mEnergySavingsSpacing - 18, upperCoolingTemp + mSetBack,
                        temps[MasterControlState.UPPER_BUILDING_LIMIT.ordinal()],
                        Color.parseColor("#5E000000"),
                        Color.parseColor("#5E000000"));
            }

            drawArrowText(canvas, "ENERGY SAVINGS RANGE", mEnergySavingsSpacing + 30,
                    upperHeatingTemp - mSetBack, upperCoolingTemp + mSetBack,
                    Color.parseColor("#5E000000"),
                    Color.parseColor("#5E231f20"));

            drawTempGauge(canvas, temps[MasterControlState.LOWER_HEATING_LIMIT.ordinal()],
                    temps[MasterControlState.UPPER_HEATING_LIMIT.ordinal()], mHeatingBarDisplacement,
                    Color.parseColor("#e24301"));

            drawTempGauge(canvas, temps[MasterControlState.LOWER_COOLING_LIMIT.ordinal()],
                    temps[MasterControlState.UPPER_COOLING_LIMIT.ordinal()], mCoolingBarDisplacement,
                    Color.parseColor("#8392c9"));
            drawBuildingLimitCircles(canvas);
            drawBuildingLimitCircle(canvas, MasterControlState.LOWER_COOLING_LIMIT);
            drawBuildingLimitCircle(canvas, MasterControlState.UPPER_COOLING_LIMIT);
            drawBuildingLimitCircle(canvas, MasterControlState.LOWER_HEATING_LIMIT);
            drawBuildingLimitCircle(canvas, MasterControlState.UPPER_HEATING_LIMIT);

            if (mSelected == MasterControlState.LOWER_COOLING_LIMIT) {
                drawSliderIcon(canvas,
                        Direction.UP, mCoolingBarDisplacement - mPaddingBetweenCoolingBarAndSliderIcon, MasterControlState.LOWER_COOLING_LIMIT);
            }

            if (mSelected == MasterControlState.LOWER_HEATING_LIMIT) {
                drawSliderIcon(canvas,
                        Direction.DOWN, mHeatingBarDisplacement - mPaddingBetweenCoolingBarAndSliderIcon, MasterControlState.LOWER_HEATING_LIMIT);
            }

            if (mSelected == MasterControlState.UPPER_COOLING_LIMIT) {
                drawSliderIcon(canvas,
                        Direction.UP, mCoolingBarDisplacement - mPaddingBetweenCoolingBarAndSliderIcon, MasterControlState.UPPER_COOLING_LIMIT);
            }

            if (mSelected == MasterControlState.UPPER_HEATING_LIMIT) {
                drawSliderIcon(canvas,
                        Direction.DOWN, mHeatingBarDisplacement - mPaddingBetweenCoolingBarAndSliderIcon, MasterControlState.UPPER_HEATING_LIMIT);
            }

            if (mSelected == MasterControlState.UPPER_BUILDING_LIMIT) {
                drawSliderIcon(canvas, Direction.UP, -mPaddingBetweenCoolingBarAndSliderIcon, MasterControlState.UPPER_BUILDING_LIMIT);
            }

            if (mSelected == MasterControlState.LOWER_BUILDING_LIMIT) {
                drawSliderIcon(canvas, Direction.UP, -mPaddingBetweenCoolingBarAndSliderIcon, MasterControlState.LOWER_BUILDING_LIMIT);
            }
        }
    }

    private void drawArrowDiffText(Canvas canvas, String text,
                                   float yValue, float lTemp,
                                   float uTemp, int lineColor, int textColor) {
        mDiffTextPaint.setColor(textColor);
        mDiffTextPaint.setTextSize(mArrowTextSize - 1.6f);
        mDiffTextPaint.getTextBounds(text, 0, text.length(), bounds);

        float textXLocation = (getPXForTemp(uTemp) + getPXForTemp(lTemp)) / 2.0f;
        float textYLocation = getTempLineYLocation() - yValue +
                (yValue > 0 ? bounds.height() / 2 : -(bounds.height() / 2));

        StaticLayout mTextLayout = new StaticLayout(text, mDiffTextPaint, canvas.getWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

        canvas.save();
        canvas.translate(textXLocation, textYLocation - mTextPadding * 11);
        mTextLayout.draw(canvas);
        canvas.restore();

        float startOfLowerTemp = getPXForTemp(lTemp);
        float yOfLowerTemp = getTempLineYLocation() - yValue;

        float endOfUpperTemp = getPXForTemp(uTemp);
        mSmallLinePaint.setStrokeWidth(mArrowLineSize);
        mSmallLinePaint.setColor(lineColor);
        //to draw  arrow
        canvas.drawBitmap(mArrowHeadLeftBitmap, startOfLowerTemp, yOfLowerTemp - mTextPadding * 7, mSmallLinePaint);
        canvas.drawBitmap(mArrowHeadRightBitmap, endOfUpperTemp - mArrowImageWidth, yOfLowerTemp - mTextPadding * 7, mSmallLinePaint);
    }


    private void drawTempGauge(Canvas canvas, float mLowerHeatingTemp, float mUpperHeatingTemp,
                               int yDisplacement, int parseColor) {
        mTempPaint.setColor(parseColor);
        mTempLinePaint.setColor(parseColor);

        //draw two more circle for shadow effect
        canvas.drawLine(getPXForTemp(mLowerHeatingTemp), getTempLineYLocation() + yDisplacement, getPXForTemp(mUpperHeatingTemp), getTempLineYLocation() + yDisplacement, mTempLinePaint);
        canvas.drawCircle(getPXForTemp(mLowerHeatingTemp), getTempLineYLocation() + yDisplacement, mTempCircleRadius, mTempPaint);
        canvas.drawCircle(getPXForTemp(mUpperHeatingTemp), getTempLineYLocation() + yDisplacement, mTempCircleRadius, mTempPaint);

    }

    private void drawBuildingLimitCircles(Canvas canvas) {
        drawBuildingLimitCircle(canvas, MasterControlState.LOWER_BUILDING_LIMIT);
        drawBuildingLimitCircle(canvas, MasterControlState.UPPER_BUILDING_LIMIT);
    }

    private void drawBuildingLimitCircle(Canvas canvas, MasterControlState controlState) {
        float temp = temps[controlState.ordinal()];
        mTempPaint.setColor(Color.parseColor("#231f20"));

        int topPadding = 0;
        if (controlState == MasterControlState.LOWER_COOLING_LIMIT || controlState == MasterControlState.UPPER_COOLING_LIMIT) {
            topPadding = -20;
        } else if (controlState == MasterControlState.LOWER_HEATING_LIMIT || controlState == MasterControlState.UPPER_HEATING_LIMIT) {
            topPadding = 20;
        }

        int xLoc = getPXForTemp(temp);
        int yLoc = getTempLineYLocation() + topPadding;

        if (controlState == MasterControlState.LOWER_BUILDING_LIMIT || controlState == MasterControlState.UPPER_BUILDING_LIMIT) {
            canvas.drawCircle(xLoc, yLoc,
                    mBuildingLimitCircleRadius, mTempPaint);
        }

        matrix.reset();
        hitBoxes[controlState.ordinal()].set(0, 0, bitmaps[controlState.ordinal()].getWidth() - 10,
                bitmaps[controlState.ordinal()].getHeight() - 10);


        matrix.reset();
        matrix.postTranslate(xLoc - mHitBoxPadding, yLoc - mHitBoxPadding);

        //Make the hit boxes easy to click.
        matrix.preScale(2.0f, 2.0f);

        matrix.mapRect(hitBoxes[controlState.ordinal()]);
        canvas.drawRect(hitBoxes[controlState.ordinal()], mDebugBoxesPaint);
    }

    private float getEnergySavingLowerLimit() {
        return temps[MasterControlState.UPPER_HEATING_LIMIT.ordinal()] - mSetBack;
    }

    private float getEnergySavingUpperLimit() {
        return temps[MasterControlState.UPPER_COOLING_LIMIT.ordinal()] + mSetBack;
    }

    public float getLowerHeatingTemp() {
        return temps[MasterControlState.LOWER_HEATING_LIMIT.ordinal()];
    }

    public float getUpperHeatingTemp() {
        return temps[MasterControlState.UPPER_HEATING_LIMIT.ordinal()];
    }

    public float getLowerCoolingTemp() {
        return temps[MasterControlState.LOWER_COOLING_LIMIT.ordinal()];
    }

    public float getUpperCoolingTemp() {
        return temps[MasterControlState.UPPER_COOLING_LIMIT.ordinal()];
    }

    public float getLowerBuildingTemp() {
        return temps[MasterControlState.LOWER_BUILDING_LIMIT.ordinal()];
    }

    public float getUpperBuildingTemp() {
        return temps[MasterControlState.UPPER_BUILDING_LIMIT.ordinal()];
    }

    //yValue distance from center line
    private void drawArrowText(Canvas canvas, String text,
                               float yValue, float lTemp,
                               float uTemp, int lineColor, int textColor) {

        mDebugTextAlignCenterPaint.setColor(textColor);
        mDebugTextAlignCenterPaint.setTextSize(mArrowTextSize);
        mDebugTextAlignCenterPaint.getTextBounds(text, 0, text.length(), bounds);

        float textXLocation = (getPXForTemp(uTemp) + getPXForTemp(lTemp)) / 2.0f;
        float textYLocation = getTempLineYLocation() - yValue +
                (yValue > 0 ? bounds.height() / 2 : -(bounds.height() / 2));

        //to draw horizontal texts
        canvas.drawText(text, textXLocation, textYLocation - mTextPadding * 6, mDebugTextAlignCenterPaint);
        float startOfLowerTemp = getPXForTemp(lTemp);
        float endOfLowerTemp = textXLocation - bounds.width() / 2.0f - mTextPadding;

        float yOfLowerTemp = getTempLineYLocation() - yValue;

        float startOfUpperTemp = textXLocation + bounds.width() / 2.0f + mTextPadding;
        float endOfUpperTemp = getPXForTemp(uTemp);

        mSmallLinePaint.setStrokeWidth(mArrowLineSize);
        mSmallLinePaint.setColor(lineColor);
        //to draw horizontal lines
        canvas.drawLine(startOfLowerTemp + mArrowImageWidth, yOfLowerTemp - mTextPadding * 6, endOfLowerTemp, yOfLowerTemp - mTextPadding * 6, mSmallLinePaint);
        canvas.drawLine(startOfUpperTemp, yOfLowerTemp - mTextPadding * 6, endOfUpperTemp - mArrowImageWidth, yOfLowerTemp - mTextPadding * 6, mSmallLinePaint);
        //to draw vertical lines
        canvas.drawLine(startOfLowerTemp, yOfLowerTemp - mTextPadding * 6, startOfLowerTemp, getTempLineYLocation() - mTextPadding, mSmallLinePaint);
        canvas.drawLine(endOfUpperTemp, yOfLowerTemp - mTextPadding * 6, endOfUpperTemp, getTempLineYLocation() - mTextPadding, mSmallLinePaint);
        //to draw  arrow
        canvas.drawBitmap(mArrowHeadLeftBitmap, startOfLowerTemp, yOfLowerTemp - mTextPadding * 7, mSmallLinePaint);
        canvas.drawBitmap(mArrowHeadRightBitmap, endOfUpperTemp - mArrowImageWidth, yOfLowerTemp - mTextPadding * 7, mSmallLinePaint);
    }


    private int getPXForTemp(float temp) {
        return Math.round(mPaddingPX + mDegreeIncremntPX * (temp - mLowerBound));
    }

    private float getTempForPX(int px) {
        return ((px - mPaddingPX) / mDegreeIncremntPX) + mLowerBound;
    }


    private void drawTempLine(Canvas canvas) {
        mLinePaint.setStrokeWidth(mTempLineHeight);

        //why 10
        canvas.drawLine(mPaddingPX - 10, getTempLineYLocation(), mViewWidth - mPaddingPX + 10, getTempLineYLocation(), mLinePaint);
    }

    private int getTempLineYLocation() {
        return mViewHeight / 2;
    }

    private void drawWhiteDelimiters(Canvas canvas) {
        for (int i = mPaddingPX; i <= mViewWidth - mPaddingPX; i = i + mDegreeIncremntPX) {
            canvas.drawLine(i, mViewHeight / 2.0f, i, mViewHeight / 2.0f + 24.0f, mDelimeterPaint);
        }

        for (int i = (int) (5 * (Math.ceil(Math.abs((int) mLowerBound / 5)))); i <= 5 * (Math.floor(Math.abs((int) mUpperBound / 5))); i += 5) {
            String temp = String.valueOf(i);

            mDebugTextPaint.setTextSize(mArrowTextSize);
            mDebugTextPaint.getTextBounds(temp, 0, temp.length(), bounds);
            canvas.drawText(temp, getPXForTemp(i) - mSetBack, getTempLineYLocation() + mEnergySavingsSpacing, mDebugTextPaint);

        }
    }

    private static float roundToHalf(float d) {
        return Math.round(d * 2) / 2.0f;
    }

    public MasterControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MasterControl(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public MasterControl(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }
}