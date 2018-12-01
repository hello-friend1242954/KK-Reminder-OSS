package com.hideaki.kk_reminder;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.List;

import static com.hideaki.kk_reminder.UtilClass.ITEM;
import static com.hideaki.kk_reminder.UtilClass.getPxFromDp;
import static com.hideaki.kk_reminder.UtilClass.serialize;

public class DoneListViewFragment extends Fragment {

  static final String TAG = DoneListViewFragment.class.getSimpleName();
  private MainActivity activity;

  public static DoneListViewFragment newInstance() {

    return new DoneListViewFragment();
  }

  @Override
  public void onAttach(Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;
    activity.drawerLayout.closeDrawer(GravityCompat.START);
    if(activity.detail != null) {
      activity.showMainEditFragment(activity.detail, TAG);
      activity.detail = null;
    }

    activity.doneListAdapter.colorStateList = new ColorStateList(
        new int[][] {
            new int[]{-android.R.attr.state_checked}, // unchecked
            new int[]{android.R.attr.state_checked} // checked
        },
        new int[] {
            ContextCompat.getColor(activity, R.color.icon_gray),
            activity.accent_color
        }
    );
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

    View view = inflater.inflate(R.layout.listview, container, false);
    view.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.background_light));
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
    DoneListAdapter.order = activity.order;
    activity.listView = view.findViewById(R.id.listView);
    View emptyView;
    if(activity.order == 0) {
      emptyView = View.inflate(activity, R.layout.expandable_list_empty_layout, null);
    }
    else {
      emptyView = View.inflate(activity, R.layout.nonscheduled_list_empty_layout, null);
    }
    ((ViewGroup)activity.listView.getParent()).addView(emptyView);
    int paddingPx = getPxFromDp(activity, 150);
    ((ViewGroup)activity.listView.getParent()).setPadding(0, paddingPx, 0, 0);
    activity.listView.setEmptyView(emptyView);
    activity.listView.setAdapter(activity.doneListAdapter);
    activity.listView.setTextFilterEnabled(true);

    AdView adView = view.findViewById(R.id.adView);
    if(activity.generalSettings.isPremium()) {
      adView.setVisibility(View.GONE);
    }
    else {
      AdRequest adRequest = new AdRequest.Builder().build();
      adView.loadAd(adRequest);
    }

    return view;
  }
}