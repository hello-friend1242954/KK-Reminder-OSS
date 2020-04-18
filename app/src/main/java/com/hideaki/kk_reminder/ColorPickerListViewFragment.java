package com.hideaki.kk_reminder;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.google.android.gms.ads.AdView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import static java.util.Objects.requireNonNull;

public class ColorPickerListViewFragment extends Fragment {

  static final String TAG = ColorPickerListViewFragment.class.getSimpleName();
  private MainActivity activity;
  private static int order;
  static int tagPosition;

  public static ColorPickerListViewFragment newInstance() {

    return new ColorPickerListViewFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;
    if(activity.generalSettings != null) {
      order = activity.order;
      ColorPickerListAdapter.order = order;
      if(!ColorPickerListAdapter.isGeneralSettings) {
        activity.colorPickerListAdapter.adapterTag = TagEditListAdapter.tagList.get(tagPosition);
        activity.colorPickerListAdapter.orgTag =
          activity.generalSettings.getTagList().get(tagPosition);
        if(order == 0 || order == 1 || order == 4 || ColorPickerListAdapter.isFromListTagEdit) {
          ColorPickerListAdapter.checkedPosition =
            activity.colorPickerListAdapter.adapterTag.getColorOrderGroup();
        }
        else if(order == 3) {
          ColorPickerListAdapter.checkedPosition = MainEditFragment.list.getColorGroup();
        }
      }
      else {
        ColorPickerListAdapter.checkedPosition =
          activity.generalSettings.getTheme().getColorGroup();
      }

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
  public void onCreate(@Nullable Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    this.setHasOptionsMenu(true);
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
          if(order == 3) {
            MainEditFragment.list.setIsColorPrimary(true);
          }
          if(ColorPickerListAdapter.isGeneralSettings) {
            ColorPickerListAdapter.isGeneralSettings = false;
            if(!activity.generalSettings.getTheme().isColorPrimary()) {
              activity.generalSettings.getTheme().setIsColorPrimary(true);
              activity.updateSettingsDB();
            }
            activity.recreate();
          }
        }

        return false;
      }
    });

    ColorPickerListAdapter.isFirst = true;
    activity.listView = view.findViewById(R.id.listView);
    activity.listView.setAdapter(activity.colorPickerListAdapter);
    activity.listView.setOnScrollListener(new AbsListView.OnScrollListener() {
      @Override
      public void onScrollStateChanged(AbsListView view, int scrollState) {

        switch(scrollState) {

          case SCROLL_STATE_IDLE: {

            ColorPickerListAdapter.isScrolling = false;
            break;
          }
          case SCROLL_STATE_FLING:
          case SCROLL_STATE_TOUCH_SCROLL: {

            ColorPickerListAdapter.isScrolling = true;
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

    Toolbar toolbar = activity.findViewById(R.id.toolbar_layout);
    activity.setSupportActionBar(toolbar);
    ActionBar actionBar = activity.getSupportActionBar();
    requireNonNull(actionBar);

    activity.drawerToggle.setDrawerIndicatorEnabled(false);
    actionBar.setHomeAsUpIndicator(activity.upArrow);
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setTitle(R.string.pick_color);

    AdView adView = view.findViewById(R.id.adView);
    adView.setVisibility(View.GONE);

    return view;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    if(item.getItemId() == android.R.id.home) {
      if(order == 3) {
        MainEditFragment.list.setIsColorPrimary(true);
      }
      if(ColorPickerListAdapter.isGeneralSettings) {
        ColorPickerListAdapter.isGeneralSettings = false;
        if(!activity.generalSettings.getTheme().isColorPrimary()) {
          activity.generalSettings.getTheme().setIsColorPrimary(true);
          activity.updateSettingsDB();
        }
        activity.recreate();
      }
      FragmentManager manager = getFragmentManager();
      requireNonNull(manager);
      manager.popBackStack();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
