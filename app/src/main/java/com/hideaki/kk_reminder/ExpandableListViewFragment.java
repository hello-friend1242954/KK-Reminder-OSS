package com.hideaki.kk_reminder;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;

import com.diegocarloslima.fgelv.lib.FloatingGroupExpandableListView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.UtilClass.getPxFromDp;

public class ExpandableListViewFragment extends Fragment {

  static final String TAG = ExpandableListViewFragment.class.getSimpleName();
  private MainActivity activity;
  private FloatingGroupExpandableListView oldExpandableListView;
  static int position;
  static int offset;
  static int group_height;
  static long updatedItemId = 0;
  static boolean isGetTagNull = false;

  public static ExpandableListViewFragment newInstance() {

    return new ExpandableListViewFragment();
  }

  @Override
  public void onAttach(Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;
    if(activity.drawerLayout != null) {
      activity.drawerLayout.closeDrawer(GravityCompat.START);
      if(activity.detail != null) {
        activity.showMainEditFragment(activity.detail);
        activity.detail = null;
      }

      activity.setUpdateListTimerTask(true);
    }
    else {
      FragmentManager manager = getFragmentManager();
      checkNotNull(manager);
      manager
          .beginTransaction()
          .remove(this)
          .commit();
    }
  }

  @Override
  public void onDetach() {

    super.onDetach();
    activity.setUpdateListTimerTask(false);
  }

  @Override
  public void onDestroyView() {

    super.onDestroyView();
    position = oldExpandableListView.getFirstVisiblePosition();
    View child = oldExpandableListView.getChildAt(0);
    if(child != null) offset = child.getTop();
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

    isGetTagNull = false;

    //すべての通知を既読する
    NotificationManager manager = (NotificationManager)activity.getSystemService(NOTIFICATION_SERVICE);
    checkNotNull(manager);
    manager.cancelAll();

    View view = inflater.inflate(R.layout.expandable_listview, container, false);
    view.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.background_light));
    view.setFocusableInTouchMode(true);
    view.requestFocus();
    view.setOnKeyListener(new View.OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
          if(activity.expandableListAdapter.actionMode != null) {
            activity.expandableListAdapter.actionMode.finish();
          }
        }
        return false;
      }
    });

    MyExpandableListAdapter.has_panel = 0;
    MyExpandableListAdapter.checked_item_num = 0;
    MyExpandableListAdapter.children = activity.getChildren(MyDatabaseHelper.TODO_TABLE);
    activity.updateExpandableParentGroups(false);
    activity.expandableListView = view.findViewById(R.id.expandable_list);
    oldExpandableListView = activity.expandableListView;
    LinearLayout linearLayout = new LinearLayout(activity);
    linearLayout.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams layoutParams =
        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
    layoutParams.gravity = Gravity.CENTER;
    layoutParams.weight = 1;
    layoutParams.height = 0;
    View emptyView = View.inflate(activity, R.layout.expandable_list_empty_layout, null);
    emptyView.setLayoutParams(layoutParams);
    linearLayout.addView(emptyView);
    int paddingPx = getPxFromDp(activity, 75);
    linearLayout.setPadding(0, 0, 0, paddingPx);
    ((ViewGroup)activity.expandableListView.getParent()).addView(linearLayout, 0, layoutParams);
    activity.expandableListView.setEmptyView(linearLayout);
    setPosition();
    activity.expandableListView.setAdapter(activity.wrapperAdapter);
    activity.expandableListView.setTextFilterEnabled(true);
    activity.expandableListView.setOnScrollListener(new AbsListView.OnScrollListener() {
      @Override
      public void onScrollStateChanged(AbsListView view, int scrollState) {

        switch(scrollState) {

          case SCROLL_STATE_IDLE: {

            MyExpandableListAdapter.is_scrolling = false;
            break;
          }
          case SCROLL_STATE_FLING:
          case SCROLL_STATE_TOUCH_SCROLL: {

            MyExpandableListAdapter.is_scrolling = true;
            break;
          }
        }
      }

      @Override
      public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
    });

    AdView adView = view.findViewById(R.id.adView);
    if(activity.is_premium) adView.setVisibility(View.GONE);
    else {
      AdRequest adRequest = new AdRequest.Builder().build();
      adView.loadAd(adRequest);
    }

    return view;
  }

  void setPosition() {

    activity.expandableListView.post(new Runnable() {
      @Override
      public void run() {

        if(updatedItemId != 0) {
          activity.setUpdatedItemPosition(updatedItemId);
          updatedItemId = 0;
        }
        if(activity.is_boot_from_notification) {
          position = 0;
          offset = 0;
          activity.is_boot_from_notification = false;
        }
        activity.expandableListView.setSelectionFromTop(position, offset);
        if(position == 0 && offset == 0 && group_height == 0) {
          View child = activity.expandableListView.getChildAt(0);
          if(child != null) group_height = child.getHeight();
        }
      }
    });
  }
}
