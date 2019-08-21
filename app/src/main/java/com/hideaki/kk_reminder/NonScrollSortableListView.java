package com.hideaki.kk_reminder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;

public class NonScrollSortableListView extends SortableListView {

  public NonScrollSortableListView(Context context) {

    super(context);
  }

  public NonScrollSortableListView(Context context, AttributeSet attrs) {

    super(context, attrs);
  }

  public NonScrollSortableListView(Context context, AttributeSet attrs, int defStyle) {

    super(context, attrs, defStyle);
  }

  @Override
  public boolean performClick() {

    super.performClick();
    return true;
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public boolean onTouchEvent(MotionEvent event) {

    if(NotesTodoListAdapter.isSorting) {
      getParent().requestDisallowInterceptTouchEvent(true);
    }
    return super.onTouchEvent(event);
  }

  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    if(NotesTodoListAdapter.isSorting) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
    else {
      int heightMeasureSpec_custom = MeasureSpec.makeMeasureSpec(
          Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
      super.onMeasure(widthMeasureSpec, heightMeasureSpec_custom);
      ViewGroup.LayoutParams params = getLayoutParams();
      params.height = getMeasuredHeight();
    }
  }
}
