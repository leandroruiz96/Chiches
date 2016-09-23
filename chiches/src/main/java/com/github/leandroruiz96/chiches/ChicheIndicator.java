package com.github.leandroruiz96.chiches;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Interpolator;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by leandro on 12/9/16.
 */
public class ChicheIndicator extends View {

    private static final long GROWING_TIME = 100;
    private static final float TRANSLATION_VELOCITY = 3.5f;
    private static final long FPS = 1000 / 120;
    private long mEndAnimMillis;
    private RectF mCircleBoundaries;
    private float mStroke;

    enum AnimationState { COMPLETE, SHRINKING, GROWING, TRANSLATING }

    int mColor;
    int mCircles;

    Paint mPaintStroke;
    Paint mPaintFill;

    long mStartAnimMillis;

    int mCurrentPos;
    int mDestinyPos;

    AnimationState mState;

    OnIndicatorChange mChangeListener;

    public interface OnIndicatorChange {
        void onIndicatorChange(int position);
    }

    public ChicheIndicator(Context context) {
        super(context);

        mColor = Color.BLUE;
        mCircles = 1;
        mStroke = 15f;

        definePaints();
        initialState();
    }

    public ChicheIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadAttrs(attrs);
        definePaints();
        initialState();
    }

    private void initialState() {
        mState = AnimationState.COMPLETE;
        mCircleBoundaries = new RectF();
        mCurrentPos = 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    void definePaints() {
        mPaintStroke = new Paint();
        mPaintStroke.setColor(mColor);
        mPaintStroke.setStyle(Paint.Style.STROKE);
        mPaintStroke.setStrokeWidth(mStroke);
        mPaintStroke.setStrokeJoin(Paint.Join.ROUND);
        mPaintStroke.setStrokeCap(Paint.Cap.ROUND);

        mPaintFill = new Paint();
        mPaintFill.setColor(mColor);
        mPaintFill.setStyle(Paint.Style.FILL);
    }

    void loadAttrs(AttributeSet attrs) {
        TypedArray ta = getContext().obtainStyledAttributes(attrs,R.styleable.ChicheIndicator,0,0);

        mColor = ta.getColor(R.styleable.ChicheIndicator_indicatorColor, Color.BLUE);
        mCircles = ta.getInt(R.styleable.ChicheIndicator_circles,1);
        mStroke = ta.getFloat(R.styleable.ChicheIndicator_stroke,15f);

        ta.recycle();
    }

    public void setCurrentPosition(int ixX) {
        if (mCurrentPos != ixX && mDestinyPos != ixX && mState == AnimationState.COMPLETE) {
            mDestinyPos = ixX;
            mStartAnimMillis = System.currentTimeMillis();
            mState = AnimationState.SHRINKING;
            mEndAnimMillis = mStartAnimMillis + GROWING_TIME;
            if (mChangeListener!=null) mChangeListener.onIndicatorChange(ixX);
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int[] positions = {0,0};
                getLocationInWindow(positions);
                float insideX = event.getRawX() - positions[0];
                float insideY = event.getRawY() - positions[1];
                int ixX = circleFor(insideX,insideY);
                if (ixX>=0) {
                    if (mCurrentPos != ixX && mDestinyPos != ixX && mState == AnimationState.COMPLETE) {
                        mDestinyPos = ixX;
                        mStartAnimMillis = System.currentTimeMillis();
                        mState = AnimationState.SHRINKING;
                        mEndAnimMillis = mStartAnimMillis + GROWING_TIME;
                        if (mChangeListener!=null) mChangeListener.onIndicatorChange(ixX);
                        invalidate();
                    }
                }
                break;
        }
        return true;
    }

    private int circleFor(float insideX,float insideY) {

        int height = getHeight();
        int width = getWidth();

        float emptyCircleRadius = height*0.8f/2;
        for (int i = 0; i<mCircles; i++) {
            float centerX = (width*(2*i+1))/(2*mCircles);
            float centerY = height/2f;
            if (norma2(centerX,centerY,insideX,insideY)<=emptyCircleRadius) return i;
        }
        return -1;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int height = canvas.getHeight();
        int width = canvas.getWidth();

        float emptyCircleRadius = height*0.8f/2;
        float filledCircleRadius = height*0.6f/2;
        float shrankCircleRadius = (height/8f)*0.6f;
        float angle = (float) Math.toDegrees(Math.asin((height/8)/(emptyCircleRadius)));
        float[] rails = {height*3/8f, height*5/8f};

        //region Draw empty circles

        for (int i = 0; i<mCircles; i++) {
            float centerX = (width*(2*i+1))/(2*mCircles);
            float centerY = height/2f;
            mCircleBoundaries.left      = centerX - emptyCircleRadius;
            mCircleBoundaries.right     = centerX + emptyCircleRadius;
            mCircleBoundaries.top       = centerY - emptyCircleRadius;
            mCircleBoundaries.bottom    = centerY + emptyCircleRadius;

            if (i==0) {
                canvas.drawArc(mCircleBoundaries,angle,360-2*angle,false,mPaintStroke);
            } else if (i==mCircles-1) {
                canvas.drawArc(mCircleBoundaries,180+angle,360-2*angle,false,mPaintStroke);
            } else {
                canvas.drawArc(mCircleBoundaries, 180+angle, 180-2*angle, false, mPaintStroke);
                canvas.drawArc(mCircleBoundaries, angle, 180-2*angle, false, mPaintStroke);
            }
        }

        //endregion

        //region Draw rails

        for (int i = 1; i < mCircles; i++) {
            float centerXL = (width*(2*i-1))/(2*mCircles);
            float centerXN = (width*(2*i+1))/(2*mCircles);
            float startX = (float) (centerXL +pitagoras(emptyCircleRadius,height/8f));
            float endX = (float) (centerXN - pitagoras(emptyCircleRadius,height/8f));
            canvas.drawLine(startX,rails[0],endX,rails[0],mPaintStroke);
            canvas.drawLine(startX,rails[1],endX,rails[1],mPaintStroke);
        }

        //endregion

        switch (mState) {
            case COMPLETE: {
                float centerX = (width*(2*mCurrentPos+1))/(2*mCircles);
                float centerY = height/2f;
                canvas.drawCircle(centerX,centerY,filledCircleRadius,mPaintFill);
                break;
            }

            case SHRINKING: {
                float centerX = (width*(2*mCurrentPos+1))/(2*mCircles);
                float centerY = height/2f;
                float radius = interpolate(filledCircleRadius,shrankCircleRadius);
                canvas.drawCircle(centerX,centerY,radius,mPaintFill);
                if (mEndAnimMillis <= System.currentTimeMillis()) {
                    mStartAnimMillis = System.currentTimeMillis();
                    float centerXP = (width*(2*mDestinyPos+1))/(2*mCircles);
                    mEndAnimMillis = mStartAnimMillis + (long)((Math.abs(centerXP-centerX))/TRANSLATION_VELOCITY);
                    mState = AnimationState.TRANSLATING;
                }
                postInvalidateDelayed(FPS);
                break;
            }

            case GROWING: {
                float centerX = (width*(2*mCurrentPos+1))/(2*mCircles);
                float centerY = height/2f;
                float radius = interpolate(shrankCircleRadius,filledCircleRadius);
                canvas.drawCircle(centerX,centerY,radius,mPaintFill);
                if (mEndAnimMillis <= System.currentTimeMillis()) {
                    mState = AnimationState.COMPLETE;
                }
                postInvalidateDelayed(FPS);
                break;
            }

            case TRANSLATING: {
                float centerXC = (width*(2*mCurrentPos+1))/(2*mCircles);
                float centerXP = (width*(2*mDestinyPos+1))/(2*mCircles);
                float centerY = height/2f;
                float currentX = interpolate(centerXC,centerXP);
                canvas.drawCircle(currentX,centerY,shrankCircleRadius,mPaintFill);
                if (mEndAnimMillis <= System.currentTimeMillis()) {
                    mCurrentPos = mDestinyPos;
                    mStartAnimMillis = System.currentTimeMillis();
                    mEndAnimMillis = mStartAnimMillis + GROWING_TIME;
                    mState = AnimationState.GROWING;
                }
                postInvalidateDelayed(FPS);
                break;
            }
        }
    }

    double norma2(float x1, float y1, float x2, float y2) {
        float x = (x1-x2);
        float y = (y1-y2);
        return Math.sqrt(x*x+y*y);
    }

    double pitagoras(float hyp, float cat) {
        return Math.sqrt(hyp*hyp-cat*cat);
    }

    float interpolate(float start, float end) {
        long elapsed = (System.currentTimeMillis()-mStartAnimMillis);
        long duration = mEndAnimMillis - mStartAnimMillis;
        float toInterpolate = end - start;
        return start + toInterpolate*elapsed/duration;
    }
}
