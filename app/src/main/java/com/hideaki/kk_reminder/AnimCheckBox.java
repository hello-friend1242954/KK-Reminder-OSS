package com.hideaki.kk_reminder;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Checkable;

public class AnimCheckBox extends View implements Checkable {

  private final double sin27 = Math.sin(Math.toRadians(27));
  private final double sin63 = Math.sin(Math.toRadians(63));
  private final int duration = 500;
  private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private RectF rectF = new RectF();
  private RectF innerRectF = new RectF();
  private Path path = new Path();
  private float sweepAngle;
  private float hookStartY;
  private float baseLeftHookOffset;
  private float endLeftHookOffset;
  private int size;
  private boolean isChecked;
  private float hookOffset;
  private float hookSize;
  private int innerCircleAlpha = 0XFF;
  private int strokeWidth = 2;
  private int strokeColor = Color.BLUE;
  //  private int mCircleColor = Color.WHITE;
  private OnCheckedChangeListener onCheckedChangeListener;
  private MainActivity activity;
  private ManuallySnoozeActivity manuallySnoozeActivity;
  private boolean isAnimation;

  public AnimCheckBox(Context context) {

    this(context, null);
    if(context instanceof MainActivity) {
      activity = (MainActivity)context;
    }
    else if(context instanceof ManuallySnoozeActivity) {
      manuallySnoozeActivity = (ManuallySnoozeActivity)context;
    }
    else if(context instanceof ContextWrapper) {
      activity = (MainActivity)((ContextWrapper)context).getBaseContext();
    }
    isAnimation = true;
  }

  public AnimCheckBox(Context context, AttributeSet attrs) {

    super(context, attrs);
    if(context instanceof MainActivity) {
      activity = (MainActivity)context;
    }
    else if(context instanceof ManuallySnoozeActivity) {
      manuallySnoozeActivity = (ManuallySnoozeActivity)context;
    }
    else if(context instanceof ContextWrapper) {
      activity = (MainActivity)((ContextWrapper)context).getBaseContext();
    }
    init(attrs);
    isAnimation = true;
  }

  private void init(AttributeSet attrs) {

    boolean checked = this.isChecked;
    if(attrs != null) {
      TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.AnimCheckBox);
      strokeWidth =
        (int)array.getDimension(R.styleable.AnimCheckBox_stroke_width, dip(strokeWidth));
      if(activity != null) {
        int order = activity.order;
        if(
          (order == 0 && !activity.isExpandableTodo) ||
            (
              order == 1 && !activity
                .generalSettings
                .getNonScheduledLists()
                .get(activity.whichMenuOpen - 1)
                .isTodo()
            )
        ) {
          strokeColor = Color.GRAY;
        }
        else {
          strokeColor =
            array.getColor(R.styleable.AnimCheckBox_stroke_color, activity.accentColor);
        }
//        mCircleColor = array.getColor(R.styleable.AnimCheckBox_circle_color, Color.WHITE);
      }
      else if(manuallySnoozeActivity != null) {
        if(manuallySnoozeActivity.isDarkMode) {
          strokeColor = manuallySnoozeActivity.secondaryTextMaterialDarkColor;
        }
        else {
          strokeColor = array.getColor(
            R.styleable.AnimCheckBox_stroke_color,
            Color.parseColor("#b0002171")
          );
        }
      }
      else {
        strokeColor = array.getColor(
          R.styleable.AnimCheckBox_stroke_color,
          Color.parseColor("#b0002171")
        );
//        mCircleColor = array.getColor(
//            R.styleable.AnimCheckBox_circle_color,
//            Color.parseColor("#FF9b0000")
//        );
      }
      checked = array.getBoolean(R.styleable.AnimCheckBox_checked, false);
      array.recycle();
    }
    else {
      strokeWidth = dip(strokeWidth);
    }
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(strokeWidth);
    paint.setColor(strokeColor);
    super.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

        setChecked(!AnimCheckBox.this.isChecked);
      }
    });
    setCheckedViewInner(checked, false);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {

    return super.dispatchTouchEvent(event);
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public boolean onTouchEvent(MotionEvent event) {

    return super.onTouchEvent(event);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);
    if(MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST &&
      MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
      ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)getLayoutParams();

      int defaultSize = 40;
      width = height = Math.min(
        dip(defaultSize) - params.leftMargin - params.rightMargin,
        dip(defaultSize) - params.bottomMargin - params.topMargin
      );
    }
    int size = Math.min(
      width - getPaddingLeft() - getPaddingRight(),
      height - getPaddingBottom() - getPaddingTop()
    );
    setMeasuredDimension(size, size);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

    super.onLayout(changed, left, top, right, bottom);
    size = getWidth();
    int radius = (getWidth() - (2 * strokeWidth)) / 2;
    //noinspection SuspiciousNameCombination
    rectF.set(strokeWidth, strokeWidth, size - strokeWidth, size - strokeWidth);
    innerRectF.set(rectF);
    innerRectF.inset((float)strokeWidth / 2, (float)strokeWidth / 2);
    hookStartY = (float)(size / 2 - (radius * sin27 + (radius - radius * sin63)));
    baseLeftHookOffset = (float)(radius * (1 - sin63)) + (float)strokeWidth / 2;
    float mBaseRightHookOffset = 0f;
    endLeftHookOffset = baseLeftHookOffset + ((float)2 * size / 3 - hookStartY) * 0.33f;
    float mEndRightHookOffset = mBaseRightHookOffset + ((float)size / 3 + hookStartY) * 0.38f;
    hookSize = size - (endLeftHookOffset + mEndRightHookOffset);
    hookOffset = isChecked ? hookSize + endLeftHookOffset - baseLeftHookOffset : 0;
  }

  @Override
  protected void onDraw(Canvas canvas) {

    super.onDraw(canvas);
    drawCircle(canvas);
    drawHook(canvas);
  }

  private void drawCircle(Canvas canvas) {

    initDrawStrokeCirclePaint();
    canvas.drawArc(rectF, 202, sweepAngle, false, paint);
    initDrawAlphaStrokeCirclePaint();
    canvas.drawArc(rectF, 202, sweepAngle - 360, false, paint);
//    initDrawInnerCirclePaint();
//    canvas.drawArc(mInnerRectF, 0, 360, false, mPaint);
  }

  private void drawHook(Canvas canvas) {

    if(hookOffset == 0) {
      return;
    }
    initDrawHookPaint();
    path.reset();
    float offset;
    if(hookOffset <= ((float)2 * size / 3 - hookStartY - baseLeftHookOffset)) {
      path.moveTo(baseLeftHookOffset, baseLeftHookOffset + hookStartY);
      path.lineTo(
        baseLeftHookOffset + hookOffset,
        baseLeftHookOffset + hookStartY + hookOffset
      );
    }
    else if(hookOffset <= hookSize) {
      path.moveTo(baseLeftHookOffset, baseLeftHookOffset + hookStartY);
      path.lineTo((float)2 * size / 3 - hookStartY, (float)2 * size / 3);
      path.lineTo(
        hookOffset + baseLeftHookOffset,
        (float)2 * size / 3 - (hookOffset - ((float)2 * size / 3 - hookStartY - baseLeftHookOffset))
      );
    }
    else {
      offset = hookOffset - hookSize;
      path.moveTo(baseLeftHookOffset + offset, baseLeftHookOffset + hookStartY + offset);
      path.lineTo((float)2 * size / 3 - hookStartY, (float)2 * size / 3);
      path.lineTo(
        hookSize + baseLeftHookOffset + offset,
        (float)2 * size / 3 - (hookSize - ((float)2 * size / 3 - hookStartY - baseLeftHookOffset) + offset)
      );
    }
    canvas.drawPath(path, paint);
  }

  private void initDrawHookPaint() {

    paint.setAlpha(0xFF);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(strokeWidth);
    paint.setColor(strokeColor);
  }

  private void initDrawStrokeCirclePaint() {

    paint.setAlpha(0xFF);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(strokeWidth);
    paint.setColor(strokeColor);
  }

  private void initDrawAlphaStrokeCirclePaint() {

    paint.setStrokeWidth(strokeWidth);
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(strokeColor);
    paint.setAlpha(0x40);
  }

//  private void initDrawInnerCirclePaint() {
//
//    mPaint.setStyle(Paint.Style.FILL);
//    mPaint.setColor(mCircleColor);
//    mPaint.setAlpha(mInnerCircleAlpha);
//  }

  private void startCheckedAnim() {

    ValueAnimator animator = new ValueAnimator();
    final float hookMaxValue = hookSize + endLeftHookOffset - baseLeftHookOffset;
    final float circleMaxFraction = hookSize / hookMaxValue;
    final float circleMaxValue = 360 / circleMaxFraction;
    animator.setFloatValues(0, 1);
    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {

        float fraction = animation.getAnimatedFraction();
        hookOffset = fraction * hookMaxValue;
        if(fraction <= circleMaxFraction) {
          sweepAngle = (int)((circleMaxFraction - fraction) * circleMaxValue);
        }
        else {
          sweepAngle = 0;
        }
        innerCircleAlpha = (int)(fraction * 0xFF);
        invalidate();
      }
    });
    animator.setInterpolator(new AccelerateDecelerateInterpolator());
    animator.setDuration(duration).start();
  }

  private void startUnCheckedAnim() {

    ValueAnimator animator = new ValueAnimator();
    final float hookMaxValue = hookSize + endLeftHookOffset - baseLeftHookOffset;
    final float circleMinFraction = (endLeftHookOffset - baseLeftHookOffset) / hookMaxValue;
    final float circleMaxValue = 360 / (1 - circleMinFraction);
    animator.setFloatValues(0, 1);
    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {

        float circleFraction = animation.getAnimatedFraction();
        float fraction = 1 - circleFraction;
        hookOffset = fraction * hookMaxValue;
        if(circleFraction >= circleMinFraction) {
          sweepAngle = (int)((circleFraction - circleMinFraction) * circleMaxValue);
        }
        else {
          sweepAngle = 0;
        }
        innerCircleAlpha = (int)(fraction * 0xFF);
        invalidate();
      }
    });
    animator.setInterpolator(new AccelerateDecelerateInterpolator());
    animator.setDuration(duration).start();
  }

  private void startAnim() {

    clearAnimation();
    if(isChecked) {
      startCheckedAnim();
    }
    else {
      startUnCheckedAnim();
    }
  }


  private int getAlphaColor(int color, int alpha) {

    alpha = Math.max(alpha, 0);
    alpha = Math.min(alpha, 255);
    return (color & 0x00FFFFFF) | alpha << 24;
  }

  @Override
  public boolean isChecked() {

    return isChecked;
  }

  /**
   * setChecked with Animation
   *
   * @param checked true if checked, false if unchecked
   */
  @Override
  public void setChecked(boolean checked) {

    setChecked(checked, true);
  }

  @Override
  public void toggle() {

    setChecked(!isChecked());
  }

  /**
   * @param checked   true if checked, false if unchecked
   * @param animation true with animation,false without animation
   */
  public void setChecked(boolean checked, boolean animation) {

    if(checked == this.isChecked) {
      return;
    }
    setCheckedViewInner(checked, animation);
    if(onCheckedChangeListener != null) {
      onCheckedChangeListener.onChange(this, this.isChecked);
    }
  }

  /**
   * @deprecated use {@link #setOnCheckedChangeListener(OnCheckedChangeListener)} instead
   */
  @Deprecated
  @Override
  public void setOnClickListener(OnClickListener l) {
    // Empty!
  }

  private void setCheckedViewInner(boolean checked, boolean animation) {

    this.isChecked = checked;
    if(animation && isAnimation) {
      startAnim();
    }
    else {
      if(this.isChecked) {
        innerCircleAlpha = 0xFF;
        sweepAngle = 0;
        hookOffset = hookSize + endLeftHookOffset - baseLeftHookOffset;
      }
      else {
        innerCircleAlpha = 0x00;
        sweepAngle = 360;
        hookOffset = 0;
      }
      invalidate();
    }
  }

  private int dip(int dip) {

    return (int)getContext().getResources().getDisplayMetrics().density * dip;
  }

  @Override
  public Parcelable onSaveInstanceState() {

    return super.onSaveInstanceState();
  }

  @Override
  public void onRestoreInstanceState(Parcelable state) {

    super.onRestoreInstanceState(state);
  }

  /**
   * setOnCheckedChangeListener
   *
   * @param listener the OnCheckedChangeListener listener
   */
  public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {

    this.onCheckedChangeListener = listener;
  }

  public interface OnCheckedChangeListener {

    void onChange(AnimCheckBox view, boolean checked);
  }

  public void setAnimationOff() {

    isAnimation = false;
  }

  public void setAnimationOn() {

    isAnimation = true;
  }
}