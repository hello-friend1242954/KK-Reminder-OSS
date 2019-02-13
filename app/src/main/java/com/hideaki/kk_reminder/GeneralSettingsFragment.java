package com.hideaki.kk_reminder;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.takisoft.fix.support.v7.preference.PreferenceCategory;
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

import static com.google.common.base.Preconditions.checkNotNull;

public class GeneralSettingsFragment extends BasePreferenceFragmentCompat implements Preference.OnPreferenceClickListener {

  static final String TAG = GeneralSettingsFragment.class.getSimpleName();

  private MainActivity activity;
  public BackupAndRestoreFragment backupAndRestoreFragment;

  public static GeneralSettingsFragment newInstance() {

    return new GeneralSettingsFragment();
  }

  @Override
  public void onAttach(Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;
    if(activity.drawerLayout != null) {
      activity.drawerLayout.closeDrawer(GravityCompat.START);
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
  public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {

    addPreferencesFromResource(R.xml.general_settings_edit);

    PreferenceScreen defaultNewTask = (PreferenceScreen)findPreference("new_task");
    PreferenceScreen manuallySnooze = (PreferenceScreen)findPreference("manually_snooze");
    PreferenceCategory adsCategory = (PreferenceCategory)findPreference("ads_category");
    PreferenceScreen disableAds = (PreferenceScreen)findPreference("disable_ads");
    PreferenceScreen primaryColor = (PreferenceScreen)findPreference("primary_color");
    PreferenceScreen secondaryColor = (PreferenceScreen)findPreference("secondary_color");
    PreferenceScreen backup = (PreferenceScreen)findPreference("backup");
    PreferenceScreen about = (PreferenceScreen)findPreference("this_app");

    defaultNewTask.setOnPreferenceClickListener(this);
    manuallySnooze.setOnPreferenceClickListener(this);
    disableAds.setOnPreferenceClickListener(this);
    primaryColor.setOnPreferenceClickListener(this);
    secondaryColor.setOnPreferenceClickListener(this);
    backup.setOnPreferenceClickListener(this);
    about.setOnPreferenceClickListener(this);

    if(activity.is_premium) getPreferenceScreen().removePreference(adsCategory);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

    super.onViewCreated(view, savedInstanceState);

    //設定項目間の区切り線の非表示
    setDivider(new ColorDrawable(Color.TRANSPARENT));
    setDividerHeight(0);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

    View view = super.onCreateView(inflater, container, savedInstanceState);
    checkNotNull(view);

    view.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.background_light));

    Toolbar toolbar = activity.findViewById(R.id.toolbar_layout);
    activity.setSupportActionBar(toolbar);
    ActionBar actionBar = activity.getSupportActionBar();
    checkNotNull(actionBar);

    activity.drawerToggle.setDrawerIndicatorEnabled(true);
    actionBar.setTitle(R.string.settings);

    return view;
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {

    switch(preference.getKey()) {

      case "new_task": {

        activity.showMainEditFragment(activity.generalSettings.getItem());
        return true;
      }
      case "manually_snooze": {

        transitionFragment(DefaultManuallySnoozeFragment.newInstance());
        return true;
      }
      case "disable_ads": {

        activity.promotionDialog.show();
        return true;
      }
      case "primary_color": {

        ColorPickerListAdapter.is_general_settings = true;
        activity.showColorPickerListViewFragment();
        return true;
      }
      case "secondary_color": {

        ColorPickerListAdapter.is_general_settings = true;
        activity.generalSettings.getTheme().setColor_primary(false);
        activity.showColorPickerListViewFragment();
        return true;
      }
      case "backup": {

        backupAndRestoreFragment = BackupAndRestoreFragment.newInstance();
        transitionFragment(backupAndRestoreFragment);
        return true;
      }
      case "this_app": {

        activity.showAboutThisAppFragment();
        return true;
      }

    }
    return false;
  }

  private void transitionFragment(PreferenceFragmentCompat next) {

    FragmentManager manager = getFragmentManager();
    checkNotNull(manager);
    manager
        .beginTransaction()
        .remove(this)
        .add(R.id.content, next)
        .addToBackStack(null)
        .commit();
  }
}