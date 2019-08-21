package com.hideaki.kk_reminder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.UtilClass.getPxFromDp;

public class ListViewFragment extends Fragment {

  static final String TAG = ListViewFragment.class.getSimpleName();
  private MainActivity activity;
  @SuppressLint("UseSparseArrays")
  static Map<Long, Integer> listPosition = new HashMap<>();
  @SuppressLint("UseSparseArrays")
  static Map<Long, Integer> listOffset = new HashMap<>();
  private ListView oldListView;
  private long id;

  public static ListViewFragment newInstance() {

    return new ListViewFragment();
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
  public void onDestroyView() {

    super.onDestroyView();
    listPosition.put(id, oldListView.getFirstVisiblePosition());
    View child = oldListView.getChildAt(0);
    if(child != null) listOffset.put(id, child.getTop());
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

    if(activity.whichMenuOpen > 0) {
      id = activity.generalSettings.getNonScheduledLists().get(activity.whichMenuOpen - 1).getId();
    }
    else id = 0;

    View view = inflater.inflate(R.layout.listview, container, false);
    view.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.background_light));
    view.setFocusableInTouchMode(true);
    view.requestFocus();
    view.setOnKeyListener(new View.OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {

          if(MyListAdapter.is_sorting) {
            new AlertDialog.Builder(activity)
                .setTitle(R.string.is_sorting_title)
                .setMessage(R.string.is_sorting_message)
                .show();

            return true;
          }

          if(activity.listAdapter.actionMode != null) {
            activity.listAdapter.actionMode.finish();
          }
        }

        return false;
      }
    });

    MyListAdapter.checked_item_num = 0;
    MyListAdapter.has_panel = 0;
    MyListAdapter.is_sorting = false;
    MyListAdapter.itemList = activity.getNonScheduledItem(MyDatabaseHelper.TODO_TABLE);
    activity.listView = view.findViewById(R.id.listView);
    oldListView = activity.listView;
    LinearLayout linearLayout = new LinearLayout(activity);
    linearLayout.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams layoutParams =
        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
    layoutParams.gravity = Gravity.CENTER;
    layoutParams.weight = 1;
    layoutParams.height = 0;
    View emptyView = View.inflate(activity, R.layout.nonscheduled_list_empty_layout, null);
    emptyView.setLayoutParams(layoutParams);
    linearLayout.addView(emptyView);
    int paddingPx = getPxFromDp(activity, 75);
    linearLayout.setPadding(0, 0, 0, paddingPx);
    ((ViewGroup)activity.listView.getParent()).addView(linearLayout, 0, layoutParams);
    activity.listView.setEmptyView(linearLayout);
    activity.listView.setDragListener(activity.listAdapter.dragListener);
    activity.listView.setSortable(true);
    activity.listView.post(new Runnable() {
      @Override
      public void run() {

        Integer list_position = listPosition.get(id);
        Integer list_offset = listOffset.get(id);
        if(list_position == null) list_position = 0;
        if(list_offset == null) list_offset = 0;
        activity.listView.setSelectionFromTop(list_position, list_offset);
      }
    });
    activity.listView.setAdapter(activity.listAdapter);
    activity.listView.setTextFilterEnabled(true);
    activity.listView.setOnScrollListener(new AbsListView.OnScrollListener() {
      @Override
      public void onScrollStateChanged(AbsListView view, int scrollState) {

        switch(scrollState) {

          case SCROLL_STATE_IDLE: {

            MyListAdapter.is_scrolling = false;
            break;
          }
          case SCROLL_STATE_FLING:
          case SCROLL_STATE_TOUCH_SCROLL: {

            MyListAdapter.is_scrolling = true;
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
}
