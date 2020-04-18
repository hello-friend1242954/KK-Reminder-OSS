package com.hideaki.kk_reminder;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import static com.hideaki.kk_reminder.UtilClass.LOCALE;
import static java.util.Objects.requireNonNull;

public class NotifyIntervalEditFragment extends BasePreferenceFragmentCompat
  implements Preference.OnPreferenceClickListener,
  MyCheckBoxPreference.MyCheckBoxPreferenceCheckedChangeListener {

  private MainActivity activity;
  private PreferenceScreen rootPreferenceScreen;
  private CheckBoxPreference none;
  private CheckBoxPreference defaultNotify;
  private CheckBoxPreference everyMinute;
  private CheckBoxPreference everyFiveMinutes;
  private CheckBoxPreference everyFifteenMinutes;
  private CheckBoxPreference everyThirtyMinutes;
  private CheckBoxPreference everyHour;
  private CheckBoxPreference everyDay;
  private CheckBoxPreference custom;
  private PreferenceScreen customDescription;
  PreferenceScreen duration;
  PreferenceScreen time;
  PreferenceScreen label;

  public static NotifyIntervalEditFragment newInstance() {

    return new NotifyIntervalEditFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;
  }

  @Override
  public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {

    addPreferencesFromResource(R.xml.notify_interval_edit);
    setHasOptionsMenu(true);

    rootPreferenceScreen = getPreferenceScreen();
    none = (CheckBoxPreference)findPreference("none");
    defaultNotify = (CheckBoxPreference)findPreference("default");
    everyMinute = (CheckBoxPreference)findPreference("every_minute");
    everyFiveMinutes = (CheckBoxPreference)findPreference("every_five_minutes");
    everyFifteenMinutes = (CheckBoxPreference)findPreference("every_fifteen_minutes");
    everyThirtyMinutes = (CheckBoxPreference)findPreference("every_thirty_minutes");
    everyHour = (CheckBoxPreference)findPreference("every_hour");
    everyDay = (CheckBoxPreference)findPreference("every_day");
    custom = (CheckBoxPreference)findPreference("custom");
    customDescription = (PreferenceScreen)findPreference("custom_description");
    duration = (PreferenceScreen)findPreference("duration");
    time = (PreferenceScreen)findPreference("time");
    label = (PreferenceScreen)findPreference("label");

    ((MyCheckBoxPreference)none).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    ((MyCheckBoxPreference)defaultNotify).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    ((MyCheckBoxPreference)everyMinute).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    ((MyCheckBoxPreference)everyFiveMinutes).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    ((MyCheckBoxPreference)everyFifteenMinutes).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    ((MyCheckBoxPreference)everyThirtyMinutes).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    ((MyCheckBoxPreference)everyHour).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    ((MyCheckBoxPreference)everyDay).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    ((MyCheckBoxPreference)custom).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    duration.setOnPreferenceClickListener(this);
    time.setOnPreferenceClickListener(this);
  }

  @Override
  public View onCreateView(
    LayoutInflater inflater,
    @Nullable ViewGroup container,
    @Nullable Bundle savedInstanceState
  ) {

    View view = super.onCreateView(inflater, container, savedInstanceState);
    requireNonNull(view);

    if(activity.isDarkMode) {
      view.setBackgroundColor(activity.backgroundMaterialDarkColor);
    }
    else {
      view.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.background_light));
    }

    Toolbar toolbar = activity.findViewById(R.id.toolbar_layout);
    activity.setSupportActionBar(toolbar);
    ActionBar actionBar = activity.getSupportActionBar();
    requireNonNull(actionBar);

    activity.drawerToggle.setDrawerIndicatorEnabled(false);
    actionBar.setHomeAsUpIndicator(activity.upArrow);
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setTitle(R.string.interval);

    // チェック状態の初期化
    none.setChecked(false);
    defaultNotify.setChecked(false);
    everyMinute.setChecked(false);
    everyFiveMinutes.setChecked(false);
    everyFifteenMinutes.setChecked(false);
    everyThirtyMinutes.setChecked(false);
    everyHour.setChecked(false);
    everyDay.setChecked(false);
    custom.setChecked(false);
    rootPreferenceScreen.removePreference(duration);
    rootPreferenceScreen.removePreference(time);
    rootPreferenceScreen.removePreference(customDescription);

    switch(MainEditFragment.notifyInterval.getWhichSet()) {

      case 0: {

        none.setChecked(true);
        break;
      }
      case 1: {

        defaultNotify.setChecked(true);
        break;
      }
      case 1 << 1: {

        everyMinute.setChecked(true);
        break;
      }
      case 1 << 2: {

        everyFiveMinutes.setChecked(true);
        break;
      }
      case 1 << 3: {

        everyFifteenMinutes.setChecked(true);
        break;
      }
      case 1 << 4: {

        everyThirtyMinutes.setChecked(true);
        break;
      }
      case 1 << 5: {

        everyHour.setChecked(true);
        break;
      }
      case 1 << 6: {

        everyDay.setChecked(true);
        break;
      }
      case 1 << 7: {

        custom.setChecked(true);
        rootPreferenceScreen.addPreference(duration);
        NotifyIntervalAdapter interval = MainEditFragment.notifyInterval;
        Resources res = activity.getResources();
        String summary = "";
        if(interval.getHour() != 0) {
          summary += res.getQuantityString(R.plurals.hour, interval.getHour(), interval.getHour());
          if(!LOCALE.equals(Locale.JAPAN)) {
            summary += " ";
          }
        }
        if(interval.getMinute() != 0) {
          summary +=
            res.getQuantityString(R.plurals.minute, interval.getMinute(), interval.getMinute());
          if(!LOCALE.equals(Locale.JAPAN)) {
            summary += " ";
          }
        }
        duration.setTitle(summary);
        int orgTime = interval.getOrgTime();
        time.setTitle(res.getQuantityString(R.plurals.times, orgTime, orgTime));
        rootPreferenceScreen.addPreference(time);
        rootPreferenceScreen.addPreference(customDescription);

        break;
      }
    }

    // ラベルの初期化
    String labelStr = MainEditFragment.notifyInterval.getLabel();
    if(labelStr == null || labelStr.equals(getString(R.string.none))) {
      label.setSummary(R.string.non_notify);
    }
    else {
      label.setSummary(labelStr);
    }

    return view;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {

    FragmentManager manager = getFragmentManager();
    requireNonNull(manager);
    manager.popBackStack();
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {

    switch(preference.getKey()) {
      case "duration": {

        NotifyIntervalDurationPickerDialogFragment dialog =
          new NotifyIntervalDurationPickerDialogFragment(this);
        dialog.show(activity.getSupportFragmentManager(), "notify_interval_duration_picker");

        return true;
      }
      case "time": {

        NotifyIntervalTimePickerDialogFragment dialog =
          new NotifyIntervalTimePickerDialogFragment(this);
        dialog.show(activity.getSupportFragmentManager(), "notify_interval_time_picker");

        return true;
      }
    }

    return false;
  }

  @Override
  public void onCheckedChange(String key, boolean checked) {

    int whichSet = MainEditFragment.notifyInterval.getWhichSet();

    none.setChecked(false);
    defaultNotify.setChecked(false);
    everyMinute.setChecked(false);
    everyFiveMinutes.setChecked(false);
    everyFifteenMinutes.setChecked(false);
    everyThirtyMinutes.setChecked(false);
    everyHour.setChecked(false);
    everyDay.setChecked(false);
    custom.setChecked(false);
    if(whichSet == (1 << 7)) {
      rootPreferenceScreen.removePreference(duration);
      rootPreferenceScreen.removePreference(time);
      rootPreferenceScreen.removePreference(customDescription);
    }

    switch(key) {

      case "none": {

        none.setChecked(true);

        if(whichSet != 0) {
          label.setSummary(R.string.non_notify);

          MainEditFragment.notifyInterval.setLabel(getString(R.string.none));
          MainEditFragment.notifyInterval.setWhichSet(0);
          MainEditFragment.notifyInterval.setHour(0);
          MainEditFragment.notifyInterval.setMinute(0);
          MainEditFragment.notifyInterval.setOrgTime(0);
        }

        break;
      }
      case "default": {

        defaultNotify.setChecked(true);

        if(whichSet != 1) {
          NotifyIntervalAdapter notifyInterval
            = activity.generalSettings.getItem().getNotifyInterval();
          String labelStr = notifyInterval.getLabel();
          if(labelStr == null || labelStr.equals(getString(R.string.none))) {
            label.setSummary(R.string.non_notify);
          }
          else {
            label.setSummary(labelStr);
          }

          MainEditFragment.notifyInterval = notifyInterval.clone();
        }

        break;
      }
      case "every_minute": {

        everyMinute.setChecked(true);

        if(whichSet != 1 << 1) {
          label.setSummary(R.string.every_minute_summary);

          MainEditFragment.notifyInterval.setLabel(getString(R.string.every_minute_summary));
          MainEditFragment.notifyInterval.setWhichSet(1 << 1);
          MainEditFragment.notifyInterval.setHour(0);
          MainEditFragment.notifyInterval.setMinute(1);
          MainEditFragment.notifyInterval.setOrgTime(-1);
        }

        break;
      }
      case "every_five_minutes": {

        everyFiveMinutes.setChecked(true);

        if(whichSet != 1 << 2) {
          label.setSummary(R.string.every_five_minutes_summary);

          MainEditFragment.notifyInterval.setLabel(getString(R.string.every_five_minutes_summary));
          MainEditFragment.notifyInterval.setWhichSet(1 << 2);
          MainEditFragment.notifyInterval.setHour(0);
          MainEditFragment.notifyInterval.setMinute(5);
          MainEditFragment.notifyInterval.setOrgTime(6);
        }

        break;
      }
      case "every_fifteen_minutes": {

        everyFifteenMinutes.setChecked(true);

        if(whichSet != 1 << 3) {
          label.setSummary(R.string.every_fifteen_minutes_summary);

          MainEditFragment.notifyInterval.setLabel(getString(R.string.every_fifteen_minutes_summary));
          MainEditFragment.notifyInterval.setWhichSet(1 << 3);
          MainEditFragment.notifyInterval.setHour(0);
          MainEditFragment.notifyInterval.setMinute(15);
          MainEditFragment.notifyInterval.setOrgTime(6);
        }

        break;
      }
      case "every_thirty_minutes": {

        everyThirtyMinutes.setChecked(true);

        if(whichSet != 1 << 4) {
          label.setSummary(R.string.every_thirty_minutes_summary);

          MainEditFragment.notifyInterval.setLabel(getString(R.string.every_thirty_minutes_summary));
          MainEditFragment.notifyInterval.setWhichSet(1 << 4);
          MainEditFragment.notifyInterval.setHour(0);
          MainEditFragment.notifyInterval.setMinute(30);
          MainEditFragment.notifyInterval.setOrgTime(6);
        }

        break;
      }
      case "every_hour": {

        everyHour.setChecked(true);

        if(whichSet != 1 << 5) {
          label.setSummary(R.string.every_hour_summary);

          MainEditFragment.notifyInterval.setLabel(getString(R.string.every_hour_summary));
          MainEditFragment.notifyInterval.setWhichSet(1 << 5);
          MainEditFragment.notifyInterval.setHour(1);
          MainEditFragment.notifyInterval.setMinute(0);
          MainEditFragment.notifyInterval.setOrgTime(6);
        }

        break;
      }
      case "every_day": {

        everyDay.setChecked(true);

        if(whichSet != 1 << 6) {
          label.setSummary(R.string.every_day_summary);

          MainEditFragment.notifyInterval.setLabel(getString(R.string.every_day_summary));
          MainEditFragment.notifyInterval.setWhichSet(1 << 6);
          MainEditFragment.notifyInterval.setHour(24);
          MainEditFragment.notifyInterval.setMinute(0);
          MainEditFragment.notifyInterval.setOrgTime(-1);
        }

        break;
      }
      case "custom": {

        custom.setChecked(true);

        MainEditFragment.notifyInterval.setWhichSet(1 << 7);
        rootPreferenceScreen.addPreference(duration);
        NotifyIntervalAdapter interval = MainEditFragment.notifyInterval;
        Resources res = activity.getResources();
        String summary = "";
        boolean isEmpty = true;
        if(interval.getHour() != 0) {
          isEmpty = false;
          summary += res.getQuantityString(R.plurals.hour, interval.getHour(), interval.getHour());
          if(!LOCALE.equals(Locale.JAPAN)) {
            summary += " ";
          }
        }
        if(interval.getMinute() != 0) {
          isEmpty = false;
          summary +=
            res.getQuantityString(R.plurals.minute, interval.getMinute(), interval.getMinute());
          if(!LOCALE.equals(Locale.JAPAN)) {
            summary += " ";
          }
        }

        if(isEmpty) {
          interval.setMinute(5);
          summary +=
            res.getQuantityString(R.plurals.minute, interval.getMinute(), interval.getMinute());
          if(!LOCALE.equals(Locale.JAPAN)) {
            summary += " ";
          }
        }

        duration.setTitle(summary);
        int orgTime = interval.getOrgTime();
        time.setTitle(res.getQuantityString(R.plurals.times, orgTime, orgTime));
        rootPreferenceScreen.addPreference(time);
        rootPreferenceScreen.addPreference(customDescription);

        break;
      }
    }
  }
}
