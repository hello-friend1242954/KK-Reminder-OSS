package com.hideaki.kk_reminder;

import android.content.Context;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import static com.hideaki.kk_reminder.UtilClass.getPxFromDp;
import static java.util.Objects.requireNonNull;

public class ManageListViewFragment extends Fragment {

  static final String TAG = ManageListViewFragment.class.getSimpleName();
  private MainActivity activity;
  private ListView oldListView;
  private static int position;
  private static int offset;

  public static ManageListViewFragment newInstance() {

    return new ManageListViewFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;
    if(activity.drawerLayout != null) {
      activity.drawerLayout.closeDrawer(GravityCompat.START);
    }
    else {
      FragmentManager manager = getFragmentManager();
      requireNonNull(manager);
      manager
        .beginTransaction()
        .remove(this)
        .commit();
    }
  }

  @Override
  public void onDestroyView() {

    super.onDestroyView();
    position = oldListView.getFirstVisiblePosition();
    View child = oldListView.getChildAt(0);
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
    View emptyView = View.inflate(activity, R.layout.nonscheduled_lists_empty_layout, null);
    emptyView.setLayoutParams(layoutParams);
    linearLayout.addView(emptyView);
    int paddingPx = getPxFromDp(activity, 75);
    linearLayout.setPadding(0, 0, 0, paddingPx);
    ((ViewGroup)activity.listView.getParent()).addView(linearLayout, 0, layoutParams);
    activity.listView.setEmptyView(linearLayout);
    activity.listView.setDragListener(activity.manageListAdapter.dragListener);
    activity.listView.setSortable(true);
    activity.listView.post(new Runnable() {
      @Override
      public void run() {

        activity.listView.setSelectionFromTop(position, offset);
      }
    });
    activity.listView.setAdapter(activity.manageListAdapter);
    activity.listView.setTextFilterEnabled(true);
    activity.listView.setOnScrollListener(new AbsListView.OnScrollListener() {
      @Override
      public void onScrollStateChanged(AbsListView view, int scrollState) {

        switch(scrollState) {

          case SCROLL_STATE_IDLE: {

            ManageListAdapter.is_scrolling = false;
            break;
          }
          case SCROLL_STATE_FLING:
          case SCROLL_STATE_TOUCH_SCROLL: {

            ManageListAdapter.is_scrolling = true;
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
