package com.hideaki.kk_reminder;

import android.content.Context;

import androidx.annotation.Nullable;

import android.util.AttributeSet;
import android.widget.Checkable;

public class CheckableTextView extends androidx.appcompat.widget.AppCompatTextView
    implements Checkable {

  private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};

  private boolean mChecked;

  public CheckableTextView(Context context, @Nullable AttributeSet attrs) {

    super(context, attrs);
  }

  @Override
  public void setChecked(boolean checked) {

    if(mChecked != checked) {
      mChecked = !mChecked;
    }
    refreshDrawableState();
  }

  @Override
  public boolean isChecked() {

    return mChecked;
  }

  @Override
  public void toggle() {

    setChecked(!mChecked);
  }

  @Override
  protected int[] onCreateDrawableState(int extraSpace) {

    final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
    if(isChecked()) {
      mergeDrawableStates(drawableState, CHECKED_STATE_SET);
    }

    return drawableState;
  }
}
