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

import static com.google.common.base.Preconditions.checkNotNull;

public class ColorPickerListViewFragment extends Fragment {

  static final String TAG = ColorPickerListViewFragment.class.getSimpleName();
  private MainActivity activity;
  static int order;
  static int tag_position;

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
      if(!ColorPickerListAdapter.is_general_settings) {
        activity.colorPickerListAdapter.adapterTag = TagEditListAdapter.tagList.get(tag_position);
        activity.colorPickerListAdapter.orgTag =
            activity.generalSettings.getTagList().get(tag_position);
        if(order == 0 || order == 1 || order == 4 || ColorPickerListAdapter.from_list_tag_edit) {
          ColorPickerListAdapter.checked_position =
              activity.colorPickerListAdapter.adapterTag.getColor_order_group();
        }
        else if(order == 3) {
          ColorPickerListAdapter.checked_position = MainEditFragment.list.getColorGroup();
        }
      }
      else {
        ColorPickerListAdapter.checked_position =
            activity.generalSettings.getTheme().getColorGroup();
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
            MainEditFragment.list.setColor_primary(true);
          }
          if(ColorPickerListAdapter.is_general_settings) {
            ColorPickerListAdapter.is_general_settings = false;
            if(!activity.generalSettings.getTheme().isColor_primary()) {
              activity.generalSettings.getTheme().setColor_primary(true);
              activity.updateSettingsDB();
            }
            activity.recreate();
          }
        }

        return false;
      }
    });

    ColorPickerListAdapter.is_first = true;
    activity.listView = view.findViewById(R.id.listView);
    activity.listView.setAdapter(activity.colorPickerListAdapter);
    activity.listView.setOnScrollListener(new AbsListView.OnScrollListener() {
      @Override
      public void onScrollStateChanged(AbsListView view, int scrollState) {

        switch(scrollState) {

          case SCROLL_STATE_IDLE: {

            ColorPickerListAdapter.is_scrolling = false;
            break;
          }
          case SCROLL_STATE_FLING:
          case SCROLL_STATE_TOUCH_SCROLL: {

            ColorPickerListAdapter.is_scrolling = true;
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
    checkNotNull(actionBar);

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

    switch(item.getItemId()) {

      case android.R.id.home: {
        if(order == 3) {
          MainEditFragment.list.setColor_primary(true);
        }
        if(ColorPickerListAdapter.is_general_settings) {
          ColorPickerListAdapter.is_general_settings = false;
          if(!activity.generalSettings.getTheme().isColor_primary()) {
            activity.generalSettings.getTheme().setColor_primary(true);
            activity.updateSettingsDB();
          }
          activity.recreate();
        }
        FragmentManager manager = getFragmentManager();
        checkNotNull(manager);
        manager.popBackStack();
        return true;
      }
      default: {
        return super.onOptionsItemSelected(item);
      }
    }
  }
}
