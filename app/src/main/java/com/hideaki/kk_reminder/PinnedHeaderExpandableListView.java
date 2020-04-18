package com.hideaki.kk_reminder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ExpandableListView;
import android.widget.AbsListView.OnScrollListener;

public class PinnedHeaderExpandableListView extends ExpandableListView implements OnScrollListener {

  public interface OnHeaderUpdateListener {

    /**
     * Viewオブジェクトを返す
     * 返すViewオブジェクトにはLayoutParamsを使うこと
     */
    View getPinnedHeader();

    void updatePinnedHeader(View headerView, int firstVisibleGroupPos);
  }

  private View headerView;
  private int headerWidth;
  private int headerHeight;

  private View touchTarget;

  private OnScrollListener scrollListener;
  private OnHeaderUpdateListener headerUpdateListener;

  private boolean actionDownHappened = false;
  protected boolean isHeaderGroupClickable = true;


  public PinnedHeaderExpandableListView(Context context) {

    super(context);
    initView();
  }

  public PinnedHeaderExpandableListView(Context context, AttributeSet attrs) {

    super(context, attrs);
    initView();
  }

  public PinnedHeaderExpandableListView(Context context, AttributeSet attrs, int defStyle) {

    super(context, attrs, defStyle);
    initView();
  }

  private void initView() {

    setFadingEdgeLength(0);
    setOnScrollListener(this);
  }

  @Override
  public void setOnScrollListener(OnScrollListener l) {

    if(l != this) {
      scrollListener = l;
    }
    else {
      scrollListener = null;
    }
    super.setOnScrollListener(this);
  }

  /**
   * クリックイベントリスナーをグループに追加する
   *
   * @param onGroupClickListener   クリックイベントリスナー
   * @param isHeaderGroupClickable ヘッダーがクリック可能かどうかを示す
   *                               note : グループをクリックできないようにするには、
   *                               OnGroupClickListener#onGroupClickでtrueを返すか、
   *                               isHeaderGroupClickableをfalseに設定する
   */
  public void setOnGroupClickListener(
    OnGroupClickListener onGroupClickListener,
    boolean isHeaderGroupClickable
  ) {

    this.isHeaderGroupClickable = isHeaderGroupClickable;
    super.setOnGroupClickListener(onGroupClickListener);
  }

  public void setOnHeaderUpdateListener(OnHeaderUpdateListener listener) {

    headerUpdateListener = listener;
    if(listener == null) {
      headerView = null;
      headerWidth = headerHeight = 0;
      return;
    }
    headerView = listener.getPinnedHeader();
    int firstVisiblePos = getFirstVisiblePosition();
    int firstVisibleGroupPos = getPackedPositionGroup(getExpandableListPosition(firstVisiblePos));
    listener.updatePinnedHeader(headerView, firstVisibleGroupPos);
    requestLayout();
    postInvalidate();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    if(headerView == null) {
      return;
    }
    measureChild(headerView, widthMeasureSpec, heightMeasureSpec);
    headerWidth = headerView.getMeasuredWidth();
    headerHeight = headerView.getMeasuredHeight();
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {

    super.onLayout(changed, l, t, r, b);
    if(headerView == null) {
      return;
    }
    int delta = headerView.getTop();
    headerView.layout(0, delta, headerWidth, headerHeight + delta);
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {

    super.dispatchDraw(canvas);
    if(headerView != null) {
      drawChild(canvas, headerView, getDrawingTime());
    }
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {

    int x = (int)ev.getX();
    int y = (int)ev.getY();
    int pos = pointToPosition(x, y);
    if(headerView != null && y >= headerView.getTop() && y <= headerView.getBottom()) {
      if(ev.getAction() == MotionEvent.ACTION_DOWN) {
        touchTarget = getTouchTarget(headerView, x, y);
        actionDownHappened = true;
      }
      else if(ev.getAction() == MotionEvent.ACTION_UP) {
        View touchTarget = getTouchTarget(headerView, x, y);
        if(touchTarget == this.touchTarget && this.touchTarget.isClickable()) {
          this.touchTarget.performClick();
          invalidate(new Rect(0, 0, headerWidth, headerHeight));
        }
        else if(isHeaderGroupClickable) {
          int groupPosition = getPackedPositionGroup(getExpandableListPosition(pos));
          if(groupPosition != INVALID_POSITION && actionDownHappened) {
            if(isGroupExpanded(groupPosition)) {
              collapseGroup(groupPosition);
            }
            else {
              expandGroup(groupPosition);
            }
          }
        }
        actionDownHappened = false;
      }
      return true;
    }

    return super.dispatchTouchEvent(ev);
  }

  private View getTouchTarget(View view, int x, int y) {

    if(!(view instanceof ViewGroup)) {
      return view;
    }

    ViewGroup parent = (ViewGroup)view;
    int childrenCount = parent.getChildCount();
    final boolean customOrder = isChildrenDrawingOrderEnabled();
    View target = null;
    for(int i = childrenCount - 1; i >= 0; i--) {
      final int childIndex = customOrder ? getChildDrawingOrder(childrenCount, i) : i;
      final View child = parent.getChildAt(childIndex);
      if(isTouchPointInView(child, x, y)) {
        target = child;
        break;
      }
    }
    if(target == null) {
      target = parent;
    }

    return target;
  }

  private boolean isTouchPointInView(View view, int x, int y) {

    return
      view.isClickable() &&
        y >= view.getTop() &&
        y <= view.getBottom() &&
        x >= view.getLeft() &&
        x <= view.getRight();
  }

  public void requestRefreshHeader() {

    refreshHeader();
    invalidate(new Rect(0, 0, headerWidth, headerHeight));
  }

  protected void refreshHeader() {

    if(headerView == null) {
      return;
    }
    int firstVisiblePos = getFirstVisiblePosition();
    int pos = firstVisiblePos + 1;
    int firstVisibleGroupPos = getPackedPositionGroup(getExpandableListPosition(firstVisiblePos));
    int group = getPackedPositionGroup(getExpandableListPosition(pos));

    if(group == firstVisibleGroupPos + 1) {
      View view = getChildAt(1);
      if(view == null) {
        return;
      }
      if(view.getTop() <= headerHeight) {
        int delta = headerHeight - view.getTop();
        headerView.layout(0, -delta, headerWidth, headerHeight - delta);
      }
      else {
        // note it, when cause bug, remove it
        headerView.layout(0, 0, headerWidth, headerHeight);
      }
    }
    else {
      headerView.layout(0, 0, headerWidth, headerHeight);
    }

    if(headerUpdateListener != null) {
      headerUpdateListener.updatePinnedHeader(headerView, firstVisibleGroupPos);
    }
  }

  @Override
  public void onScrollStateChanged(AbsListView view, int scrollState) {

    if(scrollListener != null) {
      scrollListener.onScrollStateChanged(view, scrollState);
    }
  }

  @Override
  public void onScroll(
    AbsListView view, int firstVisibleItem,
    int visibleItemCount, int totalItemCount
  ) {

    if(totalItemCount > 0) {
      refreshHeader();
    }
    if(scrollListener != null) {
      scrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
    }
  }
}
