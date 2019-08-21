package com.hideaki.kk_reminder;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_TEXT_SIZE;

public class DefaultControlTimeEditFragment extends BasePreferenceFragmentCompat {

  private MainActivity activity;
  private PreferenceScreen rootPreferenceScreen;
  private Preference model;

  public static DefaultControlTimeEditFragment newInstance() {

    return new DefaultControlTimeEditFragment();
  }

  @Override
  public void onAttach(Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;
  }

  @Override
  public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {

    addPreferencesFromResource(R.xml.default_control_time);
    setHasOptionsMenu(true);

    rootPreferenceScreen = getPreferenceScreen();
    model = findPreference("model");
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    View view = super.onCreateView(inflater, container, savedInstanceState);
    checkNotNull(view);

    view.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.background_light));

    Toolbar toolbar = activity.findViewById(R.id.toolbar_layout);
    activity.setSupportActionBar(toolbar);
    ActionBar actionBar = activity.getSupportActionBar();
    checkNotNull(actionBar);

    activity.drawerToggle.setDrawerIndicatorEnabled(false);
    actionBar.setHomeAsUpIndicator(activity.upArrow);
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setTitle(R.string.default_control_time);

    return view;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    FragmentManager manager = getFragmentManager();
    checkNotNull(manager);
    manager.popBackStack();
    return super.onOptionsItemSelected(item);
  }
}
