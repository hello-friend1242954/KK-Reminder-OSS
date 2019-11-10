package com.hideaki.kk_reminder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.UtilClass.ITEM;
import static com.hideaki.kk_reminder.UtilClass.getPxFromDp;
import static com.hideaki.kk_reminder.UtilClass.serialize;

public class DoneListViewFragment extends Fragment {

  static final String TAG = DoneListViewFragment.class.getSimpleName();
  private MainActivity activity;
  private static int expandable_list_position;
  private static int expandable_list_offset;
  @SuppressLint("UseSparseArrays")
  private static Map<Long, Integer> listPosition = new HashMap<>();
  @SuppressLint("UseSparseArrays")
  private static Map<Long, Integer> listOffset = new HashMap<>();
  private ListView oldListView;
  private int order;
  private long id;

  public static DoneListViewFragment newInstance() {

    return new DoneListViewFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {

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
    switch(order) {

      case 0: {

        expandable_list_position = oldListView.getFirstVisiblePosition();
        View child = oldListView.getChildAt(0);
        if(child != null) {
          expandable_list_offset = child.getTop();
        }
        break;
      }
      case 1: {

        listPosition.put(id, oldListView.getFirstVisiblePosition());
        View child = oldListView.getChildAt(0);
        if(child != null) {
          listOffset.put(id, child.getTop());
        }
        break;
      }
    }
  }

  @Nullable
  @Override
  public View onCreateView(
    @NonNull LayoutInflater inflater,
    @Nullable ViewGroup container,
    Bundle savedInstanceState
  ) {

    order = activity.order;
    if(order == 1) {
      id = activity.generalSettings.getNonScheduledLists().get(activity.whichMenuOpen - 1).getId();
    }

    View view = inflater.inflate(R.layout.listview, container, false);
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

          if(activity.doneListAdapter.actionMode != null) {
            activity.doneListAdapter.actionMode.finish();
          }
        }

        return false;
      }
    });

    List<Item> itemList = activity.getDoneItem();
    int size = itemList.size();
    if(size > 100) {

      List<Item> nonDeleteItemList = new ArrayList<>();
      for(int i = 0; i < size / 2; i++) {
        nonDeleteItemList.add(itemList.get(i));
      }

      DoneListAdapter.itemList = nonDeleteItemList;

      for(int i = size / 2; i < size; i++) {
        Intent intent = new Intent(activity, DeleteDoneListService.class);
        intent.putExtra(ITEM, serialize(itemList.get(i)));
        activity.startService(intent);
      }
    }
    else {
      DoneListAdapter.itemList = itemList;
    }
    DoneListAdapter.checked_item_num = 0;
    DoneListAdapter.order = order;
    activity.listView = view.findViewById(R.id.listView);
    oldListView = activity.listView;
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
    View emptyView;
    if(order == 0) {
      emptyView = View.inflate(activity, R.layout.expandable_list_empty_layout, null);
    }
    else {
      emptyView = View.inflate(activity, R.layout.nonscheduled_list_empty_layout, null);
    }
    emptyView.setLayoutParams(layoutParams);
    linearLayout.addView(emptyView);
    int paddingPx = getPxFromDp(activity, 75);
    linearLayout.setPadding(0, 0, 0, paddingPx);
    ((ViewGroup)activity.listView.getParent()).addView(linearLayout, 0, layoutParams);
    activity.listView.setEmptyView(linearLayout);
    activity.listView.post(new Runnable() {
      @Override
      public void run() {

        switch(order) {

          case 0: {

            activity.listView.setSelectionFromTop(expandable_list_position, expandable_list_offset);
            break;
          }
          case 1: {

            Integer list_position = listPosition.get(id);
            Integer list_offset = listOffset.get(id);
            if(list_position == null) {
              list_position = 0;
            }
            if(list_offset == null) {
              list_offset = 0;
            }
            activity.listView.setSelectionFromTop(list_position, list_offset);
            break;
          }
        }
      }
    });
    activity.listView.setAdapter(activity.doneListAdapter);
    activity.listView.setTextFilterEnabled(true);
    activity.listView.setOnScrollListener(new AbsListView.OnScrollListener() {
      @Override
      public void onScrollStateChanged(AbsListView view, int scrollState) {

        switch(scrollState) {

          case SCROLL_STATE_IDLE: {

            DoneListAdapter.is_scrolling = false;
            break;
          }
          case SCROLL_STATE_FLING:
          case SCROLL_STATE_TOUCH_SCROLL: {

            DoneListAdapter.is_scrolling = true;
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

    AdView adView = view.findViewById(R.id.adView);
    if(activity.is_premium) {
      adView.setVisibility(View.GONE);
    }
    else {
      AdRequest adRequest = new AdRequest.Builder().build();
      adView.loadAd(adRequest);
    }

    return view;
  }
}
