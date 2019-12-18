package com.hideaki.kk_reminder;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import static com.hideaki.kk_reminder.UtilClass.getPxFromDp;
import static java.util.Objects.requireNonNull;

public class ExpandableListViewFragment extends Fragment
  implements PinnedHeaderExpandableListView.OnHeaderUpdateListener {

  static final String TAG = ExpandableListViewFragment.class.getSimpleName();
  private MainActivity activity;
  private ExpandableListView oldExpandableListView;
  static int position;
  static int offset;
  static int group_height;
  static long updatedItemId = 0;
  static boolean isGetTagNull = false;
  private static int nullPointerCount = 0;
  private AdView adView;

  public static ExpandableListViewFragment newInstance() {

    return new ExpandableListViewFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;
    try {
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
        requireNonNull(manager);
        manager
          .beginTransaction()
          .remove(this)
          .commit();
      }
      nullPointerCount = 0;
    }
    catch(NullPointerException e) {
      try {
        Thread.sleep(10);
      }
      catch(InterruptedException ex) {
        ex.printStackTrace();
      }
      nullPointerCount++;
      if(nullPointerCount < 3) {
        Context newContext = getContext();
        if(newContext == null) {
          onAttach(context);
        }
        else {
          onAttach(newContext);
        }
      }
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
    if(child != null) {
      offset = child.getTop();
    }
  }

  @Nullable
  @Override
  public View onCreateView(
    @NonNull LayoutInflater inflater,
    @Nullable ViewGroup container,
    Bundle savedInstanceState
  ) {

    isGetTagNull = false;

    // すべての通知を既読し、通知チャネルを削除する
    activity.clearAllNotification();

    View view;
    view = inflater.inflate(R.layout.expandable_listview, container, false);

    if(activity.isDarkMode) {
      view.setBackgroundColor(activity.backgroundMaterialDarkColor);
    }
    else {
      view.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.background_light));
    }
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
      new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT
      );
    layoutParams.gravity = Gravity.CENTER;
    layoutParams.weight = 1;
    layoutParams.height = 0;
    View emptyView = View.inflate(activity, R.layout.expandable_list_empty_layout, null);
    emptyView.setLayoutParams(layoutParams);
    linearLayout.addView(emptyView);
    int paddingPx = getPxFromDp(activity, 75);
    linearLayout.setPadding(0, 0, 0, paddingPx);
    ((ViewGroup)activity.expandableListView.getParent())
      .addView(linearLayout, 0, layoutParams);
    activity.expandableListView.setEmptyView(linearLayout);
    setPosition();
    activity.expandableListView.setAdapter(activity.expandableListAdapter);
    activity.expandableListView.setTextFilterEnabled(true);
    activity.expandableListView.setOnHeaderUpdateListener(this);
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
      public void onScroll(
        AbsListView view,
        int firstVisibleItem,
        int visibleItemCount,
        int totalItemCount
      ) {

      }
    });

    adView = view.findViewById(R.id.adView);
    if(activity.is_premium) {
      adView.setVisibility(View.GONE);
    }
    else {
      AdRequest adRequest = new AdRequest.Builder().build();
      adView.loadAd(adRequest);
    }

    return view;
  }

  void disableAdView() {

    if(adView != null) {
      adView.setVisibility(View.GONE);
    }
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
      }
    });
  }

  @Override
  public View getPinnedHeader() {

    View headerView = View.inflate(activity, R.layout.parent_layout, null);
    headerView.setLayoutParams(new LayoutParams(
      LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

    TextView day = headerView.findViewById(R.id.day);
    if(activity.isDarkMode) {
      ConstraintLayout groupView = headerView.findViewById(R.id.group_view);
      if(groupView == null) {
        activity.expandableListAdapter.notifyDataSetChanged();
        return headerView;
      }
      groupView.setBackground(ContextCompat.getDrawable(
        activity,
        R.drawable.expandable_group_view_dark
      ));
      day.setTextColor(activity.secondaryTextMaterialDarkColor);
    }

    return headerView;
  }

  @Override
  public void updatePinnedHeader(View headerView, int firstVisibleGroupPos) {

    ImageView indicator = headerView.findViewById(R.id.indicator);
    if(activity.expandableListView.isGroupExpanded(firstVisibleGroupPos)) {
      indicator.setImageResource(R.drawable.ic_expand_more_grey_24dp);
    }
    else {
      indicator.setImageResource(R.drawable.ic_expand_less_right_grey_24dp);
    }
    String firstVisibleGroup =
      (String)activity.expandableListAdapter.getGroup(firstVisibleGroupPos);
    TextView textView = headerView.findViewById(R.id.day);
    textView.setText(firstVisibleGroup);
  }
}
