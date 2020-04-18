package com.hideaki.kk_reminder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.core.content.ContextCompat;

import static java.util.Objects.requireNonNull;

public class SortableListView extends ListView {

  private static final int SCROLL_SPEED_FAST = 25;
  private static final int SCROLL_SPEED_SLOW = 8;
  private static final Bitmap.Config DRAG_BITMAP_CONFIG = Bitmap.Config.ARGB_8888;

  private MainActivity activity;
  private boolean isDragging = false;
  private DragListener dragListener = new SimpleDragListener();
  private int bitmapBackgroundColor = Color.argb(0, 0xFF, 0xFF, 0xFF);
  private Bitmap dragBitmap = null;
  private ImageView dragImageView = null;
  private WindowManager.LayoutParams layoutParams = null;
  private MotionEvent actionDownEvent;
  private int positionFrom = -1;

  public SortableListView(Context context) {

    super(context);
    activity = (MainActivity)context;
  }

  public SortableListView(Context context, AttributeSet attrs) {

    super(context, attrs);
    activity = (MainActivity)context;
  }

  public SortableListView(Context context, AttributeSet attrs, int defStyleAttr) {

    super(context, attrs, defStyleAttr);
    activity = (MainActivity)context;
  }

  /**
   * ドラッグイベントリスナの設定
   */
  public void setDragListener(DragListener listener) {

    dragListener = listener;
  }

  /**
   * ソート中アイテムの背景色を設定
   */
  @Override
  public void setBackgroundColor(int color) {

    bitmapBackgroundColor = color;
  }

  /**
   * MotionEvent から position を取得する
   */
  private int eventToPosition(MotionEvent event) {

    return pointToPosition((int)event.getX(), (int)event.getY());
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {

    if(TagEditListAdapter.isSorting) {
      return eventToPosition(ev) != 0;
    }
    return MyListAdapter.isSorting || ManageListAdapter.isSorting ||
      NotesTodoListAdapter.isSorting
      || super.onInterceptTouchEvent(ev);
  }

  /**
   * タッチイベント処理
   */
  @SuppressLint("ClickableViewAccessibility")
  @Override
  public boolean onTouchEvent(MotionEvent event) {

    if(
      !MyListAdapter.isSorting &&
        !ManageListAdapter.isSorting &&
        !TagEditListAdapter.isSorting &&
        !NotesTodoListAdapter.isSorting
    ) {
      return super.onTouchEvent(event);
    }

    switch(event.getAction()) {
      case MotionEvent.ACTION_DOWN: {
        storeMotionEvent(event);
        return startDrag();
      }
      case MotionEvent.ACTION_MOVE: {
        if(duringDrag(event)) {
          return true;
        }
        break;
      }
      case MotionEvent.ACTION_UP: {
        if(stopDrag(event, true)) {
          return true;
        }
        break;
      }
      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_OUTSIDE: {
        if(stopDrag(event, false)) {
          return true;
        }
        break;
      }
    }

    return super.onTouchEvent(event);
  }

  /**
   * ACTION_DOWN時のMotionEventをプロパティに格納
   */
  private void storeMotionEvent(MotionEvent event) {

    actionDownEvent = MotionEvent.obtain(event); // 複製しないと値が勝手に変わる
  }

  /**
   * ドラッグ開始
   */
  private boolean startDrag() {

    // イベントからpositionを取得
    positionFrom = eventToPosition(actionDownEvent);

    // 取得したpositionが0未満＝範囲外の場合はドラッグを開始しない
    if(positionFrom < 0) {
      return false;
    }
    isDragging = true;

    // View, Canvas, WindowManagerの取得・生成
    final View view = getChildByIndex(positionFrom);
    final Canvas canvas = new Canvas();
    final WindowManager windowManager = getWindowManager();

    // ドラッグ対象要素のViewをCanvasに描画
    dragBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), DRAG_BITMAP_CONFIG);
    canvas.setBitmap(dragBitmap);
    view.draw(canvas);

    // 前回使用したImageViewが残っている場合は除去(念のため？)
    if(dragImageView != null) {
      windowManager.removeView(dragImageView);
    }

    // ImageView用のLayoutParamsが未設定の場合は設定する
    if(layoutParams == null) {
      initLayoutParams();
    }

    // ImageViewを生成しWindowManagerにaddViewする
    dragImageView = new ImageView(getContext());
    dragImageView.setBackgroundColor(bitmapBackgroundColor);
    GradientDrawable drawable;
    if(activity.isDarkMode) {
      drawable = (GradientDrawable)ContextCompat.getDrawable(activity, R.drawable.my_frame_dark);
    }
    else {
      drawable = (GradientDrawable)ContextCompat.getDrawable(activity, R.drawable.my_frame);
    }
    requireNonNull(drawable);
    drawable = (GradientDrawable)drawable.mutate();
    drawable.setStroke(3, activity.accentColor);
    drawable.setCornerRadius(8.0f);
    dragImageView.setBackground(drawable);
    dragImageView.setElevation(10.0f);
    dragImageView.setImageBitmap(dragBitmap);
    windowManager.addView(dragImageView, layoutParams);

    // ドラッグ開始
    if(dragListener != null) {
      positionFrom = dragListener.onStartDrag(positionFrom);
    }

    return duringDrag(actionDownEvent);
  }

  /**
   * ドラッグ処理
   */
  private boolean duringDrag(MotionEvent event) {

    if(!isDragging || dragImageView == null) {
      return false;
    }

    final int x = (int)event.getX();
    final int y = (int)event.getY();
    final int height = getHeight();
    final int middle = height / 2;

    // スクロール速度の決定
    final int speed;
    final int fastBound = height / 9;
    final int slowBound = height / 4;
    if(event.getEventTime() - event.getDownTime() < 500) {
      // ドラッグの開始から500ミリ秒の間はスクロールしない
      speed = 0;
    }
    else if(y < slowBound) {
      speed = y < fastBound ? -SCROLL_SPEED_FAST : -SCROLL_SPEED_SLOW;
    }
    else if(y > height - slowBound) {
      speed = y > height - fastBound ? SCROLL_SPEED_FAST : SCROLL_SPEED_SLOW;
    }
    else {
      speed = 0;
    }

    // スクロール処理
    if(speed != 0) {
      // 横方向はとりあえず考えない
      int middlePosition = pointToPosition(0, middle);
      if(middlePosition == AdapterView.INVALID_POSITION) {
        middlePosition = pointToPosition(0, middle + getDividerHeight() + 64);
      }

      final View middleView = getChildByIndex(middlePosition);
      if(middleView != null) {
        setSelectionFromTop(middlePosition, middleView.getTop() - speed);
      }
    }

    // ImageViewの表示や位置を更新
    if(dragImageView.getHeight() < 0) {
      dragImageView.setVisibility(View.INVISIBLE);
    }
    else {
      dragImageView.setVisibility(View.VISIBLE);
    }
    updateLayoutParams((int)event.getRawY()); // ここだけスクリーン座標を使う
    getWindowManager().updateViewLayout(dragImageView, layoutParams);
    if(dragListener != null) {
      positionFrom = dragListener.onDuringDrag(positionFrom, pointToPosition(x, y));
    }

    return true;
  }

  /**
   * ドラッグ終了
   */
  private boolean stopDrag(MotionEvent event, boolean isDrop) {

    if(!isDragging) {
      return false;
    }

    if(isDrop && dragListener != null) {
      dragListener.onStopDrag(positionFrom, eventToPosition(event));
    }

    isDragging = false;
    if(dragImageView != null) {
      getWindowManager().removeView(dragImageView);
      dragImageView = null;
      // リサイクルするとたまに死ぬけどタイミング分からない
      // dragBitmap.recycle();
      dragBitmap = null;

      actionDownEvent.recycle();
      actionDownEvent = null;
      return true;
    }

    return false;
  }

  /**
   * 指定インデックスのView要素を取得する
   */
  private View getChildByIndex(int index) {

    return getChildAt(index - getFirstVisiblePosition());
  }

  /**
   * WindowManager の取得
   */
  protected WindowManager getWindowManager() {

    return (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
  }

  /**
   * ImageView 用 LayoutParams の初期化
   */
  @SuppressLint("RtlHardcoded")
  protected void initLayoutParams() {

    layoutParams = new WindowManager.LayoutParams();
    layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
    layoutParams.x = getLeft();
    layoutParams.y = getTop();
    layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
    layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
    layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
      | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
      | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
      | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
    layoutParams.format = PixelFormat.TRANSLUCENT; // 透明を有効化
    layoutParams.windowAnimations = 0;
  }

  /**
   * ImageView用LayoutParamsの座標情報を更新
   */
  protected void updateLayoutParams(int rawY) {

    if(MyListAdapter.isSorting || ManageListAdapter.isSorting) {
      layoutParams.y = rawY - 112;
    }
    else if(TagEditListAdapter.isSorting) {
      layoutParams.y = rawY - 100;
    }
    else if(NotesTodoListAdapter.isSorting) {
      layoutParams.y = rawY - 85;
    }
  }

  /**
   * ドラッグイベントリスナーインターフェース
   */
  public interface DragListener {

    /**
     * ドラッグ開始時の処理
     */
    int onStartDrag(int position);

    /**
     * ドラッグ中の処理
     */
    int onDuringDrag(int positionFrom, int positionTo);

    /**
     * ドラッグ終了＝ドロップ時の処理
     */
    boolean onStopDrag(int positionFrom, int positionTo);
  }

  /**
   * ドラッグイベントリスナー実装
   */
  public static class SimpleDragListener implements DragListener {

    /**
     * ドラッグ開始時の処理
     */
    @Override
    public int onStartDrag(int position) {

      return position;
    }

    /**
     * ドラッグ中の処理
     */
    @Override
    public int onDuringDrag(int positionFrom, int positionTo) {

      return positionFrom;
    }

    /**
     * ドラッグ終了＝ドロップ時の処理
     */
    @Override
    public boolean onStopDrag(int positionFrom, int positionTo) {

      return positionFrom != positionTo && positionFrom >= 0 || positionTo >= 0;
    }
  }
}
