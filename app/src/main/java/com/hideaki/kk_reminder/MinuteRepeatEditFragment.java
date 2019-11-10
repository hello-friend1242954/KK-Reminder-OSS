package com.hideaki.kk_reminder;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.appcompat.widget.Toolbar;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.UtilClass.LOCALE;

public class MinuteRepeatEditFragment extends BasePreferenceFragmentCompat
  implements Preference.OnPreferenceClickListener,
  MyCheckBoxPreference.MyCheckBoxPreferenceCheckedChangeListener {

  private PreferenceScreen rootPreferenceScreen;
  PreferenceScreen label;
  PreferenceScreen interval;
  static String label_str;
  private CheckBoxPreference never;
  CheckBoxPreference count;
  CheckBoxPreference duration;
  PreferenceScreen count_picker;
  PreferenceScreen durationPicker;
  private MainActivity activity;

  public static MinuteRepeatEditFragment newInstance() {

    return new MinuteRepeatEditFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;
  }

  @Override
  public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {

    addPreferencesFromResource(R.xml.minute_repeat_edit);
    setHasOptionsMenu(true);

    rootPreferenceScreen = getPreferenceScreen();
    label = (PreferenceScreen)findPreference("label");
    interval = (PreferenceScreen)findPreference("interval");
    interval.setOnPreferenceClickListener(this);
    never = (CheckBoxPreference)findPreference("never");
    ((MyCheckBoxPreference)never).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    count = (CheckBoxPreference)findPreference("count");
    ((MyCheckBoxPreference)count).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    count.setOnPreferenceClickListener(this);
    duration = (CheckBoxPreference)findPreference("duration");
    ((MyCheckBoxPreference)duration).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    duration.setOnPreferenceClickListener(this);
    count_picker = (PreferenceScreen)findPreference("count_picker");
    count_picker.setOnPreferenceClickListener(this);
    durationPicker = (PreferenceScreen)findPreference("duration_picker");
    durationPicker.setOnPreferenceClickListener(this);
  }

  @Override
  public View onCreateView(
    LayoutInflater inflater,
    @Nullable ViewGroup container,
    @Nullable Bundle savedInstanceState
  ) {

    View view = super.onCreateView(inflater, container, savedInstanceState);
    checkNotNull(view);

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
          if(MainEditFragment.minuteRepeat.getInterval() >
            MainEditFragment.minuteRepeat.getOrgDuration()
            && duration.isChecked()) {
            new AlertDialog.Builder(activity)
              .setMessage(R.string.repeat_minute_illegal_dialog)
              .show();
            return true;
          }
        }
        return false;
      }
    });

    Toolbar toolbar = activity.findViewById(R.id.toolbar_layout);
    activity.setSupportActionBar(toolbar);
    ActionBar actionBar = activity.getSupportActionBar();
    checkNotNull(actionBar);

    activity.drawerToggle.setDrawerIndicatorEnabled(false);
    actionBar.setHomeAsUpIndicator(activity.upArrow);
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setTitle(R.string.repeat_minute_unit);

    // intervalのラベルの初期化
    String interval = "";
    int hour = MainEditFragment.minuteRepeat.getHour();
    if(hour != 0) {
      interval += activity.getResources().getQuantityString(R.plurals.hour, hour, hour);
      if(!LOCALE.equals(Locale.JAPAN)) {
        interval += " ";
      }
    }
    int minute = MainEditFragment.minuteRepeat.getMinute();
    if(minute != 0) {
      interval += activity.getResources().getQuantityString(R.plurals.minute, minute, minute);
      if(!LOCALE.equals(Locale.JAPAN)) {
        interval += " ";
      }
    }
    this.interval.setTitle(interval);

    // チェック状態の初期化
    never.setChecked(false);
    count.setChecked(false);
    duration.setChecked(false);
    switch(MainEditFragment.minuteRepeat.getWhich_setted()) {

      case 0: {

        never.setChecked(true);
        rootPreferenceScreen.removePreference(count_picker);
        rootPreferenceScreen.removePreference(durationPicker);
        break;
      }
      case 1: {

        count.setChecked(true);
        rootPreferenceScreen.addPreference(count_picker);
        rootPreferenceScreen.removePreference(durationPicker);

        // 項目のタイトル部に現在の設定値を表示
        int org_count = MainEditFragment.minuteRepeat.getOrg_count();
        count_picker.setTitle(getResources().getQuantityString(
          R.plurals.times,
          org_count,
          org_count
        ));
        break;
      }
      case 1 << 1: {

        duration.setChecked(true);
        rootPreferenceScreen.removePreference(count_picker);
        rootPreferenceScreen.addPreference(durationPicker);

        // durationのラベルの初期化
        String durationLabel = "";
        int duration_hour = MainEditFragment.minuteRepeat.getOrg_duration_hour();
        if(duration_hour != 0) {
          durationLabel += activity
            .getResources()
            .getQuantityString(R.plurals.hour, duration_hour, duration_hour);
          if(!LOCALE.equals(Locale.JAPAN)) {
            durationLabel += " ";
          }
        }
        int duration_minute = MainEditFragment.minuteRepeat.getOrg_duration_minute();
        if(duration_minute != 0) {
          durationLabel += activity
            .getResources()
            .getQuantityString(R.plurals.minute, duration_minute, duration_minute);
          if(!LOCALE.equals(Locale.JAPAN)) {
            durationLabel += " ";
          }
        }
        durationPicker.setTitle(durationLabel);

        break;
      }
    }

    // ラベルの初期化
    if(MainEditFragment.minuteRepeat.getLabel() == null) {
      label.setSummary(R.string.non_repeat);
    }
    else {
      label.setSummary(MainEditFragment.minuteRepeat.getLabel());
    }

    return view;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {

    if(MainEditFragment.minuteRepeat.getInterval() > MainEditFragment.minuteRepeat.getOrgDuration()
      && duration.isChecked()) {
      new AlertDialog.Builder(activity)
        .setMessage(R.string.repeat_minute_illegal_dialog)
        .show();
    }
    else {
      FragmentManager manager = getFragmentManager();
      checkNotNull(manager);
      manager.popBackStack();
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {

    switch(preference.getKey()) {
      case "interval": {

        MinuteRepeatIntervalPickerDialogFragment dialog =
          new MinuteRepeatIntervalPickerDialogFragment(this);
        dialog.show(activity.getSupportFragmentManager(), "minute_repeat_interval_picker");

        return true;
      }
      case "count_picker": {

        MinuteRepeatCountPickerDialogFragment dialog =
          new MinuteRepeatCountPickerDialogFragment(this);
        dialog.show(activity.getSupportFragmentManager(), "minute_repeat_count_picker");
        return true;
      }
      case "duration_picker": {

        MinuteRepeatDurationPickerDialogFragment dialog =
          new MinuteRepeatDurationPickerDialogFragment(this);
        dialog.show(activity.getSupportFragmentManager(), "minute_repeat_duration_picker");
        return true;
      }
    }

    return false;
  }

  @Override
  public void onCheckedChange(String key, boolean checked) {

    switch(key) {
      case "never": {
        if(never.isChecked()) {
          label.setSummary(R.string.non_repeat);

          MainEditFragment.minuteRepeat.setLabel(getResources().getString(R.string.none));
          MainEditFragment.minuteRepeat.setWhich_setted(0);

          if(count.isChecked()) {
            count.setChecked(false);
            rootPreferenceScreen.removePreference(count_picker);
          }
          if(duration.isChecked()) {
            duration.setChecked(false);
            rootPreferenceScreen.removePreference(durationPicker);
          }
        }
        else {
          never.setChecked(true);
        }

        break;
      }
      case "count": {
        if(count.isChecked()) {

          Resources res = getResources();
          String interval = "";
          int hour = MainEditFragment.minuteRepeat.getHour();
          if(hour != 0) {
            interval += res.getQuantityString(R.plurals.hour, hour, hour);
            if(!LOCALE.equals(Locale.JAPAN)) {
              interval += " ";
            }
          }
          int minute = MainEditFragment.minuteRepeat.getMinute();
          if(minute != 0) {
            interval += res.getQuantityString(R.plurals.minute, minute, minute);
            if(!LOCALE.equals(Locale.JAPAN)) {
              interval += " ";
            }
          }
          int count = MainEditFragment.minuteRepeat.getOrg_count();
          label_str = res.getQuantityString(R.plurals.repeat_minute_count_format,
            count, interval, count
          );
          label.setSummary(label_str);

          // 項目のタイトル部に現在の設定値を表示
          count_picker.setTitle(getResources().getQuantityString(R.plurals.times, count, count));

          MainEditFragment.minuteRepeat.setLabel(label_str);
          MainEditFragment.minuteRepeat.setWhich_setted(1);

          if(never.isChecked()) {
            never.setChecked(false);
          }
          if(duration.isChecked()) {
            duration.setChecked(false);
            rootPreferenceScreen.removePreference(durationPicker);
          }
          rootPreferenceScreen.addPreference(count_picker);
        }
        else {
          count.setChecked(true);
        }

        break;
      }
      case "duration": {
        if(duration.isChecked()) {

          Resources res = getResources();
          String interval = "";
          int hour = MainEditFragment.minuteRepeat.getHour();
          if(hour != 0) {
            interval += res.getQuantityString(R.plurals.hour, hour, hour);
            if(!LOCALE.equals(Locale.JAPAN)) {
              interval += " ";
            }
          }
          int minute = MainEditFragment.minuteRepeat.getMinute();
          if(minute != 0) {
            interval += res.getQuantityString(R.plurals.minute, minute, minute);
            if(!LOCALE.equals(Locale.JAPAN)) {
              interval += " ";
            }
          }
          String duration = "";
          int duration_hour = MainEditFragment.minuteRepeat.getOrg_duration_hour();
          if(duration_hour != 0) {
            duration += res.getQuantityString(R.plurals.hour, duration_hour, duration_hour);
            if(!LOCALE.equals(Locale.JAPAN)) {
              duration += " ";
            }
          }
          int duration_minute = MainEditFragment.minuteRepeat.getOrg_duration_minute();
          if(duration_minute != 0) {
            duration += res.getQuantityString(R.plurals.minute, duration_minute, duration_minute);
            if(!LOCALE.equals(Locale.JAPAN)) {
              duration += " ";
            }
          }
          label_str = getString(R.string.repeat_minute_duration_format, interval, duration);

          label.setSummary(label_str);
          durationPicker.setTitle(duration);

          MainEditFragment.minuteRepeat.setLabel(label_str);
          MainEditFragment.minuteRepeat.setWhich_setted(1 << 1);

          if(never.isChecked()) {
            never.setChecked(false);
          }
          if(count.isChecked()) {
            count.setChecked(false);
            rootPreferenceScreen.removePreference(count_picker);
          }
          rootPreferenceScreen.addPreference(durationPicker);
        }
        else {
          duration.setChecked(true);
        }

        break;
      }
    }
  }
}