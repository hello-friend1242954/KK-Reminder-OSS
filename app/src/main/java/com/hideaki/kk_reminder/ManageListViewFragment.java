package com.hideaki.kk_reminder;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import static com.hideaki.kk_reminder.UtilClass.getPxFromDp;

public class ManageListViewFragment extends Fragment {

  static final String TAG = ManageListViewFragment.class.getSimpleName();
  private MainActivity activity;

  public static ManageListViewFragment newInstance() {

    return new ManageListViewFragment();
  }

  @Override
  public void onAttach(Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;
    activity.drawerLayout.closeDrawer(GravityCompat.START);
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

          if(ManageListAdapter.is_sorting) {
            new AlertDialog.Builder(activity)
                .setTitle(R.string.is_sorting_title)
                .setMessage(R.string.is_sorting_message)
                .show();

            return true;
          }
        }

        return false;
      }
    });

    activity.listView = view.findViewById(R.id.listView);
    View emptyView = View.inflate(activity, R.layout.nonscheduled_lists_empty_layout, null);
    ((ViewGroup)activity.listView.getParent()).addView(emptyView);
    int paddingPx = getPxFromDp(activity, 150);
    ((ViewGroup)activity.listView.getParent()).setPadding(0, paddingPx, 0, 0);
    activity.listView.setEmptyView(emptyView);
    activity.listView.setDragListener(activity.manageListAdapter.dragListener);
    activity.listView.setSortable(true);
    activity.listView.setAdapter(activity.manageListAdapter);
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